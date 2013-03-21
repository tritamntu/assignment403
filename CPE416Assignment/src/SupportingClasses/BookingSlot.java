package SupportingClasses;

public class BookingSlot {
	private TimePoint startTime;
	private Duration interval;
	
	public BookingSlot(TimePoint tp, Duration dr) {
		startTime = new TimePoint(tp.getDate(), tp.getHour(), tp.getMin());
		interval = new Duration(dr.getDay(), dr.getHour());
	}
	
	public int compareTime(TimePoint tp) {
		return this.startTime.compareTime(tp);
	}
	
	public int compareTime(BookingSlot slot) {
		return this.startTime.compareTime(slot.startTime);
	}
	
	
	// get and set method
	public String toString() {
		String printStr = "Start at: " + this.startTime.toString() + "\n";
		printStr += "Duration: " + this.interval.getDay() + " day(s), " + this.interval.getHour() + " hour(s).";
		return printStr;
	}
	
	// get EndTime Method
	public TimePoint getEndTime() {
		return new TimePoint(startTime, interval);
	}
}