
import java.io.*;
import java.net.*;
import java.util.*;


public abstract class Connection {
	protected String destIp;
	protected int destPort, currentSequenceNumber, 
		currentAckNumber, sWindowSize, currentSWindow,
		debugLevel,
		maxDepth = 5;
	protected DatagramSocket listeningSocket;
	protected final int MAX_DATA = 900;//1024 - 10;
	protected Packet currentSendingPacket;
	protected List<Packet> packetList = new ArrayList<Packet>();
	protected List<Integer> packetRef = new ArrayList<Integer>();
	protected boolean isClient = false, dropPackets = false;	
	
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
					currentSequenceNumber-currentSendingPacket.getLength() + 1, //ackNr
					50,  // option = timeout
					"");
			incomingPacket.setRaw_data();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		incomingPacket.stripPacket();
		printAckAndSn("Recieved:  ", incomingPacket);
		if(incomingPacket.getAckNr() != currentSequenceNumber + 1  && isClient == true) {
			//TODO: other side is not cought up, resend packet from packetref
			debugPrint("Packet drop detected, resending lost packets");
			int indexOfNextPacket = packetRef.indexOf(incomingPacket.getAckNr() - 1);
			while (indexOfNextPacket < packetList.size() - 1) {
				resendPacket(packetList.get(indexOfNextPacket+1));
				indexOfNextPacket++;
			}
			if (packetList.get(indexOfNextPacket).getOption() != 13 && packetList.get(indexOfNextPacket).getOption() != 12) {
				currentSendingPacket = new Packet( 
						destIp,
						destPort,
						0, //seqNr
						10, //ackNr
						13,  // option = interrupt and continue
						"Interrupt and continue");
				sendPacket();
			}
			if (maxDepth-- == 0) {
				debugPrint("Max resend attempts reached, closing connection");
				incomingPacket = new Packet( 
						destIp,
						destPort,
						0, //seqNr
						10, //ackNr
						-1,  // option = None
						"Ignore incoming packet");
			}
			else {
				receivePacket(10000);
				maxDepth++;
			}
		}
		else if ((isClient == false &&
				incomingPacket.getSequenceNr() != currentAckNumber + incomingPacket.getLength() - 1 
				&& incomingPacket.getOption() != 50)
				|| (dropPackets == true && incomingPacket.getOption() == 11)) {
			printAckAndSn("Dropping:  ", incomingPacket);
			incomingPacket = new Packet( 
					destIp,
					destPort,
					0, //seqNr
					10, //ackNr
					52,  // option = Ignore incoming packet
					"Ignore incoming packet");
		}
		else {
			//currentAckNumber = incomingPacket.getSequenceNr() + 1;
			currentAckNumber = currentAckNumber + incomingPacket.getLength();
			//System.out.println("Seq by value: " + incomingPacket.getSequenceNr() + ", Seq by calc: " + (currentAckNumber + incomingPacket.getLength()));
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
			printAckAndSn("Sending: ", currentSendingPacket);
			listeningSocket.send(currentSendingPacket.getDatagram());
			packetList.add(currentSendingPacket);
			packetRef.add(currentSendingPacket.getSequenceNr());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	protected void resendPacket(Packet outgoingPacket) {
		try {
			printAckAndSn("reSending: ", outgoingPacket);
			listeningSocket.send(outgoingPacket.getDatagram());
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
	
	private void printAckAndSn(String action, Packet pk) {
		if (debugLevel > 2 ) {
			System.out.println(action + " SN: " + pk.getSequenceNr() + "   Ack: " + pk.getAckNr() + "   Packet option: " + pk.getOption() + "     Length of packet: " + pk.getLength());
		}	
	}
	
	protected void debugPrint(String output) {
		if (debugLevel > 1 ) {
			System.out.println(output);
		}
	}
}
