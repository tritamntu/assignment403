package bookingserver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

import data.RequestPackage;

/*
 * Class: RequestMessage 
 * Purpose: hold information of a client request
 * Including: RequestPackage, client address, client port
 * Including: data buffer of reply message 
 * */
public class RequestMessage {
	private RequestPackage request;
	private InetAddress clientAddr;
	private int clientPort;
	private byte [] dataBuffer;
	
	// constructor
	public RequestMessage(RequestPackage request, InetAddress clientAddr, int port) 
			throws UnknownHostException {
		this.request = new RequestPackage(request.getRequestId(), 
										  request.getServiceId(),
										  request.getFacilityId(),
										  request.getOptionalId());
		this.clientAddr = InetAddress.getByAddress(clientAddr.getAddress());
		this.clientPort = port;
	}
	
	// get and set method
	// get request id of the message
	public int getRequestId() {
		return this.request.getRequestId();
	}
	
	// get client address
	public String getClientAddress() {
		return this.clientAddr.getHostAddress();
	}
	
	// get client port
	public int getPort() {
		return this.clientPort;
	}
	
	// copy new data buffer from parameter
	public void setBuffer(byte[] dataBuffer) {
		this.dataBuffer = new byte[dataBuffer.length];
		System.arraycopy(dataBuffer, 0, this.dataBuffer, 0, dataBuffer.length);
	}
	
	// get data buffer array
	public byte[] getDataBuffer() {
		return this.dataBuffer;
	}
	
	// get text information of this Request Message
	public String toString() {
		return clientAddr.getHostAddress() + ":" + clientPort + ", " + this.request.toString();
	}
}
