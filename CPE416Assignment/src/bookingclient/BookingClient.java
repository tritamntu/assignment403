package bookingclient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import data.DataPackage;
import data.RequestPackage;
import data.StatusCode;

import booking.TimePoint;

public class BookingClient {

	static DatagramSocket socket;
	static DatagramPacket packet;
	static InetAddress serverAddr;
	static int serverPort = 2000;
	static int clientPort = 2002;
	static byte[] buffer;
	static int requestId;
	
	public static void main(String [] args) {
		
		buffer = new byte[500];
		
		try {
			socket = new DatagramSocket(clientPort);
			serverAddr = InetAddress.getByName("127.0.0.1");
			TimePoint tp = new TimePoint(TimePoint.MONDAY, 10, 1);
			requestId = 1;
			queryAvailability(1, tp);
			System.out.println("Client terminates ..");
		} catch (SocketException | UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void queryAvailability(int facilityId, TimePoint tp) throws IOException {
		// 1. send request package to server 
		RequestPackage queryRequest = new RequestPackage(
				requestId, 
				RequestPackage.SERVICE_QUERY, 
				facilityId, 0);
		buffer = queryRequest.serialize();
		packet = new DatagramPacket(buffer, 0, buffer.length, serverAddr, serverPort);
		socket.send(packet);
		// 2. send data package to server
		buffer = DataPackage.serialize(tp);
		packet = new DatagramPacket(buffer, 0, buffer.length, serverAddr, serverPort);
		socket.send(packet);
		// 3. receive reply package to server
		socket.receive(packet);
		buffer = packet.getData();
		int statusCode = ByteBuffer.wrap(buffer, 0, 4).getInt();
		
		// 4. receive data package to server
		socket.receive(packet);
		buffer = packet.getData();
		int nextDate = ByteBuffer.wrap(buffer, 0, 4).getInt();
		int nextHour = ByteBuffer.wrap(buffer, 4, 4).getInt();
		int nextMin  = ByteBuffer.wrap(buffer, 8, 4).getInt();
		System.out.println(nextDate + " - " + nextHour + " - " + nextMin);
		TimePoint nextTime = new TimePoint(nextDate, nextHour, nextMin);
		if(statusCode == StatusCode.SUCCESS_AVAILABLE) {
			System.out.println("The Facility is Available.");
			System.out.println("The next occupied time slot is: " + nextTime.toString());
		} else {
			System.out.println("The Facility is not Available.");
			System.out.println("The next available time slot is: " + nextTime.toString());
		}
	}
}
