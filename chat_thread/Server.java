import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {
	public final int port = 7654;
	private ServerSocket server;
	public ArrayList<Socket> clients;

	public void init() throws IOException {
		server = new ServerSocket(port);
		System.out.println("Lancement du serveur sur le port :"
				+ server.getLocalPort());
		clients = new ArrayList<Socket>();
		this.acceptClients();
	}

	public void acceptClients() throws IOException {
		while (true) {
			Slave s = new Slave(server.accept(), this);
			s.start();
		}
	}

	public static void main(String[] args) {
		Server s = new Server();
		try {
			s.init();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
