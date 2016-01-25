import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;

public class Server {
	public final int port = 7654;
	public static final int BUFFERSIZE = 262144;
	private Selector selector;
	private ByteBuffer readBuffer, tempBuffer;
	private ArrayList<Entry> pendingEcho;
	private ArrayList<Entry> pendingAck;

	public Server() throws IOException {
		selector = Selector.open();
		// Initialisation du channel du serveur
		ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.socket().bind(new InetSocketAddress(this.port));
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		readBuffer = ByteBuffer.allocate(BUFFERSIZE);
		tempBuffer = ByteBuffer.allocate(BUFFERSIZE);
		pendingEcho = new ArrayList<Entry>();
		pendingAck = new ArrayList<Entry>();
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
				if (sk.isWritable())
					this.write(sk);
			}
		}
	}

	/* Accept new connection */
	void acceptFromChannel(ServerSocketChannel channel) throws IOException {
		SocketChannel client = channel.accept();
		System.out.println("Connecting new client");
		client.configureBlocking(false);
		ByteBuffer buffer = ByteBuffer.allocate(BUFFERSIZE);
		client.write(ByteBuffer.wrap("hello\n".getBytes()));
		client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE,
				buffer);
		System.out.println("New client connected");
	}

	/* Read from a channel */
	private void readFromChannel(SocketChannel channel) throws IOException {
		// Si on trouve le channel dans les pending Echos, alors il faut faire
		// un read special
		Entry entry = null;
		for (Entry e : pendingEcho)
			if (e.channel == channel)
				entry = e;
		if (entry != null) {
			this.readEcho(entry);
			return;
		}
		// Si on trouve le channel dans les pending Acks, alors il faut faire
		// un read special
		entry = null;
		for (Entry e : pendingAck)
			if (e.channel == channel)
				entry = e;
		if (entry != null) {
			this.readAck(entry);
			return;
		}
		// Si la chaine lue est une commande, on parse l'exécute et on retourne
		readBuffer.clear();
		try {
			int size = channel.read(readBuffer);
			if (this.parseCommand(channel))
				return;
			// Sinon, on lit normalement et on renvoie à tous les utilisateurs
			this.resendMessage(size - 1);
		} catch (IOException e){
			System.out.println("Client perdu");
		}
		
	}

	/* Special read for pending echo channels */
	private void readEcho(Entry entry) throws IOException {
		tempBuffer.clear();
		int size = entry.channel.read(tempBuffer);
		entry.decrRemaining(size - 2);
		entry.output.put(tempBuffer.array(), 0, tempBuffer.position());
		// Ici on force l'écriture si l'entry a fini de lire (pas bien)
		if (entry.remaining <= 0) {
			this.writeEcho(entry);
			pendingEcho.remove(entry);
		}
	}

	/* Special read for pending ack channels */
	private void readAck(Entry entry) throws IOException {
		tempBuffer.clear();
		int size = entry.channel.read(tempBuffer);
		entry.decrRemaining(size - 2);
		// Ici on force l'écriture si l'entry a fini de lire (pas bien)
		if (entry.remaining <= 0) {
			pendingAck.remove(entry);
			this.sendOkMessage(entry);
		}
	}

	/*
	 * Detects and execute commands, if there is no command, does nothing and
	 * return false
	 */
	private boolean parseCommand(SelectableChannel channel) throws IOException {
		String command = new String(this.readBuffer.array());
		if (command.startsWith("/")) {
			String[] words = command.split(" ");
			switch (words[0]) {
			case "/echo":
				this.pendingEcho.add(new Entry((SocketChannel) channel, Integer
						.parseInt(words[1].trim())));
				try {
					((SocketChannel) channel).write(ByteBuffer.wrap("echo :\n"
							.getBytes()));
				} catch (IOException e) {
					System.out.println("Parti");
				}
				break;
			case "/ack":
				this.pendingAck.add(new Entry((SocketChannel) channel, Integer
						.parseInt(words[1].trim())));
				try {
					((SocketChannel) channel).write(ByteBuffer.wrap("ack :\n"
							.getBytes()));
				} catch (IOException e) {
					System.out.println("Parti");
				}
				break;
			default:
				System.out.println("Wrong command : " + command);
				break;
			}
			this.readBuffer.clear();
			return true;
		} else
			return false;
	}

	/* Diffuse the message to all channels */
	private void resendMessage(int size) throws IOException {
		for (SelectionKey sk : selector.keys())
			if (sk.attachment() != null) {
				((ByteBuffer) sk.attachment()).put(readBuffer.array(), 0,
						readBuffer.position());
			}
	}

	/* Writes to a client */
	private void write(SelectionKey sk) throws IOException {
		Entry entry = null;
		for (Entry e : pendingEcho)
			if (e.channel == sk.channel())
				entry = e;
		if (entry != null) {
			this.writeEcho(entry);
			return;
		}
		((ByteBuffer) sk.attachment()).flip();
		try {
			((SocketChannel) sk.channel()).write((ByteBuffer) sk.attachment());
		} catch (IOException e) {
			System.out.println("Il est parti");
		}
		((ByteBuffer) sk.attachment()).clear();
	}

	/* Special Write for pending echo channels */
	private void writeEcho(Entry e) throws IOException {
		e.output.flip();
		e.channel.write(e.output);
		e.output.clear();
		if (e.remaining <= 0) {
			this.sendOkMessage(e);
		}
	}

	/*
	 * Sends a message to an Entry channel to tell that the command has been
	 * completed
	 */
	private void sendOkMessage(Entry e) throws IOException {
		String s = "[ok " + e.initialnb + "]\n";
		ByteBuffer buffer = ByteBuffer.wrap(s.getBytes());
		e.channel.write(buffer);
	}
}
