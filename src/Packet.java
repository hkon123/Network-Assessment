
import java.io.*;
import java.net.*;
import java.util.Arrays;

/**Class to hold and manipulate a custom udp packet.
 * <br><b>---custom Headerfields----</b><br>
 * <b>Sequence number</b>: 2 bytes<br>
 * <b>Acknowledge number</b>: 2 bytes<br>
 * <b>Packet option</b>: 1 byte<br>
 * <b>Data length</b>: 2 bytes<br>
 * <b>Data</b>: max 1017 bytes<br>
 * @author Håkon
 * 
 */
/**
 * @author Anonymous
 *
 */
public class Packet {
	private InetAddress destIp;
	private int destPort;
	private int sequenceNr, ackNr, option, length;
	private String data;
	private byte[] raw_data;
	private DatagramPacket datagram; 
	
	
	/**Use this constructor to create an empty Packet object that is ready
	 * to receive input from an incoming datagram.
	 * After the datagram has been received call the function <b>stripPacket()</b>
	 * to set header fields.
	 */
	public Packet() {
		raw_data = new byte[1024];
		datagram = new DatagramPacket(raw_data, raw_data.length);
	}
	
	/**use this constructor to create Packet object from raw input when 
	 * preparing to send a datagram. Before sending the datagram, call the function
	 * <b>setRaw_data()</b> to encapsulate the datagram.
	 * @param destIpIn Destination IP 
	 * @param destPortIn Destination port
	 * @param sequenceNrIn Sequence number (only use dummy values if they are corrected later) 
	 * @param ackNrIn Acknowledge number (only use dummy values if they are corrected later)
	 * @param optionIn Packet option
	 * @param dataIn Data to be included in the datagram
	 */
	public Packet(String destIpIn, int destPortIn, 
			int sequenceNrIn, int ackNrIn, int optionIn, String dataIn) {
		try {
			destIp = InetAddress.getByName(destIpIn);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		destPort = destPortIn;
		sequenceNr = sequenceNrIn;
		ackNr = ackNrIn;
		option = optionIn;
		data = dataIn;
		length = data.length();
	}
	
	
	/**Method that encapsulated the packet with the custom header.
	 * Call this method before sending the packet.
	 */
	public void setRaw_data() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write((byte) sequenceNr); // minor byte
		int temp = sequenceNr/256; //floor division gives major byte
		baos.write((byte) temp);
		baos.write((byte) ackNr);
		temp = ackNr/256;
		baos.write((byte) temp);
		baos.write((byte) option);
		baos.write((byte) length);
		temp = length/256;
		baos.write((byte) temp);
		try {
			baos.write(data.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		raw_data = baos.toByteArray();
		datagram = new DatagramPacket(raw_data, raw_data.length, destIp, destPort);
	}
	
	/**Method that extracts the custom header from the datagram.
	 * Call this method after receiving a datagram.
	 */
	public void stripPacket() {
		destIp = datagram.getAddress();
		destPort = datagram.getPort();
		raw_data = datagram.getData();
		
		sequenceNr = raw_data[0] & 0xff;
		sequenceNr = sequenceNr + (raw_data[1] & 0xff) * 256;
		ackNr = raw_data[2] & 0xff;
		ackNr = ackNr + (raw_data[3] & 0xff) * 256;
		option = raw_data[4] & 0xff;
		length = raw_data[5] & 0xff;
		length = length + (raw_data[6] & 0xff) * 256;
		data = new String(Arrays.copyOfRange(raw_data, 7, 7+length));
	}
		

	/**Get destination IP
	 * @return <b>InetAddress</b> destination IP
	 */
	public InetAddress getDestIp() {
		return destIp;
	}

	/**Get destination port
	 * @return <b>int</b> destination port
	 */
	public int getDestPort() {
		return destPort;
	}

	/**Get sequence number
	 * @return <b>int</b> sequence number
	 */
	public int getSequenceNr() {
		return sequenceNr;
	}

	/**Get acknowledgement number
	 * @return <b>int</b> acknowledgement number
	 */
	public int getAckNr() {
		return ackNr;
	}

	/**Get packet option
	 * @return <b>int</b> Packet option
	 */
	public int getOption() {
		return option;
	}
	
	/**Get data length
	 * @return <b>int</b> Data length
	 */
	public int getLength() {
		return length;
	}

	/**Get data
	 * @return <b>String</b> Data
	 */
	public String getData() {
		return data;
	}

	/**Get datagram
	 * @return <b>DatagramPacket</b> Datagram
	 */
	public DatagramPacket getDatagram() {
		return datagram;
	}
	
	/**Set sequence number
	 * @param seqNr sequence number
	 */
	public void setSequenceNr(int seqNr) {
		sequenceNr = seqNr;
	}
	
	/**Set acknowledgement number
	 * @param ackNrIn acknowledgement number
	 */
	public void setAckNr(int ackNrIn) {
		ackNr = ackNrIn;
	}
}
