
import java.io.*;
import java.net.*;
import java.util.Arrays;

public class Packet {
	private InetAddress destIp;
	private int destPort;
	private int sequenceNr, ackNr, option;
	private String data;
	private byte[] raw_data;
	private DatagramPacket datagram; 
	
	//Use this constructor to create Packet object from datagram
	public Packet() {
		raw_data = new byte[1024];
		datagram = new DatagramPacket(raw_data, raw_data.length);
	}
	public void stripPacket() {
		destIp = datagram.getAddress();
		destPort = datagram.getPort();
		raw_data = datagram.getData();
		
		sequenceNr = raw_data[0] & 0xff;
		ackNr = raw_data[1] & 0xff;
		option = raw_data[2] & 0xff;
		data = new String(Arrays.copyOfRange(raw_data, 3, raw_data.length));
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
		setRaw_data();
		datagram = new DatagramPacket(raw_data, raw_data.length, destIp, destPort);
	}
	
	private void setRaw_data() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write((byte) sequenceNr);
		baos.write((byte) ackNr);
		baos.write((byte) option);
		try {
			baos.write(data.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		raw_data = baos.toByteArray();
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

	public String getData() {
		return data;
	}

	public DatagramPacket getDatagram() {
		return datagram;
	}
	
}
