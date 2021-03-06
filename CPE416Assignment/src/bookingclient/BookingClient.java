package bookingclient;

import java.io.IOException;
import java.util.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import data.DataPackage;
import data.RequestPackage;
import data.StatusCode;

import booking.BookingSlot;
import booking.Duration;
import booking.TimePoint;
import bookingclient.bookingclientUI.ClientUI;
import bookingserver.BookingServer;

/*
 * Class: BookingClient
 * Purpose: Main Program of Client Side
 * */
public class BookingClient {

	// constant data for ClientUI
	public static final String[] dayList = {
		"Monday", "Tuesday", "Wednesday", "Thursday", 
		"Friday", "Saturday", "Sunday"};
	public static final String[] hourList = { "00",
		"01", "02", "03", "04", "05", "06",
		"07", "08", "09", "10", "11", "12",
		"13", "14", "15", "16", "17", "18",
		"19", "20", "21", "22", "23"};
	public static final String[] minList = {"0", "15", "30", "45"};
	public static final String[] weekDayList = {"0","1","2","3","4","5","6"};
	public static final int MAX_TIMEOUT = 8;
	
	// global data objects
	public static String[] facilityName = {};
	static ArrayList<Integer> confirmIdList;
	// global UDP connection objects
	static DatagramSocket socket;
	static DatagramPacket sendPacket;
	static DatagramPacket receivePacket; 
	static InetAddress serverAddr;
	static int serverPort = 2000;
	static int clientPort = 2002;
	static byte[] sendBuffer;
	static byte[] receiveBuffer;
	static int requestId;
	static Scanner sc = new Scanner(System.in);
	// user interface
	static ClientUI window;
	public static boolean stopMonitor;
	static int ackTimeoutCount;
	static int dataTimeoutCount;
	
	public static void main(String [] args) {
		try {
			BookingClient.init();
			while(true) {
			}
		} catch (SocketException | UnknownHostException e) {
			e.printStackTrace();
		}		
	}
	
	// initiate attributes of client program
	public static void init() 
			throws SocketException, UnknownHostException {
		System.out.println("Init");
		// initialize UDP connection objects
		sendBuffer = new byte[500];
		receiveBuffer = new byte[500];
		receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
		socket = new DatagramSocket(clientPort);
		serverAddr = InetAddress.getByName("127.0.0.1");
		// initialize data objects
		confirmIdList = new ArrayList<Integer>();
		requestId = 1;
		ackTimeoutCount = 0;
		dataTimeoutCount = 0;
		// initialize user interface
		window = new ClientUI();
		System.out.println("Setup window done");
		window.setVisible(true);
	}
	
