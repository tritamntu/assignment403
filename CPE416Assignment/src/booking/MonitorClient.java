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
	private TimePoint endTime;
	
	// constructor
	public MonitorClient(InetAddress clientAddr, int clientPort, TimePoint endTime) 
			throws UnknownHostException {
		this.clientAddr = clientAddr.getHostName();
		this.clientPort = clientPort;
		this.endTime = new TimePoint(endTime.getDate(), endTime.getHour(), endTime.getMin());
	}
	
	// get text client address
	public String getClientAddress() {
		return this.clientAddr;
	}
	
	// get client port
	public int getClientPort(){
		return this.clientPort;
	}
	
	public boolean finishMonitor(TimePoint current) {
		int timeCompare = this.endTime.compareTime(current);
		if(timeCompare <= 0) {
			return true;
		} else return false;
	}
}
