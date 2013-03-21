package SupportingClasses;

import java.util.ArrayList;

public class Facility {
	private int id;
	private String desc;
	private ArrayList<BookingSlot> slots;
	
	public Facility(int id, String desc) {
		this.id = id;
		this.desc = desc;
		slots = new ArrayList<BookingSlot>();
	}
	
	// toString method
	public String toString() {
		return "Id-" + this.id + ": " + this.desc;
	}
	
	// add method
	public void addSlot(BookingSlot newSlot) {
		int index = 0;
		// find ordering index to add slot
		while(index < slots.size()) {
			//System.out.println("index: " + index + ", size = " + slots.size());
			BookingSlot currentSlot = slots.get(index);
			if(newSlot.compareTime(currentSlot) <= 0) {
				break;
			} else {
				index++;
			}
		}
		// check violation
		// before violation: start time of new slot violates end time of previous
		if(index > 0) {
			BookingSlot prevSlot = slots.get(index - 1);
			if(newSlot.compareTime(prevSlot.getEndTime()) < 0) {
				System.out.println("Time Violation: before");
				return;
			}
		}
		// after violation: end time of new slot violates start time of the next
		if(index < slots.size()) {
			BookingSlot currSlot = slots.get(index);
			if(currSlot.compareTime(newSlot.getEndTime()) < 0) {
				System.out.println("Time Violation: after");
				return;
			}
		}
		slots.add(index, newSlot);
	}
	
	public void printSlot() {
		for(int i = 0; i < slots.size(); i++) {
			System.out.println("Slot " + i + ":");
			System.out.println(slots.get(i).toString());
		}
	}
	public static void main(String [] args) {
		Facility books = new Facility(1, "Books");
		BookingSlot bs1 = new BookingSlot(new TimePoint(TimePoint.MONDAY, 10, 0), new Duration(0, 3));
		
		BookingSlot bs3 = new BookingSlot(new TimePoint(TimePoint.SUNDAY, 10, 1), new Duration(0, 3));
		BookingSlot bs4 = new BookingSlot(new TimePoint(TimePoint.FRIDAY, 10, 1), new Duration(0, 3));
		BookingSlot bs5 = new BookingSlot(new TimePoint(TimePoint.THURSDAY, 10, 1), new Duration(0, 3));
		BookingSlot bs2 = new BookingSlot(new TimePoint(TimePoint.THURSDAY, 13, 0), new Duration(0, 3));
		books.addSlot(bs1);
		
		books.addSlot(bs3);
		books.addSlot(bs4);
		books.addSlot(bs5);
		books.addSlot(bs2);
		books.printSlot();
		System.out.println("Terminate");
	}
}