	// send a request to server
	public static int sendRequest(int serviceId, int facilityId, int optionalId, TimePoint tp, Duration dr) 
			throws SocketException {
		Boolean sending = true;
		dataTimeoutCount = 0;
		socket.setSoTimeout(750);
		int statusCode = StatusCode.SUCCESS_NOTAVAILABLE;
		// condition of sending:
		// timeout = 800 msec, max number of repeat = MAX_TIMEOUT
		while(sending && dataTimeoutCount <= BookingClient.MAX_TIMEOUT) {
			dataTimeoutCount++;
			statusCode = StatusCode.SUCCESS_NOTAVAILABLE;
			try {
				window.appendTextLine(BookingClient.getServiceName(serviceId));
				// 1. send request package
				sendRequestPackage(serviceId, facilityId, optionalId, 00);
				// 2. receive acknowledgment package
				statusCode = receiveAckPackage();
				System.out.println("StatusCode " + statusCode + ";");
				if(statusCode == StatusCode.ACKNOWLEDGEMENT_FAILED) {
					window.appendTextLine("Ack: Failed Acknowedgment From Server");
					window.appendTextLine("End Request .................");
					window.appendTextLine("");
					return statusCode;
				}
				// 3. send data package if the request is not a duplicate
				if(statusCode != StatusCode.REQUEST_DUPLICATE) {
					sendDataPackage(serviceId, tp, dr, 00);
				// 4. receive data package if the request is not a duplicate
					socket.receive(receivePacket);
					receiveBuffer = receivePacket.getData();
					statusCode = ByteBuffer.wrap(receiveBuffer, 0, 4).getInt();
				}
				// 5. process data package and get result
				processDataPackage(serviceId, statusCode);
				if(serviceId == RequestPackage.SERVICE_MONITOR) {
					// if the service is monitor call back, continue to read data
					stopMonitor = false;
					long monitorSecs = dr.getDay() * 24 * 3600 + dr.getHour() * 3600 + dr.getMin() * 60;
					Date startMonitorTime = new Date();
					while(!stopMonitor) {
						socket.setSoTimeout(500);
						try {
							socket.receive(receivePacket);
							receiveBuffer = receivePacket.getData();
							window.appendTextLine("Monitor: receive data from server");
							DataPackage.printByteArray(receiveBuffer);
							ArrayList<BookingSlot> slotList = DataPackage.extractSlotList(receiveBuffer, 0);
							window.appendTextLine("Monitor : size = " + slotList.size());
							for(int i = 0; i < slotList.size(); i++) {
								BookingSlot slot = slotList.get(i);
								window.appendTextLine(slot.toString());
							}
						} catch (SocketTimeoutException e ) {
							Date currentTime = new Date();
							long secDiff = (currentTime.getTime() - startMonitorTime.getTime()) / 1000L;
							if(secDiff >= monitorSecs) {
								stopMonitor = true;
								window.appendTextLine("Stop Monitor Process");
							}
							continue;
						}
					}
				}
				sending = false;
			} catch (SocketTimeoutException e) {
				// print time out message
				window.appendTextLine("Timeout : " + dataTimeoutCount);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// if the timeout exceeds MAX COUNT, server is not available
		if(sending) {
			statusCode = StatusCode.SERVER_NOT_AVAILABLE;
			window.appendTextLine("Error: Server Not Available, Try Again Later");
		}
		dataTimeoutCount = 0;
		window.appendTextLine("End Request .................");
		window.appendTextLine("");
		requestId++;
		return statusCode;
	}
	
	// method to send request package to server
	public static void sendRequestPackage(int serviceId, int facilityId, int optionalId, int lostPercent) 
			throws IOException {
		RequestPackage rp = new RequestPackage(requestId, serviceId, facilityId, optionalId);
		sendBuffer = rp.serialize();
		sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, serverAddr, serverPort);
		// send with a lost percent
		BookingClient.sendWithLoss(lostPercent);
	}
	
	// method to receive acknowledgment package from server
	public static int receiveAckPackage() 
			throws IOException, SocketTimeoutException {
		socket.receive(receivePacket);
		receiveBuffer = receivePacket.getData();
		return ByteBuffer.wrap(receiveBuffer, 0, 4).getInt();
	}
	
	// method to send data package to send
	public static void sendDataPackage(int serviceId, TimePoint tp, Duration dr, int lostPercent) 
			throws IOException {
		switch(serviceId) {
		case RequestPackage.SERVICE_QUERY:
			sendBuffer = DataPackage.serialize(tp);
			break;
		case RequestPackage.SERVICE_BOOK:
			sendBuffer = DataPackage.serialize(tp, dr);
			break;
		case RequestPackage.SERVICE_CHANGE:
			sendBuffer = DataPackage.serialize(dr);
			break;
		case RequestPackage.SERVICE_MONITOR:
			sendBuffer = DataPackage.serialize(dr);
			break;
		case RequestPackage.SERVICE_PROGRAM:
			sendBuffer = null;
			break;
		case RequestPackage.SERVICE_SPEC:
			sendBuffer = null;
			break;
		case RequestPackage.SERVICE_REMOVE_ALL:
			sendBuffer = null;
			break;
		case RequestPackage.SERVICE_REMOVE_LAST:
			sendBuffer = null;
			break;
		}
		
		if(sendBuffer != null) {
			sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, serverAddr, serverPort);
			//socket.send(sendPacket);
			BookingClient.sendWithLoss(lostPercent);
		}
	}
	
