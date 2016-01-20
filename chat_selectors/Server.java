import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class Server {
	public final int port = 7654;
	private ChannelsHandler handler;

	public Server() throws IOException {
		handler = new ChannelsHandler();
	}

	public static void main(String[] args) throws IOException {
		Server server = new Server();

		// Initialisation du channel du serveur
		ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.socket().bind(new InetSocketAddress(server.port));
		// Lancement du Thread qui gèrera les discussions
		Thread t = new Thread(server.handler);
		t.start();

		System.out.println("Server activated on port " + server.port);

		// Boucle écoutant les connexions entrantes
		while (true) {
			SocketChannel channel = serverSocketChannel.accept();
			System.out.println("Attente de connexion entrante");
			server.handler.registerChannel(channel);
		}
	}
}