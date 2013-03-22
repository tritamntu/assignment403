package data;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import booking.Serializable;


public class ReplyPackage implements Serializable{
	private int statusCode;
	
	public ReplyPackage(int status) {
		this.statusCode = status;
	}

	@Override
	public byte[] serialize() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(4*4);
		IntBuffer intBuffer = byteBuffer.asIntBuffer();
		intBuffer.put(this.statusCode);
		return byteBuffer.array();
	}
	
	
}
