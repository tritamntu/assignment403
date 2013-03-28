package bookingserver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import data.RequestPackage;

/*
 * Class: RequestHistory
 * Purpose: story list successful requests from client*/
public class RequestHistory {
	
	ArrayList<RequestMessage> requestList;
	
	// constructor
	public RequestHistory() {
		requestList = new ArrayList<RequestMessage>();
	}
	
	// add a new request to the list
	// return: -1 if the new request is not a duplicate
	// return index of the duplicate request
	public int addRequestMessage(RequestPackage request, InetAddress clientAddr, int port) 
			throws UnknownHostException {
		int index = this.searchRequest(clientAddr, port, request.getRequestId());
		if(index != -1) { 
			RequestMessage requestMessage = new RequestMessage(request, clientAddr, port);
			this.requestList.add(requestMessage);
		} 
		return index;
	}
	
	// raw add message method
	public void addMessage(RequestPackage request, InetAddress clientAddr, int port) 
			throws UnknownHostException {
		RequestMessage requestMessage = new RequestMessage(request, clientAddr, port);
		this.requestList.add(requestMessage);
	}
	
	// raw add message method
	public void addMessage(RequestMessage requestMessage) {
		if(requestMessage != null) {
			this.requestList.add(requestMessage);
		}
	}
	
	// search for a duplicate request
	// return: -1 if no duplicate
	// return: index if there is a duplicate
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
	
	// get RequestMessage by index
	public RequestMessage getMessage(int index) {
		return this.requestList.get(index);
	}
	
	
}
