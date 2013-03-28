package booking;

/*
 * Class: TimePoint
 * Purpose: measure property of a time point
 * Including: date, hour, minutes
 * Values of date are from Monday to Sunday 
 * */
public class TimePoint {
	 
	// range of date
	public static final int MONDAY 		= 0;
	public static final int TUESDAY 	= 1;
	public static final int WEDNESDAY 	= 2;
	public static final int THURSDAY	= 3;
	public static final int FRIDAY 		= 4;
	public static final int SATURDAY 	= 5;
	public static final int SUNDAY 		= 6;
	
	private int date;
	private int hour;
	private int min;
	
	// constructor
	public TimePoint() {
		this.date = this.hour = this.min = -1;
	}
	
	// constructor
	public TimePoint(int date, int hour, int min) {
		this.date = date;
		this.hour = hour;
		this.min  = min;
	}
	
	// constructor with time point and interval
	// return a new time point, which equals parameter time point + parameter duration
	// out of bound values can be set to:
	// Monday, 00:00 or Sunday, 24:00
	public TimePoint(TimePoint tPoint, Duration interval) {
		this.date = tPoint.getDate() + interval.getDay();
		this.hour = tPoint.getHour() + interval.getHour();
		this.min = tPoint.getMin() + interval.getMin();
		if(this.min >= 60) {
			this.min -= 60;
			this.hour++;
		} else if(this.min < 0) {
			this.min += 60;
			this.hour--;
		}
		if(this.hour >= 24) {
			this.hour -= 24;
			this.date++;
		} else if(this.hour < 0) {
			this.hour += 24;
			this.date--;
		}
		if(this.date > TimePoint.SUNDAY) {
			this.date = TimePoint.SUNDAY;
			this.hour = 24;
			this.min = 0;
		} else if (this.date < TimePoint.MONDAY) {
			this.date = TimePoint.MONDAY;
			this.hour = 0;
			this.min = 0;
		}
	}
	
	// time compare with another time point
	// return -1 if this time point happens earlier
	// return  0 if 2 time point happen at the same time
	// return +1 if this time point happens later
	public int compareTime(TimePoint tp) {
		if(this.date < tp.getDate()) 
			return -1;
		else if(this.date > tp.getDate()) 
			return +1;
		else if(this.hour < tp.getHour())
			return -1;
		else if(this.hour > tp.getHour())
			return +1;
		else if(this.min < tp.getMin())
			return -1;
		else if(this.min > tp.getMin())
			return +1;
		return 0;
	}
	
	// get and set methods
	// get this time point date
	public int getDate() {
		return this.date;
	}
	
	// get this time point hour
	public int getHour() {
		return this.hour;
	}
	
	// get this time point minute
	public int getMin() {
		return this.min;
	}

	// get text information of this time point
	public String toString() {
		String printStr = "";
		switch(this.date) {
		case -1:
			return "Empty TimePoint";
		case TimePoint.MONDAY:
			printStr +="Monday, ";
			break;
		case TimePoint.TUESDAY:
			printStr +="TUESDAY, ";
			break;
		case TimePoint.WEDNESDAY:
			printStr +="WEDNESDAY, ";
			break;
		case TimePoint.THURSDAY:
			printStr +="THURSDAY, ";
			break;
		case TimePoint.FRIDAY:
			printStr +="FRIDAY, ";
			break;
		case TimePoint.SATURDAY:
			printStr +="SATURDAY, ";
			break;
		case TimePoint.SUNDAY:
			printStr +="SUNDAY, ";
			break;
		}
		printStr += this.hour + "hour(s), " + this.min + "min(s)";
		return printStr;
	}
	
}

