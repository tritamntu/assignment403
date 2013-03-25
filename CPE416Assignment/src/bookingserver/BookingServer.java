package bookingserver;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
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
	
	// semantics constant values
	public static final int AT_LEAST_ONCE = 1;
	public static final int AT_MOST_ONCE = 2;
	
	static int port = 2000;
	static DatagramSocket socket;
	static DatagramPacket receivePacket;
	static DatagramPacket sendPacket;
	static int statusCode; 
	static Facility[] fList;
	static byte [] receiveBuffer;
	static byte [] replyBuffer;
	static byte [] dataBuffer;
	static RequestHistory history;
	static int sematicsCode = BookingServer.AT_LEAST_ONCE;
	
	public static void main(String [] args) {
		try {
			// 1. initialize Facility and Network Socket
			createFacilities();
			socket = new DatagramSocket(port);
			receiveBuffer = new byte[50];
			receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
			history = new RequestHistory();
			// 2. start listening to request
			while(true) {
				System.out.println("Waiting for a request ..");
				// 2.1 receive request
				BookingServer.socket.setSoTimeout(0);
				BookingServer.socket.receive(receivePacket);
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
				
				// 2.2 send acknowledgment
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
					BookingServer.printHandlerClosing();
					continue;
				}
				
				// 2.2 * check duplicate and handle request
				int index = BookingServer.history.searchRequest(clientAddr, clientPort, clientRequest.getRequestId());
				if(index != -1 && BookingServer.sematicsCode == BookingServer.AT_MOST_ONCE) {
					// handle duplicate
					System.out.println("Client Duplicate Request: ");
					RequestMessage message = BookingServer.history.getMessage(index);
					System.out.println(message.toString());
					// send request message to client
					ReplyPackage replyPackage = new ReplyPackage(StatusCode.REQUEST_DUPLICATE);
					dataBuffer = replyPackage.serialize(message.getDataBuffer());
					BookingServer.sendPacket = new DatagramPacket(
							dataBuffer, dataBuffer.length, 
							InetAddress.getByName(message.getClientAddress()),
							message.getPort());
					BookingServer.socket.send(BookingServer.sendPacket);
					BookingServer.printHandlerClosing();
					continue;
				}
				
				// 2.3 receive data package from client and execute command
				TimePoint startTime = null;
				Duration interval = null;
				socket.setSoTimeout(800);
				try {
				switch(clientRequest.getServiceId()) {
				case RequestPackage.SERVICE_QUERY: 
					// service 1 query Availability
					socket.receive(receivePacket);
					dataBuffer = receivePacket.getData();
					startTime = DataPackage.extractTimePoint(dataBuffer, 0);
					BookingServer.queryAvailibity(clientRequest.getFacilityId(), startTime);
					break;
				case RequestPackage.SERVICE_BOOK: 
					// service 2 booking request
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
				case RequestPackage.SERVICE_CHANGE: 
					// service 3 booking change
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
				case RequestPackage.SERVICE_MONITOR: 
					// service 4 monitor call back
					socket.receive(receivePacket);
					dataBuffer = receivePacket.getData();
					interval = DataPackage.extractDuration(dataBuffer, 0);
					BookingServer.monitor(clientRequest.getFacilityId(), 
							clientAddr, clientPort, interval);
					break;
				case RequestPackage.SERVICE_PROGRAM: 
					// service 5 query specification
					BookingServer.runProgram(
							clientRequest.getFacilityId(), 
							clientRequest.getOptionalId());
					break;
				case RequestPackage.SERVICE_SPEC: 
					// service 6 run a program
					BookingServer.queryDescription(clientRequest.getFacilityId());
					break;
				} } catch (SocketTimeoutException e) {
					// Timeout: server can't receive data package from client, execution terminates
					System.out.println("Timeout: can't receive data package");
					BookingServer.printHandlerClosing();
					continue;
				} 
				// 2.5 store request in history
				RequestMessage requestMessage = new RequestMessage(clientRequest, clientAddr, clientPort);
				requestMessage.setBuffer(dataBuffer);
				BookingServer.history.addMessage(requestMessage);
				// 2.6 send data package to client
				BookingServer.sendPacket = new DatagramPacket(
						dataBuffer, dataBuffer.length, 
						clientAddr, clientPort);
				BookingServer.socket.send(sendPacket);
				// 2.7 callback if a booking slot is changed
				if(statusCode == StatusCode.SUCCESS_BOOKING
				|| statusCode == StatusCode.SUCCESS_BOOKING_CHANGE) {
					BookingServer.callback(clientRequest.getFacilityId());
				} 
					// reset statusCode
				statusCode = StatusCode.FACILITY_NOT_FOUND;
				BookingServer.printHandlerClosing();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block

		}
	}
	
	public static void printHandlerClosing() {
		System.out.println("RequestHandler ends");
		System.out.println("....................");
		System.out.println();
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
	public static int queryAvailibity(
			int facilityId, TimePoint startTime)  {
		System.out.println("Start Service 1: Query Availability");
		TimePoint nextTime = null;
		// 1. check availability and status code
		boolean available = fList[facilityId].queryAvailibility(startTime, nextTime);
		int statusCode = -1;
		if(available) statusCode = StatusCode.SUCCESS_AVAILABLE;
		else 		  statusCode = StatusCode.SUCCESS_NOTAVAILABLE;
		ReplyPackage replyPackage = new ReplyPackage(statusCode);	
		// 2. setup data package (TimePoint nextTime) to client
		dataBuffer = replyPackage.serialize(DataPackage.serialize(nextTime));	
		return statusCode;
	}
	
	// service 2 bookRequest 
	public static int bookRequest(
			int facilityId, TimePoint startTime, Duration interval) {
		// 1. add slot to schedule
		System.out.println("Start Service 2: Book Request");
		int confirmId = fList[facilityId].addSlot(new BookingSlot(startTime, interval));
		int statusCode;
		if(confirmId == -1) 
			statusCode = StatusCode.FAILED_BOOKING;
		else statusCode =StatusCode.SUCCESS_BOOKING;
		// 2. setup data package to reply
		ReplyPackage replyPackage = new ReplyPackage(statusCode);
		dataBuffer = replyPackage.serialize(DataPackage.serialize(confirmId));
		return statusCode;
	}
	
	public static int bookChange(
			int facilityId, int confirmationId, Duration interval) {
		// 1. change for a book record 
		System.out.println("Start Service 3: Booking Change");
		int statusCode = -1;
		int confirmId = fList[facilityId].bookChange(confirmationId, interval);
		if(confirmId == -1) 
			statusCode = StatusCode.FAILED_BOOKING_CHANGE;
		else statusCode = StatusCode.SUCCESS_BOOKING_CHANGE;
		// 2. setup reply data
		ReplyPackage replyPackage = new ReplyPackage(statusCode);
		dataBuffer = replyPackage.serialize(DataPackage.serialize(confirmId));
		return statusCode;
	}
	
	public static int monitor(
			int facilityId,  InetAddress clientAddr, int clientPort, Duration interval) 
			throws UnknownHostException {
		// 1. add client to monitor list
		System.out.println("Start Service 4: Monitor");
		MonitorClient newClient = new MonitorClient(clientAddr, clientPort, interval);
		fList[facilityId].addMonitorClient(newClient);
		int statusCode = StatusCode.SUCCESS_ADD_MONITOR;
		// 2. setup reply data
		ReplyPackage replyPackage = new ReplyPackage(statusCode);
		dataBuffer = replyPackage.serialize(null);
		return statusCode;
	}
	
	public static void callback(int facilityId) 
			throws IOException {
		System.out.println("Start Call back");
		ArrayList<MonitorClient> monitorList = fList[facilityId].getClientList();
		if(monitorList.size() > 0) {
			System.out.println("Monitor - clientlist > 0");
			ArrayList<BookingSlot> slotList = fList[facilityId].getBookSlots();
			dataBuffer = DataPackage.serialize(slotList);
			System.out.println("Monitor - list of booking slots");
			DataPackage.printByteArray(dataBuffer);
			for(int i = 0; i < monitorList.size(); i++) {
				MonitorClient client = monitorList.get(i);
				InetAddress clientAddr = InetAddress.getByName(client.getClientAddress());
				int clientPort = client.getClientPort();
				System.out.println("Monitor - client: " + clientAddr.getHostAddress() + ":" + clientPort);
				BookingServer.sendPacket = new DatagramPacket(
						dataBuffer, dataBuffer.length,
						clientAddr, clientPort);
				BookingServer.socket.send(BookingServer.sendPacket);
			}
		} else {
			System.out.println("Monitor - Client list is empty");
		}
	}
	
	public static int  queryDescription(int facilityId) 
			throws UnsupportedEncodingException {
		// 1. search description of the facility
		String str = fList[facilityId].toString();
		int statusCode = StatusCode.SUCCESS_AVAILABLE;
		ReplyPackage replyPackage = new ReplyPackage(statusCode);
		// 2. setup data buffer to client
		dataBuffer = replyPackage.serialize(DataPackage.serialize(str));
		return statusCode;
	}
	
	public static int runProgram(int facilityId, int applicationId) {
		// 1. get the program id
		int statusCode = StatusCode.FACILITY_NOT_FOUND;
		if(facilityId == 3 || facilityId == 4) {
			statusCode = StatusCode.SUCCESS_PROGRAM;
			System.out.println("Application Id " + applicationId + " is executed.");
		}
		ReplyPackage replyPackage = new ReplyPackage(statusCode);
		// 2. setup data buffer to client
		dataBuffer = replyPackage.serialize(null);
		return statusCode;
	}
	
}
