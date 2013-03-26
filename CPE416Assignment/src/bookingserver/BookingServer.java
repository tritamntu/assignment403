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
	static byte [] dataBuffer;
	static RequestHistory history;
	static int sematicsCode = BookingServer.AT_MOST_ONCE;
	static int lastValue = -1;
	static int lastService = -1;
	
	public static void main(String [] args) {
		try {
			// 1. initialize Facility and Network Socket
			createFacilities();
			socket = new DatagramSocket(port);
			receiveBuffer = new byte[500];
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
				System.out.println("Request Id = " + clientRequest.getRequestId());
				// 2.2 check if service can be served or not
				int ackCode;
				RequestMessage message = null;
				if(clientRequest.getServiceId() >= RequestPackage.SERVICE_QUERY
						&& clientRequest.getServiceId() <= RequestPackage.SERVICE_SPEC) {
					ackCode = StatusCode.ACKNOWLEDGEMENT;
				} else { 
					ackCode = StatusCode.ACKNOWLEDGEMENT_FAILED;
				}
				// 2.2 * check duplicate and handle request
				int index = BookingServer.history.searchRequest(clientAddr, clientPort, clientRequest.getRequestId());
				System.out.println("Search Index " + index);
				if(index != -1 && BookingServer.sematicsCode == BookingServer.AT_MOST_ONCE) {
					// handle duplicate
					System.out.println("Client Duplicate Request: ");
					message = BookingServer.history.getMessage(index);
					System.out.println(message.toString());
					// send request message to client
					ackCode = StatusCode.REQUEST_DUPLICATE;
				}
				// 2.2 * send acknowledgment to client
				ReplyPackage rp = new ReplyPackage(ackCode);
				if(ackCode == StatusCode.REQUEST_DUPLICATE) 
					dataBuffer = rp.serialize(message.getDataBuffer());
				else dataBuffer = rp.serialize();
				BookingServer.sendPacket = new DatagramPacket(dataBuffer, dataBuffer.length, clientAddr, clientPort);
				BookingServer.socket.send(BookingServer.sendPacket);
				
				if(ackCode != StatusCode.ACKNOWLEDGEMENT) {
					System.out.println("Out of band request!");
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
					System.out.println("Service Query Availability: ");
					System.out.println("\tFacility: " + clientRequest.getFacilityId());
					System.out.println("\tStartTime: " + startTime.toString());
					BookingServer.queryAvailibity(clientRequest.getFacilityId(), startTime);
					break;
				case RequestPackage.SERVICE_BOOK: 
					// service 2 booking request
					socket.receive(receivePacket);
					dataBuffer = receivePacket.getData();
					startTime = DataPackage.extractTimePoint(dataBuffer, 0);
					interval = DataPackage.extractDuration(dataBuffer, 3 * 4);
					System.out.println("Service Book Request: ");
					System.out.println("\tStart time: " + startTime.toString());
					System.out.println("\tDuration: " + interval.toString());
					System.out.println("Facility id: " + clientRequest.getFacilityId());
					statusCode = BookingServer.bookRequest(clientRequest.getFacilityId(), startTime, interval);
					if(clientRequest.getFacilityId() >= 0 && clientRequest.getFacilityId() < fList.length)
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
					statusCode = BookingServer.bookChange(clientRequest.getFacilityId(), confirmationId, interval);
					if(clientRequest.getFacilityId() >= 0 && clientRequest.getFacilityId() < fList.length)
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
					// service 5 run a program
					System.out.println("Before invoking the Service_program");
					System.out.println("last Service: " + lastService);
					System.out.println("last Value: " + lastValue);
					if (clientRequest.getServiceId() == lastService && clientRequest.getOptionalId() ==lastValue){
						System.out.println("case 1: repeated service and number");
						runProgram(clientRequest.getOptionalId(), true);
					}
					else{
						System.out.println("case 2: NOT repeated service and number");
						runProgram(clientRequest.getOptionalId(), false);						
					}
					break;
				case RequestPackage.SERVICE_SPEC: 
					// get facility names
					BookingServer.queryFacilityList();
					break;
				} } catch (SocketTimeoutException e) {
					// Timeout: server can't receive data package from client, execution terminates
					System.out.println("Timeout: can't receive data package");
					BookingServer.printHandlerClosing();
					continue;
				} 
				// 2.5 store request in history
				RequestMessage requestMessage = new RequestMessage(clientRequest, clientAddr, clientPort);
				DataPackage.printByteArray(dataBuffer);
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
		boolean available = false;
		if(facilityId > 0 && facilityId < fList.length) {
			available = fList[facilityId].queryAvailibility(startTime);
			nextTime = fList[facilityId].getNextTime(startTime);
		} else {
			nextTime = null;
		}
		System.out.println("Availability : " + available);
		if(nextTime != null)
			System.out.println(nextTime.toString());
		else System.out.println("Null NextTime");
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
		int confirmId = -1;
		if(facilityId > 0 && facilityId < fList.length) {
			confirmId = fList[facilityId].addSlot(new BookingSlot(startTime, interval));
		} else {
			confirmId = -1;
		}
		int statusCode;
		if(confirmId == -1) 
			statusCode = StatusCode.SUCCESS_NOTAVAILABLE;
		else statusCode =StatusCode.SUCCESS_BOOKING;
		// 2. setup data package to reply
		ReplyPackage replyPackage = new ReplyPackage(statusCode);
		dataBuffer = replyPackage.serialize(DataPackage.serialize(confirmId));
		DataPackage.printByteArray(dataBuffer);
		return statusCode;
	}
	
	public static int bookChange(
			int facilityId, int confirmationId, Duration interval) {
		// 1. change for a book record 
		System.out.println("Start Service 3: Booking Change");
		int statusCode = -1;
		int confirmId = -1;
		if(facilityId > 0 && facilityId < fList.length) {
			confirmId = fList[facilityId].bookChange(confirmationId, interval);
		} else {
			confirmId = -1;
		}
		if(confirmId == -1) 
			statusCode = StatusCode.SUCCESS_NOTAVAILABLE;
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
		int statusCode = StatusCode.SUCCESS_ADD_MONITOR;
		MonitorClient newClient = new MonitorClient(clientAddr, clientPort, interval);
		if(facilityId > 0 && facilityId < fList.length) {
			fList[facilityId].addMonitorClient(newClient);
		} else {
			statusCode = StatusCode.FACILITY_NOT_FOUND;
		}
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
	
	public static int queryFacilityList() throws UnsupportedEncodingException {
		// 1. create string array of facility names
		int statusCode = StatusCode.SUCCESS_AVAILABLE;
		if(fList == null) {
			statusCode = StatusCode.SUCCESS_NOTAVAILABLE;
			ReplyPackage rp = new ReplyPackage(statusCode);
			dataBuffer = rp.serialize();
		} else {
			String[] strAr = new String[fList.length];
			for(int i = 0; i < strAr.length; i++)
				strAr[i] = fList[i].getDesc();
			ReplyPackage rp = new ReplyPackage(statusCode);
			// 2. setup data buffer to client
			dataBuffer = rp.serialize(DataPackage.serialize(strAr));
		}
		return statusCode;
	}
	
	public static int runProgram(int input, boolean runAgain) 
			throws UnsupportedEncodingException {
		System.out.println("Start Service 6: Quote of the day");
		String str = "nothing";
		int statusCode = StatusCode.SUCCESS_NOTAVAILABLE;
		if (input>9 ||input<0){
			str = "Please make sure that your number is between 0 an 9";
		}
		else if(input<=9 && input>=0){
			int output;
			if (runAgain == true){
				lastValue++;
				output = lastValue;
			}
			else{
				output = input;
				lastValue = output;
			}
			str = quote(output);
			System.out.println(str);
		}
		statusCode = StatusCode.SUCCESS_PROGRAM; 
		ReplyPackage rp = new ReplyPackage(statusCode);
		dataBuffer = rp.serialize(DataPackage.serialize(str));
		return statusCode;
	}
	
	
	public static String quote(int output){
		String[] quotes = new String [10];
		quotes[0] = "Quote of the day: A day without sunshine is like, you know, night.!!!";
		quotes[1] = "Quote of the day: Oh, love will make a dog howl in rhyme.!!!";
		quotes[2] = "Quote of the day: Do not take life too seriously. You will never get out of it alive.!!!";
		quotes[3] = "Quote of the day: Weather forecast for tonight: dark.!!!";
		quotes[4] = "Quote of the day: I found there was only one way to look thin: hang out with fat people.!!!";
		quotes[5] = "Quote of the day: I intend to live forever. So far, so good.!!!";
		quotes[6] = "Quote of the day: All generalizations are false, including this one.!!!";
		quotes[7] = "Quote of the day: Why do they call it rush hour when nothing moves?!!!";
		quotes[8] = "Quote of the day: They say marriages are made in Heaven. But so is thunder and lightning.!!!";
		quotes[9] = "Quote of the day: If you have a secret, people will sit a little bit closer.!!!";
		return quotes[output];

	}

}
