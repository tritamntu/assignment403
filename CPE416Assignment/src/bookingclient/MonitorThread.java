package bookingclient;

import java.io.IOException;

import booking.Duration;

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
			BookingClient.monitor(facilityId, dr);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
