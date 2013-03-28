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
	static int sematicsCode = BookingServer.AT_LEAST_ONCE;
	static int lastValue = -1;
	static int lastService = -1;
	static ServerUI window;
	static int ackLossRate = 0;
	static int dataLossRate = 0;
	
	public static void main(String [] args) {
		try {
			// 1. initialize Facility and Network Socket
			window = new ServerUI();
			window.setVisible(true);
			createFacilities();
			socket = new DatagramSocket(port);
			receiveBuffer = new byte[500];
			receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
			history = new RequestHistory();
			// 2. start listening to request
			while(true) {
				window.appendTextLine("Waiting for a request ..");
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
				
				window.appendTextLine("Request from: " + clientAddr.getHostAddress() + ":" + clientPort);
				window.appendText("Service Id = " + clientRequest.getServiceId());
				window.appendText(", Facility Id = " + clientRequest.getFacilityId());
				window.appendText(", Request Id = " + clientRequest.getRequestId());
				window.appendTextLine(", Optional Id = " + clientRequest.getOptionalId());
				// 2.2 check if service can be served or not
				int ackCode;
				RequestMessage message = null;
				if(clientRequest.getServiceId() >= RequestPackage.SERVICE_SPEC
						&& clientRequest.getServiceId() <= RequestPackage.SERVICE_REMOVE_LAST) {
					ackCode = StatusCode.ACKNOWLEDGEMENT;
				} else { 
					ackCode = StatusCode.ACKNOWLEDGEMENT_FAILED;
				}
				// 2.2 * check duplicate and handle request
				int index = BookingServer.history.searchRequest(clientAddr, clientPort, clientRequest.getRequestId());
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
				if(ackCode == StatusCode.REQUEST_DUPLICATE && sematicsCode == BookingServer.AT_MOST_ONCE) 
					dataBuffer = rp.serialize(message.getDataBuffer());
				else dataBuffer = rp.serialize();
				BookingServer.sendPacket = new DatagramPacket(dataBuffer, dataBuffer.length, clientAddr, clientPort);
				// BookingServer.socket.send(BookingServer.sendPacket);
				BookingServer.sendWithLoss(ackLossRate);
				switch(ackCode) {
				case StatusCode.ACKNOWLEDGEMENT:
					window.appendTextLine("StatusCode = ACKNOWLEDGEMENT");
					break;
				case StatusCode.ACKNOWLEDGEMENT_FAILED:
					window.appendTextLine("StatusCode = ACKNOWLEDGEMENT_FAILED");
					break;
				case StatusCode.REQUEST_DUPLICATE:
					window.appendTextLine("StatusCode = REQUEST_DUPLICATE");
					break;
				}
				if(ackCode == StatusCode.ACKNOWLEDGEMENT_FAILED || 
						(ackCode == StatusCode.REQUEST_DUPLICATE && sematicsCode == BookingServer.AT_MOST_ONCE)) {
					window.appendTextLine("Request handler is going to end due to Fail Acknoledgment or Request Duplicate");
					BookingServer.printHandlerClosing();
					continue;
				}
				// 2.3 receive data package from client and execute command
				TimePoint startTime = null;
				Duration interval = null;
				socket.setSoTimeout(800);
				window.appendTextLine("Receiving Data Package and Execute Handler");
				try {
				switch(clientRequest.getServiceId()) {
				case RequestPackage.SERVICE_QUERY: 
					// service 1 query Availability
					socket.receive(receivePacket);
					dataBuffer = receivePacket.getData();
					startTime = DataPackage.extractTimePoint(dataBuffer, 0);
					window.appendTextLine("Service Query Availability: ");
					window.appendTextLine("\tFacility: " + clientRequest.getFacilityId());
					window.appendTextLine("\tStartTime: " + startTime.toString());
					BookingServer.queryAvailibity(clientRequest.getFacilityId(), startTime);
					break;
				case RequestPackage.SERVICE_BOOK: 
					// service 2 booking request
					socket.receive(receivePacket);
					dataBuffer = receivePacket.getData();
					startTime = DataPackage.extractTimePoint(dataBuffer, 0);
					interval = DataPackage.extractDuration(dataBuffer, 3 * 4);
					window.appendTextLine("Service Book Request: ");
					window.appendTextLine("\tStart time: " + startTime.toString());
					window.appendTextLine("\tDuration: " + interval.toString());
					window.appendTextLine("\tFacility id: " + clientRequest.getFacilityId());
					statusCode = BookingServer.bookRequest(clientRequest.getFacilityId(), startTime, interval, clientAddr, clientPort);
					if(clientRequest.getFacilityId() >= 0 && clientRequest.getFacilityId() < fList.length)
						System.out.println(fList[clientRequest.getFacilityId()].getBookSchedule());
					break;
				case RequestPackage.SERVICE_CHANGE: 
					// service 3 booking change
					socket.receive(receivePacket);
					dataBuffer = receivePacket.getData();
					int confirmationId = clientRequest.getOptionalId();
					interval = DataPackage.extractDuration(dataBuffer, 0);
					window.appendTextLine("Service Book Change: ");
					window.appendTextLine("\tConfirmId: " + confirmationId);
					window.appendTextLine("\tDuration:  " + interval.toString());
					statusCode = BookingServer.bookChange(clientRequest.getFacilityId(), confirmationId, interval);
					if(clientRequest.getFacilityId() >= 0 && clientRequest.getFacilityId() < fList.length)
						System.out.println(fList[clientRequest.getFacilityId()].getBookSchedule());
					break;
				case RequestPackage.SERVICE_MONITOR: 
					// service 4 monitor call back
					socket.receive(receivePacket);
					dataBuffer = receivePacket.getData();
					interval = DataPackage.extractDuration(dataBuffer, 0);
					window.appendTextLine("Service Monitor: ");
					window.appendTextLine("\tFacilityId: " + clientRequest.getFacilityId());
					window.appendTextLine("\tDuration:  " + interval.toString());
					BookingServer.monitor(clientRequest.getFacilityId(), 
							clientAddr, clientPort, interval);
					break;
				case RequestPackage.SERVICE_PROGRAM: 
					// service 5 run a program
					window.appendTextLine("Service Get Quote of The Day:");
					window.appendTextLine("\t QuoteId: " + clientRequest.getOptionalId());
					if (clientRequest.getServiceId() == lastService && clientRequest.getOptionalId() ==lastValue){
						runProgram(clientRequest.getOptionalId(), true);
					}
					else{
						runProgram(clientRequest.getOptionalId(), false);						
					}
					break;
				case RequestPackage.SERVICE_SPEC: 
					// get facility names
					window.appendTextLine("Service Get All Facility Name: ");
					BookingServer.queryFacilityList();
					break;
				case RequestPackage.SERVICE_REMOVE_ALL:
					window.appendTextLine("Service Remove All Booking Slot: ");
					window.appendTextLine("\tClient Address: " + clientAddr.getHostAddress());
					window.appendTextLine("\tClient Port: " + clientPort);
					BookingServer.removeAllSlots(clientRequest.getFacilityId(), clientAddr, clientPort);
					break;
				case RequestPackage.SERVICE_REMOVE_LAST:
					window.appendTextLine("Service Last Booking Slot: ");
					window.appendTextLine("\tClient Address: " + clientAddr.getHostAddress());
					window.appendTextLine("\tClient Port: " + clientPort);
					BookingServer.removeLastSlot(clientRequest.getFacilityId(), clientAddr, clientPort);
					break;
				} } catch (SocketTimeoutException e) {
					// Timeout: server can't receive data package from client, execution terminates
					window.appendTextLine("Timeout: Can't Receive Request");
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
				//BookingServer.socket.send(sendPacket);
				BookingServer.sendWithLoss(dataLossRate);
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
		window.appendTextLine("RequestHandler ends");
		window.appendTextLine("....................");
		window.appendTextLine("");
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
		if(facilityId >= 0 && facilityId < fList.length) {
			available = fList[facilityId].queryAvailibility(startTime);
			nextTime = fList[facilityId].getNextTime(startTime);
		} else {
			nextTime = null;
		}
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
			int facilityId, TimePoint startTime, Duration interval, InetAddress clientAddr, int clientPort) 
					throws UnknownHostException {
		// 1. add slot to schedule
		System.out.println("Start Service 2: Book Request");
		int confirmId = -1;
		if(facilityId >= 0 && facilityId < fList.length) {
			confirmId = fList[facilityId].addSlot(new BookingSlot(startTime, interval, clientAddr, clientPort));
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
		window.appendTextLine(fList[facilityId].getBookSchedule());
		return statusCode;
	}
	
	// service 3 book change
	public static int bookChange(
			int facilityId, int confirmationId, Duration interval) throws UnknownHostException {
		// 1. change for a book record 
		System.out.println("Start Service 3: Booking Change");
		int statusCode = -1;
		int confirmId = -1;
		if(facilityId >= 0 && facilityId < fList.length) {
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
		window.appendTextLine(fList[facilityId].getBookSchedule());
		return statusCode;
	}
	
	// service 4 monitor
	public static int monitor(
			int facilityId,  InetAddress clientAddr, int clientPort, Duration interval) 
			throws UnknownHostException {
		// 1. add client to monitor list
		System.out.println("Start Service 4: Monitor");
		int statusCode = StatusCode.SUCCESS_ADD_MONITOR;
		MonitorClient newClient = new MonitorClient(clientAddr, clientPort, interval);
		if(facilityId >= 0 && facilityId < fList.length) {
			fList[facilityId].addMonitorClient(newClient);
		} else {
			statusCode = StatusCode.FACILITY_NOT_FOUND;
		}
		// 2. setup reply data
		ReplyPackage replyPackage = new ReplyPackage(statusCode);
		dataBuffer = replyPackage.serialize(null);
		return statusCode;
	}
	
	// service 4 call back monitor
	public static void callback(int facilityId) 
			throws IOException {
		System.out.println("Start Call back");
		ArrayList<MonitorClient> monitorList = fList[facilityId].getClientList();
		if(monitorList.size() > 0) {
		
			ArrayList<BookingSlot> slotList = fList[facilityId].getBookSlots();
			dataBuffer = DataPackage.serialize(slotList);
		
			DataPackage.printByteArray(dataBuffer);
			for(int i = 0; i < monitorList.size(); i++) {
				MonitorClient client = monitorList.get(i);
				InetAddress clientAddr = InetAddress.getByName(client.getClientAddress());
				int clientPort = client.getClientPort();
				window.appendTextLine("Monitor - client: " + clientAddr.getHostAddress() + ":" + clientPort);
				BookingServer.sendPacket = new DatagramPacket(
						dataBuffer, dataBuffer.length,
						clientAddr, clientPort);
				BookingServer.sendWithLoss(0);
			}
		} 
	}
	
	// service 5 query facility List
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
	
	// service 6 get quotes of the day
	public static int runProgram(int input, boolean runAgain) 
			throws UnsupportedEncodingException {
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
	
	// service 7 remove all slots
	public static int removeAllSlots(int facilityId, InetAddress clientAddr, int clientPort) {
		int statusCode = StatusCode.FACILITY_NOT_FOUND;
		if(facilityId < fList.length && facilityId >= 0) {
			fList[facilityId].removeAllSlot(clientAddr, clientPort);
			statusCode = StatusCode.SUCCESS_REMOVE;
			window.appendTextLine(fList[facilityId].getBookSchedule());
		} 
		ReplyPackage rp = new ReplyPackage(statusCode);
		dataBuffer = rp.serialize();
		return statusCode;
	}
	
	// service 8 remove the latest slot
	public static int removeLastSlot(int facilityId, InetAddress clientAddr, int clientPort) {
		int statusCode = StatusCode.FACILITY_NOT_FOUND;
		if(facilityId < fList.length && facilityId >= 0) {
			BookingSlot slot = fList[facilityId].removeLastSlot(clientAddr, clientPort);
			if(slot != null)
				statusCode = StatusCode.SUCCESS_REMOVE;
			else
				statusCode = StatusCode.SUCCESS_EMPTY;
			window.appendTextLine(fList[facilityId].getBookSchedule());
		} 
		ReplyPackage rp = new ReplyPackage(statusCode);
		dataBuffer = rp.serialize();
		return statusCode;
	}
	
	// method get the quotes
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
	
	// method to change semantics code
	// used in UI classes
	public static void changeSemantics(int code) {
		BookingServer.sematicsCode = code;
	}

	// method to send with a lost percent
	private static void sendWithLoss(int lostPercent) throws IOException {
		int randomNum = (int) (100 * Math.random());
		if(randomNum > lostPercent) {
			BookingServer.socket.send(sendPacket);
		} else {
			window.appendTextLine("Sending packet was lost by simulation ... :( ");
		}
	}

	// method to change loss rate
	// used in UI classes
	public static void changeLostRate(int ackRate, int dataRate) {
		// TODO Auto-generated method stub
		ackLossRate = ackRate;
		dataLossRate = dataRate;
		window.appendTextLine("New Acknowledgment Loss Rate = " + ackLossRate);
		window.appendTextLine("New Data Loss Rate = " + dataLossRate);
	}
}
