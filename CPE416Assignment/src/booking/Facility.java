package booking;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/*
 * Class Facility
 * Purpose: hold information of a facility
 * Including: ID and description
 * Including: list of booking slots
 * Including: list of clients who monitor this facility
 * */
public class Facility {
	
	private int id;
	private String desc;
	private ArrayList<BookingSlot> slots;
	private ArrayList<MonitorClient> monitorList;
	private int confirmId = 0;
	
	// constructor
	public Facility(int id, String desc) {
		this.id = id;
		this.desc = desc;
		slots = new ArrayList<BookingSlot>();
		monitorList = new ArrayList<MonitorClient>();
	}
	
	// get text ID and Description of this facility
	public String toString() {
		return "Id:" + this.id + ":" + this.desc;
	}
	
	// get description of this facility
	public String getDesc() {
		return this.desc;
	}
	
	// get a string of all booking slots
	public String getBookSchedule() {
		if(this.slots.size() == 0) 
			return "There is no booking slots for " + this.desc;
		String str = "";
		for(int i = 0;  i < this.slots.size(); i++) {
			str += "Slot " + i + ": \n" + slots.get(i).toString() + "\n";
		}
		return str;
	}
	
	// return client list object
	// used in BookingServer class
	public ArrayList<MonitorClient> getClientList() {
		return this.monitorList;
	}
	
	// return booking slot list object
	// used in BookingServer class
	public ArrayList<BookingSlot> getBookSlots() {
		return this.slots;
	} 
	
	// query Availability of a service
	// output = true (mean available at startTime), 
	// output = false(mean not available at startTime)
	public boolean queryAvailibility(TimePoint startTime) {
		// return true of there is no booking available
		if(slots.size() == 0)
			return true;
		int index = 0;
		// find the slot that happens later and is nearest to parameter startTime
		while(index < slots.size()) {
			BookingSlot currentSlot = slots.get(index);
			if(currentSlot.compareTime(startTime) > 0) {
				break;
			}
			index++;
		}
		// check if previous slot's end time violates with parameter startTime
		// return false if violated
		if(index > 0 && slots.get(index-1).getEndTime().compareTime(startTime) > 0) {
			return false;
		}
		// otherwise return true
		return true;
	}
	
	// get the next available / occupied TimePoint from the parameter startTime
	// this method is similar to above queryAvailability
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
	
	// add slot into the booking slot
	// return: new confirmation id if the new slot could be added
	// return: -1 else
	public int addSlot(BookingSlot newSlot) {
		int index = 0;
		// find ordering index to add slot
		while(index < slots.size()) {
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
		// increase confirmation id and set to the added slot
		this.confirmId ++;
		newSlot.setConfirmationId(confirmId);
		System.out.println("ConfirmId = " + confirmId);
		slots.add(index, newSlot);
		System.out.println("Slotsize = " + this.slots.size());
		return confirmId;
	}
	
	// add new booking slot with out change the confirmation id
	// used in Booking Change service
	public int addSlotWithoutChangeConfirmId(BookingSlot newSlot) {
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
		
		slots.add(index, newSlot);
		return confirmId;
	}
		
	// remove a slot
	// return the removed booking slot
	public BookingSlot removeSlot(int index) {
		if(this.slots.size() == 0 || this.slots.size() > 0 && index >= this.slots.size())
			return null;
		return this.slots.remove(index);
	}
	
	// remove all slots that are booked by parameter client address
	public void removeAllSlot(InetAddress clientAddr, int clientPort) {
		System.out.println("Size = " + this.slots.size());		
		for (Iterator i = slots.iterator(); i.hasNext(); ) {
		    BookingSlot slot = (BookingSlot)i.next();
		    if(slot.compareClient(clientAddr, clientPort))
		    	i.remove();
		}
		System.out.println("Size = " + this.slots.size());
	}
	
	// remove the latest booking slot that is made by a client
	// the latest booking slot has the biggest confirmation id
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
		return index;
	}
	
	// change a booking slot with its confirmation id and a duration
	// return -1 if failed
	// otherwise return the new confirmation id
	public int bookChange(int confirmId, Duration dr) throws UnknownHostException {
		int index = this.searchBookSlot(confirmId);
		System.out.println("ConfirmId: " + confirmId + ", Index" + index);
		if(index == -1)
			return -1;
		BookingSlot currSlot = this.removeSlot(index);
		BookingSlot updateSlot = currSlot.getUpdateSlot(dr);
		// use add slot without change confirmation id for this
		int addResult = this.addSlotWithoutChangeConfirmId(updateSlot);
		if(addResult == -1)
			this.addSlotWithoutChangeConfirmId(currSlot);
		return addResult;
	}

	// add a new client into monitor list
	public void addMonitorClient(MonitorClient client) {
		if(client != null)
			this.monitorList.add(client);
	}
	
}
