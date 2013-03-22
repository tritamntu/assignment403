package bookingserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import data.DataPackage;
import data.ReplyPackage;
import data.RequestPackage;
import data.StatusCode;

import booking.Facility;
import booking.TimePoint;

public class BookingServer {

	static int port = 2000;
	static DatagramSocket socket;
	static DatagramPacket receivePacket;
	static DatagramPacket sendPacket;
	
	static Facility[] fList;
	static byte [] receiveBuffer;
	static byte [] replyBuffer;
	static byte [] dataBuffer;
	
	public static void main(String [] args) {
		try {
			// 1. initialize Facility and Network Socket
			createFacilities();
			socket = new DatagramSocket(port);
			receiveBuffer = new byte[500];
			receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
			// 2. start listening to request
			while(true) {
				System.out.println("Waiting for a request ..");
				// 2.1 receive and extract argument from RequestPackage
				socket.receive(receivePacket);
					// extract client address and port
				InetAddress clientAddr = receivePacket.getAddress();
				int clientPort = receivePacket.getPort();
				byte[] receiveBuffer = receivePacket.getData();
					// extract RequestPackage arguments
				RequestPackage clientRequest = new RequestPackage(
						ByteBuffer.wrap(receiveBuffer, 0 , 4).getInt(),
						ByteBuffer.wrap(receiveBuffer, 4 , 4).getInt(),
						ByteBuffer.wrap(receiveBuffer, 8 , 4).getInt(),
						ByteBuffer.wrap(receiveBuffer, 12 , 4).getInt());
				// 2.2 * semantics: check store request in history
				
				// 2.3 execute service
				switch(clientRequest.getServiceId()) {
				case RequestPackage.SERVICE_QUERY: 
					// 2.3.1 query Availability
					// receive DataPackage: TimePoint startTime
					socket.receive(receivePacket);
					byte[] dataBuffer = receivePacket.getData();
					TimePoint startTime = new TimePoint(
							ByteBuffer.wrap(dataBuffer, 0, 4).getInt(), 
							ByteBuffer.wrap(dataBuffer, 0, 4).getInt(),
							ByteBuffer.wrap(dataBuffer, 0, 4).getInt());
					BookingServer.queryAvailibity(clientRequest.getFacilityId(), startTime, clientAddr, clientPort);
					break;
				case RequestPackage.SERVICE_BOOK: // booking request
					break;
				case RequestPackage.SERVICE_CHANGE: // booking change
					break;
				case RequestPackage.SERVICE_MONITOR: // monitor call back
					break;
				case RequestPackage.SERVICE_PROGRAM: // query specification
					break;
				case RequestPackage.SERVICE_SPEC: // run a program
					break;
				}
				
				// 2.4 response to client by sending reply and data package
				BookingServer.sendPacket = new DatagramPacket(
						replyBuffer, 0, 
						replyBuffer.length, 
						clientAddr, 
						clientPort);
				BookingServer.socket.send(sendPacket);
					// for some service, there is no data package
				if(dataBuffer == null) 
					continue;
				BookingServer.sendPacket = new DatagramPacket(
						dataBuffer, 0, 
						dataBuffer.length, 
						clientAddr, 
						clientPort);
				BookingServer.socket.send(sendPacket);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
	
	public static void queryAvailibity(
			int facilityId, TimePoint startTime, 
			InetAddress clientAddr, int clientPort) 
					throws IOException {
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
}
