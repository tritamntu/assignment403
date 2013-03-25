package bookingclient;

import java.io.IOException;
import java.util.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
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
	
	public static void main(String [] args) {
		try {
			BookingClient.init();
			while(true) {
				
			}
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
	
	// service 1 query Availability
	public static void queryAvailability(int facilityId, TimePoint tp) throws IOException {
		System.out.println("Service 1: query Availability");
		// 1. send request package to server 
		System.out.println(requestId + ", " + RequestPackage.SERVICE_QUERY + ", " + facilityId + ", 0");
		RequestPackage queryRequest = new RequestPackage(
				requestId, RequestPackage.SERVICE_QUERY, facilityId, 0);
		sendPackage(queryRequest.serialize());
		// 1.a receive acknowledgment package
		int statusCode = receiveAckPackage();
		if(statusCode != StatusCode.ACKNOWLEDGEMENT) {
			System.out.println("Failed Acknowledgement from server");
			return ;
		} 
		// 2. send data package to server
		sendPackage(DataPackage.serialize(tp));
		// 3. receive data package to server
		socket.receive(receivePacket);
		receiveBuffer = receivePacket.getData();
		statusCode = DataPackage.extractInt(receiveBuffer, 0);
		TimePoint nextTime = DataPackage.extractTimePoint(receiveBuffer, 4);
		if(statusCode == StatusCode.SUCCESS_AVAILABLE) {
			System.out.println("The Facility is Available.");
			System.out.println("The next occupied time slot is: " + nextTime.toString());
		} else {
			System.out.println("The Facility is not Available.");
			System.out.println("The next available time slot is: " + nextTime.toString());
		}
	}
	
	// service 2 booking request
	public static int bookRequest(int facilityId, TimePoint startTime, Duration interval) throws IOException {
		System.out.println("Service 2: book request");
		// 1. send request package to server
		RequestPackage queryRequest = new RequestPackage(
				requestId, RequestPackage.SERVICE_BOOK,	facilityId, 0);
		sendPackage(queryRequest.serialize());
		// 1.a receive acknowledgment package
		int statusCode = receiveAckPackage();
		if(statusCode != StatusCode.ACKNOWLEDGEMENT) {
			System.out.println("Failed Acknowledgement from server");
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
			System.out.println("Booking was successful, ConfirmationID = " + confirmId);
		} else {
			System.out.println("Booking was failed due to time violation with other booking slots!");
		}
		return confirmId;
	}
	
	// service 3 booking change 
	public static void bookChange(int facilityId, int confirmationId, Duration interval) throws IOException {
		System.out.println("Service 3: booking change");
		// 1. send request package to server
		RequestPackage queryRequest = new RequestPackage(
				requestId, RequestPackage.SERVICE_CHANGE, facilityId, confirmationId);
		sendPackage(queryRequest.serialize());
		// 1.a receive acknowledgment package
		int statusCode = receiveAckPackage();
		if(statusCode != StatusCode.ACKNOWLEDGEMENT) {
			System.out.println("Failed Acknowledgement from server");
			return;
		} 
		// 2. send data package to server
		sendPackage(DataPackage.serialize(interval));
		// 3. receive data package to server
		socket.receive(receivePacket);
		receiveBuffer = receivePacket.getData();
		statusCode = DataPackage.extractInt(receiveBuffer, 0);
		int confirmId = DataPackage.extractInt(receiveBuffer, 4);
		if(statusCode == StatusCode.SUCCESS_BOOKING_CHANGE) {
			System.out.println("Booking change was successful, new ConfirmationID = " + confirmId);
		} else {
			System.out.println("Booking change was failed due to time violation with other booking slots!");
		}
	}
	
	public static void monitor(int facilityId, Duration interval) throws IOException {
		System.out.println("Monitor: send monitor request");
		// 1. send request package to server
		RequestPackage requestPackage = new RequestPackage(
				requestId, RequestPackage.SERVICE_MONITOR, facilityId, 0);
		sendPackage(requestPackage.serialize());
		// 1.a receive acknowledgment package
		int statusCode = receiveAckPackage();
		if(statusCode != StatusCode.ACKNOWLEDGEMENT) {
			System.out.println("Failed Acknowledgement from server");
			return;
		} 
		// 2. send data package to server
		sendPackage(DataPackage.serialize(interval));
		// 3. receive data package to server
		socket.receive(receivePacket);
		receiveBuffer = receivePacket.getData();
		statusCode = DataPackage.extractInt(receiveBuffer, 0);
		System.out.println("StatusCode = " + statusCode);
		// 4. receive data package from server 
		if(statusCode == StatusCode.SUCCESS_ADD_MONITOR) {
			System.out.println("Monitor: successful continue receive");
			while(true) {
				socket.receive(receivePacket);
				receiveBuffer = receivePacket.getData();
				System.out.println("Monitor: receive data from server");
				DataPackage.printByteArray(receiveBuffer);
				ArrayList<BookingSlot> slotList = DataPackage.extractSlotList(receiveBuffer, 0);
				System.out.println("Monitor : size = " + slotList.size());
				for(int i = 0; i < slotList.size(); i++) {
					BookingSlot slot = slotList.get(i);
					System.out.println(slot.toString());
				}
			}
		}
	}
	
	public static int queryFacilityName() throws IOException {
		int statusCode = StatusCode.FACILITY_NOT_FOUND;
		// 1. send request package
		if(window == null) {
			System.out.println("window == null");
		}
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
		window.appendTextLine("EndRequest .........");
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
	
}
