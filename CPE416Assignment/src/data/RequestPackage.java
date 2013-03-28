package data;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/*
 * Class RequestPackage
 * Purpose: package that is used to send to server from client
 * Including: request Id of customer
 * Including: service Id of the requested service
 * Including: facility Id
 * Including: optional Id for some service*/
public class RequestPackage implements Serializable {
	
	// constant list of all service 
	public static final int SERVICE_SPEC = 0;
	public static final int SERVICE_QUERY = 1;
	public static final int SERVICE_BOOK = 2;
	public static final int SERVICE_CHANGE = 3;
	public static final int SERVICE_MONITOR = 4;
	public static final int SERVICE_PROGRAM = 5;
	public static final int SERVICE_REMOVE_ALL = 6;
	public static final int SERVICE_REMOVE_LAST = 7;
	
	private int requestId;
	private int serviceId;
	private int facilityId;
	private int optionalId;
	
	// constructor
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
	// get request id of this package
	public int getRequestId() {
		return this.requestId;
	}
	
	// get service id of this package
	public int getServiceId() {
		return this.serviceId;
	}
	
	// get facility id of this package
	public int getFacilityId() {
		return this.facilityId;
	}
	
	// get optional id of this package
	public int getOptionalId() {
		return this.optionalId;
	}
	
	// get text string information of this package
	public String toString() {
		return "Request:" + this.requestId + 
			 ", Service: " + this.serviceId + 
			 ", Facility: " + this.facilityId + 
			 ", OptionalId :" + this.optionalId;
	}
	
}
