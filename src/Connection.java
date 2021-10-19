
import java.io.*;
import java.net.*;
import java.util.Arrays;

public abstract class Connection {
	protected String destIp;
	protected int destPort, currentSequenceNumber, currentAckNumber;
	protected DatagramSocket listeningSocket;
	protected final int MAX_DATA = 200;//1024 - 10;
	
	protected Packet receivePacket(int timeout) {
		Packet incomingPacket = new Packet();
		try {
			listeningSocket.setSoTimeout(timeout);
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			listeningSocket.receive(incomingPacket.getDatagram());
		} catch (SocketTimeoutException e) {
			System.out.println("Timeout on recieve Packet!");
			incomingPacket = new Packet( 
					destIp,
					destPort,
					0, //seqNr
					10, //ackNr
					50,  // option = timeout
					"timeout");
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		incomingPacket.stripPacket();
		return incomingPacket;
	}
	
	protected void sendPacket(Packet outgoingPacket) {
		try {
			listeningSocket.send(outgoingPacket.getDatagram());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	protected void sendTestPacket(int option) {
		Packet testMsg = new Packet( 
				destIp,
				destPort,
				0, //seqNr
				10, //ackNr
				option,  // option 
				"test.txt");
		sendPacket(testMsg);
	}
	
}
