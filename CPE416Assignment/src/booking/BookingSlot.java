package booking;

import java.net.InetAddress;
import java.net.UnknownHostException;

/* Class: Booking Slot
 * Purpose: contain data of a booking slot
 * Including: start time, duration, confirmation id, 
 * address of client who book this slot
 * */

public class BookingSlot {
	
	private TimePoint startTime;
	private Duration interval;
	private int confirmationId;
	private InetAddress clientAddr;
	private int clientPort;
	
	//constructor
	public BookingSlot(TimePoint tp, Duration dr) {
		startTime = new TimePoint(tp.getDate(), tp.getHour(), tp.getMin());
		interval = new Duration(dr.getDay(), dr.getHour(), dr.getMin());
	}
	
	// constructor with client address and port
	public BookingSlot(TimePoint tp, Duration dr, InetAddress clientAddr, int clientPort) 
			throws UnknownHostException {
		this(tp, dr);
		this.clientAddr = InetAddress.getByName(clientAddr.getHostAddress());
		this.clientPort = clientPort;
	}
	
	// compare time with a TimePoint
	// return: -1 if this booking slot is earlier
	//		    0 if happens at the same time
	//          1 if this booking slot is later
	public int compareTime(TimePoint tp) {
		return this.startTime.compareTime(tp);
	}
	
	// compare time with a TimePoint
		// return: -1 if this booking slot is earlier
		//		    0 if happens at the same time
		//          1 if this booking slot is later
	public int compareTime(BookingSlot slot) {
		return this.startTime.compareTime(slot.startTime);
	}
	
	// check if owner and confirmation is correct
	// return: true of match client address, client port and confirmation id
	// else    false
	public boolean compareClient(InetAddress clientAddr, int clientPort, int confirmationId) {
		return this.clientAddr.equals(clientAddr) 
				&& (this.clientPort == clientPort) 
				&& (this.confirmationId == confirmationId);
	}
	
	// check if owner and confirmation is correct
		// return: true of match client address, client port
		// else    false
	public boolean compareClient(InetAddress clientAddr, int clientPort) {
		if(this.clientAddr == null)
			System.out.println("Null Client Address");
		return this.clientAddr.equals(clientAddr) 
				&& (this.clientPort == clientPort);
	}
	
	// get and set methods
	// get an update slot from this slot
	// return booking slot, which equals to this booking slot, 
	//  but shifted by a time interval
	public BookingSlot getUpdateSlot(Duration dr) 
			throws UnknownHostException {
		BookingSlot updateSlot = null;
		TimePoint updateTP = new TimePoint(this.startTime, dr);
		updateSlot = new BookingSlot(updateTP, this.interval);
		// set existed client address, client port and confirm id to updated slot
		updateSlot.setClientAddress(this.clientAddr.getHostAddress());
		updateSlot.setClientPort(this.clientPort);
		updateSlot.setConfirmationId(this.confirmationId);
		return updateSlot;
	}
	
	// get text information of this booking slot
	public String toString() {
		String printStr = "Start at: " + this.startTime.toString() + "\n";
		printStr += "Duration: " + this.interval.getDay() + " day(s), " 
		         + this.interval.getHour() + " hour(s),"
		         + this.interval.getMin() + " min(s)";
		return printStr;
	}
	
	// get EndTime Method 
	// return (this BookingSlot.startTime + this BookingSlot.interval)
	public TimePoint getEndTime() {
		return new TimePoint(startTime, interval);
	}
	
	// get start date of booking slot
	public int getStartDate() {
		return this.startTime.getDate();
	}
	
	// get start hour of booking slot
	public int getStartHour() {
		return this.startTime.getHour();
	}
	
	// get start minute of booking slot
	public int getStartMin() {
		return this.startTime.getMin();
	}
	
	// get interval day of booking slot
	public int getIntervalDay() {
		return this.interval.getDay();
	}

	// get interval hour of booking slot
	public int getIntervalHour() {
		return this.interval.getHour();
	}
	
	// get interval minute of booking slot
	public int getIntervalMin() {
		return this.interval.getMin();
	}
	
	// get confirmation id of this booking slot
	public int getConfirmationId() {
		return this.confirmationId;
	}
	
	// set new confirmation id to this booking slot
	public void setConfirmationId(int confirmationId) {
		this.confirmationId = confirmationId;
	}
	
	// get text client address of this booking slot
	public String getClientAddress() {
		return this.clientAddr.getHostAddress();
	}
	
	// get client port of this booking slot
	public int getClientPort() {
		return this.clientPort;
	}
	
	// set new client address to this booking address
	public void setClientAddress(String hostAddress) throws UnknownHostException {
		this.clientAddr = InetAddress.getByName(hostAddress);
	}
	
	// set new client port to this address
	public void setClientPort(int port) {
		this.clientPort = port;
	}
	
}
