package booking;

public class Duration {
	private int day;
	private int hour;
	
	public Duration(int day, int hour) {
		this.day = day;
		this.hour = hour;
	}
	
	// get and set method
	public int getDay(){
		return this.day;
	}
	
	public int getHour(){
		return this.hour;
	}
}
