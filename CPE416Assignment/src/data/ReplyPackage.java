package data;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/*
 * Class: ReplyPackage
 * Purpose: package that contains status code 
 * and is used to send to client
 * Implement Serializable interface
 * */
public class ReplyPackage implements Serializable{
	
	private int statusCode;
	
	// contructor
	public ReplyPackage(int status) {
		this.statusCode = status;
	}

	@Override
	// a normal serialize method
	public byte[] serialize() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(1*4);
		IntBuffer intBuffer = byteBuffer.asIntBuffer();
		intBuffer.put(this.statusCode);
		return byteBuffer.array();
	}
	
	// serialize method with existing byte buffer
	public byte[] serialize(byte [] buffer) {
		byte[] statusBuffer = this.serialize();
		if(buffer == null)
			return statusBuffer;
		byte[] allBuffer = new byte[statusBuffer.length + buffer.length];
		System.arraycopy(statusBuffer, 0, allBuffer, 0, statusBuffer.length);
		System.arraycopy(buffer, 0, allBuffer, statusBuffer.length, buffer.length);
		return allBuffer;
	}
}
