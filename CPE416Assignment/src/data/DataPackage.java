package data;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import booking.*;

/* Class: DataPackage
 * Purpose: provide serialize and de-serialize
 * methods with different parameters
 * Implement Serializable interface
 * */
public class DataPackage implements Serializable {

	@Override
	public byte[] serialize() {
		return null;
	}
	
	// serialize method: with timepoint
	public static byte[] serialize(TimePoint tp) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(3*4);
		IntBuffer intBuffer = byteBuffer.asIntBuffer();
		if(tp == null) {
			intBuffer.put(-1);
			intBuffer.put(-1);
			intBuffer.put(-1);
		} else {
			intBuffer.put(tp.getDate());
			intBuffer.put(tp.getHour());
			intBuffer.put(tp.getMin());
		}
		return byteBuffer.array();
	}
	
	// serialize method: with timepoint and duration
	public static byte[] serialize(TimePoint tp, Duration dr) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(6*4);
		IntBuffer intBuffer = byteBuffer.asIntBuffer();
		intBuffer.put(tp.getDate());
		intBuffer.put(tp.getHour());
		intBuffer.put(tp.getMin());
		intBuffer.put(dr.getDay());
		intBuffer.put(dr.getHour());
		intBuffer.put(dr.getMin());
		return byteBuffer.array();
	}
	
	// serialize method: with duration
	public static byte[] serialize(Duration dr) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(3*4);
		IntBuffer intBuffer = byteBuffer.asIntBuffer();
		intBuffer.put(dr.getDay());
		intBuffer.put(dr.getHour());
		intBuffer.put(dr.getMin());
		return byteBuffer.array();
	}
	
	// serialize method: with integer
	public static byte[] serialize(int confirmId) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(4);
		byteBuffer.putInt(confirmId);
		return byteBuffer.array();
	}
	
	// serialize method: with list of booking slots
	public static byte[] serialize(ArrayList<BookingSlot> slots) {
		int size = slots.size();
		ByteBuffer byteBuffer = ByteBuffer.allocate(4 + size * 6 * 4);
		IntBuffer intBuffer = byteBuffer.asIntBuffer();
		intBuffer.put(size);
		for(int i = 0; i < size; i++) {
			BookingSlot slot = slots.get(i);
			intBuffer.put(slot.getStartDate());
			intBuffer.put(slot.getStartHour());
			intBuffer.put(slot.getStartMin());
			intBuffer.put(slot.getIntervalDay());
			intBuffer.put(slot.getIntervalHour());
			intBuffer.put(slot.getIntervalMin());
		}
		return byteBuffer.array();
	}
	
	// serialize method: with String
	public static byte[] serialize(String str) 
			throws UnsupportedEncodingException {
		return str.getBytes(StandardCharsets.US_ASCII);
	}
	
	// serialize method: with String array
	public static byte[] serialize(String[] strAr) 
			throws UnsupportedEncodingException {
		String str = "";
		for(int i = 0; i < strAr.length -1; i++) {
			str += strAr[i] + "\n";
		}
		str += strAr[strAr.length -1] + "!!!";
		return DataPackage.serialize(str);
	}
	
	// de-serialize method to get Integer
	public static int extractInt(byte[] buffer, int offset) {
		return ByteBuffer.wrap(buffer, offset, 4).getInt();
	}
	
	// de-serialize methods to get Timepoint
	public static TimePoint extractTimePoint(byte[] buffer, int offset) {
		return new TimePoint(
				ByteBuffer.wrap(buffer, offset, 4).getInt(), 
				ByteBuffer.wrap(buffer, offset + 4, 4).getInt(),
				ByteBuffer.wrap(buffer, offset + 8, 4).getInt());
	}

	// de-serialize methods to get a duration
	public static Duration extractDuration(byte[] buffer, int offset) {
		return new Duration(
				ByteBuffer.wrap(buffer, offset, 4).getInt(), 
				ByteBuffer.wrap(buffer, offset + 4, 4).getInt(),
				ByteBuffer.wrap(buffer, offset + 8, 4).getInt());
	}
	
	// de-serialize methods to get a booking slot list
	public static ArrayList<BookingSlot> extractSlotList(byte[] buffer, int offset) {
		ArrayList<BookingSlot> slotList = new ArrayList<BookingSlot>();
		int slotSize = ByteBuffer.wrap(buffer, offset, 4).getInt();
		for(int i = 0; i < slotSize; i++) {
			TimePoint tp = DataPackage.extractTimePoint(buffer, offset + 4 + i * 6 * 4);
			Duration dr = DataPackage.extractDuration(buffer, offset + 4 + i * 6 * 4 + 3 * 4);
			BookingSlot slot = new BookingSlot(tp, dr);
			slotList.add(slot);
		}
		return slotList;
	}
	
	// de-serialize method to get a String list
	public static String[] extractStringList(byte[] buffer, int offset) {
		String str = new String(buffer, offset, buffer.length - offset, StandardCharsets.US_ASCII);
		String [] strAr = str.split("!!!")[0].split("\n");
		return strAr;
	}
	
	// de-serialize methods to get a String
	public static String extractString(byte[] buffer, int offset) {
		String str = new String(buffer, offset, buffer.length - offset, StandardCharsets.US_ASCII);
		str = str.split("!!!")[0];
		return str;
	}
	
	// method to print byte array
	public static void printByteArray(byte [] buffer) {
		StringBuilder sb = new StringBuilder();
	    for (byte b : buffer) {
	        sb.append(String.format("%02X ", b));
	    }
	    System.out.println(sb.toString());
	}

}
