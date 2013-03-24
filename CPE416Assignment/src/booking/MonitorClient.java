package booking;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class MonitorClient {
	
	private String clientAddr;
	private int clientPort;
	private Duration interval;
	
	public MonitorClient(InetAddress clientAddr, int clientPort, Duration interval) 
			throws UnknownHostException {
		this.clientAddr = clientAddr.getHostName();
		this.clientPort = clientPort;
		interval = new Duration(interval.getDay(), interval.getHour(), interval.getMin());
	}
	
	public String getClientAddress() {
		return this.clientAddr;
	}
	
	public int getClientPort(){
		return this.clientPort;
	}
	
	public int getDurationDate() {
		return this.interval.getDay();
	}
	
	public int getDurationHour() {
		return this.interval.getHour();
	}
	
	public int getDurationMin() {
		return this.interval.getMin();
	}
}
