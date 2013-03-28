package booking;

import java.net.InetAddress;
import java.net.UnknownHostException;

/*
 * Class: MonitorCliet
 * Purpose: hold information of a monitoring client
 * Including: client address, client port 
 * Including: time interval
 * */
public class MonitorClient {
	
	private String clientAddr;
	private int clientPort;
	private Duration interval;
	
	// constructor
	public MonitorClient(InetAddress clientAddr, int clientPort, Duration interval) 
			throws UnknownHostException {
		this.clientAddr = clientAddr.getHostName();
		this.clientPort = clientPort;
		interval = new Duration(interval.getDay(), interval.getHour(), interval.getMin());
	}
	
	// get text client address
	public String getClientAddress() {
		return this.clientAddr;
	}
	
	// get client port
	public int getClientPort(){
		return this.clientPort;
	}
}
