package booking;

public class Duration {
	private int day;
	private int hour;
	private int min; 
	
	public Duration() {
		this.day = this.hour = this.min = -1;
	}
	
	public Duration(int day, int hour, int min) {
		this.day = day;
		this.hour = hour;
		this.min = min;
	}
	
	// get and set method
	public int getDay(){
		return this.day;
	}
	
	public int getHour(){
		return this.hour;
	}
	
	public int getMin(){
		return this.min;
	}
	
	public String toString() {
		return this.day + " day(s), " + this.hour + " hour(s), " + this.min + " min(s)";
	}
}
