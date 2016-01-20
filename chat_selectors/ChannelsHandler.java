import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ChannelsHandler implements Runnable {
	private Selector selector;

	public ChannelsHandler() throws IOException {
		selector = Selector.open();
	}

	public void run() {
		while (true) {
			// On attend que l'un des channels soit prêt soit en lecture soit en
			// écriture
			try {
				selector.select();
			} catch (IOException e) {
				e.printStackTrace();
			}
			Iterator<SelectionKey> it = selector.selectedKeys().iterator();
			while (it.hasNext()) {
				SelectionKey sk = it.next();
				if (sk.isReadable())
					this.readFromChannel((SocketChannel) sk.channel());
				if (sk.isWritable())
					this.write(sk);
			}
		}
	}

	public void readFromChannel(SocketChannel channel) {
		System.out.println("Reading");
		Set<ByteBuffer> destinations = new HashSet<ByteBuffer>();
		for (SelectionKey sk : selector.keys())
			destinations.add((ByteBuffer) sk.attachment());
		try {
			channel.read((ByteBuffer[]) destinations.toArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void write(SelectionKey sk) {
		System.out.println("Writing");
		ByteBuffer buffer = (ByteBuffer) sk.attachment();
		while (buffer.hasRemaining()) {
			try {
				((SocketChannel) sk.channel()).write(buffer);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void registerChannel(SocketChannel channel) throws IOException {
		System.out.println("Connecting new client");
		channel.register(selector,
				SelectionKey.OP_READ | SelectionKey.OP_WRITE,
				ByteBuffer.allocate(512));
		System.out.println("New client connected");
	}

}
