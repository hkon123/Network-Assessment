import java.io.*;
import java.net.DatagramSocket;

public class ServerConnection extends Connection {
	
	boolean validConnection;
	String outputPath = "../receiveData/";

	public ServerConnection( Packet establishingPacket, DatagramSocket listeningSocketIn) {
		if (establishingPacket.getOption() != 1) {
			validConnection = false;
			System.out.println("Unsupported connection attempt detected!");
			//TODO add response to client about unsupported connection attempt
		}
		else {
			validConnection = true;
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
			
			readyToRecieve();
			
		}
	}
	
	public void readyToRecieve() {
		Packet incomingData = receivePacket(10000);
		while (incomingData.getOption() != 51 && incomingData.getOption() != 50) { //option 51: EOT, 50:TO 
			switch(incomingData.getOption()) {
			case 10: //option: fileName
				//TODO: Add handling if filename is not received before data.
				outputPath = outputPath.concat(incomingData.getData());
				try {
					File outputFile = new File(outputPath);
					if (outputFile.createNewFile()) {
						System.out.println("Output file created: " + outputFile.getName());
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
						System.out.println("The output file already exists");
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
				      System.out.println("An error occurred when creating output file.");
				      System.out.println("attempted path: " + outputPath);
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
					System.out.println("Succsessfully written to file!");
					currentSendingPacket = new Packet( 
							destIp,
							destPort,
							0, //seqNr
							10, //ackNr
							22,  // option = File written to OK
							"file written to OK");
					sendPacket();		
				} catch (IOException e) {
					System.out.println("Could not write to file. send filename before content");
					//TODO: send back error code that file could not be written to
					currentSendingPacket = new Packet( 
							destIp,
							destPort,
							0, //seqNr
							10, //ackNr
							23,  // option = File written to OK
							"file write ERROR");
					sendPacket();	
				}
				break;
			default:
				System.out.println("Unrecognized option");
			}
			
			incomingData = receivePacket(10000);
				
		}
	}
}
