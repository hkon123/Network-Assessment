import java.io.*;
import java.net.*;
import java.util.Arrays;

public class Launch {

	public static void main(String[] args) throws Exception {
		System.out.println(System.getProperty("java.version"));
		if (args[0].equals("Client")) {
			new ClientConnection(args[1], Integer.parseInt(args[2]), Integer.parseInt(args[4]), 
					args[3], Integer.parseInt(args[5]));
		}
		else if (args[0].equals("Server")){
			DatagramSocket serverSocket;
			
			
			serverSocket = new DatagramSocket(9876);
			
			while(true) {
				serverSocket.setSoTimeout(0);
				Packet incomingPacket = new Packet();
				System.out.println("\n\nServer online!\n\n");
				serverSocket.receive(incomingPacket.getDatagram());
				incomingPacket.stripPacket();
				
				new ServerConnection(incomingPacket, serverSocket,
						Integer.parseInt(args[2]),
						Boolean.valueOf(args[3]),
						args[1]);
			}
		}
		else if (args[0].equals("help")) {
			System.out.println("------Simple File Transfer program-------\n");
			System.out.println("To setup a server use use the following parameters:");
			System.out.println("java Launch Server [path to receive folder] "
					+ "[debug level] [enable packet drops]");
			System.out.println("*Options:*");
			System.out.println("[path to receive folder]: Relative path to the folder "
					+ "where received files wil be saved. Use <\"\"> to use current folder. ");
			System.out.println("[debug level]: Level of debug prints: 1=minimal, 2=descriptive, 3=detailed packet info");
			System.out.println("[enable packet drops]: true/false. Enables a 10% chance an incoming packet will be dropped.");
			System.out.println("");
			System.out.println("To setup a client use use the following parameters:");
			System.out.println("java Launch Client [server ip] [server port] [path to file] "
					+ "[sliding window size] [debug level]");
			System.out.println("*Options:*");
			System.out.println("[server ip]: Ip adress of server you are trying to connect to (xxx.xxx.xxx.xxx/hostname)");
			System.out.println("[server port]: Port number of the server you are trying to connect to");
			System.out.println("[path to file]: Relative path to the file to be sent");
			System.out.println("[sliding window size]: size of the sliding window (1->X)");
			System.out.println("[debug level]: Level of debug prints: 1=minimal, 2=descriptive, 3=detailed packet info");
			System.out.println("");
			System.out.println("");
			System.out.println("");
			
		}
		else if (args[0].equals("Test")) {
			System.out.println(((byte) 253) & 0xff);
			System.out.println(((byte) 254) & 0xff);
			System.out.println(((byte) 255) & 0xff);
			System.out.println(((byte) 257) & 0xff + (257/256)*256);
			System.out.println(255/256);
		}
		else {
			System.out.println("Use command <java Launch help> to display input parameters!");
		}

	}

}
