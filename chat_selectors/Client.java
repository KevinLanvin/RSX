import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
	public static Socket socket;
	public InputStream input;
	public OutputStream output;
	byte[] buffer;

	public Client(InetAddress address, int port) throws IOException {
		socket = new Socket(address, port);
		input = socket.getInputStream();
		output = socket.getOutputStream();
		buffer = new byte[512];
		/* Read the hello message */
		input.read(buffer);
	}

	/* Sends Echo command and returns current time */
	public long sendEcho(long nb) throws IOException {
		output.write(("/echo " + nb).getBytes());
		output.flush();
		/* read "echo :" message */
		this.waitForString("echo :\n");
		return System.currentTimeMillis();
	}

	/*
	 * Sends <nb> bytes to the server <pas> by <pas> Returns the time taken to
	 * receive the answer
	 */
	public long sendBytes(int nb, int pas) throws IOException {
		buffer = new byte[(int) Math.max(nb, 512)];
		long begin = this.sendEcho(nb);
		for (int i = 0; i < nb; i += pas) {
			String msg = "";
			for (int j = 0; j < pas; ++j)
				msg += "a";
			output.write(msg.getBytes());
			output.flush();
		}
		for (int i = 0; i < nb; ++i)
			input.read();
		this.waitForString("ok " + nb + "\n");
		return System.currentTimeMillis() - begin;
	}

	private void waitForString(String expected) throws IOException {
		String s = new String(buffer);
		while (!s.contains(expected)) {
			int size = input.read(buffer);
			s = new String(buffer, 0, size);
		}
	}

	/* Closes the connection of client */
	public void close() throws IOException {
		socket.close();
	}

	public static void main(String[] args) throws UnknownHostException,
			IOException {
		for (int i = 1; i < 2000000; i *= 2) {
			System.out.print(i + "\t");
			Client client = new Client(InetAddress.getLocalHost(), 7654);
			System.out.print(client.sendBytes(i, 1) + "\t");
			client.close();
			client = new Client(InetAddress.getLocalHost(), 7654);
			System.out.println(client.sendBytes(i, i));
			client.close();
		}
	}
}

