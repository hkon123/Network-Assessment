import java.io.*;
import java.net.*;
import java.util.*;

public class ClientConnection extends Connection {
	
	
	public ClientConnection(String destIpIn, int destPortIn, int sWindowIn) {
		destIp = destIpIn;
		destPort = destPortIn;
		sWindowSize = sWindowIn;
		isClient = true;
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
			sendCloseConnection();
			
		} 
		else {
			System.out.println("Connection NOT established!");
		}
		
	}
	
	private boolean establishConnection() {
		currentSendingPacket = new Packet( 
				destIp,
				destPort,
				0, //seqNr
				10, //ackNr
				1,  // option = hello
				"Hello");
		sendPacket();
		Packet serverResponse = receivePacket(10000);
		if( serverResponse.getOption() != 2 ) {
			return false;
		}
		else {
			currentSendingPacket = new Packet( 
					destIp,
					destPort,
					0, //seqNr
					10, //ackNr
					3,  // option = set sWindowSize
					Integer.toString(sWindowSize));
			sendPacket();
			serverResponse = receivePacket(10000);
			if( serverResponse.getOption() == 4 && Integer.parseInt(serverResponse.getData()) == sWindowSize) {
				return true;
			}
			else {
				return false;
			}
		}
		
	}
	
	private boolean transferFile(String filePath) throws Exception {
		File inputFile = new File(filePath);
		int numberOfRemaingPackets = ((int) inputFile.length() + MAX_DATA - 1)/MAX_DATA; //Rounding up
		if (inputFile.exists()) {
			Scanner fileReader = new Scanner(inputFile);
			Scanner fileChecker = new Scanner(inputFile);
			String fileName = filePath.split("/")[filePath.split("/").length-1];
			currentSendingPacket = new Packet( 
					destIp,
					destPort,
					0, //seqNr
					10, //ackNr
					10,  // option = filename
					fileName);
			sendPacket();
			Packet serverResponse = receivePacket(10000);
			switch (serverResponse.getOption()) {
			case 20: //file was created on server
				System.out.println("File name transfered OK");
				sendFileContent(numberOfRemaingPackets, fileReader, fileChecker);
				System.out.println("Full file transfered ok");
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
		int resendAttempts = 5;
		int temp = 0;
		boolean resend = true;
		temp = fileChecker.nextLine().length();
		while (numberOfRemaingPackets>0) {
			currentSWindow = sWindowSize;
			while(numberOfRemaingPackets>0 && currentSWindow>0) {
				dataSize = MAX_DATA - temp;
				resend = true;
				String dataLine = "";
				while(fileReader.hasNextLine() 
						&& dataSize > 0) {
					dataLine = dataLine.concat(fileReader.nextLine() + "\n");
					if (fileChecker.hasNextLine()) {
						temp = fileChecker.nextLine().length();
						dataSize = dataSize - temp;
					}
					else break;
				}
				currentSendingPacket = new Packet( 
						destIp,
						destPort,
						0, //seqNr
						10, //ackNr
						11,  // option = file content
						dataLine);
				if (currentSWindow!=1) {
					sendPacket();
					numberOfRemaingPackets--;
				}
				currentSWindow--;
			}
			if ( numberOfRemaingPackets == 0 && currentSWindow!=0) {
				currentSendingPacket = new Packet( 
						destIp,
						destPort,
						0, //seqNr
						10, //ackNr
						12,  // option = full file sent (interrupt sWindow)
						"interrupt");
			}
			while (resend == true && resendAttempts > 0) {
				sendPacket();
				Packet serverResponse = receivePacket(10000);
				switch (serverResponse.getOption()) {
				case 22:
					System.out.println("data line transfered OK");
					numberOfRemaingPackets--;
					resend = false;
					break;
				case 23:
					resendAttempts--;
					System.out.println("There was an error when the server attempted to write to file, re-sending");
					System.out.println(resendAttempts + " resend attempts remaining");
					break;
				case 24:
					System.out.println("Interrupt accepted by server");
					return;
				case 50:
					resendAttempts--;
					System.out.println(resendAttempts + " resend attempts remaining");
					break;
				default:
					System.out.println("Unrecognized option");
					resend = false;
					sendCloseConnection();
					break;
				}
			}
		}
	}
	
	private void sendCloseConnection() {
		System.out.println("Closing connection with server");
		currentSendingPacket = new Packet( 
				destIp,
				destPort,
				0, //seqNr
				10, //ackNr
				51,  // option = close connection
				"Close");
		sendPacket();
	}
	
}
