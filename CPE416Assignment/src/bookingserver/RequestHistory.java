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
	
	public void addRequestMessage(RequestPackage request, InetAddress clientAddr, int port) 
			throws UnknownHostException {
		RequestMessage requestMessage = new RequestMessage(request, clientAddr, port);
		this.requestList.add(requestMessage);
	}
	
	public boolean searchRequest(InetAddress clientAddr, int port, int requestId) {
		for(int i = 0; i < requestList.size(); i++) {
			RequestMessage request = this.requestList.get(i);
			if(request.getClientAddress().equalsIgnoreCase(clientAddr.getHostAddress()) 
			   && request.getPort() == port
			   && request.getRequestId() == requestId)
				return true;
		}
		return false;
	}
}
