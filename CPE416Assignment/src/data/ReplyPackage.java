package data;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;



public class ReplyPackage implements Serializable{
	private int statusCode;
	
	public ReplyPackage(int status) {
		this.statusCode = status;
	}

	@Override
	public byte[] serialize() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(1*4);
		IntBuffer intBuffer = byteBuffer.asIntBuffer();
		intBuffer.put(this.statusCode);
		return byteBuffer.array();
	}
	
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
