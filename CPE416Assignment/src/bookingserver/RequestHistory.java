package bookingserver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import data.RequestPackage;

public class RequestHistory {
	ArrayList<RequestMessage> requestList;
	
	public RequestHistory() {
		requestList = new ArrayList<RequestMessage>();
	}
	
	public int addRequestMessage(RequestPackage request, InetAddress clientAddr, int port) 
			throws UnknownHostException {
		int index = this.searchRequest(clientAddr, port, request.getRequestId());
		if(index != -1) { 
			RequestMessage requestMessage = new RequestMessage(request, clientAddr, port);
			this.requestList.add(requestMessage);
		} 
		return index;
	}
	
	public void addMessage(RequestPackage request, InetAddress clientAddr, int port) 
			throws UnknownHostException {
		RequestMessage requestMessage = new RequestMessage(request, clientAddr, port);
		this.requestList.add(requestMessage);
	}
	
	public void addMessage(RequestMessage requestMessage) {
		if(requestMessage != null) {
			this.requestList.add(requestMessage);
		}
	}
	
	public int searchRequest(InetAddress clientAddr, int port, int requestId) {
		// return -1 if no duplicate
		for(int i = 0; i < requestList.size(); i++) {
			RequestMessage request = this.requestList.get(i);
			if(request.getClientAddress().equalsIgnoreCase(clientAddr.getHostAddress()) 
			   && request.getPort() == port
			   && request.getRequestId() == requestId)
				return i;
		}
		return -1;
	}
	
	public RequestMessage getMessage(int index) {
		return this.requestList.get(index);
	}
	
	
}
