import java.io.*;
import java.net.*;
import java.util.Arrays;

public class Launch {

	public static void main(String[] args) throws Exception {
		
		if (args[0].equals("Client")) {
			new ClientConnection("localhost", 9876);
		}
		else {
			DatagramSocket serverSocket;
			Packet incomingPacket = new Packet();
			
			serverSocket = new DatagramSocket(9876);
			serverSocket.receive(incomingPacket.getDatagram());
			
			incomingPacket.stripPacket();
			
			new ServerConnection(incomingPacket, serverSocket);
			
		}

	}

}
