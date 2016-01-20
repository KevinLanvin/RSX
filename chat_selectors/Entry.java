package selectors;

import java.io.ByteArrayOutputStream;
import java.nio.channels.SocketChannel;

public class Entry {
	public SocketChannel channel;
	public int initialnb;
	public int remaining;
	protected ByteArrayOutputStream output;
	
	public Entry (SocketChannel channel, int remaining){
		this.initialnb = remaining;
		this.remaining = remaining;
		this.channel = channel;
		this.output = new ByteArrayOutputStream();
	}
	
	public Entry (SocketChannel channel){
		this.channel = channel;
	}
	
	void decrRemaining(int nb){
		this.remaining-=nb;
	}


	@Override
	public boolean equals(Object obj) {
		return ((Entry)obj).channel.equals(channel);
			
	}
	
	

}
