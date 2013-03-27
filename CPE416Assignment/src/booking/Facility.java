package booking;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Facility {
	private int id;
	private String desc;
	private ArrayList<BookingSlot> slots;
	private ArrayList<MonitorClient> monitorList;
	private int confirmId = 0;
	
	public Facility(int id, String desc) {
		this.id = id;
		this.desc = desc;
		slots = new ArrayList<BookingSlot>();
		monitorList = new ArrayList<MonitorClient>();
	}
	
	// toString method
	public String toString() {
		return "Id:" + this.id + ":" + this.desc;
	}
	
	public String getDesc() {
		return this.desc;
	}
	
	// get all bookings slot string
	public String getBookSchedule() {
		if(this.slots.size() == 0) 
			return "There is no booking slots for " + this.desc;
		String str = "";
		for(int i = 0;  i < this.slots.size(); i++) {
			str += "Slot " + i + ": \n" + slots.get(i).toString() + "\n";
		}
		return str;
	}
	
	public ArrayList<MonitorClient> getClientList() {
		return this.monitorList;
	}
	
	public ArrayList<BookingSlot> getBookSlots() {
		return this.slots;
	}
	
	// query Availability
	// output = true (mean available at startTime), nextTime = next occupied Time
	// output = false(mean not available at startTime), nextTime = next available Time
	public boolean queryAvailibility(TimePoint startTime) {
		if(slots.size() == 0) {
	
			return true;
		}
		int index = 0;
		while(index < slots.size()) {
			BookingSlot currentSlot = slots.get(index);
			if(currentSlot.compareTime(startTime) > 0) {
				break;
			}
			index++;
		}
		if(index > 0 && slots.get(index-1).getEndTime().compareTime(startTime) > 0) {
			return false;
		}
		if(index == 0 || index < slots.size()) {
			BookingSlot nextSlot = slots.get(index);
			
		} 
		return true;
	}
	
	public TimePoint getNextTime(TimePoint startTime) {
		if(slots.size() == 0) {
			return null;
		}
		TimePoint nextTime;
		int index = 0;
		while(index < slots.size()) {
			BookingSlot currentSlot = slots.get(index);
			if(currentSlot.compareTime(startTime) > 0) {
				break;
			}
			index++;
		}
		System.out.println("Index = " + index);
		if(index > 0 && slots.get(index-1).getEndTime().compareTime(startTime) > 0) {
			nextTime = slots.get(index - 1).getEndTime();
		} else if(index == 0 || index < slots.size()) {
			BookingSlot nextSlot = slots.get(index);
			nextTime =  new TimePoint(
					nextSlot.getStartDate(), 
					nextSlot.getStartHour(), 
					nextSlot.getStartMin());
		} else { 
			nextTime = null;
		}
		return nextTime;
	}
	
	// add method
	public int addSlot(BookingSlot newSlot) {
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
				return -1;
			}
		}
		// after violation: end time of new slot violates start time of the next
		if(index < slots.size()) {
			BookingSlot currSlot = slots.get(index);
			if(currSlot.compareTime(newSlot.getEndTime()) < 0) {
				System.out.println("Time Violation: after");
				return -1;
			}
		}
		this.confirmId ++;
		newSlot.setConfirmationId(confirmId);
		System.out.println("ConfirmId = " + confirmId);
		slots.add(index, newSlot);
		System.out.println("Slotsize = " + this.slots.size());
		return confirmId;
	}
	
	public BookingSlot removeSlot(int index) {
		if(this.slots.size() == 0 || this.slots.size() > 0 && index >= this.slots.size())
			return null;
		return this.slots.remove(index);
	}
	
	public BookingSlot removeSlot(InetAddress clientAddr, int clientPort, int confirmationId) {
		int i;
		for(i = 0; i < this.slots.size(); i++) {
			BookingSlot slot = this.slots.get(i);
			if(slot.compareClient(clientAddr, clientPort, confirmationId)) {
				return this.slots.remove(i);
			}
		}
		return null;
		
	}
	
	public void removeAllSlot(InetAddress clientAddr, int clientPort) {
		System.out.println("Size = " + this.slots.size());		
		for (Iterator i = slots.iterator(); i.hasNext(); ) {
		    BookingSlot slot = (BookingSlot)i.next();
		    if(slot.compareClient(clientAddr, clientPort))
		    	i.remove();
		}
		System.out.println("Size = " + this.slots.size());
	}
	
	public BookingSlot removeLastSlot(InetAddress clientAddr, int clientPort) {
		int latestConfirmId = -1;
		int latestIndex = -1;
		for(int i = 0; i < slots.size(); i++) {
			BookingSlot slot = slots.get(i);
			if(slot.compareClient(clientAddr, clientPort)) {
				if(slot.getConfirmationId() > latestConfirmId) {
					latestConfirmId = slot.getConfirmationId();
					latestIndex = i;
				}
			}
		}
		if(latestIndex != -1)
			return slots.remove(latestIndex);
		else return null;
	}
	
	// search booking slot by confirmation id
	public int searchBookSlot(int confirmId) {
		int index = -1;
		if(this.slots.size() > 0) {
			for(int i = 0; i < this.slots.size(); i++) {
				BookingSlot slot = this.slots.get(i);
				System.out.println("Slot CID " + slot.getConfirmationId() );
				if(slot.getConfirmationId() == confirmId) {
					index = i;
					break;
				}
			}
		}
		else {
			System.out.println("Empty slots " + this.slots.size());
		}
		return index;
	}
	
	public int bookChange(int confirmId, Duration dr) {
		// return -1 if failed
		// otherwise return the new confirmation id
		int index = this.searchBookSlot(confirmId);
		System.out.println("ConfirmId: " + confirmId + ", Index" + index);
		if(index == -1)
			return -1;
		BookingSlot currSlot = this.removeSlot(index);
		BookingSlot updateSlot = currSlot.getUpdateSlot(dr);
		int addResult = this.addSlot(updateSlot);
		if(addResult == -1)
			this.addSlot(currSlot);
		return addResult;
	}

	public void addMonitorClient(MonitorClient client) {
		if(client != null)
			this.monitorList.add(client);
	}
	
	public void printSlot() {
	    for(int i = 0; i < slots.size(); i++) {
	      System.out.println("Slot " + i + ":");
	      System.out.println(slots.get(i).toString());
	    }
	}
	
	public static void main(String [] args) {
		
		Facility books = new Facility(1, "Books");

		BookingSlot bs1 = new BookingSlot(new TimePoint(TimePoint.MONDAY, 10, 0), new Duration(0, 3, 0));
		BookingSlot bs3 = new BookingSlot(new TimePoint(TimePoint.SUNDAY, 10, 1), new Duration(0, 3, 0));
		BookingSlot bs4 = new BookingSlot(new TimePoint(TimePoint.FRIDAY, 10, 1), new Duration(0, 3, 0));
		BookingSlot bs5 = new BookingSlot(new TimePoint(TimePoint.THURSDAY, 10, 1), new Duration(0, 3, 0));
		BookingSlot bs2 = new BookingSlot(new TimePoint(TimePoint.THURSDAY, 13, 0), new Duration(0, 3, 0));

		books.addSlot(bs1);
		books.addSlot(bs2);
		books.addSlot(bs3);
		books.addSlot(bs4);
		books.addSlot(bs5);
		System.out.println(books.toString());
		System.out.println(books.getBookSchedule());
		System.out.println("Terminate"); 
	}
}
