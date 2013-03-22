package data;


import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import booking.*;

public class DataPackage implements Serializable {

	@Override
	public byte[] serialize() {
		
		return null;
	}
	
	public static byte[] serialize(TimePoint tp) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(3*4);
		IntBuffer intBuffer = byteBuffer.asIntBuffer();
		intBuffer.put(tp.getDate());
		intBuffer.put(tp.getHour());
		intBuffer.put(tp.getMin());
		return byteBuffer.array();
	}
	
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
	
	public static byte[] serialize(Duration dr) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(3*4);
		IntBuffer intBuffer = byteBuffer.asIntBuffer();
		intBuffer.put(dr.getDay());
		intBuffer.put(dr.getHour());
		intBuffer.put(dr.getMin());
		return byteBuffer.array();
	}
	
	public static byte[] serialize(int confirmId) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(4);
		byteBuffer.putInt(confirmId);
		return byteBuffer.array();
	}
	
	public static byte[] serialize(ArrayList<BookingSlot> slots) {
		int size = slots.size();
		ByteBuffer byteBuffer = ByteBuffer.allocate(size * 5 * 4);
		IntBuffer intBuffer = byteBuffer.asIntBuffer();
		for(int i = 0; i < size; i++) {
			BookingSlot slot = slots.get(i);
			intBuffer.put(slot.getStartDate());
			intBuffer.put(slot.getStartHour());
			intBuffer.put(slot.getStartMin());
			intBuffer.put(slot.getIntervalDay());
			intBuffer.put(slot.getIntervalHour());
		}
		return byteBuffer.array();
	}
	
	public static byte[] serialize(String str) throws UnsupportedEncodingException {
		return str.getBytes(StandardCharsets.US_ASCII);
	}

}
