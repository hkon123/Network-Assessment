
import java.io.*;
import java.net.*;
import java.util.*;


/**The Connection class is an abstract class containing 
 * all the functionality that both a Client and a Server use.
 * Extend this class to enable functionality.z
 * @author Håkon
 * 
 */
public abstract class Connection {
	protected String destIp;
	protected int destPort, currentSequenceNumber, 
		currentAckNumber, sWindowSize, currentSWindow,
		debugLevel,
		maxDepth = 5;
	protected DatagramSocket listeningSocket;
	protected final int MAX_DATA = 900, timeoutSize = 5000;//1024 - 10;
	protected Packet currentSendingPacket;
	protected List<Packet> packetList = new ArrayList<Packet>();
	protected List<Integer> packetRef = new ArrayList<Integer>();
	protected boolean isClient = false, dropPackets = false;	
	
	/**
	 * Open the socket and receive an incoming packet.
	 * Will wait for <b> timeout</b> time before declaring the socket timeout.
	 * Contains logic for handling packets received out of sequence for a server.
	 * Contains client logic for handling and re-sending packets if it 
	 * receives a packet with to low Ack number (ie. server has not received all packets)
	 * 
	 * @param timeout the specified socket timeout in milliseconds
	 * @return <b>Packet</b> object containing the incoming datagram. Unless the socket times out, 
	 * then it will return a special timeout <b>Packet</b>, or if packet drops are simulated it will 
	 * return a special <b>Packet</b> that will be ignored
	 */
	protected Packet receivePacket(int timeout) {
		Packet incomingPacket = new Packet();
		try {
			listeningSocket.setSoTimeout(timeout); //set socket timeout
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
		try {
			listeningSocket.receive(incomingPacket.getDatagram()); //incoming datagram is mapped to incomingPacket object
		} catch (SocketTimeoutException e) { //Timeout has occured
			System.out.println("Timeout on recieve Packet!");
			incomingPacket = new Packet( 
					destIp,
					destPort,
					0, //seqNr  dummy value that wont be processed
					/**
					 * ackNr is set to the seqNr of last sent packet.
					 * This enables logic so the lest sent packet can be resent in case of a timeout.
					 */
					currentSequenceNumber-currentSendingPacket.getLength() + 1, //ackNr 
					50,  // option = timeout
					"");
			incomingPacket.setRaw_data();
		}catch (IOException e) {
			e.printStackTrace();
		} 
		incomingPacket.stripPacket(); //Header and data is extracted from the incoming datagram
		printAckAndSn("Recieved:  ", incomingPacket);
		/**
		 * The following if condition is only applicable for a client
		 * implements go-back-n
		 * The condition is true if the reply received from the server indicates the server has lost packets
		 * Server should expect the next not received bit, 
		 * ie. ackNr should be the current seqNr+1
		 */
		if(incomingPacket.getAckNr() != currentSequenceNumber + 1  && isClient == true) { 
			//TODO: other side is not cought up, resend packet from packetref
			debugPrint("Packet drop detected, resending lost packets");
			int indexOfNextPacket = packetRef.indexOf(incomingPacket.getAckNr() - 1); //find the index of the last packet received by the server
			while (indexOfNextPacket < packetList.size() - 1) { //loop through all packets after the last received by server
				resendPacket(packetList.get(indexOfNextPacket+1));
				indexOfNextPacket++;
			}
			/**
			 * After all dropped packets are re-sent a final package that interrupts
			 * the servers sliding window is sent.
			 * This is done so the client and the server are synched up
			 * If the last packet send during the re-send loop was an interrupt, 
			 * there is no need to send another one.
			 * The If condition is there in case the interrupting packet is dropped.
			 */
			if (packetList.get(indexOfNextPacket).getOption() != 13 && packetList.get(indexOfNextPacket).getOption() != 12) {
				currentSendingPacket = new Packet( 
						destIp,
						destPort,
						0, //seqNr dummy value changed during sendPacket()
						10, //ackNr dummy value changed during sendPacket()
						13,  // option = interrupt and continue
						"Interrupt and continue");
				sendPacket();
			}
			if (maxDepth-- == 0) { //max recursion depth to escape from a completely lost connection
				debugPrint("Max resend attempts reached, closing connection");
				incomingPacket = new Packet( 
						destIp,
						destPort,
						0, //seqNr dummy value that wont be processed
						10, //ackNr dummy value that wont be processed
						-1,  // option = None
						"Ignore incoming packet");
			}
			else {
				receivePacket(timeoutSize); //recursive go-back-n in case resent packets are also dropped
				maxDepth++;
			}
		}
		/**
		 * The following condition is only applicable for servers.
		 * The following condition handles both simulated and real packet loss.
		 * The next packet that the server is expecting should have data that starts at
		 *  "incoming seqNr" - "incoming data size", this means that
		 * if there is no packet loss the incoming packet should have a seqNr that equals
		 * the current Server ackNr + the size of the data in the incoming packet - 1
		 * 
		 * simulated packet loss is only done with packets containing file data.
		 */
		else if ((isClient == false &&
				incomingPacket.getSequenceNr() != currentAckNumber + incomingPacket.getLength() - 1 
				&& incomingPacket.getOption() != 50)
				|| (dropPackets == true && incomingPacket.getOption() == 11)) {
			printAckAndSn("Dropping:  ", incomingPacket);
			incomingPacket = new Packet( 
					destIp,
					destPort,
					0, //seqNr dummy value that wont be processed
					10, //ackNr dummy value that wont be processed
					52,  // option = Ignore incoming packet
					"Ignore incoming packet");
			if (dropPackets == true) {
				currentSWindow++; //This ensures that the packet is dropped completely and not counted as part of the sliding window
			}
		}
		/**
		 * The following is for both client and server
		 * The ack number is updated to include the data received
		 */
		else {
			currentAckNumber = currentAckNumber + incomingPacket.getLength();
		}
		return incomingPacket;
	}
	
	/**Send the packet currently stored in <b>currentSendingPacket</b>
	 * contains the logic for updating the seqNr and ackNr before sending
	 * The first packet sent by a server will the determine the starting point for
	 * both seqNr and ackNr
	 */
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
	
	/**Use this method to re-send a packet that has previously been processed.
	 * @param outgoingPacket the <b>Packet</b> to be sent
	 */
	protected void resendPacket(Packet outgoingPacket) {
		try {
			printAckAndSn("reSending: ", outgoingPacket);
			listeningSocket.send(outgoingPacket.getDatagram());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**Method that prints the full header of a <b>Packet</b> to the terminal
	 * only prints if debug level is high enough(>2)
	 * @param action Context of the packet; where was this called from
	 * @param pk the <b>Packet</b> to be printed
	 */
	private void printAckAndSn(String action, Packet pk) {
		if (debugLevel > 2 ) {
			System.out.println(action + " SN: " + pk.getSequenceNr() + "   Ack: " + pk.getAckNr() + "   Packet option: " + pk.getOption() + "     Length of packet: " + pk.getLength());
		}	
	}
	
	/**Method that prints input <b>String</b> to terminal if the debug level is
	 * high enough(>1). Use to limit the number of printouts when not debugging
	 * @param output text to be printed
	 */
	protected void debugPrint(String output) {
		if (debugLevel > 1 ) {
			System.out.println(output);
		}
	}
}
