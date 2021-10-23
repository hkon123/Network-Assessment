import java.io.*;
import java.net.DatagramSocket;

public class ServerConnection extends Connection {
	
	boolean validConnection, once = false;
	String outputPath = "../receiveData/";
	

	public ServerConnection( Packet establishingPacket, DatagramSocket listeningSocketIn) {
		if (establishingPacket.getOption() != 1) {
			validConnection = false;
			System.out.println("Unsupported connection attempt detected!");
			//TODO add response to client about unsupported connection attempt
		}
		else {
			validConnection = true;
			//sWindowSize = 5;  //Temp hardcoded value
			listeningSocket = listeningSocketIn;
			destIp = establishingPacket.getDestIp().toString().substring(1);
			destPort = establishingPacket.getDestPort();
			currentSequenceNumber = establishingPacket.getAckNr();
			currentAckNumber = establishingPacket.getSequenceNr() + 1;
			currentSendingPacket = new Packet( 
					destIp,
					destPort,
					0, //seqNr
					10, //ackNr
					2,  // option = Established
					"Established");
			sendPacket();
			Packet clientResponse = receivePacket(10000);
			if (clientResponse.getOption() == 3) {
				sWindowSize = Integer.parseInt(clientResponse.getData());
				currentSendingPacket = new Packet( 
						destIp,
						destPort,
						0, //seqNr
						10, //ackNr
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
	
	public void readyToRecieve() {
		Packet incomingData = receivePacket(10000);
		currentSWindow = sWindowSize;
		while (incomingData.getOption() != 50) { //option 50:TimeOut 
			switch(incomingData.getOption()) {
			case 10: //option: fileName
				//TODO: Add handling if filename is not received before data.
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
						//TODO: send back error code that the output file already exists
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
				      //TODO: Handle error
				}
				break;
			case 11: //option: file content
				//TODO: write to File
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
					//TODO: send back error code that file could not be written to
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
			case 12:
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
			case 13:
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
			case 51:
				System.out.println("Closing connection with client ip: " + destIp);
				return;
			case 52:
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
			
			incomingData = receivePacket(10000);
				
		}
	}
	
	private void CheckSWindow() {
		if (currentSWindow == 1) {
			sendPacket();
			currentSWindow = sWindowSize;
		}
		else {
			if(currentSWindow == 3 && dropPackets == false && once == false) {
				//TODO: make random
				dropPackets = true;
				once = true; // tamp value to be removed when random
			}
			else if (dropPackets == true) {
				dropPackets = false;
			}
			currentSWindow--;
		}
	}
}
