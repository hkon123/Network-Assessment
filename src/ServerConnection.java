import java.io.*;
import java.net.DatagramSocket;
import java.util.Random;

/**Use this class to hold the server side of the connection. Extends the <b>Connection</b> class.
 * The class contains functionality for parsing incoming messages and writing data received over UDP to a file.
 * @author hkonh
 *
 */
public class ServerConnection extends Connection {
	
	boolean validConnection, randomPacketDrop = false;
	String outputPath;// = "../receiveData/";
	

	/**Constructor for creating the server side of a connection.
	 * Replies to the Client that initiates the connection, 
	 * configures sliding window size and prepares to receive data.
	 * @param establishingPacket The <b>Packet</b> sent from the client to initialize connection
	 * @param listeningSocketIn The servers <b>DatagramSocket</b>
	 * @param debugLevelIn Level of debug printouts
	 * @param randomPacketDropIn whether dropped packets are simulated
	 * @param outputPathIn Location were received files are stored
	 */
	public ServerConnection( Packet establishingPacket, DatagramSocket listeningSocketIn,
			int debugLevelIn, boolean randomPacketDropIn, String outputPathIn) {
		if (establishingPacket.getOption() != 1) {
			validConnection = false;
			System.out.println("Unsupported connection attempt detected!");
		}
		else {
			validConnection = true;
			randomPacketDrop = randomPacketDropIn;
			debugLevel = debugLevelIn;
			outputPath = outputPathIn;
			listeningSocket = listeningSocketIn;
			destIp = establishingPacket.getDestIp().toString().substring(1);
			destPort = establishingPacket.getDestPort();
			currentSequenceNumber = establishingPacket.getAckNr();
			currentAckNumber = establishingPacket.getSequenceNr() + 1;
			currentSendingPacket = new Packet( 
					destIp,
					destPort,
					0, //seqNr dummy value changed during sendPacket()
					10, //ackNr dummy value changed during sendPacket()
					2,  // option = Established
					"Established");
			sendPacket();
			Packet clientResponse = receivePacket(timeoutSize);
			if (clientResponse.getOption() == 3) { //option set sliding window size
				sWindowSize = Integer.parseInt(clientResponse.getData());
				currentSendingPacket = new Packet( 
						destIp,
						destPort,
						0, //seqNr dummy value changed during sendPacket()
						10, //ackNr dummy value changed during sendPacket()
						4,  // option = sWindowSize is set
						Integer.toString(sWindowSize));
				sendPacket();
				System.out.println("Connection established with client ip: " + destIp);
				readyToRecieve();
			}
			else {
				System.out.println("Connection attempt failed with client ip: " + destIp);
			}
			
			
		}
	}
	
	/**Method that handles all the Server side communication 
	 * once the connection has been set up.
	 * Handles the parsing of the header field "option" and "data" as 
	 * the other fields are parsed in the superclass <b>Connection</b>
	 * 
	 */
	public void readyToRecieve() {
		int maxTimeouts = 5;
		Packet incomingData = receivePacket(timeoutSize);
		currentSWindow = sWindowSize;
		while (true) { 
			switch(incomingData.getOption()) {
			case 10: //option: fileName
				outputPath = outputPath.concat(incomingData.getData());
				try {
					File outputFile = new File(outputPath);
					if (outputFile.createNewFile()) {
						debugPrint("Output file created: " + outputFile.getName());
						currentSendingPacket = new Packet( 
								destIp,
								destPort,
								2, //seqNr
								3, //ackNr
								20,  // option = File created OK
								"file created OK");
						sendPacket();
					}
					else {
						debugPrint("The output file already exists");
						currentSendingPacket = new Packet( 
								destIp,
								destPort,
								0, //seqNr
								10, //ackNr
								21,  // option = File already exists
								"file already exists");
						sendPacket();
					}
				} catch (IOException e) {
					debugPrint("An error occurred when creating output file.");
					debugPrint("attempted path: " + outputPath);
				      e.printStackTrace();
				}
				break;
			case 11: //option: file content
				try {
					FileWriter fWriter = new FileWriter(outputPath, true); //create writer object here
					fWriter.write(incomingData.getData());
					fWriter.close();
					debugPrint("Succsessfully written to file!");
					currentSendingPacket = new Packet( 
							destIp,
							destPort,
							0, //seqNr
							10, //ackNr
							22,  // option = File written to OK
							"file written to OK");
					CheckSWindow();
				} catch (IOException e) {
					debugPrint("Could not write to file. send filename before content");
					currentSendingPacket = new Packet( 
							destIp,
							destPort,
							0, //seqNr
							10, //ackNr
							23,  // option = File written to OK
							"file write ERROR");
					CheckSWindow();	
				}
				break;
			case 12: //option: Full file transfered
				debugPrint("Sliding window interrupted due to file completion!");
				currentSendingPacket = new Packet( 
						destIp,
						destPort,
						0, //seqNr
						10, //ackNr
						24,  // option = Interrupt accepted
						"Interrupt accepted");
				sendPacket();
				break;
			case 13: // option: Interrupt sliding window
				debugPrint("Sliding window interrupted, continuing");
				currentSWindow = sWindowSize;
				currentSendingPacket = new Packet( 
						destIp,
						destPort,
						0, //seqNr
						10, //ackNr
						22,  // option = File written to OK
						"Sliding window interrupted, continuing");
				sendPacket();
				break;
			case 14: // option: overwrite file on server
				debugPrint("Overwriting file: " + outputPath.split("/")[outputPath.split("/").length-1]);
				try {
					new FileWriter(outputPath, false).close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				currentSendingPacket = new Packet( 
						destIp,
						destPort,
						0, //seqNr
						10, //ackNr
						20,  // option = File created OK
						"File overwritten");
				sendPacket();
				break;
			case 50: // option: socket timeout (local)
				if (maxTimeouts > 0) {
					System.out.println(""
							+ "Listening for new packet\n"
							+ (maxTimeouts -1) + " attempts remaining.");
					maxTimeouts--;
					break;
				}
				else {
					System.out.println("Max timeout attempts, closing connection");
					return;
				}
			case 51: // option: Close connection
				System.out.println("Closing connection with client ip: " + destIp);
				return;
			case 52: // option: ignore packet (local)
				debugPrint("Out of sequence packet recieved, ignoring incoming.");
				currentSendingPacket = new Packet( 
						destIp,
						destPort,
						0, //seqNr
						10, //ackNr
						22,  // option = File written to OK, use this option to trigger resend
						"Out of sequence");
				CheckSWindow();
				break;
			default:
				debugPrint("Unrecognized option");
			}
			
			/**If simulated packet drop is set to true,
			 * then there is a 10% chance of a packet being ignored.
			 */
			if (dropPackets == true) {
				dropPackets = false;
			}
			else if(dropPackets == false && randomPacketDrop == true 
					&& new Random().nextInt(100) > 90) {
				dropPackets = true;
			}
			incomingData = receivePacket(timeoutSize);
				
		}
	}
	
	/**Method for handling sliding window functionality on the server side.
	 * The server will only respond to the client once it has received enough packets from
	 * the client.
	 */
	private void CheckSWindow() {
		if (currentSWindow == 1) {
			sendPacket();
			currentSWindow = sWindowSize;
		}
		else {
			currentSWindow--;
		}
	}
}
