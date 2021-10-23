import java.io.*;
import java.net.*;
import java.util.Arrays;

public class Launch {

	public static void main(String[] args) throws Exception {
		System.out.println(System.getProperty("java.version"));
		if (args[0].equals("Client")) {
			new ClientConnection("localhost", 9876, 5);
		}
		else if (args[0].equals("Server")){
			DatagramSocket serverSocket;
			Packet incomingPacket = new Packet();
			
			serverSocket = new DatagramSocket(9876);
			serverSocket.receive(incomingPacket.getDatagram());
			
			incomingPacket.stripPacket();
			
			new ServerConnection(incomingPacket, serverSocket);	
		}
		else if (args[0].equals("Test")) {
			System.out.println(((byte) 253) & 0xff);
			System.out.println(((byte) 254) & 0xff);
			System.out.println(((byte) 255) & 0xff);
			System.out.println(((byte) 257) & 0xff + (257/256)*256);
			System.out.println(255/256);
		}

	}

}
