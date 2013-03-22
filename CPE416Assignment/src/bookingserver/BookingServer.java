package bookingserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;

import booking.Facility;
import booking.TimePoint;

public class BookingServer {

	static int port = 2000;
	static DatagramSocket socket;
	static DatagramPacket packet;
	static Facility[] fList;
	
	public static void main(String [] args) {
		try {
			// initialize Facility
			fList = new Facility[5];
			// initialize Network Socket
			socket = new DatagramSocket(port);
			byte[] buffer = new byte[1000];
			packet = new DatagramPacket(buffer, buffer.length);
			
			while(true) {
				socket.receive(packet);
				byte[] requestBuffer = packet.getData();
				// extract request information
				int requestId = ByteBuffer.wrap(requestBuffer, 0 , 4).getInt();
				int serviceId = ByteBuffer.wrap(requestBuffer, 4 , 4).getInt();
				int facilityId = ByteBuffer.wrap(requestBuffer, 8 , 4).getInt();
				int optionalId = ByteBuffer.wrap(requestBuffer, 0 , 4).getInt();
				// * sematics: check store request in history
				
				// execute service
				switch(serviceId) {
				case 0: // query Availability
					// receive data package: TimePoint startTime
					socket.receive(packet);
					byte[] dataBuffer = packet.getData();
					int startDate = ByteBuffer.wrap(dataBuffer, 0, 4).getInt();
					int startHour = ByteBuffer.wrap(dataBuffer, 4, 4).getInt();
					int startMin = ByteBuffer.wrap(dataBuffer, 8, 4).getInt();
					TimePoint startTime = new TimePoint(startDate, startHour, startHour);
					BookingServer.queryAvailibity(facilityId, startTime);
					break;
				case 1: // booking request
					break;
				case 2: // booking change
					break;
				case 3: // monitor call back
					break;
				case 4: // query specification
					break;
				case 5: // run a program
					break;
				}
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
	public static void queryAvailibity(int facilityId, TimePoint tp) {
		
	}
}
