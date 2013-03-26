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
	public static final int MAX_TIMEOUT = 4;
	
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
	static int ackTimeoutCount;
	static int dataTimeoutCount;
	
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
		ackTimeoutCount = 0;
		dataTimeoutCount = 0;
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
	
	
	public static int sendRequest(int serviceId, int facilityId, int optionalId, TimePoint tp, Duration dr) 
			throws SocketException {
		System.out.println("Serivce Id " + serviceId);
		
		Boolean sending = true;
		dataTimeoutCount = 0;
		socket.setSoTimeout(800);
		int statusCode = StatusCode.SUCCESS_NOTAVAILABLE;
		while(sending && dataTimeoutCount <= BookingClient.MAX_TIMEOUT) {
			dataTimeoutCount++;
			statusCode = StatusCode.SUCCESS_NOTAVAILABLE;
			try {
				window.appendTextLine(BookingClient.getServiceName(serviceId));
				// 1. send request package
				sendRequestPackage(serviceId, facilityId, optionalId);
				// 2. receive acknowledgment package
				statusCode = receiveAckPackage();
				System.out.println("StatusCode " + statusCode + ";");
				if(statusCode == StatusCode.ACKNOWLEDGEMENT_FAILED) {
					window.appendTextLine("Failed Acknowedgment From Server");
					window.appendTextLine("End Request .................");
					window.appendTextLine("");
					return statusCode;
				}
				// 3. send data package
				if(statusCode != StatusCode.REQUEST_DUPLICATE) {
					sendDataPackage(serviceId, tp, dr);
				// 4. receive data package if the request is not a duplicate
					socket.receive(receivePacket);
					receiveBuffer = receivePacket.getData();
					statusCode = ByteBuffer.wrap(receiveBuffer, 0, 4).getInt();
				}
				processDataPackage(serviceId, statusCode);
				System.out.println("StatusCode " + statusCode + ";");
				if(serviceId == RequestPackage.SERVICE_MONITOR) {
					// if the service is monitor call back, continue to read data
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
				sending = false;
			} catch (SocketTimeoutException e) {
				window.appendTextLine("Timeout : " + dataTimeoutCount);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if(sending) {
			statusCode = StatusCode.SERVER_NOT_AVAILABLE;
			window.appendTextLine("Server Not Available, Try Again Later");
		}
		dataTimeoutCount = 0;
		window.appendTextLine("End Request .................");
		window.appendTextLine("");
		//requestId++;
		return statusCode;
	}
	
	public static void sendRequestPackage(int serviceId, int facilityId, int optionalId) 
			throws IOException {
		RequestPackage rp = new RequestPackage(requestId, serviceId, facilityId, optionalId);
		sendBuffer = rp.serialize();
		sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, serverAddr, serverPort);
		socket.send(sendPacket);
	}
	
	public static int receiveAckPackage() 
			throws IOException, SocketTimeoutException {
		socket.receive(receivePacket);
		receiveBuffer = receivePacket.getData();
		return ByteBuffer.wrap(receiveBuffer, 0, 4).getInt();
	}
	
	public static void sendDataPackage(int serviceId, TimePoint tp, Duration dr) 
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
		}
		
		if(sendBuffer != null) {
			sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, serverAddr, serverPort);
			socket.send(sendPacket);
		}
	}
	
	public static void processDataPackage(int serviceId, int statusCode) {
		int confirmId = 0;
		TimePoint nextTime = null;
		switch(serviceId) {
		case RequestPackage.SERVICE_QUERY:
			
			if(statusCode == StatusCode.SUCCESS_AVAILABLE) {
				nextTime = DataPackage.extractTimePoint(receiveBuffer, 4);
				window.appendTextLine("The Facility is Available.");
				window.appendTextLine("The next occupied time slot is: " + nextTime.toString());
			} else if(statusCode == StatusCode.SUCCESS_NOTAVAILABLE){
				nextTime = DataPackage.extractTimePoint(receiveBuffer, 4);
				window.appendTextLine("The Facility is not Available.");
				window.appendTextLine("The next available time slot is: " + nextTime.toString());
			} else if(statusCode == StatusCode.REQUEST_DUPLICATE) {
				nextTime = DataPackage.extractTimePoint(receiveBuffer, 8);
				window.appendTextLine("Duplicate Request: The Facility's availability is unknown.");
				window.appendTextLine("The next available/occupied time slot is: " + nextTime.toString());
			}
			break;
			
		case RequestPackage.SERVICE_BOOK:
			
			if(statusCode == StatusCode.SUCCESS_BOOKING) {
				confirmId = DataPackage.extractInt(receiveBuffer, 4);
				window.appendTextLine("Booking was successful, ConfirmationID = " + confirmId);
			} else if(statusCode == StatusCode.SUCCESS_NOTAVAILABLE){
				window.appendTextLine("Booking was failed due to time violation with other booking slots!");
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
				window.appendTextLine("Booking change was successful, new ConfirmationID = " + confirmId);
			} else if (statusCode == StatusCode.SUCCESS_NOTAVAILABLE) {
				window.appendTextLine("Booking change was failed due to time violation or empty booking slots!");
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
			} else if(statusCode == StatusCode.REQUEST_DUPLICATE) {
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
		}
	}
	
	public static String getServiceName(int serviceId) {
		String str = "";
		switch(serviceId) {
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
		case RequestPackage.SERVICE_SPEC:
			return "Service 6: Query Facility List";
		}
		return str;
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
