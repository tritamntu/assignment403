package bookingclient.bookingclientUI;

import java.io.IOException;

import data.RequestPackage;

import booking.Duration;
import bookingclient.BookingClient;

public class MonitorThread extends Thread{
	private int facilityId;
	private Duration dr;
	
	public MonitorThread(int facilityId, Duration dr){
		this.facilityId = facilityId;
		this.dr = new Duration(dr.getDay(), dr.getHour(), dr.getMin());
	}
	
	public MonitorThread(int fId, int day, int hour, int min) {
		this.facilityId = fId;
		this.dr = new Duration(day, hour, min);
	}
	
	public void run() {
		try {
			BookingClient.sendRequest(RequestPackage.SERVICE_MONITOR, facilityId, 0, null, dr);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
