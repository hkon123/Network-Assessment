import java.io.*;
import java.net.*;
import java.util.*;

/**Class used to setup and contain a connection from a client
 * contains functionality for transferring a file to server
 * and parsing response messages
 * @author Håkon
 *
 */
public class ClientConnection extends Connection {
	
	
	/**Constructs a client connection and attempts to initiate connection with a
	 * server.
	 * @param destIpIn Server IP
	 * @param destPortIn Server port
	 * @param sWindowIn Size of the sliding window
	 * @param filePath Path to file to be transfered
	 * @param debugLevelIn Level of debug printouts
	 */
	public ClientConnection(String destIpIn, int destPortIn, int sWindowIn,
			String filePath, int debugLevelIn) {
		destIp = destIpIn;
		destPort = destPortIn;
		sWindowSize = sWindowIn;
		debugLevel = debugLevelIn;
		isClient = true;
		try {
			listeningSocket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}
		if (establishConnection()) {
			System.out.println("Connection established!");
			try {
				transferFile(filePath);
			} catch (Exception e) {
				e.printStackTrace();
			}
			sendCloseConnection();
			
		} 
		else {
			System.out.println("Connection NOT established!");
		}
		
	}
	
	/**Method for initiating a connection with an idle server.
	 * @return <b>true</b> if the connection is established<br>
	 * <b>false</b> if the connection is rejected.
	 */
	private boolean establishConnection() {
		currentSendingPacket = new Packet( 
				destIp,
				destPort,
				0, //seqNr Defines the start point for the client sequence numbers
				10, //ackNr Defines the start point for the server sequence numbers
				1,  // option = hello
				"Hello");
		sendPacket();
		Packet serverResponse = receivePacket(timeoutSize);
		if( serverResponse.getOption() != 2 ) {
			return false;
		}
		else {
			currentSendingPacket = new Packet( 
					destIp,
					destPort,
					0, //seqNr dummy value changed during sendPacket()
					10, //ackNr dummy value changed during sendPacket()
					3,  // option = set sWindowSize
					Integer.toString(sWindowSize));
			sendPacket();
			serverResponse = receivePacket(timeoutSize);
			if( serverResponse.getOption() == 4 && Integer.parseInt(serverResponse.getData()) == sWindowSize) {
				return true;
			}
			else {
				return false;
			}
		}
		
	}
	
	/**Method for initiating file transfer.
	 * Will first send the name of the file to be transfered, and if the file can be created
	 * on the server it will proceed with sending the file content.
	 * If the file already exists in the server, the user will be prompted to decide
	 * whether or not to overwrite it.
	 * @param filePath Path to file to be transfered
	 * @return <b>true</b> if the file was successfully transferred<br>
	 * <b>false</b> if the file was not successfully transferred
	 * @throws Exception
	 */
	private boolean transferFile(String filePath) throws Exception {
		File inputFile = new File(filePath);
		int numberOfRemaingPackets = ((int) inputFile.length() + MAX_DATA - 1)/MAX_DATA; //Rounding up
		if (inputFile.exists()) {
			Scanner fileReader = new Scanner(inputFile); //reader passes input into the packet to be sent
			Scanner fileChecker = new Scanner(inputFile); //checker verifies that the next line will not make the packet data overflow
			String fileName = filePath.split("/")[filePath.split("/").length-1];
			currentSendingPacket = new Packet( 
					destIp,
					destPort,
					0, //seqNr dummy value changed during sendPacket()
					10, //ackNr dummy value changed during sendPacket()
					10,  // option = filename
					fileName);
			sendPacket();
			Packet serverResponse = receivePacket(timeoutSize);
			while (true) {
				switch (serverResponse.getOption()) {
				case 20: //file was created on server
					debugPrint("File name transfered OK");
					sendFileContent(numberOfRemaingPackets, fileReader, fileChecker);
					System.out.println("Full file transfered ok");
					fileReader.close();
					fileChecker.close();
					return true;
				case 21: //File already exists on the server
					System.out.println("The file you are trying to transfer already exists on the server");
					Scanner in = new Scanner(System.in);
					while (true) {
						System.out.println("Would you like to overwrite the file on the server?(Y/n)");
						String answer = in.nextLine();
						if (answer.equals("n")) {
							fileReader.close();
							fileChecker.close();
							return false;
						}
						else if (answer.equals("Y")) {
							currentSendingPacket = new Packet( 
									destIp,
									destPort,
									0, //seqNr dummy value changed during sendPacket()
									10, //ackNr dummy value changed during sendPacket()
									14,  // option = overwrite file
									"Overwrite file");
							sendPacket();
							break;
						}
					}
					break;
				default:
					debugPrint("Unrecognized option");
					fileReader.close();
					fileChecker.close();
					return false;
				}
				serverResponse = receivePacket(timeoutSize);
			}
		}
		else {
			sendCloseConnection();
			return false;
		}
	}

	/**Method for transferring the content of a file from the client to a server.
	 * Handles the sliding window functionality Client side.
	 * @param numberOfRemaingPackets Total number of packets required to transfer the file
	 * @param fileReader <b>Scanner</b> object used to read file content.
	 * @param fileChecker <b>Scanner</b> object used to check the size of the next line.
	 */
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
				while(fileReader.hasNextLine() //loop that creates the data field for each packet.
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
						0, //seqNr dummy value changed during sendPacket()
						10, //ackNr dummy value changed during sendPacket()
						11,  // option = file content
						dataLine);
				if (currentSWindow!=1) {
					sendPacket();
					numberOfRemaingPackets--;
				}
				currentSWindow--;
			}
			if ( numberOfRemaingPackets == 0 && currentSWindow!=0) { //send an interrupt to the server if full file is sent, while inside a sliding window
				currentSendingPacket = new Packet( 
						destIp,
						destPort,
						0, //seqNr dummy value changed during sendPacket()
						10, //ackNr dummy value changed during sendPacket()
						12,  // option = full file sent (interrupt sWindow)
						"interrupt");
			}
			while (resend == true && resendAttempts > 0) {
				sendPacket();
				Packet serverResponse = receivePacket(timeoutSize);
				switch (serverResponse.getOption()) {
				case 22: //option: File written to OP
					debugPrint("data line transfered OK");
					numberOfRemaingPackets--;
					resend = false;
					break;
				case 23: //option: file write error
					resendAttempts--;
					debugPrint("There was an error when the server attempted to write to file, re-sending");
					debugPrint(resendAttempts + " resend attempts remaining");
					break;
				case 24: //option: interrupt was accepted by the server
					debugPrint("Interrupt accepted by server");
					return;
				case 50: //option: timeout (local)
					resendAttempts--;
					debugPrint(resendAttempts + " resend attempts remaining");
					break;
				default:
					debugPrint("Unrecognized option");
					resend = false;
					sendCloseConnection();
					break;
				}
			}
		}
	}
	
	/**Call this method to send a packet to the server containing the "close connection" option.
	 * 
	 */
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
