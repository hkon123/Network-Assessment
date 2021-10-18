import java.net.DatagramSocket;

public class ServerConnection extends Connection {
	
	boolean validConnection;

	public ServerConnection( Packet establishingPacket, DatagramSocket listeningSocketIn) {
		if (establishingPacket.getOption() != 1) {
			validConnection = false;
			System.out.println("Unsupported connection attempt detected!");
			//TODO add response to client about unsupported connection attempt
		}
		else {
			validConnection = true;
			listeningSocket = listeningSocketIn;
			destIp = establishingPacket.getDestIp().toString().substring(1);
			destPort = establishingPacket.getDestPort();
			Packet establishedMsg = new Packet( 
					destIp,
					destPort,
					0, //seqNr
					10, //ackNr
					2,  // option = Established
					"Established");
			sendPacket(establishedMsg);
		}
	}
}