	// method to process data package from server
	public static void processDataPackage(int serviceId, int statusCode) {
		int confirmId = 0;
		TimePoint nextTime = null;
		switch(serviceId) {
		case RequestPackage.SERVICE_QUERY:
			
			if(statusCode == StatusCode.SUCCESS_AVAILABLE) {
				nextTime = DataPackage.extractTimePoint(receiveBuffer, 4);
				window.appendTextLine("Query: The Facility is Available.");
				window.appendTextLine("Query: The next occupied time slot is: " + nextTime.toString());
			} else if(statusCode == StatusCode.SUCCESS_NOTAVAILABLE){
				nextTime = DataPackage.extractTimePoint(receiveBuffer, 4);
				window.appendTextLine("Query: The Facility is not Available.");
				window.appendTextLine("Query: The next available time slot is: " + nextTime.toString());
			} else if(statusCode == StatusCode.REQUEST_DUPLICATE) {
				nextTime = DataPackage.extractTimePoint(receiveBuffer, 8);
				window.appendTextLine("Duplicate Request: The Facility's availability is unknown.");
				window.appendTextLine("The next available/occupied time slot is: " + nextTime.toString());
			}
			break;
			
		case RequestPackage.SERVICE_BOOK:
			
			if(statusCode == StatusCode.SUCCESS_BOOKING) {
				confirmId = DataPackage.extractInt(receiveBuffer, 4);
				window.appendTextLine("Booking: Booking was successful, ConfirmationID = " + confirmId);
			} else if(statusCode == StatusCode.SUCCESS_NOTAVAILABLE){
				window.appendTextLine("Booking: Booking was failed due to time violation with other booking slots!");
			} else if(statusCode == StatusCode.REQUEST_DUPLICATE) {
				confirmId = DataPackage.extractInt(receiveBuffer, 8);
				window.appendText("Duplicate Request: ");
				if(confirmId > 0)
					window.appendTextLine("Booking was successful, ConfirmationID = " + confirmId);
				else window.appendTextLine("Boooking was unsuccessful.");
			}
			break;
			
		case RequestPackage.SERVICE_CHANGE:
			
			if(statusCode == StatusCode.SUCCESS_BOOKING_CHANGE) {
				confirmId = DataPackage.extractInt(receiveBuffer, 4);
				window.appendTextLine("Change: Booking change was successful, new ConfirmationID = " + confirmId);
			} else if (statusCode == StatusCode.SUCCESS_NOTAVAILABLE) {
				window.appendTextLine("Change: Booking change was failed due to time violation or empty booking slots!");
			} else if(statusCode == StatusCode.REQUEST_DUPLICATE) {
				confirmId = DataPackage.extractInt(receiveBuffer, 8);
				window.appendText("Duplicate Request: ");
				if(confirmId > 0)
					window.appendTextLine("Booking change was successful, ConfirmationID = " + confirmId);
				else window.appendTextLine("Boooking change was unsuccessful.");
			}
			break;
			
		case RequestPackage.SERVICE_MONITOR:
			if(statusCode == StatusCode.SUCCESS_ADD_MONITOR) {
				window.appendTextLine("Monitor: successful continue receive");
			} else if(statusCode == StatusCode.SUCCESS_NOTAVAILABLE) {
				window.appendTextLine("Monitor: Not Available Facility");
			} else if(statusCode == StatusCode.REQUEST_DUPLICATE) {
				window.appendTextLine("Duplicate Request: Please Send a New Request");
			}
			break;
		case RequestPackage.SERVICE_PROGRAM:
			String str= "";
			if(statusCode == StatusCode.SUCCESS_PROGRAM) {
				str = DataPackage.extractString(receiveBuffer, 4);
				window.appendTextLine("Quotes of the day");
				window.appendTextLine(str);
			} else if(statusCode == StatusCode.SERVER_NOT_AVAILABLE) {
				window.appendTextLine("Quotes of the day is unavailable");
			} else if(statusCode == StatusCode.REQUEST_DUPLICATE) {
				str = DataPackage.extractString(receiveBuffer, 8);
				window.appendTextLine("Duplicate Request: ");
				if(str != null && !str.equals(""))
					window.appendTextLine(str);
			}
			break;
		case RequestPackage.SERVICE_SPEC:
			if(statusCode == StatusCode.SUCCESS_AVAILABLE) {
				facilityName = DataPackage.extractStringList(receiveBuffer, 4);
				window.appendTextLine("Facility Name List:");
				for(int i = 0; i < facilityName.length; i++) {
					window.appendTextLine((i+1) + ": " + facilityName[i]);
				}
			} else if(statusCode == StatusCode.SERVER_NOT_AVAILABLE) {
				window.appendTextLine("Quotes of the day is unavailable");
			} else if(statusCode == StatusCode.REQUEST_DUPLICATE) {
				facilityName = DataPackage.extractStringList(receiveBuffer, 8);
				window.appendTextLine("Duplicate Request: Please Send a New One");
				window.appendTextLine("Facility Name List:");
				for(int i = 0; i < facilityName.length; i++) {
					window.appendTextLine((i+1) + ": " + facilityName[i]);
				}
			}
			break;
		case RequestPackage.SERVICE_REMOVE_ALL:
			if(statusCode == StatusCode.SUCCESS_REMOVE) {
				window.appendTextLine("Remove: all slots booked by this client have been removed");
			} else if(statusCode == StatusCode.FACILITY_NOT_FOUND) {
				window.appendTextLine("Remove: Error of not found facility");
			} else if(statusCode == StatusCode.REQUEST_DUPLICATE) {
				window.appendTextLine("Duplicate : All records have been removed");
			}
			break;
		case RequestPackage.SERVICE_REMOVE_LAST:
			if(statusCode == StatusCode.SUCCESS_REMOVE) {
				window.appendTextLine("Remove: The latest booked by this client have been removed");
			} else if(statusCode == StatusCode.SUCCESS_EMPTY) {
				window.appendTextLine("Remove: Empty Booking Slots");
			} else if(statusCode == StatusCode.REQUEST_DUPLICATE) {
				window.appendTextLine("Duplicate : The latest slot has been removed");
			}
			break;
		}
	}
	
