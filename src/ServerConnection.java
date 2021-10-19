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
			Packet establishedMsg = new Packet( 
					destIp,
					destPort,
					0, //seqNr
					10, //ackNr
					2,  // option = Established
					"Established");
			sendPacket(establishedMsg);
			
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
						Packet fileNameOkMsg = new Packet( 
								destIp,
								destPort,
								0, //seqNr
								10, //ackNr
								20,  // option = File created OK
								"file created OK");
						sendPacket(fileNameOkMsg);
					}
					else {
						System.out.println("The output file already exists");
						//TODO: send back error code that the output file already exists
						Packet fileExistsMsg = new Packet( 
								destIp,
								destPort,
								0, //seqNr
								10, //ackNr
								21,  // option = File already exists
								"file already exists");
						sendPacket(fileExistsMsg);
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
					FileWriter fWriter = new FileWriter(outputPath); //create writer object here
					fWriter.write(incomingData.getData());
					fWriter.close();
					System.out.println("Succsessfully written to file!");
					//TODO: send back written to file OK
				} catch (IOException e) {
					System.out.println("Could not write to file. send filename before content");
					//TODO: send back error code that file could not be written to
				}
				break;
			default:
				System.out.println("Unrecognized option");
			}
			
			incomingData = receivePacket(10000);
				
		}
	}
}
