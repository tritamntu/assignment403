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
	// global data objects
	static String[] facilityName = {};
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
	static boolean stopMonitor;
	
	public static void main(String [] args) {
		try {

			BookingClient.init();
			while(true) {
				
			}
			//System.out.println("Client terminates ..");

			//socket = new DatagramSocket(clientPort);
			//serverAddr = InetAddress.getByName("192.168.0.109");
			//serverAddr = InetAddress.getByName("127.0.0.1");

			//interfaceControl();
		
			
			
//			TimePoint tp = new TimePoint(TimePoint.MONDAY, 10, 1);
//			requestId = 1;
//			//queryAvailability(1, tp);
//			int confirmId1 = bookRequest(1, tp, new Duration(0, 1, 0));
//			requestId++;
//			tp = new TimePoint(TimePoint.MONDAY, 12, 1);
//			int confirmId2 = bookRequest(1, tp, new Duration(0, 1, 0));
//			requestId++;
//			if(confirmId1 != -1) {
//				bookChange(1, confirmId1, new Duration(0, 2, 0));
//			}
//			
//			Duration interval = new Duration(1, 2, 0);
//			BookingClient.monitor(1, interval);
	
			//System.out.println("Client terminates ..");


		} catch (SocketException | UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	

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
		// initialize user interface
		window = new ClientUI();
		System.out.println("Setup window done");
		window.setVisible(true);
	}
	
	public static void displayInterface()
	{
		System.out.println("Please enter an option from 1 to 5");
		System.out.println("1. Query availablity of a facility");
		System.out.println("2. Book a facility");
		System.out.println("3. Change a booking");
		System.out.println("4. Monitor the availability of a facility");
		System.out.println("5. Exit");
	}
	
	public static void interfaceControl() throws IOException
	{
		// The input format complies with the requirement stated in question
		// 
		int option;
		do
		{
			System.out.println("\nPlease enter an option from 1 to 5");
			System.out.println("1. Query availablity of a facility");
			System.out.println("2. Book a facility");
			System.out.println("3. Change a booking");
			System.out.println("4. Monitor the availability of a facility");
			System.out.println("5. Exit");
			
			option= sc.nextInt();
			sc.nextLine(); // to remove the \n in buffer after nextInt
			
			String facilityName;
			String days;
			TimePoint startTime, endTime;
			Duration duration;
			char AP;
			int confID;
			
			switch(option){
			
			case 1: 
				System.out.print("Enter facility Name: " );
				facilityName = sc.nextLine().trim();
				System.out.print("Enter days as in MONDAY TUESDAY: " );
				days= sc.nextLine().trim().toUpperCase();
				//queryAvailability();
				break;
				
			case 2:
				System.out.print("Enter facility Name: " );
				facilityName = sc.nextLine().trim();
				System.out.println("Enter start time" );
				startTime= enterTime();
				System.out.println("Enter end time" );
				endTime= enterTime();
				//bookRequest();
				break;
				
			case 3:
				System.out.print("Enter facility Name: " );
				facilityName = sc.nextLine().trim();
				System.out.print("Enter the confirmation ID: " );
				confID= Integer.parseInt(sc.nextLine().trim());
				System.out.print("Enter the Advance or Postpone time [A/P]: " );
				AP = sc.nextLine().trim().charAt(0);
				duration= enterDuration();
				// bookChange();
				break;
				
			case 4: 
				System.out.print("Enter facility Name: " );
				facilityName = sc.nextLine().trim();
				System.out.println("Enter monitor interval" );
				duration= enterDuration();
				//monitor(facilityName, duration);
				break;
			}// end switch		
		}while(option!=5);	
	}
	
	public static TimePoint enterTime()
	{
		int day, hour, min;
		System.out.println("1. Monday\n2. Tuesday\n3. Wednesday\n4. Thurday\n5. Friday\n6. Saturday\n7. Sunday");
		System.out.print("Select a day [1 to 7]: ");
		day= Integer.parseInt(sc.nextLine().trim())-1;
		System.out.print("Enter hour [0 to 23]: ");
		hour=Integer.parseInt(sc.nextLine().trim());
		System.out.print("Enter mins [0 to 59]: " );
		min= Integer.parseInt(sc.nextLine().trim());
		return new TimePoint(day, hour, min);
	}
	
	public static Duration enterDuration()
	{
		int day, hour, min;
		System.out.print("Select number of days [1 to 7]: ");
		day= Integer.parseInt(sc.nextLine().trim())-1;
		System.out.print("Enter hour [0 to 23]: ");
		hour=Integer.parseInt(sc.nextLine().trim());
		System.out.print("Enter mins [0 to 59]: " );
		min= Integer.parseInt(sc.nextLine().trim());
		return new Duration(day,hour,min);

	}
	
	// service 1 query Availability
	public static int queryAvailability(int facilityId, TimePoint tp) throws IOException {
		// 1. send request package to server 
		window.appendTextLine("Request Service: Query Availability on " + facilityId);
		RequestPackage queryRequest = new RequestPackage(
				requestId, RequestPackage.SERVICE_QUERY, facilityId, 0);
		sendPackage(queryRequest.serialize());
		// 1.a receive acknowledgment package
		int statusCode = receiveAckPackage();
		if(statusCode != StatusCode.ACKNOWLEDGEMENT) {
			window.appendTextLine("Failed Acknowledgement from server");
			return statusCode;
		} 
		// 2. send data package to server
		sendPackage(DataPackage.serialize(tp));
		// 3. receive data package to server
		socket.receive(receivePacket);
		receiveBuffer = receivePacket.getData();
		statusCode = DataPackage.extractInt(receiveBuffer, 0);
		TimePoint nextTime = DataPackage.extractTimePoint(receiveBuffer, 4);
		if(statusCode == StatusCode.SUCCESS_AVAILABLE) {
			window.appendTextLine("The Facility is Available.");
			window.appendTextLine("The next occupied time slot is: " + nextTime.toString());
		} else {
			window.appendTextLine("The Facility is not Available.");
			window.appendTextLine("The next available time slot is: " + nextTime.toString());
		}
		window.appendTextLine("End Request .................");
		window.appendTextLine("");
		return statusCode;
	}
	
	// service 2 booking request
	public static int bookRequest(int facilityId, TimePoint startTime, Duration interval) throws IOException {
		// 1. send request package to server
		window.appendTextLine("Request Service: Book Request on Facility " + facilityId);
		RequestPackage queryRequest = new RequestPackage(
				requestId, RequestPackage.SERVICE_BOOK,	facilityId, 0);
		sendPackage(queryRequest.serialize());
		// 1.a receive acknowledgment package
		int statusCode = receiveAckPackage();
		if(statusCode != StatusCode.ACKNOWLEDGEMENT) {
			window.appendTextLine("Failed Acknowedgment From Server");
			return StatusCode.ACKNOWLEDGEMENT_FAILED;
		} 
		// 2. send data package to server
		sendPackage(DataPackage.serialize(startTime, interval));
		// 3. receive data package to server
		socket.receive(receivePacket);
		receiveBuffer = receivePacket.getData();
		statusCode = DataPackage.extractInt(receiveBuffer, 0);
		int confirmId = DataPackage.extractInt(receiveBuffer, 4);
		if(statusCode == StatusCode.SUCCESS_BOOKING) {
			window.appendTextLine("Booking was successful, ConfirmationID = " + confirmId);
		} else {
			window.appendTextLine("Booking was failed due to time violation with other booking slots!");
		}
		window.appendTextLine("End Request .................");
		window.appendTextLine("");
		return confirmId;
	}
	
	// service 3 booking change 
	public static int bookChange(int facilityId, int confirmationId, Duration interval) throws IOException {
		// 1. send request package to server
		window.appendTextLine("Request Service: Book Change on Facility " 
				+ facilityId + ", ConfirmationId-" + confirmationId);
		RequestPackage queryRequest = new RequestPackage( 
				requestId, RequestPackage.SERVICE_CHANGE, facilityId, confirmationId);
		sendPackage(queryRequest.serialize());
		// 1.a receive acknowledgment package
		int statusCode = receiveAckPackage();
		if(statusCode != StatusCode.ACKNOWLEDGEMENT) {
			window.appendTextLine("Failed Acknowledgement from server");
			return statusCode;
		} 
		// 2. send data package to server
		sendPackage(DataPackage.serialize(interval));
		// 3. receive data package to server
		socket.receive(receivePacket);
		receiveBuffer = receivePacket.getData();
		statusCode = DataPackage.extractInt(receiveBuffer, 0);
		int confirmId = DataPackage.extractInt(receiveBuffer, 4);
		if(statusCode == StatusCode.SUCCESS_BOOKING_CHANGE) {
			window.appendTextLine("Booking change was successful, new ConfirmationID = " + confirmId);
		} else {
			window.appendTextLine("Booking change was failed due to time violation with other booking slots!");
		}
		window.appendTextLine("End Request .................");
		window.appendTextLine("");
		return statusCode;
	}
	
	public static int monitor(int facilityId, Duration interval) throws IOException {
		// 1. send request package to server
		window.appendTextLine("Service Request: Monitor Callback on Facility " + facilityId);
		RequestPackage requestPackage = new RequestPackage(
				requestId, RequestPackage.SERVICE_MONITOR, facilityId, 0);
		sendPackage(requestPackage.serialize());
		// 1.a receive acknowledgment package
		int statusCode = receiveAckPackage();
		if(statusCode != StatusCode.ACKNOWLEDGEMENT) {
			window.appendTextLine("Failed Acknowledgment From Server");
			return statusCode;
		} 
		// 2. send data package to server
		sendPackage(DataPackage.serialize(interval));
		// 3. receive data package to server
		socket.receive(receivePacket);
		receiveBuffer = receivePacket.getData();
		statusCode = DataPackage.extractInt(receiveBuffer, 0);
		// 4. receive data package from server 
		if(statusCode == StatusCode.SUCCESS_ADD_MONITOR) {
			window.appendTextLine("Monitor: successful continue receive");
			stopMonitor = false;
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
					continue;
				}
			}
		}
		window.appendTextLine("End Request .................");
		window.appendTextLine("");
		return statusCode;
	}
	
	public static int queryFacilityName() throws IOException {
		int statusCode = StatusCode.FACILITY_NOT_FOUND;
		// 1. send request package
		window.appendTextLine("Request Service: Query List of Facility");
		RequestPackage rq = new RequestPackage(requestId, RequestPackage.SERVICE_SPEC, 0 , 0);
		sendPackage(rq.serialize());
		// 2. receive acknowledgment
		statusCode = receiveAckPackage();
		if(statusCode == StatusCode.ACKNOWLEDGEMENT_FAILED) {
			window.appendTextLine("Failed Acknowledgment from Server");
			return statusCode;
		} 
		// 3. send data package: there is no data for this service
		// 4. receive reply package
		statusCode = receiveReplyPackage();
		if(statusCode == StatusCode.SUCCESS_AVAILABLE) {
			facilityName = DataPackage.extractStringList(receiveBuffer, 4);
			window.appendTextLine("Facility Name List:");
			for(int i = 0; i < facilityName.length; i++) {
				window.appendTextLine((i+1) + ": " + facilityName[i]);
			}
		}
		window.appendTextLine("End Request .................");
		window.appendTextLine("");
		return statusCode;
	}
	
	public static void sendPackage(byte [] buffer) throws IOException {
		sendBuffer = buffer;
		sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, serverAddr, serverPort);
		socket.send(sendPacket);
	}
	
	public static int receiveReplyPackage() throws IOException {
		socket.receive(receivePacket);
		receiveBuffer = receivePacket.getData();
		return ByteBuffer.wrap(receiveBuffer, 0, 4).getInt();
	}
	
	public static int receiveAckPackage() throws IOException {
		socket.receive(receivePacket);
		receiveBuffer = receivePacket.getData();
		return ByteBuffer.wrap(receiveBuffer, 0, 4).getInt();
	}
	
	public static int getDayIndex(String day) {
		for(int i = 0; i < BookingClient.dayList.length; i++) {
			if(day.equalsIgnoreCase(BookingClient.dayList[i]))
				return i;
		}
		return -1;
	}
	
	public static int getFacilityIndex(String f) {
		for(int i = 0; i < BookingClient.facilityName.length; i++) {
			if(f.equalsIgnoreCase(BookingClient.facilityName[i]))
				return i;
		}
		return -1;
	}
	
	
}
