import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Slave extends Thread {

	Server server;
	Socket client;
	BufferedReader input;

	public Slave(Socket socket, Server server) {
		this.client = socket;
		this.server = server;
	}

	public void run() {
		this.addClient(client);
		this.sayHello();
		try {
			this.listen();
		} catch (IOException e) {
			e.printStackTrace();
			try {
				client.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

	}

	private void sayHello() {
		PrintWriter output;
		try {
			output = new PrintWriter(client.getOutputStream());
			output.println("Hello !");
			output.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void listen() throws IOException {

		input = new BufferedReader(new InputStreamReader(
				client.getInputStream()));

		String message;
		while (!(message = input.readLine()).contains("quit")
				&& (message != null))
			this.diffuseMessage(message);
		input.close();
		this.removeClient(client);
	}

	public void addClient(Socket client) {
		System.out.println("Client " + client + " connected.");
		server.clients.add(client);
	}

	public void removeClient(Socket client) throws IOException {
		server.clients.remove(client);
		System.out.println("Client " + client + " disconnected.");
		client.close();
	}
	
	public void diffuseMessage(String message) {
		for (Socket s : server.clients) {
			if (!s.equals(this.client)) {
				try {
					PrintWriter output = new PrintWriter(s.getOutputStream());
					output.println(message);
					output.flush();
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("Client " + s + " not reached.");
				}
			}
		}
	}
}
