package data;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;



public class RequestPackage implements Serializable {
	
	public static final int SERVICE_QUERY = 1;
	public static final int SERVICE_BOOK = 2;
	public static final int SERVICE_CHANGE = 3;
	public static final int SERVICE_MONITOR = 4;
	public static final int SERVICE_PROGRAM = 5;
	public static final int SERVICE_SPEC = 6;
	
	private int requestId;
	private int serviceId;
	private int facilityId;
	private int optionalId;
	
	public RequestPackage(int request, int service, int facility, int optional) {
		this.requestId = request;
		this.serviceId = service;
		this.facilityId = facility;
		this.optionalId = optional;
	}
	
	// serialized integer to byte array
	@Override
	public byte[] serialize() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(4*4);
		IntBuffer intBuffer = byteBuffer.asIntBuffer();
		intBuffer.put(this.requestId);
		intBuffer.put(this.serviceId);
		intBuffer.put(this.facilityId);
		intBuffer.put(this.optionalId);
		
		return byteBuffer.array();
	}
	
	// get and set method
	public int getRequestId() {
		return this.requestId;
	}
	
	public int getServiceId() {
		return this.serviceId;
	}
	
	public int getFacilityId() {
		return this.facilityId;
	}
	
	public int getOptionalId() {
		return this.optionalId;
	}
	
	public static void main(String [] args) {
		int[] data = { 100, 201, 300, 400 };

        ByteBuffer byteBuffer = ByteBuffer.allocate(data.length * 4);        
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        for(int i = 0; i < data.length; i++) {
        	intBuffer.put(data[i]);
        }
        

        byte[] array = byteBuffer.array();

        ByteBuffer byteDebuf = ByteBuffer.allocate(array.length);
        byteDebuf.wrap(array);
        
        IntBuffer intDebuf = byteDebuf.asIntBuffer();
        int[] returnData = {1,2,3,4};
        
        for(int i = 0; i < 4; i++) {
	       int extracted = ByteBuffer.wrap(array, i * 4, 4).getInt();
	       System.out.println(extracted);
        }
	}
	
}
