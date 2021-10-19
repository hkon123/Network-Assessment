import java.io.*;
import java.net.*;
import java.util.*;

public class ClientConnection extends Connection {

	
	public ClientConnection(String destIpIn, int destPortIn) {
		destIp = destIpIn;
		destPort = destPortIn;
		try {
			listeningSocket = new DatagramSocket();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (establishConnection()) {
			System.out.println("Connection established!");
			try {
				transferFile("../sendData/test.txt");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} 
		else {
			System.out.println("Connection NOT established!");
		}
		
	}
	
	private boolean establishConnection() {
		Packet helloMsg = new Packet( 
				destIp,
				destPort,
				0, //seqNr
				10, //ackNr
				1,  // option = hello
				"Hello");
		sendPacket(helloMsg);
		Packet serverResponse = receivePacket(10000);
		if( serverResponse.getOption() != 2 ) {
			return false;
		}
		else {
			return true;
		}
		
	}
	
	private boolean transferFile(String filePath) throws Exception {
		File inputFile = new File(filePath);
		int numberOfRemaingPackets = ((int) inputFile.length() + MAX_DATA - 1)/MAX_DATA; //Rounding up
		int dataSize;
		if (inputFile.exists()) {
			Scanner fileReader = new Scanner(inputFile);
			Scanner fileChecker = new Scanner(inputFile);
			String fileName = filePath.split("/")[filePath.split("/").length-1];
			Packet sendFileNameMsg = new Packet( 
					destIp,
					destPort,
					0, //seqNr
					10, //ackNr
					10,  // option = filename
					fileName);
			sendPacket(sendFileNameMsg);
			Packet serverResponse = receivePacket(10000);
			switch (serverResponse.getOption()) {
			case 20: //file was created on server
				sendFileContent(numberOfRemaingPackets, fileReader, fileChecker);
				fileReader.close();
				fileChecker.close();
				break;
			case 21: //File already exists on the server
				System.out.println("The file you are trying to transfer already exists on the server");
				sendCloseConnection();
				fileReader.close();
				fileChecker.close();
				//TODO: create option for deleting file on server and re-sending
				return false;
			default:
				System.out.println("Unrecognized option");
			}
		}
		else {
			sendCloseConnection();
			return false;
		}
		return true;
		
	}

	private void sendFileContent(int numberOfRemaingPackets, Scanner fileReader, Scanner fileChecker) {
		int dataSize;
		System.out.println("File name transfered OK");
		dataSize = MAX_DATA - fileChecker.nextLine().length();
		while (numberOfRemaingPackets>0) {
			String dataLine = "";
			while(fileReader.hasNextLine() 
					&& dataSize > 0) {
				dataLine = dataLine.concat(fileReader.nextLine() + "\n");
				if (fileChecker.hasNextLine()) {
					dataSize = dataSize - fileChecker.nextLine().length();
				}
				else break;
			}
			Packet sendFileDataMsg = new Packet( 
					destIp,
					destPort,
					0, //seqNr
					10, //ackNr
					11,  // option = file content
					dataLine);
			sendPacket(sendFileDataMsg);
			numberOfRemaingPackets--;
		}
	}
	
	private void sendCloseConnection() {
		Packet closeMsg = new Packet( 
				destIp,
				destPort,
				0, //seqNr
				10, //ackNr
				51,  // option = close connection
				"Close");
		sendPacket(closeMsg);
	}
	
}
