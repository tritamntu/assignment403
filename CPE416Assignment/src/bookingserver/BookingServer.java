package bookingserver;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import data.DataPackage;
import data.ReplyPackage;
import data.RequestPackage;
import data.StatusCode;

import booking.BookingSlot;
import booking.Duration;
import booking.Facility;
import booking.MonitorClient;
import booking.TimePoint;

public class BookingServer {

	static int port = 2000;
	static DatagramSocket socket;
	static DatagramPacket receivePacket;
	static DatagramPacket sendPacket;
	static int statusCode; 
	static Facility[] fList;
	static byte [] receiveBuffer;
	static byte [] replyBuffer;
	static byte [] dataBuffer;
	
	public static void main(String [] args) {
		try {
			// 1. initialize Facility and Network Socket
			createFacilities();
			socket = new DatagramSocket(port);
			receiveBuffer = new byte[50];
			receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
			// 2. start listening to request
			while(true) {
				System.out.println("Waiting for a request ..");
				// 2.1 receive and extract argument from RequestPackage
				socket.receive(receivePacket);
					// extract client address and port
				InetAddress clientAddr = receivePacket.getAddress();
				int clientPort = receivePacket.getPort();
				receiveBuffer = receivePacket.getData();
					// extract RequestPackage arguments
				RequestPackage clientRequest = new RequestPackage(
						ByteBuffer.wrap(receiveBuffer, 0 , 4).getInt(),
						ByteBuffer.wrap(receiveBuffer, 4 , 4).getInt(),
						ByteBuffer.wrap(receiveBuffer, 8 , 4).getInt(),
						ByteBuffer.wrap(receiveBuffer, 12 , 4).getInt());
				
				System.out.println("Request from: " + clientAddr.getHostAddress() + ":" + clientPort);
				System.out.println("Service Id = " + clientRequest.getServiceId());
				
				// 2.1a send acknowledgment
				int ackCode;
				if(clientRequest.getServiceId() >= RequestPackage.SERVICE_QUERY
						&& clientRequest.getRequestId() <= RequestPackage.SERVICE_SPEC) {
					ackCode = StatusCode.ACKNOWLEDGEMENT;
				} else ackCode = StatusCode.ACKNOWLEDGEMENT_FAILED;
				
				ReplyPackage ackPackage = new ReplyPackage(ackCode);
				BookingServer.replyBuffer = ackPackage.serialize();
				BookingServer.sendPacket = new DatagramPacket(replyBuffer, replyBuffer.length, clientAddr, clientPort);
				BookingServer.socket.send(BookingServer.sendPacket);
				
				if(ackCode != StatusCode.ACKNOWLEDGEMENT) {
					System.out.println("Out of band request!");
					System.out.println("RequestHandler ends");
					System.out.println("....................");
					System.out.println();
					continue;
				}
				// 2.2 * semantics: check store request in history
				
				// 2.3 execute service
				TimePoint startTime = null;
				Duration interval = null;
				switch(clientRequest.getServiceId()) {
				case RequestPackage.SERVICE_QUERY: 
					// 2.3.1 query Availability
					// receive DataPackage: TimePoint startTime
					socket.receive(receivePacket);
					dataBuffer = receivePacket.getData();
					startTime = DataPackage.extractTimePoint(dataBuffer, 0);
					BookingServer.queryAvailibity(clientRequest.getFacilityId(), startTime);
					break;
				case RequestPackage.SERVICE_BOOK: 
					// 2.3.2 booking request
					// receive DataPackage: TimePoint startTime and Duration interval
					socket.receive(receivePacket);
					dataBuffer = receivePacket.getData();
					startTime = DataPackage.extractTimePoint(dataBuffer, 0);
					interval = DataPackage.extractDuration(dataBuffer, 3 * 4);
					System.out.println("Service_Book Request: ");
					System.out.println("\tStart time: " + startTime.toString());
					System.out.println("\tDuration: " + interval.toString());
					System.out.println("Facility id: " + clientRequest.getFacilityId());
					statusCode = BookingServer.bookRequest(clientRequest.getFacilityId(), startTime, interval);
					System.out.println(fList[clientRequest.getFacilityId()].getBookSchedule());
					break;
				case RequestPackage.SERVICE_CHANGE: // booking change
					socket.receive(receivePacket);
					dataBuffer = receivePacket.getData();
					int confirmationId = clientRequest.getOptionalId();
					interval = DataPackage.extractDuration(dataBuffer, 0);
					System.out.println("Service_BookChange: ");
					System.out.println("ConfirmId: " + confirmationId);
					System.out.println("Duration:  " + interval.toString());
					System.out.println();
					statusCode = BookingServer.bookChange(clientRequest.getOptionalId(), confirmationId, interval);
					System.out.println(fList[clientRequest.getFacilityId()].getBookSchedule());
					break;
				case RequestPackage.SERVICE_MONITOR: // monitor call back
					socket.receive(receivePacket);
					dataBuffer = receivePacket.getData();
					interval = DataPackage.extractDuration(dataBuffer, 0);
					BookingServer.monitor(clientRequest.getFacilityId(), 
							clientAddr, clientPort, interval);
					break;
				case RequestPackage.SERVICE_PROGRAM: // query specification
					
					break;
				case RequestPackage.SERVICE_SPEC: // run a program
					BookingServer.queryDescription(clientRequest.getFacilityId());
					break;
				}
				
				// 2.4 response to client by sending reply and data package
				BookingServer.sendPacket = new DatagramPacket(
						replyBuffer, 
						replyBuffer.length, 
						clientAddr, 
						clientPort);
				BookingServer.socket.send(sendPacket);
					// for some service, there is no data package
				if(dataBuffer != null) {
					BookingServer.sendPacket = new DatagramPacket(
							dataBuffer,
							dataBuffer.length, 
							clientAddr, 
							clientPort);
					BookingServer.socket.send(sendPacket);
				}
				
				if(statusCode == StatusCode.SUCCESS_BOOKING
				|| statusCode == StatusCode.SUCCESS_BOOKING_CHANGE) {
					BookingServer.callback(clientRequest.getFacilityId());
				} 
				
				System.out.println("RequestHandler ends");
				System.out.println("....................");
				System.out.println();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block

		}
	}
	
	public static void createFacilities() {
		fList = new Facility[5];
		fList[0] = new Facility(0, "LectureRoom");
		fList[1] = new Facility(0, "LearningPod");
		fList[2] = new Facility(0, "RecordingRoom");
		fList[3] = new Facility(0, "Server1");
		fList[4] = new Facility(0, "Server2");
	}
	
	// service 1 queryAvailability
	public static void queryAvailibity(
			int facilityId, TimePoint startTime)  {
		System.out.println("Start Service 1: Query Availability");
		TimePoint nextTime = null;
		// 1. check availability and status code
		boolean available = fList[facilityId].queryAvailibility(startTime, nextTime);
		int statusCode = -1;
		if(available) statusCode = StatusCode.SUCCESS_AVAILABLE;
		else 		  statusCode = StatusCode.SUCCESS_NOTAVAILABLE;
		ReplyPackage replyPackage = new ReplyPackage(statusCode);	
		// 2. setup reply package to client 
		replyBuffer = replyPackage.serialize();
		// 3. setup data package (TimePoint nextTime) to client
		dataBuffer = DataPackage.serialize(nextTime);		
	}
	
	// service 2 bookRequest 
	public static int bookRequest(
			int facilityId, 
			TimePoint startTime, 
			Duration interval) {
		System.out.println("Start Service 2: Book Request");
		int confirmId = fList[facilityId].addSlot(new BookingSlot(startTime, interval));
		int statusCode;
		if(confirmId == -1) 
			statusCode = StatusCode.FAILED_BOOKING;
		else statusCode =StatusCode.SUCCESS_BOOKING;
		replyBuffer = (new ReplyPackage(statusCode)).serialize();
		dataBuffer = DataPackage.serialize(confirmId);
		return statusCode;
	}
	
	public static int bookChange(
			int facilityId, int confirmationId, 
			Duration interval) {
		System.out.println("Start Service 3: Booking Change");
		int statusCode = -1;
		int confirmId = fList[facilityId].bookChange(confirmationId, interval);
		if(confirmId == -1) 
			statusCode = StatusCode.FAILED_BOOKING_CHANGE;
		else statusCode = StatusCode.SUCCESS_BOOKING_CHANGE;
		replyBuffer = (new ReplyPackage(statusCode)).serialize();
		dataBuffer = DataPackage.serialize(confirmId);
		return statusCode;
	}
	
	public static void monitor(
			int facilityId,  InetAddress clientAddr, int clientPort, Duration interval) 
					throws UnknownHostException {
		System.out.println("Start Service 4: Monitor");
		MonitorClient newClient = new MonitorClient(clientAddr, clientPort, interval);
		fList[facilityId].addMonitorClient(newClient);
		int statusCode = StatusCode.SUCCESS_ADD_MONITOR;
		replyBuffer = (new ReplyPackage(statusCode)).serialize();
		dataBuffer = null;
	}
	
	public static void callback(int facilityId) 
			throws IOException {
		System.out.println("Start Call back");
		ArrayList<MonitorClient> monitorList = fList[facilityId].getClientList();
		if(monitorList.size() > 0) {
			System.out.println("Callback: clientlist > 0");
			ArrayList<BookingSlot> slotList = fList[facilityId].getBookSlots();
			dataBuffer = DataPackage.serialize(slotList);
			for(int i = 0; i < monitorList.size(); i++) {
				MonitorClient client = monitorList.get(i);
				InetAddress clientAddr = InetAddress.getByName(client.getClientAddress());
				int clientPort = client.getClientPort();
				BookingServer.sendPacket = new DatagramPacket(
						dataBuffer, 0, dataBuffer.length,
						clientAddr, clientPort);
				BookingServer.socket.send(BookingServer.sendPacket);
			}
		} else {
			System.out.println("Client list is empty");
		}
	}
	
	public static void  queryDescription(int facilityId) 
			throws UnsupportedEncodingException {
		String str = fList[facilityId].toString();
		int statusCode = StatusCode.SUCCESS_AVAILABLE;
		replyBuffer = (new ReplyPackage(statusCode)).serialize();
		dataBuffer = DataPackage.serialize(str);
	}
	
	public static void runProgram(int facilityId, int applicationId) {
		int statusCode = StatusCode.FACILITY_NOT_FOUND;
		if(facilityId == 3 || facilityId == 4) {
			statusCode = StatusCode.SUCCESS_PROGRAM;
			System.out.println("Application Id " + applicationId + " is executed.");
		}
		replyBuffer = (new ReplyPackage(statusCode)).serialize();
		dataBuffer = null;
	}
	
}
