package booking;

/*
 * Class: Duration
 * Purpose: express a time duration / interval
 * Including: duration's day, hour, minute
 * */
public class Duration {
	
	private int day;
	private int hour;
	private int min; 
	
	// constructor
	public Duration() {
		this.day = this.hour = this.min = -1;
	}
	
	// constructor
	public Duration(int day, int hour, int min) {
		this.day = day;
		this.hour = hour;
		this.min = min;
	}
	
	// get and set method
	// get this duration day
	public int getDay(){
		return this.day;
	}
	
	// get this duration hour
	public int getHour(){
		return this.hour;
	}
	
	// get this duration minute
	public int getMin(){
		return this.min;
	}
	
	// get this duration's text information
	public String toString() {
		return this.day + " day(s), " + this.hour + " hour(s), " + this.min + " min(s)";
	}
}
