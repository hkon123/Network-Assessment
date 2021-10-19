
import java.io.*;
import java.net.*;
import java.util.*;


public abstract class Connection {
	protected String destIp;
	protected int destPort, currentSequenceNumber, currentAckNumber;
	protected DatagramSocket listeningSocket;
	protected final int MAX_DATA = 200;//1024 - 10;
	protected Packet currentSendingPacket;
	protected List<Packet> packetList = new ArrayList<Packet>();
	protected List<Integer> packetRef = new ArrayList<Integer>();
	
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
		if(incomingPacket.getAckNr() != currentSequenceNumber + 1 ) {
			//TODO: other side is not cought up, resend packet from packetref
			System.out.println("Missing Data!!!");
		}
		else {
			currentAckNumber = incomingPacket.getSequenceNr() + 1;
		}
		return incomingPacket;
	}
	
	protected void sendPacket() {
		if (currentSendingPacket.getOption() != 1) {
			currentSequenceNumber = currentSequenceNumber + currentSendingPacket.getLength();
			currentSendingPacket.setSequenceNr(currentSequenceNumber);
			currentSendingPacket.setAckNr(currentAckNumber);
		}
		else {
			currentSequenceNumber = currentSendingPacket.getSequenceNr();
			currentAckNumber = currentSendingPacket.getAckNr();
		}
		try {
			currentSendingPacket.setRaw_data();
			listeningSocket.send(currentSendingPacket.getDatagram());
			packetList.add(currentSendingPacket);
			packetRef.add(currentSendingPacket.getSequenceNr());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	protected void sendTestPacket(int option) {
		currentSendingPacket = new Packet( 
				destIp,
				destPort,
				0, //seqNr
				10, //ackNr
				option,  // option 
				"test.txt");
		sendPacket();
	}
	
	private void printAckAndSn(Packet pk) {
		System.out.println("SN: " + pk.getSequenceNr() + "   Ack: " + pk.getAckNr());
	}
}
