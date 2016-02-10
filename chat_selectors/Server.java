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
	public static final int BUFFERSIZE = 2000001;
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
				if (sk.isWritable())
					this.write(sk);
				if (sk.isReadable())
					this.readFromChannel((SocketChannel) sk.channel());
			}
		}
	}

	/* Accept new connection */
	void acceptFromChannel(ServerSocketChannel channel) throws IOException {
		SocketChannel client = channel.accept();
		//System.out.println("Connecting new client");
		client.configureBlocking(false);
		client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE,
				ByteBuffer.allocate(BUFFERSIZE));
		//System.out.println("New client connected");
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
			if (size == -1) {
				//System.out.println("End of stream");
				channel.close();
				return;
			}
			if (this.parseCommand(channel))
				return;
			// Sinon, on lit normalement et on renvoie à tous les utilisateurs
			this.resendMessage(size);
			readBuffer.clear();
		} catch (IOException e) {
			//System.out.println("Client perdu");
		}

	}

	/* Special read for pending echo channels */
	private void readEcho(Entry entry) {
		try {
			//tempBuffer.clear();
			int size = entry.channel.read(tempBuffer);
			/* End of stream */
			if(size == -1){
				//System.out.println("End of stream");
				entry.channel.close();
				pendingEcho.remove(entry);
				return;
			}
			entry.decrRemaining(size);
			entry.output.put(tempBuffer.array(), tempBuffer.position()-size, tempBuffer.position());
		} catch (IOException e) {
			//System.out.println("Client parti");
		}
	}

	/* Special read for pending ack channels */
	private void readAck(Entry entry) throws IOException {
		tempBuffer.clear();
		int size = entry.channel.read(tempBuffer);
		entry.decrRemaining(size);
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
		byte b[] = readBuffer.array();
		String command = new String(b, 0,
				readBuffer.position());
		System.out.println("Command : [" + command+"]"+b.length);
		if (command.startsWith("/")) {
			String[] words = command.split(" ");
			switch (words[0]) {
			case "/echo":
				System.out.println(readBuffer.position());
				this.pendingEcho.add(new Entry((SocketChannel) channel, Integer
						.parseInt(words[1].trim())));
				try {
					((SocketChannel) channel).write(ByteBuffer.wrap("echo :\n"
							.getBytes()));
				} catch (IOException e) {
					//System.out.println("Parti parce qu'il a fini");
				}
				break;
			case "/ack":
				this.pendingAck.add(new Entry((SocketChannel) channel, Integer
						.parseInt(words[1].trim())));
				try {
					((SocketChannel) channel).write(ByteBuffer.wrap("ack :\n"
							.getBytes()));
				} catch (IOException e) {
					//System.out.println("Parti parce qu'il a fini");
				}
				break;
			default:
				System.out.println("Wrong command : " + command);
				break;
			}
			this.readBuffer.position(words[0].length() + words[1].length());
			System.out.println(readBuffer.position());
			return true;
		} else
			return false;
	}

	/* Diffuse the message to all channels */
	private void resendMessage(int size) throws IOException {
		for (SelectionKey sk : selector.keys())
			if (sk.attachment() != null) {
				((ByteBuffer) sk.attachment()).clear();
				((ByteBuffer) sk.attachment()).put(readBuffer.array(), 0,
						size - 1);
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
			//System.out.println("Il est parti");
		}
		((ByteBuffer) sk.attachment()).clear();
	}

	/* Special Write for pending echo channels */
	private void writeEcho(Entry e) throws IOException {
		e.output.flip();
		try {
			e.channel.write(e.output);
		} catch (IOException ex) {
			//System.out.println("Il a pas attendu");
			return;
		}
		e.output.clear();
		if (e.remaining <= 0) {
			this.sendOkMessage(e);
			pendingEcho.remove(e);
		}
	}

	/*
	 * Sends a message to an Entry channel to tell that the command has been
	 * completed
	 */
	private void sendOkMessage(Entry e) {
		String s = "ok " + e.initialnb + "\n";
		ByteBuffer buffer = ByteBuffer.wrap(s.getBytes());
		try {
			e.channel.write(buffer);
		} catch (IOException ex) {
			//System.out.println("Bah le mec il attend meme pas que ca soit ok");
		}
	}
}
