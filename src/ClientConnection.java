import java.io.*;
import java.net.*;
import java.util.Arrays;

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
}
