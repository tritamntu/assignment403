package bookingserver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

import data.RequestPackage;

public class RequestMessage {
	private RequestPackage request;
	private InetAddress clientAddr;
	private int clientPort;
	private Date createdTime;
	private byte [] dataBuffer;
	
	public RequestMessage(RequestPackage request, InetAddress clientAddr, int port) 
			throws UnknownHostException {
		this.request = new RequestPackage(request.getRequestId(), 
										  request.getServiceId(),
										  request.getFacilityId(),
										  request.getOptionalId());
		this.clientAddr = InetAddress.getByAddress(clientAddr.getAddress());
		this.clientPort = port;
		this.createdTime = new Date();
	}
	
	// get and set method
	public int getRequestId() {
		return this.request.getRequestId();
	}
	
	public String getClientAddress() {
		return this.clientAddr.getHostAddress();
	}
	
	public int getPort() {
		return this.clientPort;
	}
	
	public String getCreatedTimeString() {
		return this.createdTime.toString();
	}
	
	public long getCreateTime() {
		return this.createdTime.getTime();
	}
	
	public void setBuffer(byte[] dataBuffer) {
		this.dataBuffer = new byte[dataBuffer.length];
		System.arraycopy(dataBuffer, 0, this.dataBuffer, 0, dataBuffer.length);
	}
	
	public byte[] getDataBuffer() {
		return this.dataBuffer;
	}
	
	public String toString() {
		return clientAddr.getHostAddress() + ":" + clientPort + ", " + this.request.toString();
	}
}
