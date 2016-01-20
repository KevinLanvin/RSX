package selectors;

import java.io.ByteArrayOutputStream;
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
	public final int BUFFERSIZE = 512;
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
	private void readFromChannel(SocketChannel channel) throws IOException {
		Entry entry = null;
		for (Entry e : pendingEcho)
			if (e.channel == channel)
				entry = e;
		if (entry != null) {
			System.out.println("Read Echo");
			this.readEcho(entry);
			return;
		}
		int size = channel.read(this.readBuffer);
		if (this.parseCommand(channel)){
			System.out.println("Command");
			return;
		}
		readBuffer.putChar('\n');
		this.resendMessage(size);
		this.readBuffer.clear();
	}

	private void readEcho(Entry entry) throws IOException {
		int size = entry.channel.read(tempBuffer);
		entry.decrRemaining(size);
		entry.output.write(tempBuffer.array());
	}

	private boolean parseCommand(SelectableChannel channel) {
		String command = new String(this.readBuffer.array());
		System.out.println(command);

		if (command.startsWith("/")) {
			String[] words = command.split(" ");
			switch (words[0]) {
			case "/echo":
				System.out.println("[" + words[1]+ "]");
				this.pendingEcho.add(new Entry((SocketChannel) channel, Integer
						.parseInt(words[1])));
				break;
			case "/ack":
				this.pendingAck.add(new Entry((SocketChannel) channel, Integer
						.parseInt(words[1])));
				break;
			default:
				System.out.println("Wrong command : " + command);
				break;
			}
			return true;
		} else
			return false;
	}

	/* Diffuse the message to all channels */
	private void resendMessage(int size) throws IOException {
		for (SelectionKey sk : selector.keys())
			if (sk.attachment() != null
					&& ((ByteArrayOutputStream) sk.attachment()).size() < 2048)
				((ByteArrayOutputStream) sk.attachment()).write(this.readBuffer
						.array());
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
		ByteBuffer buffer = ByteBuffer.wrap(((ByteArrayOutputStream) sk
				.attachment()).toByteArray());
		while (buffer.hasRemaining())
			((SocketChannel) sk.channel()).write(buffer);
		((ByteArrayOutputStream) sk.attachment()).reset();
	}

	private void writeEcho(Entry e) throws IOException {
		ByteBuffer buffer = ByteBuffer.wrap(e.output.toByteArray());
		while (buffer.hasRemaining())
			e.channel.write(buffer);
		e.output.reset();
		if (e.remaining <= 0) {
			pendingEcho.remove(e);
			String s = "\n[ok " + e.initialnb + "]";
			buffer = ByteBuffer.wrap(s.getBytes());
			while (buffer.hasRemaining())
				e.channel.write(buffer);
		}
	}

}