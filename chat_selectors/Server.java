import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class Server {
	public final int port = 7654;
	public final int BUFFERSIZE = 512;
	private Selector selector;
	private ByteBuffer readBuffer;

	public Server() throws IOException {
		selector = Selector.open();
		// Initialisation du channel du serveur
		ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.socket().bind(new InetSocketAddress(this.port));
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		readBuffer = ByteBuffer.allocate(BUFFERSIZE);
	}

	public static void main(String[] args) throws IOException {
		Server server = new Server();
		System.out.println("Server activated on port " + server.port);
		server.run();
	}

	public void run() throws IOException {
		while (true) {
			// On attend que l'un des channels soit prêt soit en lecture soit en
			// écriture
			selector.select();
			Iterator<SelectionKey> it = selector.selectedKeys().iterator();
			while (it.hasNext()) {
				SelectionKey sk = it.next();
				it.remove();
				if (sk.isAcceptable())
					this.acceptFromChannel((ServerSocketChannel) sk.channel());
				if (sk.isReadable())
					this.readFromChannel((SocketChannel) sk.channel());
				if (sk.isWritable()
						&& ((ByteArrayOutputStream) sk.attachment()).size() > 0)
					this.write(sk);
			}
		}
	}

	/* Accept new connection */
	void acceptFromChannel(ServerSocketChannel channel) throws IOException {
		SocketChannel client = channel.accept();
		System.out.println("Connecting new client");
		client.configureBlocking(false);
		client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE,
				new ByteArrayOutputStream());
		System.out.println("New client connected");
	}

	/* Read from a channel */
	public void readFromChannel(SocketChannel channel) throws IOException {
		int size = channel.read(this.readBuffer);
		readBuffer.putChar('\n');
		this.resendMessage(size);
		this.readBuffer.clear();
	}

	/* Diffuse the message to all channels */
	public void resendMessage(int size) throws IOException {
		for (SelectionKey sk : selector.keys())
			if (sk.attachment() != null
					&& ((ByteArrayOutputStream) sk.attachment()).size() < 2048)
				((ByteArrayOutputStream) sk.attachment()).write(this.readBuffer.array());
	}

	/* Writes to a client */
	public void write(SelectionKey sk) throws IOException {
		ByteBuffer buffer = ByteBuffer.wrap(((ByteArrayOutputStream) sk.attachment()).toByteArray());
		while (buffer.hasRemaining())
			((SocketChannel) sk.channel()).write(buffer);
		((ByteArrayOutputStream)sk.attachment()).reset();
	}

}