	// method to get service name from server
	public static String getServiceName(int serviceId) {
		String str = "";
		switch(serviceId) {
		case RequestPackage.SERVICE_SPEC:
			return "Service 0: Query Facility List";
		case RequestPackage.SERVICE_QUERY:
			return "Service 1: Query Availability";
		case RequestPackage.SERVICE_BOOK:
			return "Service 2: Request Booking Slot";
		case RequestPackage.SERVICE_CHANGE:
			return "Service 3: Change Booking Slot";
		case RequestPackage.SERVICE_MONITOR:
			return "Service 4: Monitor Call Back";
		case RequestPackage.SERVICE_PROGRAM:
			return "Service 5: Get A Quote";
		case RequestPackage.SERVICE_REMOVE_ALL:
			return "Service 6: Remove All Slots";
		case RequestPackage.SERVICE_REMOVE_LAST:
			return "Service 7: Remove Latest Booking Slot";
		}
		return str;
	}
	
	// method to get day index
	// used for UI classes
	public static int getDayIndex(String day) {
		for(int i = 0; i < BookingClient.dayList.length; i++) {
			if(day.equalsIgnoreCase(BookingClient.dayList[i]))
				return i;
		}
		return -1;
	}
	
	// method to get facility index
	// used for UI classes
	public static int getFacilityIndex(String f) {
		for(int i = 0; i < BookingClient.facilityName.length; i++) {
			if(f.equalsIgnoreCase(BookingClient.facilityName[i]))
				return i;
		}
		return -1;
	}
	
	// method to change server
	// used in UI classes
	public static void changeServer(String ipAddr, int port) throws UnknownHostException {
		serverAddr = InetAddress.getByName(ipAddr);
		serverPort = port;
		window.appendTextLine("Address changed: " + serverAddr.getHostAddress() + ":" + serverPort);
	}
	
	// send package with loss
	// send package if random number is higher that lostPercent values
	// else, print a message
	private static void sendWithLoss(int lostPercent) throws IOException {
		int randomNum = (int) (100 * Math.random());
		if(randomNum > lostPercent) {
			BookingClient.socket.send(sendPacket);
		} else {
			window.appendTextLine("Sending packet was lost by simulation ... :( ");
		}
	}
	
	
}

/*
 * Class: MonitorThread
 * Purpose: Thread for Monitor Service
 * */
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

