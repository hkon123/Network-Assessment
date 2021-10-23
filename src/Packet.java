
import java.io.*;
import java.net.*;
import java.util.Arrays;

public class Packet {
	private InetAddress destIp;
	private int destPort;
	private int sequenceNr, ackNr, option, length;
	private String data;
	private byte[] raw_data;
	private DatagramPacket datagram; 
	
	//Use this constructor to create Packet object from datagram
	public Packet() {
		raw_data = new byte[1024];
		datagram = new DatagramPacket(raw_data, raw_data.length);
	}
	
	//use this constructor to create Packet object from raw input
	public Packet(String destIpIn, int destPortIn, 
			int sequenceNrIn, int ackNrIn, int optionIn, String dataIn) {
		try {
			destIp = InetAddress.getByName(destIpIn);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		destPort = destPortIn;
		sequenceNr = sequenceNrIn;
		ackNr = ackNrIn;
		option = optionIn;
		data = dataIn;
		length = data.length();
		//setRaw_data();
		//datagram = new DatagramPacket(raw_data, raw_data.length, destIp, destPort);
	}
	
	
	public void setRaw_data() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write((byte) sequenceNr);
		int temp = sequenceNr/256;
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		raw_data = baos.toByteArray();
		datagram = new DatagramPacket(raw_data, raw_data.length, destIp, destPort);
	}
	
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
		

	public InetAddress getDestIp() {
		return destIp;
	}

	public int getDestPort() {
		return destPort;
	}

	public int getSequenceNr() {
		return sequenceNr;
	}

	public int getAckNr() {
		return ackNr;
	}

	public int getOption() {
		return option;
	}
	
	public int getLength() {
		return length;
	}

	public String getData() {
		return data;
	}

	public DatagramPacket getDatagram() {
		return datagram;
	}
	
	public void setSequenceNr(int seqNr) {
		//System.out.println("Before: " + sequenceNr);
		sequenceNr = seqNr;
		//System.out.println("After: " + getSequenceNr());
	}
	
	public void setAckNr(int ackNrIn) {
		ackNr = ackNrIn;
	}
}
