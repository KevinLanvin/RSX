import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Time;

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
	public void sendEcho(long nb) throws IOException {
		output.write(("/echo " + nb).getBytes());
		output.flush();
		/* read "echo :" message */
		this.waitForString("echo :\n");
	}

	/*
	 * Sends <nb> bytes to the server <pas> by <pas> Returns the time taken to
	 * receive the answer
	 */
	public void sendBytes(int nb) throws IOException {
		this.sendEcho(nb);
		byte [] array = new byte [nb];
		output.write(array);
		output.flush();
		for (int i = 0; i < nb; ++i)
			input.read();
		this.waitForString("ok " + nb + "\n");
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
	
	
	public static void sendBytes(int nb, int pas) throws IOException{
		for(int i=pas;i<=nb;i+=pas){
			Client client = new Client(InetAddress.getByName("localhost"),7654);
			client.sendBytes(pas);
			client.close();
		}
	}

	public static void main(String[] args) throws UnknownHostException,
			IOException {
		int begin=1;
		int end=1;
		int step=1024;
		int size= 1048576;
		for(int i = 0 ; i < args.length ; ++i){
			switch(args[i]){
			case "-begin" : 
				begin = Integer.parseInt(args[i+1]);
				break;
			case "-end" :
				end = Integer.parseInt(args[i+1]);
				break;
			case "-step" :
				step = Integer.parseInt(args[i+1]);
			case "-size" :
				size = Integer.parseInt(args[i+1]);
				break;
			default: break;
			}
		}
			
		for (int i = begin; i < end; i += step){
			long beginTime = System.currentTimeMillis();
			Client.sendBytes(size,i);
			long endTime = System.currentTimeMillis();
			System.out.println(i+"\t"+ (endTime-beginTime) + "\t"+(size/(endTime-beginTime)));
		}
	}
}

