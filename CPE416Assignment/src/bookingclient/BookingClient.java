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
	
	public static void main(String [] args) {
		sendBuffer = new byte[500];
		receiveBuffer = new byte[500];
		receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
		try {
			socket = new DatagramSocket(clientPort);
			serverAddr = InetAddress.getByName("127.0.0.1");
			/*
			int option;
			do
			{
				displayInterface();
				option= sc.nextInt();
				sc.nextLine(); // to remove the \n in buffer after nextInt
				
				String facilityName;
				String days;
				String startTime, endTime;
				switch(option){
				
				case 1: 
					System.out.print("Enter facility Name: " );
					facilityName = sc.nextLine().trim();
					System.out.print("Enter days as in MONDAY TUESDAY: " );
					days= sc.nextLine().trim().toUpperCase();
					
					break;
				case 2:
					System.out.print("Enter facility Name: " );
					facilityName = sc.nextLine().trim();
					System.out.print("Enter start time in DAY/24hrs/mins: " );
					startTime= sc.nextLine().trim().toUpperCase();
					System.out.print("Enter end time in DAY/24hrs/mins: " );
					endTime= sc.nextLine().trim().toUpperCase();
					
					break;
				case 3:
					
					break;
				case 4: 
					break;
				}
				
				
			}while(option!=5);
			 
			*/
			
			TimePoint tp = new TimePoint(TimePoint.MONDAY, 10, 1);
			requestId = 1;
			//queryAvailability(1, tp);
			int confirmId1 = bookRequest(1, tp, new Duration(0, 1, 0));
			requestId++;
			tp = new TimePoint(TimePoint.MONDAY, 12, 1);
			int confirmId2 = bookRequest(1, tp, new Duration(0, 1, 0));
			requestId++;
			if(confirmId1 != -1) {
				bookChange(1, confirmId1, new Duration(0, 2, 0));
			}
			
			
			Duration interval = new Duration(1, 2, 0);
			BookingClient.monitor(1, interval);
			
			System.out.println("Client terminates ..");

		} catch (SocketException | UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
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
		// 1. send request package to server 
		RequestPackage queryRequest = new RequestPackage(
				requestId, RequestPackage.SERVICE_QUERY, facilityId, 0);
		sendPackage(queryRequest.serialize());
		// 2. send data package to server
		sendPackage(DataPackage.serialize(tp));
		// 3. receive reply package to server
		int statusCode = receiveReplyPackage();
		// 4. receive data package to server
		socket.receive(receivePacket);
		receiveBuffer = receivePacket.getData();
		TimePoint nextTime = DataPackage.extractTimePoint(receiveBuffer, 0);
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
		// 1. send request package to server
		RequestPackage queryRequest = new RequestPackage(
				requestId, RequestPackage.SERVICE_BOOK,	facilityId, 0);
		sendPackage(queryRequest.serialize());
		// 2. send data package to server
		sendPackage(DataPackage.serialize(startTime, interval));
		// 3. receive reply package from server
		int statusCode = receiveReplyPackage();
		// 4. receive data package from server
		socket.receive(receivePacket);
		receiveBuffer = receivePacket.getData();
		int confirmId = ByteBuffer.wrap(receiveBuffer).getInt();
		if(statusCode == StatusCode.SUCCESS_BOOKING) {
			System.out.println("Booking was successful, ConfirmationID = " + confirmId);
		} else {
			System.out.println("Booking was failed due to time violation with other booking slots!");
		}
		return confirmId;
	}
	
	// service 3 booking change 
	public static void bookChange(int facilityId, int confirmationId, Duration interval) throws IOException {
		// 1. send request package to server
		RequestPackage queryRequest = new RequestPackage(
				requestId, RequestPackage.SERVICE_CHANGE, facilityId, confirmationId);
		sendPackage(queryRequest.serialize());
		// 2. send data package to server
		sendPackage(DataPackage.serialize(interval));
		// 3. receive reply package from server
		int statusCode = receiveReplyPackage();
		// 4. receive data package from server
		socket.receive(receivePacket);
		receiveBuffer = receivePacket.getData();
		int confirmId = ByteBuffer.wrap(receiveBuffer).getInt();
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
		// 2. send data package to server
		sendPackage(DataPackage.serialize(interval));
		// 3. receive reply package from server
		int statusCode = receiveReplyPackage();
		System.out.println("StatusCode = " + statusCode);
		// 4. receive data package from server 
		if(statusCode == StatusCode.SUCCESS_ADD_MONITOR) {
			System.out.println("Monitor: successful continue receive");
			while(true) {
				socket.receive(receivePacket);
				receiveBuffer = receivePacket.getData();
				ArrayList<BookingSlot> slotList = DataPackage.extractSlotList(receiveBuffer, 0);
				for(int i = 0; i < slotList.size(); i++) {
					BookingSlot slot = slotList.get(i);
					System.out.println(slot.toString());
				}
			}
		}
	}
	
	public static void sendPackage(byte [] buffer) throws IOException {
		sendBuffer = buffer;
		sendPacket = new DatagramPacket(sendBuffer, 0, sendBuffer.length, serverAddr, serverPort);
		socket.send(sendPacket);
	}
	public static int receiveReplyPackage() throws IOException {
		socket.receive(receivePacket);
		receiveBuffer = receivePacket.getData();
		return ByteBuffer.wrap(receiveBuffer, 0, 4).getInt();
	}
	
}
