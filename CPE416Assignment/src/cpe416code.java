public class BookingServer {
	
	// semantics constant values
	public static final int AT_LEAST_ONCE = 1;
	public static final int AT_MOST_ONCE = 2;
	
	static int port = 2000;
	static DatagramSocket socket;
	static DatagramPacket receivePacket;
	static DatagramPacket sendPacket;
	static int statusCode; 
	static Facility[] fList;
	static byte [] receiveBuffer;
	static byte [] dataBuffer;
	static RequestHistory history;
	static int sematicsCode = BookingServer.AT_LEAST_ONCE;
	static int lastValue = -1;
	static int lastService = -1;
	static ServerUI window;
	static int ackLossRate = 0;
	static int dataLossRate = 0;
	static Date startTime;

	public static void main(String [] args) {
		try {
			// 1. initialize Facility and Network Socket
			startTime = new Date();
			window = new ServerUI();
			window.setVisible(true);
			createFacilities();
			socket = new DatagramSocket(port);
			receiveBuffer = new byte[500];
			receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
			history = new RequestHistory();
			// 2. start listening to request
			while(true) {
				window.appendTextLine("Waiting for a request ..");
				// 2.1 receive request
				BookingServer.socket.setSoTimeout(0);
				BookingServer.socket.receive(receivePacket);
					// extract client address and port
				InetAddress clientAddr = receivePacket.getAddress();
				int clientPort = receivePacket.getPort();
				receiveBuffer = receivePacket.getData();
					// extract RequestPackage arguments
				RequestPackage clientRequest = new RequestPackage(
						ByteBuffer.wrap(receiveBuffer, 0 , 4).getInt(),
						ByteBuffer.wrap(receiveBuffer, 4 , 4).getInt(),
						ByteBuffer.wrap(receiveBuffer, 8 , 4).getInt(),
						ByteBuffer.wrap(receiveBuffer, 12 , 4).getInt());
				
				window.appendTextLine("Request from: " + clientAddr.getHostAddress() + ":" + clientPort);
				window.appendText("Service Id = " + clientRequest.getServiceId());
				window.appendText(", Facility Id = " + clientRequest.getFacilityId());
				window.appendText(", Request Id = " + clientRequest.getRequestId());
				window.appendTextLine(", Optional Id = " + clientRequest.getOptionalId());
				// 2.2 check if service can be served or not
				int ackCode;
				RequestMessage message = null;
				if(clientRequest.getServiceId() >= RequestPackage.SERVICE_SPEC
						&& clientRequest.getServiceId() <= RequestPackage.SERVICE_REMOVE_LAST) {
					ackCode = StatusCode.ACKNOWLEDGEMENT;
				} else { 
					ackCode = StatusCode.ACKNOWLEDGEMENT_FAILED;
				}
				// 2.2 * check duplicate and handle request
				int index = BookingServer.history.searchRequest(clientAddr, clientPort, clientRequest.getRequestId());
				if(index != -1 && BookingServer.sematicsCode == BookingServer.AT_MOST_ONCE) {
					// handle duplicate
					System.out.println("Client Duplicate Request: ");
					message = BookingServer.history.getMessage(index);
					System.out.println(message.toString());
					// send request message to client
					ackCode = StatusCode.REQUEST_DUPLICATE;
				}
				// 2.2 * send acknowledgment to client
				ReplyPackage rp = new ReplyPackage(ackCode);
				if(ackCode == StatusCode.REQUEST_DUPLICATE && sematicsCode == BookingServer.AT_MOST_ONCE) 
					dataBuffer = rp.serialize(message.getDataBuffer());
				else dataBuffer = rp.serialize();
				BookingServer.sendPacket = new DatagramPacket(dataBuffer, dataBuffer.length, clientAddr, clientPort);
				// BookingServer.socket.send(BookingServer.sendPacket);
				BookingServer.sendWithLoss(ackLossRate);
				switch(ackCode) {
				case StatusCode.ACKNOWLEDGEMENT:
					window.appendTextLine("StatusCode = ACKNOWLEDGEMENT");
					break;
				case StatusCode.ACKNOWLEDGEMENT_FAILED:
					window.appendTextLine("StatusCode = ACKNOWLEDGEMENT_FAILED");
					break;
				case StatusCode.REQUEST_DUPLICATE:
					window.appendTextLine("StatusCode = REQUEST_DUPLICATE");
					break;
				}
				if(ackCode == StatusCode.ACKNOWLEDGEMENT_FAILED || 
						(ackCode == StatusCode.REQUEST_DUPLICATE && sematicsCode == BookingServer.AT_MOST_ONCE)) {
					window.appendTextLine("Request handler is going to end due to Fail Acknoledgment or Request Duplicate");
					BookingServer.printHandlerClosing();
					continue;
				}
				// 2.3 receive data package from client and execute command
				TimePoint startTime = null;
				Duration interval = null;
				socket.setSoTimeout(500);
				window.appendTextLine("Receiving Data Package and Execute Handler");
				try {
				switch(clientRequest.getServiceId()) {
				case RequestPackage.SERVICE_QUERY: 
					// service 1 query Availability
					socket.receive(receivePacket);
					dataBuffer = receivePacket.getData();
					startTime = DataPackage.extractTimePoint(dataBuffer, 0);
					window.appendTextLine("Service Query Availability: ");
					window.appendTextLine("\tFacility: " + clientRequest.getFacilityId());
					window.appendTextLine("\tStartTime: " + startTime.toString());
					BookingServer.queryAvailibity(clientRequest.getFacilityId(), startTime);
					break;
				case RequestPackage.SERVICE_BOOK: 
					// service 2 booking request
					socket.receive(receivePacket);
					dataBuffer = receivePacket.getData();
					startTime = DataPackage.extractTimePoint(dataBuffer, 0);
					interval = DataPackage.extractDuration(dataBuffer, 3 * 4);
					window.appendTextLine("Service Book Request: ");
					window.appendTextLine("\tStart time: " + startTime.toString());
					window.appendTextLine("\tDuration: " + interval.toString());
					window.appendTextLine("\tFacility id: " + clientRequest.getFacilityId());
					statusCode = BookingServer.bookRequest(clientRequest.getFacilityId(), startTime, interval, clientAddr, clientPort);
					if(clientRequest.getFacilityId() >= 0 && clientRequest.getFacilityId() < fList.length)
						System.out.println(fList[clientRequest.getFacilityId()].getBookSchedule());
					break;
				case RequestPackage.SERVICE_CHANGE: 
					// service 3 booking change
					socket.receive(receivePacket);
					dataBuffer = receivePacket.getData();
					int confirmationId = clientRequest.getOptionalId();
					interval = DataPackage.extractDuration(dataBuffer, 0);
					window.appendTextLine("Service Book Change: ");
					window.appendTextLine("\tConfirmId: " + confirmationId);
					window.appendTextLine("\tDuration:  " + interval.toString());
					statusCode = BookingServer.bookChange(clientRequest.getFacilityId(), confirmationId, interval);
					if(clientRequest.getFacilityId() >= 0 && clientRequest.getFacilityId() < fList.length)
						System.out.println(fList[clientRequest.getFacilityId()].getBookSchedule());
					break;
				case RequestPackage.SERVICE_MONITOR: 
					// service 4 monitor call back
					socket.receive(receivePacket);
					dataBuffer = receivePacket.getData();
					interval = DataPackage.extractDuration(dataBuffer, 0);
					window.appendTextLine("Service Monitor: ");
					window.appendTextLine("\tFacilityId: " + clientRequest.getFacilityId());
					window.appendTextLine("\tDuration:  " + interval.toString());
					BookingServer.monitor(clientRequest.getFacilityId(), 
							clientAddr, clientPort, interval);
					break;
				case RequestPackage.SERVICE_PROGRAM: 
					// service 5 run a program
					window.appendTextLine("Service Get Quote of The Day:");
					window.appendTextLine("\t QuoteId: " + clientRequest.getOptionalId());
					if (clientRequest.getServiceId() == lastService && clientRequest.getOptionalId() ==lastValue){
						runProgram(clientRequest.getOptionalId(), true);
					}
					else{
						runProgram(clientRequest.getOptionalId(), false);						
					}
					break;
				case RequestPackage.SERVICE_SPEC: 
					// get facility names
					window.appendTextLine("Service Get All Facility Name: ");
					BookingServer.queryFacilityList();
					break;
				case RequestPackage.SERVICE_REMOVE_ALL:
					window.appendTextLine("Service Remove All Booking Slot: ");
					window.appendTextLine("\tClient Address: " + clientAddr.getHostAddress());
					window.appendTextLine("\tClient Port: " + clientPort);
					BookingServer.removeAllSlots(clientRequest.getFacilityId(), clientAddr, clientPort);
					break;
				case RequestPackage.SERVICE_REMOVE_LAST:
					window.appendTextLine("Service Last Booking Slot: ");
					window.appendTextLine("\tClient Address: " + clientAddr.getHostAddress());
					window.appendTextLine("\tClient Port: " + clientPort);
					BookingServer.removeLastSlot(clientRequest.getFacilityId(), clientAddr, clientPort);
					break;
				} } catch (SocketTimeoutException e) {
					// Timeout: server can't receive data package from client, execution terminates
					window.appendTextLine("Timeout: Can't Receive Request");
					BookingServer.printHandlerClosing();
					continue;
				} 
				// 2.5 store request in history
				RequestMessage requestMessage = new RequestMessage(clientRequest, clientAddr, clientPort);
				DataPackage.printByteArray(dataBuffer);
				requestMessage.setBuffer(dataBuffer);
				BookingServer.history.addMessage(requestMessage);
				// 2.6 send data package to client
				BookingServer.sendPacket = new DatagramPacket(
						dataBuffer, dataBuffer.length, 
						clientAddr, clientPort);
				//BookingServer.socket.send(sendPacket);
				BookingServer.sendWithLoss(dataLossRate);
				// 2.7 callback if a booking slot is changed
				if(statusCode == StatusCode.SUCCESS_BOOKING
				|| statusCode == StatusCode.SUCCESS_BOOKING_CHANGE) {
					BookingServer.callback(clientRequest.getFacilityId());
				} 
					// reset statusCode
				statusCode = StatusCode.FACILITY_NOT_FOUND;
				BookingServer.printHandlerClosing();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block

		}
	}
	
	public static void printHandlerClosing() {
		window.appendTextLine("RequestHandler ends");
		window.appendTextLine("....................");
		window.appendTextLine("");
	}
	
	public static void createFacilities() {
		fList = new Facility[5];
		fList[0] = new Facility(0, "LectureRoom");
		fList[1] = new Facility(0, "LearningPod");
		fList[2] = new Facility(0, "RecordingRoom");
		fList[3] = new Facility(0, "Server1");
		fList[4] = new Facility(0, "Server2");
	}
	
	// service 1 queryAvailability
	public static int queryAvailibity(
			int facilityId, TimePoint startTime)  {
		System.out.println("Start Service 1: Query Availability");
		TimePoint nextTime = null;
		// 1. check availability and status code
		boolean available = false;
		if(facilityId >= 0 && facilityId < fList.length) {
			available = fList[facilityId].queryAvailibility(startTime);
			nextTime = fList[facilityId].getNextTime(startTime);
		} else {
			nextTime = null;
		}
		int statusCode = -1;
		if(available) statusCode = StatusCode.SUCCESS_AVAILABLE;
		else 		  statusCode = StatusCode.SUCCESS_NOTAVAILABLE;
		ReplyPackage replyPackage = new ReplyPackage(statusCode);	
		// 2. setup data package (TimePoint nextTime) to client
		dataBuffer = replyPackage.serialize(DataPackage.serialize(nextTime));	
		return statusCode;
	}
	
	// service 2 bookRequest 
	public static int bookRequest(
			int facilityId, TimePoint startTime, Duration interval, InetAddress clientAddr, int clientPort) 
					throws UnknownHostException {
		// 1. add slot to schedule
		System.out.println("Start Service 2: Book Request");
		int confirmId = -1;
		if(facilityId >= 0 && facilityId < fList.length) {
			confirmId = fList[facilityId].addSlot(new BookingSlot(startTime, interval, clientAddr, clientPort));
		} else {
			confirmId = -1;
		}
		int statusCode;
		if(confirmId == -1) 
			statusCode = StatusCode.SUCCESS_NOTAVAILABLE;
		else statusCode =StatusCode.SUCCESS_BOOKING;
		// 2. setup data package to reply
		ReplyPackage replyPackage = new ReplyPackage(statusCode);
		dataBuffer = replyPackage.serialize(DataPackage.serialize(confirmId));
		DataPackage.printByteArray(dataBuffer);
		window.appendTextLine(fList[facilityId].getBookSchedule());
		return statusCode;
	}
	
	// service 3 book change
	public static int bookChange(
			int facilityId, int confirmationId, Duration interval) throws UnknownHostException {
		// 1. change for a book record 
		System.out.println("Start Service 3: Booking Change");
		int statusCode = -1;
		int confirmId = -1;
		if(facilityId >= 0 && facilityId < fList.length) {
			confirmId = fList[facilityId].bookChange(confirmationId, interval);
		} else {
			confirmId = -1;
		}
		if(confirmId == -1) 
			statusCode = StatusCode.SUCCESS_NOTAVAILABLE;
		else statusCode = StatusCode.SUCCESS_BOOKING_CHANGE;
		// 2. setup reply data
		ReplyPackage replyPackage = new ReplyPackage(statusCode);
		dataBuffer = replyPackage.serialize(DataPackage.serialize(confirmId));
		window.appendTextLine(fList[facilityId].getBookSchedule());
		return statusCode;
	}
	
	// service 4 monitor
	public static int monitor(
			int facilityId,  InetAddress clientAddr, int clientPort, Duration interval) 
			throws UnknownHostException {
		// 1. add client to monitor list
		System.out.println("Start Service 4: Monitor");
		int statusCode = StatusCode.SUCCESS_ADD_MONITOR;
		// get the current timePoint
		TimePoint monitorEndTime = new TimePoint(getCurrentTimePoint(), interval);
		MonitorClient newClient = new MonitorClient(clientAddr, clientPort, monitorEndTime);
		if(facilityId >= 0 && facilityId < fList.length) {
			fList[facilityId].addMonitorClient(newClient);
		} else {
			statusCode = StatusCode.FACILITY_NOT_FOUND;
		}
		// 2. setup reply data
		ReplyPackage replyPackage = new ReplyPackage(statusCode);
		dataBuffer = replyPackage.serialize(null);
		return statusCode;
	}
	
	public static TimePoint getCurrentTimePoint() {
		Date currentTime = new Date();
		long secondDiff = (currentTime.getTime() - startTime.getTime()) / 1000L;
		int currentDay = (int) (secondDiff / (24 * 3600));
		int currentHour = (int) (secondDiff - currentDay * 24 * 3600) / (3600);
		int currentMin = (int) (secondDiff - currentDay * 24 * 3600 - currentHour * 3600) / 60;
		return new TimePoint(currentDay, currentHour, currentMin);
	}
	
	// service 4 call back monitor
	public static void callback(int facilityId) 
			throws IOException {
		System.out.println("Start Call back");
		ArrayList<MonitorClient> monitorList = fList[facilityId].getClientList();
		if(monitorList.size() > 0) {
		
			ArrayList<BookingSlot> slotList = fList[facilityId].getBookSlots();
			dataBuffer = DataPackage.serialize(slotList);
		
			DataPackage.printByteArray(dataBuffer);
			for(Iterator i = monitorList.iterator(); i.hasNext();) {
				TimePoint currentTimePoint = getCurrentTimePoint();
				MonitorClient client = (MonitorClient)i.next();
				if(client.finishMonitor(currentTimePoint)) {
					i.remove();
					window.appendTextLine("Remove " 
							+ client.getClientAddress() + ":" 
							+ client.getClientPort() 
							+ " as monitor interval ends.");
					continue;
				}
				InetAddress clientAddr = InetAddress.getByName(client.getClientAddress());
				int clientPort = client.getClientPort();
				window.appendTextLine("Monitor - client: " + clientAddr.getHostAddress() + ":" + clientPort);
				BookingServer.sendPacket = new DatagramPacket(
						dataBuffer, dataBuffer.length,
						clientAddr, clientPort);
				BookingServer.sendWithLoss(0);
			}
		} 
	}
	
	// service 5 query facility List
	public static int queryFacilityList() throws UnsupportedEncodingException {
		// 1. create string array of facility names
		int statusCode = StatusCode.SUCCESS_AVAILABLE;
		if(fList == null) {
			statusCode = StatusCode.SUCCESS_NOTAVAILABLE;
			ReplyPackage rp = new ReplyPackage(statusCode);
			dataBuffer = rp.serialize();
		} else {
			String[] strAr = new String[fList.length];
			for(int i = 0; i < strAr.length; i++)
				strAr[i] = fList[i].getDesc();
			ReplyPackage rp = new ReplyPackage(statusCode);
			// 2. setup data buffer to client
			dataBuffer = rp.serialize(DataPackage.serialize(strAr));
		}
		return statusCode;
	}
	
	// service 6 get quotes of the day
	public static int runProgram(int input, boolean runAgain) 
			throws UnsupportedEncodingException {
		String str = "nothing";
		int statusCode = StatusCode.SUCCESS_NOTAVAILABLE;
		if (input>9 ||input<0){
			str = "Please make sure that your number is between 0 an 9";
		}
		else if(input<=9 && input>=0){
			int output;
			if (runAgain == true){
				lastValue++;
				output = lastValue;
			}
			else{
				output = input;
				lastValue = output;
			}
			str = quote(output);
			System.out.println(str);
		}
		statusCode = StatusCode.SUCCESS_PROGRAM; 
		ReplyPackage rp = new ReplyPackage(statusCode);
		dataBuffer = rp.serialize(DataPackage.serialize(str));
		return statusCode;
	}
	
	// service 7 remove all slots
	public static int removeAllSlots(int facilityId, InetAddress clientAddr, int clientPort) {
		int statusCode = StatusCode.FACILITY_NOT_FOUND;
		if(facilityId < fList.length && facilityId >= 0) {
			fList[facilityId].removeAllSlot(clientAddr, clientPort);
			statusCode = StatusCode.SUCCESS_REMOVE;
			window.appendTextLine(fList[facilityId].getBookSchedule());
		} 
		ReplyPackage rp = new ReplyPackage(statusCode);
		dataBuffer = rp.serialize();
		return statusCode;
	}
	
	// service 8 remove the latest slot
	public static int removeLastSlot(int facilityId, InetAddress clientAddr, int clientPort) {
		int statusCode = StatusCode.FACILITY_NOT_FOUND;
		if(facilityId < fList.length && facilityId >= 0) {
			BookingSlot slot = fList[facilityId].removeLastSlot(clientAddr, clientPort);
			if(slot != null)
				statusCode = StatusCode.SUCCESS_REMOVE;
			else
				statusCode = StatusCode.SUCCESS_EMPTY;
			window.appendTextLine(fList[facilityId].getBookSchedule());
		} 
		ReplyPackage rp = new ReplyPackage(statusCode);
		dataBuffer = rp.serialize();
		return statusCode;
	}
	
	// method get the quotes
	public static String quote(int output){
		String[] quotes = new String [10];
		quotes[0] = "Quote of the day: A day without sunshine is like, you know, night.!!!";
		quotes[1] = "Quote of the day: Oh, love will make a dog howl in rhyme.!!!";
		quotes[2] = "Quote of the day: Do not take life too seriously. You will never get out of it alive.!!!";
		quotes[3] = "Quote of the day: Weather forecast for tonight: dark.!!!";
		quotes[4] = "Quote of the day: I found there was only one way to look thin: hang out with fat people.!!!";
		quotes[5] = "Quote of the day: I intend to live forever. So far, so good.!!!";
		quotes[6] = "Quote of the day: All generalizations are false, including this one.!!!";
		quotes[7] = "Quote of the day: Why do they call it rush hour when nothing moves?!!!";
		quotes[8] = "Quote of the day: They say marriages are made in Heaven. But so is thunder and lightning.!!!";
		quotes[9] = "Quote of the day: If you have a secret, people will sit a little bit closer.!!!";
		return quotes[output];
	}
	
	// method to change semantics code
	// used in UI classes
	public static void changeSemantics(int code) {
		BookingServer.sematicsCode = code;
	}

	// method to send with a lost percent
	private static void sendWithLoss(int lostPercent) throws IOException {
		int randomNum = (int) (100 * Math.random());
		if(randomNum > lostPercent) {
			BookingServer.socket.send(sendPacket);
		} else {
			window.appendTextLine("Sending packet was lost by simulation ... :( ");
		}
	}

	// method to change loss rate
	// used in UI classes
	public static void changeLostRate(int ackRate, int dataRate) {
		// TODO Auto-generated method stub
		ackLossRate = ackRate;
		dataLossRate = dataRate;
		window.appendTextLine("New Acknowledgment Loss Rate = " + ackLossRate);
		window.appendTextLine("New Data Loss Rate = " + dataLossRate);
	}
}

/*
 * Class: RequestHistory
 * Purpose: store list successful requests from client*/
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

/*
 * Class: ServerUI
 * Purpose: provide user interface to server app
 * Including: a text area that show messages
 * Including: configuration panel
 * */
public class ServerUI extends JFrame{

	JTextArea textArea;
	JComboBox semanticsCombo;
	JTextArea ackLossRate;
	JTextArea dataLossRate;
	String [] semantics = {"AT_LEAST_ONCE", "AT_MOST_ONCE"};
	
	// contrustor
	public ServerUI() {
		initUI();
	}

	public final void initUI() {
		// create content panel
		JPanel panel = new JPanel();
		this.getContentPane().add(panel);
	    panel.setLayout(new BorderLayout());
	    // create text area
	    this.textArea = new JTextArea();
	    this.textArea.setEditable(false);
	    this.add( new JScrollPane( this.textArea ), BorderLayout.CENTER);
        
        // add configuration panel
	    JPanel confPanel = new JPanel();
	    confPanel.setLayout(new GridLayout(10,1));
	    semanticsCombo = new JComboBox(semantics);
	    confPanel.add(semanticsCombo);
	    JLabel ackLabel = new JLabel("Ack Loss Rate");
	    confPanel.add(ackLabel);
	    ackLossRate = new JTextArea();
	    confPanel.add(ackLossRate);
	    JLabel dataLable = new JLabel("Data Loss Rate");
	    confPanel.add(dataLable);
	    dataLossRate = new JTextArea();
	    confPanel.add(dataLossRate);
	    JButton changeBtn = new JButton("ChangeConfiguration");
	    changeBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				getFormValues();
			}	    	
	    });
	    confPanel.add(changeBtn);
	    this.add(confPanel, BorderLayout.WEST);
        // set property
		this.setTitle("Server User Interface");
		this.setSize(800, 600);
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
	}
	
	// append text to the text area
	public void appendText(String str) {
		this.textArea.append(str);
	}
	
	// append new text line to the area
	public void appendTextLine(String str) {
		this.textArea.append(str + "\n");
	}
	
	// change values from the configuration panel
	private void getFormValues() {
		String str = (String)semanticsCombo.getSelectedItem();
		int semanticCode;
		if(str.equals(semantics[0])) {
			semanticCode = BookingServer.AT_LEAST_ONCE;
		} else {
			semanticCode = BookingServer.AT_MOST_ONCE;
		}
		this.appendTextLine("Change semantics code to " + str);
		int ackRate, dataRate;
		if(!this.ackLossRate.getText().equals(""))
			ackRate = Integer.parseInt(this.ackLossRate.getText());
		else ackRate =0;
		if(!this.dataLossRate.getText().equals(""))
			dataRate = Integer.parseInt(this.dataLossRate.getText());
		else dataRate = 0;
		BookingServer.changeSemantics(semanticCode);
		BookingServer.changeLostRate(ackRate, dataRate);
	}
	
	public static void main(String[] args) {
	    ServerUI graphics = new ServerUI();
	    graphics.setVisible(true);
	}
}

/*
 * Class: BookingClient
 * Purpose: Main Program of Client Side
 * */
public class BookingClient {

	// constant data for ClientUI
	public static final String[] dayList = {
		"Monday", "Tuesday", "Wednesday", "Thursday", 
		"Friday", "Saturday", "Sunday"};
	public static final String[] hourList = { "00",
		"01", "02", "03", "04", "05", "06",
		"07", "08", "09", "10", "11", "12",
		"13", "14", "15", "16", "17", "18",
		"19", "20", "21", "22", "23"};
	public static final String[] minList = {"0", "15", "30", "45"};
	public static final String[] weekDayList = {"0","1","2","3","4","5","6"};
	public static final int MAX_TIMEOUT = 8;
	
	// global data objects
	public static String[] facilityName = {};
	static ArrayList<Integer> confirmIdList;
	// global UDP connection objects
	static DatagramSocket socket;
	static DatagramPacket sendPacket;
	static DatagramPacket receivePacket; 
	static InetAddress serverAddr;
	static int serverPort = 2000;
	static int clientPort = 2002;
	static byte[] sendBuffer;
	static byte[] receiveBuffer;
	static int requestId;
	static Scanner sc = new Scanner(System.in);
	// user interface
	static ClientUI window;
	public static boolean stopMonitor;
	static int ackTimeoutCount;
	static int dataTimeoutCount;
	
	public static void main(String [] args) {
		try {
			BookingClient.init();
			while(true) {
			}
		} catch (SocketException | UnknownHostException e) {
			e.printStackTrace();
		}		
	}
	
	// initiate attributes of client program
	public static void init() 
			throws SocketException, UnknownHostException {
		System.out.println("Init");
		// initialize UDP connection objects
		sendBuffer = new byte[500];
		receiveBuffer = new byte[500];
		receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
		socket = new DatagramSocket(clientPort);
		serverAddr = InetAddress.getByName("127.0.0.1");
		// initialize data objects
		confirmIdList = new ArrayList<Integer>();
		requestId = 1;
		ackTimeoutCount = 0;
		dataTimeoutCount = 0;
		// initialize user interface
		window = new ClientUI();
		System.out.println("Setup window done");
		window.setVisible(true);
	}
	
	// send a request to server
	public static int sendRequest(int serviceId, int facilityId, int optionalId, TimePoint tp, Duration dr) 
			throws SocketException {
		Boolean sending = true;
		dataTimeoutCount = 0;
		socket.setSoTimeout(750);
		int statusCode = StatusCode.SUCCESS_NOTAVAILABLE;
		// condition of sending:
		// timeout = 800 msec, max number of repeat = MAX_TIMEOUT
		while(sending && dataTimeoutCount <= BookingClient.MAX_TIMEOUT) {
			dataTimeoutCount++;
			statusCode = StatusCode.SUCCESS_NOTAVAILABLE;
			try {
				window.appendTextLine(BookingClient.getServiceName(serviceId));
				// 1. send request package
				sendRequestPackage(serviceId, facilityId, optionalId, 00);
				// 2. receive acknowledgment package
				statusCode = receiveAckPackage();
				System.out.println("StatusCode " + statusCode + ";");
				if(statusCode == StatusCode.ACKNOWLEDGEMENT_FAILED) {
					window.appendTextLine("Ack: Failed Acknowedgment From Server");
					window.appendTextLine("End Request .................");
					window.appendTextLine("");
					return statusCode;
				}
				// 3. send data package if the request is not a duplicate
				if(statusCode != StatusCode.REQUEST_DUPLICATE) {
					sendDataPackage(serviceId, tp, dr, 00);
				// 4. receive data package if the request is not a duplicate
					socket.receive(receivePacket);
					receiveBuffer = receivePacket.getData();
					statusCode = ByteBuffer.wrap(receiveBuffer, 0, 4).getInt();
				}
				// 5. process data package and get result
				processDataPackage(serviceId, statusCode);
				if(serviceId == RequestPackage.SERVICE_MONITOR) {
					// if the service is monitor call back, continue to read data
					stopMonitor = false;
					long monitorSecs = dr.getDay() * 24 * 3600 + dr.getHour() * 3600 + dr.getMin() * 60;
					Date startMonitorTime = new Date();
					while(!stopMonitor) {
						socket.setSoTimeout(500);
						try {
							socket.receive(receivePacket);
							receiveBuffer = receivePacket.getData();
							window.appendTextLine("Monitor: receive data from server");
							DataPackage.printByteArray(receiveBuffer);
							ArrayList<BookingSlot> slotList = DataPackage.extractSlotList(receiveBuffer, 0);
							window.appendTextLine("Monitor : size = " + slotList.size());
							for(int i = 0; i < slotList.size(); i++) {
								BookingSlot slot = slotList.get(i);
								window.appendTextLine(slot.toString());
							}
						} catch (SocketTimeoutException e ) {
							Date currentTime = new Date();
							long secDiff = (currentTime.getTime() - startMonitorTime.getTime()) / 1000L;
							if(secDiff >= monitorSecs) {
								stopMonitor = true;
								window.appendTextLine("Stop Monitor Process");
							}
							continue;
						}
					}
				}
				sending = false;
			} catch (SocketTimeoutException e) {
				// print time out message
				window.appendTextLine("Timeout : " + dataTimeoutCount);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// if the timeout exceeds MAX COUNT, server is not available
		if(sending) {
			statusCode = StatusCode.SERVER_NOT_AVAILABLE;
			window.appendTextLine("Error: Server Not Available, Try Again Later");
		}
		dataTimeoutCount = 0;
		window.appendTextLine("End Request .................");
		window.appendTextLine("");
		requestId++;
		return statusCode;
	}
	
	// method to send request package to server
	public static void sendRequestPackage(int serviceId, int facilityId, int optionalId, int lostPercent) 
			throws IOException {
		RequestPackage rp = new RequestPackage(requestId, serviceId, facilityId, optionalId);
		sendBuffer = rp.serialize();
		sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, serverAddr, serverPort);
		// send with a lost percent
		BookingClient.sendWithLoss(lostPercent);
	}
	
	// method to receive acknowledgment package from server
	public static int receiveAckPackage() 
			throws IOException, SocketTimeoutException {
		socket.receive(receivePacket);
		receiveBuffer = receivePacket.getData();
		return ByteBuffer.wrap(receiveBuffer, 0, 4).getInt();
	}
	
	// method to send data package to send
	public static void sendDataPackage(int serviceId, TimePoint tp, Duration dr, int lostPercent) 
			throws IOException {
		switch(serviceId) {
		case RequestPackage.SERVICE_QUERY:
			sendBuffer = DataPackage.serialize(tp);
			break;
		case RequestPackage.SERVICE_BOOK:
			sendBuffer = DataPackage.serialize(tp, dr);
			break;
		case RequestPackage.SERVICE_CHANGE:
			sendBuffer = DataPackage.serialize(dr);
			break;
		case RequestPackage.SERVICE_MONITOR:
			sendBuffer = DataPackage.serialize(dr);
			break;
		case RequestPackage.SERVICE_PROGRAM:
			sendBuffer = null;
			break;
		case RequestPackage.SERVICE_SPEC:
			sendBuffer = null;
			break;
		case RequestPackage.SERVICE_REMOVE_ALL:
			sendBuffer = null;
			break;
		case RequestPackage.SERVICE_REMOVE_LAST:
			sendBuffer = null;
			break;
		}
		
		if(sendBuffer != null) {
			sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, serverAddr, serverPort);
			//socket.send(sendPacket);
			BookingClient.sendWithLoss(lostPercent);
		}
	}
	
	// method to process data package from server
	public static void processDataPackage(int serviceId, int statusCode) {
		int confirmId = 0;
		TimePoint nextTime = null;
		switch(serviceId) {
		case RequestPackage.SERVICE_QUERY:
			
			if(statusCode == StatusCode.SUCCESS_AVAILABLE) {
				nextTime = DataPackage.extractTimePoint(receiveBuffer, 4);
				window.appendTextLine("Query: The Facility is Available.");
				window.appendTextLine("Query: The next occupied time slot is: " + nextTime.toString());
			} else if(statusCode == StatusCode.SUCCESS_NOTAVAILABLE){
				nextTime = DataPackage.extractTimePoint(receiveBuffer, 4);
				window.appendTextLine("Query: The Facility is not Available.");
				window.appendTextLine("Query: The next available time slot is: " + nextTime.toString());
			} else if(statusCode == StatusCode.REQUEST_DUPLICATE) {
				nextTime = DataPackage.extractTimePoint(receiveBuffer, 8);
				window.appendTextLine("Duplicate Request: The Facility's availability is unknown.");
				window.appendTextLine("The next available/occupied time slot is: " + nextTime.toString());
			}
			break;
			
		case RequestPackage.SERVICE_BOOK:
			
			if(statusCode == StatusCode.SUCCESS_BOOKING) {
				confirmId = DataPackage.extractInt(receiveBuffer, 4);
				window.appendTextLine("Booking: Booking was successful, ConfirmationID = " + confirmId);
			} else if(statusCode == StatusCode.SUCCESS_NOTAVAILABLE){
				window.appendTextLine("Booking: Booking was failed due to time violation with other booking slots!");
			} else if(statusCode == StatusCode.REQUEST_DUPLICATE) {
				confirmId = DataPackage.extractInt(receiveBuffer, 8);
				window.appendText("Duplicate Request: ");
				if(confirmId > 0)
					window.appendTextLine("Booking was successful, ConfirmationID = " + confirmId);
				else window.appendTextLine("Boooking was unsuccessful.");
			}
			break;
			
		case RequestPackage.SERVICE_CHANGE:
			
			if(statusCode == StatusCode.SUCCESS_BOOKING_CHANGE) {
				confirmId = DataPackage.extractInt(receiveBuffer, 4);
				window.appendTextLine("Change: Booking change was successful, new ConfirmationID = " + confirmId);
			} else if (statusCode == StatusCode.SUCCESS_NOTAVAILABLE) {
				window.appendTextLine("Change: Booking change was failed due to time violation or empty booking slots!");
			} else if(statusCode == StatusCode.REQUEST_DUPLICATE) {
				confirmId = DataPackage.extractInt(receiveBuffer, 8);
				window.appendText("Duplicate Request: ");
				if(confirmId > 0)
					window.appendTextLine("Booking change was successful, ConfirmationID = " + confirmId);
				else window.appendTextLine("Boooking change was unsuccessful.");
			}
			break;
			
		case RequestPackage.SERVICE_MONITOR:
			if(statusCode == StatusCode.SUCCESS_ADD_MONITOR) {
				window.appendTextLine("Monitor: successful continue receive");
			} else if(statusCode == StatusCode.SUCCESS_NOTAVAILABLE) {
				window.appendTextLine("Monitor: Not Available Facility");
			} else if(statusCode == StatusCode.REQUEST_DUPLICATE) {
				window.appendTextLine("Duplicate Request: Please Send a New Request");
			}
			break;
		case RequestPackage.SERVICE_PROGRAM:
			String str= "";
			if(statusCode == StatusCode.SUCCESS_PROGRAM) {
				str = DataPackage.extractString(receiveBuffer, 4);
				window.appendTextLine("Quotes of the day");
				window.appendTextLine(str);
			} else if(statusCode == StatusCode.SERVER_NOT_AVAILABLE) {
				window.appendTextLine("Quotes of the day is unavailable");
			} else if(statusCode == StatusCode.REQUEST_DUPLICATE) {
				str = DataPackage.extractString(receiveBuffer, 8);
				window.appendTextLine("Duplicate Request: ");
				if(str != null && !str.equals(""))
					window.appendTextLine(str);
			}
			break;
		case RequestPackage.SERVICE_SPEC:
			if(statusCode == StatusCode.SUCCESS_AVAILABLE) {
				facilityName = DataPackage.extractStringList(receiveBuffer, 4);
				window.appendTextLine("Facility Name List:");
				for(int i = 0; i < facilityName.length; i++) {
					window.appendTextLine((i+1) + ": " + facilityName[i]);
				}
			} else if(statusCode == StatusCode.SERVER_NOT_AVAILABLE) {
				window.appendTextLine("Quotes of the day is unavailable");
			} else if(statusCode == StatusCode.REQUEST_DUPLICATE) {
				facilityName = DataPackage.extractStringList(receiveBuffer, 8);
				window.appendTextLine("Duplicate Request: Please Send a New One");
				window.appendTextLine("Facility Name List:");
				for(int i = 0; i < facilityName.length; i++) {
					window.appendTextLine((i+1) + ": " + facilityName[i]);
				}
			}
			break;
		case RequestPackage.SERVICE_REMOVE_ALL:
			if(statusCode == StatusCode.SUCCESS_REMOVE) {
				window.appendTextLine("Remove: all slots booked by this client have been removed");
			} else if(statusCode == StatusCode.FACILITY_NOT_FOUND) {
				window.appendTextLine("Remove: Error of not found facility");
			} else if(statusCode == StatusCode.REQUEST_DUPLICATE) {
				window.appendTextLine("Duplicate : All records have been removed");
			}
			break;
		case RequestPackage.SERVICE_REMOVE_LAST:
			if(statusCode == StatusCode.SUCCESS_REMOVE) {
				window.appendTextLine("Remove: The latest booked by this client have been removed");
			} else if(statusCode == StatusCode.SUCCESS_EMPTY) {
				window.appendTextLine("Remove: Empty Booking Slots");
			} else if(statusCode == StatusCode.REQUEST_DUPLICATE) {
				window.appendTextLine("Duplicate : The latest slot has been removed");
			}
			break;
		}
	}
	
	// method to get service name from server
	public static String getServiceName(int serviceId) {
		String str = "";
		switch(serviceId) {
		case RequestPackage.SERVICE_SPEC:
			return "Service 0: Query Facility List";
		case RequestPackage.SERVICE_QUERY:
			return "Service 1: Query Availability";
		case RequestPackage.SERVICE_BOOK:
			return "Service 2: Request Booking Slot";
		case RequestPackage.SERVICE_CHANGE:
			return "Service 3: Change Booking Slot";
		case RequestPackage.SERVICE_MONITOR:
			return "Service 4: Monitor Call Back";
		case RequestPackage.SERVICE_PROGRAM:
			return "Service 5: Get A Quote";
		case RequestPackage.SERVICE_REMOVE_ALL:
			return "Service 6: Remove All Slots";
		case RequestPackage.SERVICE_REMOVE_LAST:
			return "Service 7: Remove Latest Booking Slot";
		}
		return str;
	}
	
	// method to get day index
	// used for UI classes
	public static int getDayIndex(String day) {
		for(int i = 0; i < BookingClient.dayList.length; i++) {
			if(day.equalsIgnoreCase(BookingClient.dayList[i]))
				return i;
		}
		return -1;
	}
	
	// method to get facility index
	// used for UI classes
	public static int getFacilityIndex(String f) {
		for(int i = 0; i < BookingClient.facilityName.length; i++) {
			if(f.equalsIgnoreCase(BookingClient.facilityName[i]))
				return i;
		}
		return -1;
	}
	
	// method to change server
	// used in UI classes
	public static void changeServer(String ipAddr, int port) throws UnknownHostException {
		serverAddr = InetAddress.getByName(ipAddr);
		serverPort = port;
		window.appendTextLine("Address changed: " + serverAddr.getHostAddress() + ":" + serverPort);
	}
	
	// send package with loss
	// send package if random number is higher that lostPercent values
	// else, print a message
	private static void sendWithLoss(int lostPercent) throws IOException {
		int randomNum = (int) (100 * Math.random());
		if(randomNum > lostPercent) {
			BookingClient.socket.send(sendPacket);
		} else {
			window.appendTextLine("Sending packet was lost by simulation ... :( ");
		}
	}
}

/*
 * Class: ClientUI
 * Purpose: provide user interface to client app
 * Including: a text area that show messages
 * Including: panel of service button
 * */
public class ClientUI extends JFrame {
	
	JTextArea textArea;
	JButton [] btn = new JButton[8];
	BookChangeForm bookChangeForm;
	BookRequestForm bookRequestForm;
	MonitorCallForm monitorCallForm;
	QueryAvailForm queryAvailForm;
	RunProgramForm programForm;
	RemoveSlotForm removeForm;
	RemoveLastForm removeLastForm;
	
	// constructor
	public ClientUI() {
		initUI();
	}

	// initiate UI
	public final void initUI() {
		// create content panel
		JPanel panel = new JPanel();
		this.getContentPane().add(panel);
	    panel.setLayout(new BorderLayout());
	    // create text area
	    this.textArea = new JTextArea();
	    this.textArea.setEditable(false);
        this.add( new JScrollPane( this.textArea ), BorderLayout.CENTER);
        // add button list
        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new GridLayout(9,1));
        // generate action listeners to buttons
        for(int i = 0; i < btn.length; i++) {
        	switch(i) {
        	case RequestPackage.SERVICE_QUERY:
        		btn[i] = new JButton("Query Availability");
        		btn[i].addActionListener(new ActionListener() {
        			public void actionPerformed(ActionEvent event) {
     	        	   queryAvailForm = new QueryAvailForm();
     	        	   queryAvailForm.setVisible(true);
        			}
        		});
        		break;
        	case RequestPackage.SERVICE_BOOK:
        		btn[i] = new JButton("Book Facility");
        		btn[i].addActionListener(new ActionListener() {
        			public void actionPerformed(ActionEvent event) {
        				bookRequestForm = new BookRequestForm();
        				bookRequestForm.setVisible(true);
        			}
        		});
        		break;
        	case RequestPackage.SERVICE_CHANGE:
        		btn[i] = new JButton("Change Bookslot");
        		btn[i].addActionListener(new ActionListener() {
        			public void actionPerformed(ActionEvent event) {
        				bookChangeForm = new BookChangeForm();
        				bookChangeForm.setVisible(true);
        			}
        		});
        		break;
        	case RequestPackage.SERVICE_MONITOR:
        		btn[i] = new JButton("Monitor Facility");
        		btn[i].addActionListener(new ActionListener() {
        			public void actionPerformed(ActionEvent event) {
        				monitorCallForm = new MonitorCallForm();
        				monitorCallForm.setVisible(true);
        			}
        		});
        		break;
        	case RequestPackage.SERVICE_PROGRAM:
        		btn[i] = new JButton("Get Quotes of The Day");
        		btn[i].addActionListener(new ActionListener() {
        			public void actionPerformed(ActionEvent event) {
        				programForm = new RunProgramForm();
        				programForm.setVisible(true);
        			}
        		});
        		break;
        	case RequestPackage.SERVICE_SPEC:
        		btn[i] = new JButton("Connect to Server");
        		btn[i].addActionListener(new ActionListener() {
        			public void actionPerformed(ActionEvent event) {
      	        	   try {
						BookingClient.sendRequest(RequestPackage.SERVICE_SPEC, 0, 0, null, null);
						//updateFacilityList();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
         			}
         		});
        		break;
        	case RequestPackage.SERVICE_REMOVE_ALL:
        		btn[i] = new JButton("Remove All Slot");
        		btn[i].addActionListener(new ActionListener() {
        			public void actionPerformed(ActionEvent event) {
        				removeForm = new RemoveSlotForm();
        				removeForm.setVisible(true);
         			}
         		});
        		break;
        	case RequestPackage.SERVICE_REMOVE_LAST:
        		btn[i] = new JButton("Remove The Last Slot");
        		btn[i].addActionListener(new ActionListener() {
        			public void actionPerformed(ActionEvent event) {
        				removeLastForm = new RemoveLastForm();
        				removeLastForm.setVisible(true);
         			}
         		});
        		break;
        	}
        	btnPanel.add(btn[i]);
        }
        JButton changeServerBtn = new JButton("Change Server");
        changeServerBtn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				(new ChangeServerForm()).setVisible(true);
			}
        	
        });
        btnPanel.add(changeServerBtn);
        this.add(btnPanel, BorderLayout.WEST);
        // set property
		this.setTitle("Client User Interface");
		this.setSize(800, 600);
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
	}
	
	// append new text to the text area
	public void appendText(String str) {
		this.textArea.append(str);
	}
	
	// append new line to the text area
	public void appendTextLine(String str) {
		this.textArea.append(str + "\n");
	}
	
	// update facility list to the combo boxes
	public void updateFacilityList() {
		this.queryAvailForm.updateFList();
		this.bookRequestForm.updateFList();
		this.monitorCallForm.updateFList();
	}
	
	public static void main(String[] args) {
	    ClientUI graphics = new ClientUI();
	    graphics.setVisible(true);
	}
	  
}

/*
 * Class: RunProgramForm
 * Purpose: Input Form for Get Quote Service
 * */
public class RunProgramForm extends JFrame {
	
	JButton submitBtn;
	JButton cancelBtn;
	JFrame frame = this;
	JComboBox nCombo;
	
	public RunProgramForm() {
		// create content panel
		JPanel panel = new JPanel();
		this.getContentPane().add(panel);
	    panel.setLayout(new GridLayout(2,2));
	    // add components:
	    panel.add(new JLabel("Your Lucky Number: "));
	    String [] strAr = {"0","1","2","3","4","5","6","7","8","9"};
	    nCombo = new JComboBox(strAr);
	    panel.add(nCombo);
	    // add buttons
	    submitBtn = new JButton("Submit");
	    panel.add(submitBtn);
	    submitBtn.addActionListener(new ActionListener() {
	           public void actionPerformed(ActionEvent event) {
	        	   try {
					getFormValues();
					frame.dispose();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	          }
	       });
	    cancelBtn = new JButton("Cancel");
	    cancelBtn.addActionListener(new ActionListener() {
	           public void actionPerformed(ActionEvent event) {
	        	   frame.dispose();
	          }
	       });
	    panel.add(cancelBtn);
        // set property
		this.setTitle("Query Form");
		this.setSize(300, 150);
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}
	
	public void getFormValues() throws SocketException {
		int optionalId = Integer.parseInt((String)nCombo.getSelectedItem());
		BookingClient.sendRequest(RequestPackage.SERVICE_PROGRAM, 0, optionalId, null, null);
	}
}

/*
 * Class: RemoveSlotForm
 * Purpose: input form for remove all slot service
 * */
public class RemoveSlotForm extends JFrame {
	
	JButton submitBtn;
	JButton cancelBtn;
	JFrame frame = this;
	JComboBox fCombo;
	
	public RemoveSlotForm() {
		// create content panel
		JPanel panel = new JPanel();
		this.getContentPane().add(panel);
	    panel.setLayout(new GridLayout(2,2));
	    // add components:
	    panel.add(new JLabel("Facility Name : "));
	    fCombo = new JComboBox(BookingClient.facilityName);
	    panel.add(fCombo);
	    // add buttons
	    submitBtn = new JButton("Submit");
	    panel.add(submitBtn);
	    submitBtn.addActionListener(new ActionListener() {
	           public void actionPerformed(ActionEvent event) {
	        	   try {
					getFormValues();
					frame.dispose();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	          }
	       });
	    cancelBtn = new JButton("Cancel");
	    cancelBtn.addActionListener(new ActionListener() {
	           public void actionPerformed(ActionEvent event) {
	        	   frame.dispose();
	          }
	       });
	    panel.add(cancelBtn);
        // set property
		this.setTitle("Query Form");
		this.setSize(300, 150);
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}
	
	public void getFormValues() throws SocketException {
		int fIndex = BookingClient.getFacilityIndex((String)fCombo.getSelectedItem());
		BookingClient.sendRequest(RequestPackage.SERVICE_REMOVE_ALL, fIndex, 0, null, null);
	}
	
}

/*
 * Class: RemoveLastForm
 * Purpose: Input Form for remove latest slot service
 * */
public class RemoveLastForm extends JFrame{
	
	JButton submitBtn;
	JButton cancelBtn;
	JFrame frame = this;
	JComboBox fCombo;
	
	public RemoveLastForm() {
		// create content panel
		JPanel panel = new JPanel();
		this.getContentPane().add(panel);
	    panel.setLayout(new GridLayout(2,2));
	    // add components:
	    panel.add(new JLabel("Facility Name : "));
	    fCombo = new JComboBox(BookingClient.facilityName);
	    panel.add(fCombo);
	    // add buttons
	    submitBtn = new JButton("Submit");
	    panel.add(submitBtn);
	    submitBtn.addActionListener(new ActionListener() {
	           public void actionPerformed(ActionEvent event) {
	        	   try {
					getFormValues();
					frame.dispose();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	          }
	       });
	    cancelBtn = new JButton("Cancel");
	    cancelBtn.addActionListener(new ActionListener() {
	           public void actionPerformed(ActionEvent event) {
	        	   frame.dispose();
	          }
	       });
	    panel.add(cancelBtn);
        // set property
		this.setTitle("Query Form");
		this.setSize(300, 150);
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}
	
	public void getFormValues() throws SocketException {
		int fIndex = BookingClient.getFacilityIndex((String)fCombo.getSelectedItem());
		BookingClient.sendRequest(RequestPackage.SERVICE_REMOVE_LAST, fIndex, 0, null, null);
	}
	
}

/*
 * Class: QueryAvailForm
 * Purpose: Input form for Query Availibity
 * */
public class QueryAvailForm extends JFrame{
	
	JButton submitBtn;
	JButton cancelBtn;
	JFrame frame = this;
	JComboBox fCombo;
	JComboBox dCombo;
	JComboBox hCombo;
	JComboBox mCombo;
	
	public QueryAvailForm() {
		// create content panel
		JPanel panel = new JPanel();
		this.getContentPane().add(panel);
	    panel.setLayout(new GridLayout(5,2));
	    // add components:
	    panel.add(new JLabel("Facility Name: "));
	    fCombo = new JComboBox(BookingClient.facilityName);
	    panel.add(fCombo);
	    panel.add(new JLabel("TimePoint Day: "));
	    dCombo = new JComboBox(BookingClient.dayList);
	    panel.add(dCombo);
	    panel.add(new JLabel("TimePoint Hour: "));
	    hCombo = new JComboBox(BookingClient.hourList);
	    panel.add(hCombo);
	    panel.add(new JLabel("TimePoint Min: "));
	    mCombo = new JComboBox(BookingClient.minList);
	    panel.add(mCombo);
	    // add buttons
	    submitBtn = new JButton("Submit");
	    submitBtn.addActionListener(new ActionListener() {
	           public void actionPerformed(ActionEvent event) {
	        	   try {
					getFormValues();
					frame.dispose();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	          }
	       });
	    panel.add(submitBtn);
	    cancelBtn = new JButton("Cancel");
	    cancelBtn.addActionListener(new ActionListener() {
	           public void actionPerformed(ActionEvent event) {
	        	   frame.dispose();
	          }
	       });
	    panel.add(cancelBtn);
        // set property
		this.setTitle("Query Form");
		this.setSize(300, 200);
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}
	
	private void getFormValues() throws IOException {
		String fname = (String)this.fCombo.getSelectedItem();
		int fIndex = BookingClient.getFacilityIndex(fname);
		String timeDay = (String)this.dCombo.getSelectedItem();
		int day = BookingClient.getDayIndex(timeDay);
		String timeHour = (String)this.hCombo.getSelectedItem();
		int hour = Integer.parseInt(timeHour);
		String timeMin = (String)this.mCombo.getSelectedItem();
		int min = Integer.parseInt(timeMin);
		TimePoint tp = new TimePoint(day, hour, min);
		BookingClient.sendRequest(RequestPackage.SERVICE_QUERY, fIndex, 0, tp, null);
	}
	
	public void updateFList() {
		System.out.println("Service 1 update FList");
		this.fCombo.removeAllItems();
		for(String s: BookingClient.facilityName) {
			System.out.println(s);
			fCombo.addItem(s);
		}
	}
	
	public static void main(String [] args) {
		QueryAvailForm form = new QueryAvailForm();
		form.setVisible(true);
	}
}

/*
 * Class: MonitorCallForm
 * Purpose: Input Form for Monitor Callback service
 * */
public class MonitorCallForm extends JFrame{
	
	private static final int MONITOR = 1;
	private static final int CANCEL = 2;
	
	JButton submitBtn;
	JButton cancelBtn;
	JFrame frame = this;
	JComboBox fCombo;
	JComboBox durDayCombo;
	JComboBox durHourCombo;
	JTextArea durMinText;
	int status;
	
	public MonitorCallForm() {
		status = MonitorCallForm.MONITOR;
		// create content panel
		JPanel panel = new JPanel();
		this.getContentPane().add(panel);
	    panel.setLayout(new GridLayout(5,2));
	    // add components:
	    panel.add(new JLabel("Facility Name: "));
	    fCombo = new JComboBox(BookingClient.facilityName);
	    panel.add(fCombo);
	    // add Duration Combo
	    panel.add(new JLabel("Duration Day: "));
	    durDayCombo = new JComboBox(BookingClient.weekDayList);
	    panel.add(durDayCombo);
	    panel.add(new JLabel("Duration Hour: "));
	    durHourCombo = new JComboBox(BookingClient.hourList);
	    panel.add(durHourCombo);
	    panel.add(new JLabel("Duration Min: "));
	    durMinText = new JTextArea();
	    panel.add(durMinText);
	    // add buttons
	    submitBtn = new JButton("Submit");
	    submitBtn.addActionListener(new ActionListener() {
	           public void actionPerformed(ActionEvent event) {
	        	   try {
					getFormValues();
					frame.dispose();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	          }
	       });
	    panel.add(submitBtn);
	    cancelBtn = new JButton("Cancel");
	    cancelBtn.addActionListener(new ActionListener() {
	           public void actionPerformed(ActionEvent event) {
	        	   BookingClient.stopMonitor = true;
	        	   frame.dispose();
	          }
	       });
	    panel.add(cancelBtn);
        // set property
		this.setTitle("Query Form");
		this.setSize(300, 200);
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}
	
	public void updateFList() {
		this.fCombo.removeAllItems();
		for(String s: BookingClient.facilityName) {
			fCombo.addItem(s);
		}
	}
	
	public void getFormValues() throws IOException {
		int fIndex = BookingClient.getFacilityIndex((String)fCombo.getSelectedItem());
		int drDay = Integer.parseInt((String)durDayCombo.getSelectedItem());
		int drHour = Integer.parseInt((String)durHourCombo.getSelectedItem());
		String str = durMinText.getText();
		int drMin = 0;
		if(!str.equals(""))
			drMin = Integer.parseInt(str);
		MonitorThread thread = new MonitorThread(fIndex, drDay, drHour, drMin);
		thread.start();
	}
	
	
	public static void main(String [] args) {
		MonitorCallForm form = new MonitorCallForm();
		form.setVisible(true);
	}
}

/*
 * Class: ChangeServerForm
 * Purpose: Input Form to change server address
 * */
public class ChangeServerForm extends JFrame{
	
	JButton submitBtn;
	JButton cancelBtn;
	JFrame frame = this;
	JTextField ipAddr;
	JTextField port;
	
	public ChangeServerForm() {
		// create content panel
		JPanel panel = new JPanel();
		this.getContentPane().add(panel);
	    panel.setLayout(new GridLayout(3,2));
	    // add components:
	    panel.add(new JLabel("IP Address : "));
	    ipAddr = new JTextField();
	    panel.add(ipAddr);
	    panel.add(new JLabel("Port : "));
	    port = new JTextField();
	    panel.add(port);
	    // add buttons
	    submitBtn = new JButton("Submit");
	    submitBtn.addActionListener(new ActionListener() {
	           public void actionPerformed(ActionEvent event) {
	        	   try {
					getFormValues();
					frame.dispose();
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	          }
	    });
	    panel.add(submitBtn);
	    cancelBtn = new JButton("Cancel");
	    cancelBtn.addActionListener(new ActionListener() {
	           public void actionPerformed(ActionEvent event) {
	        	   frame.dispose();
	          }
	    });
	    panel.add(cancelBtn);
        // set property
		this.setTitle("Query Form");
		this.setSize(300, 300);
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}
	
	private void getFormValues() throws UnknownHostException {
		String ipAddress = ipAddr.getText();
		String portStr = port.getText();
		int port = Integer.parseInt(portStr);
		BookingClient.changeServer(ipAddress, port);
	}
}

/*
 * Class: BookRequestForm
 * Purpose: Input Form for Book Request Service
 * */
public class BookRequestForm extends JFrame{

	JButton submitBtn;
	JButton cancelBtn;
	JFrame frame = this;
	JComboBox fCombo;
	JComboBox timeDayCombo;
	JComboBox timeHourCombo;
	JComboBox timeMinCombo;
	JComboBox durDayCombo;
	JComboBox durHourCombo;
	JTextArea durMinText;
	
	public BookRequestForm() {
		// create content panel
		JPanel panel = new JPanel();
		this.getContentPane().add(panel);
	    panel.setLayout(new GridLayout(8,2));
	    // add components:
	    panel.add(new JLabel("Facility Name: "));
	    fCombo = new JComboBox(BookingClient.facilityName);
	    panel.add(fCombo);
	    // add TimePoint Components
	    panel.add(new JLabel("TimePoint Day: "));
	    timeDayCombo = new JComboBox(BookingClient.dayList);
	    panel.add(timeDayCombo);
	    panel.add(new JLabel("TimePoint Hour: "));
	    timeHourCombo = new JComboBox(BookingClient.hourList);
	    panel.add(timeHourCombo);
	    panel.add(new JLabel("TimePoint Min: "));
	    timeMinCombo = new JComboBox(BookingClient.minList);
	    panel.add(timeMinCombo);
	    // add Duration Combo
	    panel.add(new JLabel("Duration Day: "));
	    durDayCombo = new JComboBox(BookingClient.weekDayList);
	    panel.add(durDayCombo);
	    panel.add(new JLabel("Duration Hour: "));
	    durHourCombo = new JComboBox(BookingClient.hourList);
	    panel.add(durHourCombo);
	    panel.add(new JLabel("Duration Min: "));
	    durMinText = new JTextArea();
	    panel.add(durMinText);
	    // add buttons
	    submitBtn = new JButton("Submit");
	    submitBtn.addActionListener(new ActionListener() {
	           public void actionPerformed(ActionEvent event) {
	        	   try {
					getFormValues();
					frame.dispose();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	          }
	       });
	    panel.add(submitBtn);
	    cancelBtn = new JButton("Cancel");
	    cancelBtn.addActionListener(new ActionListener() {
	           public void actionPerformed(ActionEvent event) {
	        	   frame.dispose();
	          }
	       });
	    panel.add(cancelBtn);
        // set property
		this.setTitle("Query Form");
		this.setSize(300, 400);
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}
	
	public void updateFList() {
		this.fCombo.removeAllItems();
		for(String s: BookingClient.facilityName) {
			fCombo.addItem(s);
		}
	}
	
	private void getFormValues() throws IOException {
		int fIndex = BookingClient.getFacilityIndex((String)fCombo.getSelectedItem());
		int timeDay = BookingClient.getDayIndex((String)timeDayCombo.getSelectedItem());
		int timeHour = Integer.parseInt((String)timeHourCombo.getSelectedItem());
		int timeMin = Integer.parseInt((String)timeMinCombo.getSelectedItem());
		TimePoint tp = new TimePoint(timeDay, timeHour, timeMin);
		int drDay = Integer.parseInt((String)durDayCombo.getSelectedItem());
		int drHour = Integer.parseInt((String)durHourCombo.getSelectedItem());
		String str = durMinText.getText();
		int drMin = 0;
		if(!str.equals(""))
			drMin = Integer.parseInt(str);
		Duration dr = new Duration(drDay, drHour, drMin);
		BookingClient.sendRequest(RequestPackage.SERVICE_BOOK, fIndex, 0, tp, dr);
	}
	
	public static void main(String [] args) {
		BookRequestForm form = new BookRequestForm();
		form.setVisible(true);
	}
}

/*
 * Class: BookChangeForm
 * Purpose: Input Form for Booking Slot Change service
 * */

public class BookChangeForm extends JFrame{
	
	JButton submitBtn;
	JButton cancelBtn;
	JFrame frame = this;
	JComboBox fCombo;
	JTextField cText;
	JComboBox durDayCombo;
	JComboBox durHourCombo;
	JTextArea durMinText;
	
	public BookChangeForm() {
		// create content panel
		JPanel panel = new JPanel();
		this.getContentPane().add(panel);
	    panel.setLayout(new GridLayout(6,2));
	    // add components:
	    panel.add(new JLabel("Facility Name: "));
	    fCombo = new JComboBox(BookingClient.facilityName);
	    panel.add(fCombo);
	    panel.add(new JLabel("ConfirmationId: "));
	    cText = new JTextField();
	    panel.add(cText);
	    // add Duration Combo
	    panel.add(new JLabel("Duration Day: "));
	    durDayCombo = new JComboBox(BookingClient.weekDayList);
	    panel.add(durDayCombo);
	    panel.add(new JLabel("Duration Hour: "));
	    durHourCombo = new JComboBox(BookingClient.hourList);
	    panel.add(durHourCombo);
	    panel.add(new JLabel("Duration Min: "));
	    durMinText = new JTextArea();
	    panel.add(durMinText);
	    // add buttons
	    submitBtn = new JButton("Submit");
	    panel.add(submitBtn);
	    submitBtn.addActionListener(new ActionListener() {
	           public void actionPerformed(ActionEvent event) {
	        	   try {
	        		System.out.println("Book Change");
					getFormValues();
					frame.dispose();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	          }
	       });
	    cancelBtn = new JButton("Cancel");
	    cancelBtn.addActionListener(new ActionListener() {
	           public void actionPerformed(ActionEvent event) {
	        	   frame.dispose();
	          }
	       });
	    panel.add(cancelBtn);
        // set property
		this.setTitle("Query Form");
		this.setSize(300, 200);
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}
	
	public void getFormValues() throws IOException {
		int fIndex = BookingClient.getFacilityIndex((String)fCombo.getSelectedItem());
		int cIndex = -1;
		if(!cText.getText().equals("")) {
			cIndex = Integer.parseInt(cText.getText());
		}
		int drDay = Integer.parseInt((String)durDayCombo.getSelectedItem());
		int drHour = Integer.parseInt((String)durHourCombo.getSelectedItem());
		String str = durMinText.getText();
		int drMin = 0;
		if(!str.equals(""))
			drMin = Integer.parseInt(str);
		Duration dr = new Duration(drDay, drHour, drMin);
		BookingClient.sendRequest(RequestPackage.SERVICE_CHANGE, fIndex, cIndex, null, dr);
	}
	
	public static void main(String [] args) {
		BookChangeForm form = new BookChangeForm();
		form.setVisible(true);
	} 
}

/*
 * Class StatusCode
 * Purpose: contain all constant status codes 
 * that are used in service */
public class StatusCode {
	
	public static final int FACILITY_NOT_FOUND = -1;
	public static final int SUCCESS_AVAILABLE = 0;
	public static final int SUCCESS_NOTAVAILABLE = 1;
	public static final int SUCCESS_BOOKING = 2;
	public static final int SUCCESS_BOOKING_CHANGE = 3;
	public static final int SUCCESS_ADD_MONITOR = 4;
	public static final int SUCCESS_PROGRAM = 5;
	public static final int ACKNOWLEDGEMENT = 6;
	public static final int ACKNOWLEDGEMENT_FAILED = 7;
	public static final int REQUEST_DUPLICATE = 8;
	public static final int SERVER_NOT_AVAILABLE = 9;
	public static final int SUCCESS_REMOVE = 10;
	public static final int SUCCESS_EMPTY = 11;
	
}

/*
 * Interface Serializable 
 * */
public interface Serializable {
	public abstract byte[] serialize();
}


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

/* Class: DataPackage
 * Purpose: provide serialize and de-serialize
 * methods with different parameters
 * Implement Serializable interface
 * */
public class DataPackage implements Serializable {

	@Override
	public byte[] serialize() {
		return null;
	}
	
	// serialize method: with timepoint
	public static byte[] serialize(TimePoint tp) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(3*4);
		IntBuffer intBuffer = byteBuffer.asIntBuffer();
		if(tp == null) {
			intBuffer.put(-1);
			intBuffer.put(-1);
			intBuffer.put(-1);
		} else {
			intBuffer.put(tp.getDate());
			intBuffer.put(tp.getHour());
			intBuffer.put(tp.getMin());
		}
		return byteBuffer.array();
	}
	
	// serialize method: with timepoint and duration
	public static byte[] serialize(TimePoint tp, Duration dr) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(6*4);
		IntBuffer intBuffer = byteBuffer.asIntBuffer();
		intBuffer.put(tp.getDate());
		intBuffer.put(tp.getHour());
		intBuffer.put(tp.getMin());
		intBuffer.put(dr.getDay());
		intBuffer.put(dr.getHour());
		intBuffer.put(dr.getMin());
		return byteBuffer.array();
	}
	
	// serialize method: with duration
	public static byte[] serialize(Duration dr) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(3*4);
		IntBuffer intBuffer = byteBuffer.asIntBuffer();
		intBuffer.put(dr.getDay());
		intBuffer.put(dr.getHour());
		intBuffer.put(dr.getMin());
		return byteBuffer.array();
	}
	
	// serialize method: with integer
	public static byte[] serialize(int confirmId) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(4);
		byteBuffer.putInt(confirmId);
		return byteBuffer.array();
	}
	
	// serialize method: with list of booking slots
	public static byte[] serialize(ArrayList<BookingSlot> slots) {
		int size = slots.size();
		ByteBuffer byteBuffer = ByteBuffer.allocate(4 + size * 6 * 4);
		IntBuffer intBuffer = byteBuffer.asIntBuffer();
		intBuffer.put(size);
		for(int i = 0; i < size; i++) {
			BookingSlot slot = slots.get(i);
			intBuffer.put(slot.getStartDate());
			intBuffer.put(slot.getStartHour());
			intBuffer.put(slot.getStartMin());
			intBuffer.put(slot.getIntervalDay());
			intBuffer.put(slot.getIntervalHour());
			intBuffer.put(slot.getIntervalMin());
		}
		return byteBuffer.array();
	}
	
	// serialize method: with String
	public static byte[] serialize(String str) 
			throws UnsupportedEncodingException {
		return str.getBytes(StandardCharsets.US_ASCII);
	}
	
	// serialize method: with String array
	public static byte[] serialize(String[] strAr) 
			throws UnsupportedEncodingException {
		String str = "";
		for(int i = 0; i < strAr.length -1; i++) {
			str += strAr[i] + "\n";
		}
		str += strAr[strAr.length -1] + "!!!";
		return DataPackage.serialize(str);
	}
	
	// de-serialize method to get Integer
	public static int extractInt(byte[] buffer, int offset) {
		return ByteBuffer.wrap(buffer, offset, 4).getInt();
	}
	
	// de-serialize methods to get Timepoint
	public static TimePoint extractTimePoint(byte[] buffer, int offset) {
		return new TimePoint(
				ByteBuffer.wrap(buffer, offset, 4).getInt(), 
				ByteBuffer.wrap(buffer, offset + 4, 4).getInt(),
				ByteBuffer.wrap(buffer, offset + 8, 4).getInt());
	}

	// de-serialize methods to get a duration
	public static Duration extractDuration(byte[] buffer, int offset) {
		return new Duration(
				ByteBuffer.wrap(buffer, offset, 4).getInt(), 
				ByteBuffer.wrap(buffer, offset + 4, 4).getInt(),
				ByteBuffer.wrap(buffer, offset + 8, 4).getInt());
	}
	
	// de-serialize methods to get a booking slot list
	public static ArrayList<BookingSlot> extractSlotList(byte[] buffer, int offset) {
		ArrayList<BookingSlot> slotList = new ArrayList<BookingSlot>();
		int slotSize = ByteBuffer.wrap(buffer, offset, 4).getInt();
		for(int i = 0; i < slotSize; i++) {
			TimePoint tp = DataPackage.extractTimePoint(buffer, offset + 4 + i * 6 * 4);
			Duration dr = DataPackage.extractDuration(buffer, offset + 4 + i * 6 * 4 + 3 * 4);
			BookingSlot slot = new BookingSlot(tp, dr);
			slotList.add(slot);
		}
		return slotList;
	}
	
	// de-serialize method to get a String list
	public static String[] extractStringList(byte[] buffer, int offset) {
		String str = new String(buffer, offset, buffer.length - offset, StandardCharsets.US_ASCII);
		String [] strAr = str.split("!!!")[0].split("\n");
		return strAr;
	}
	
	// de-serialize methods to get a String
	public static String extractString(byte[] buffer, int offset) {
		String str = new String(buffer, offset, buffer.length - offset, StandardCharsets.US_ASCII);
		str = str.split("!!!")[0];
		return str;
	}
	
	// method to print byte array
	public static void printByteArray(byte [] buffer) {
		StringBuilder sb = new StringBuilder();
	    for (byte b : buffer) {
	        sb.append(String.format("%02X ", b));
	    }
	    System.out.println(sb.toString());
	}

}

/*
 * Class: TimePoint
 * Purpose: measure property of a time point
 * Including: date, hour, minutes
 * Values of date are from Monday to Sunday 
 * */
public class TimePoint {
	 
	// range of date
	public static final int MONDAY 		= 0;
	public static final int TUESDAY 	= 1;
	public static final int WEDNESDAY 	= 2;
	public static final int THURSDAY	= 3;
	public static final int FRIDAY 		= 4;
	public static final int SATURDAY 	= 5;
	public static final int SUNDAY 		= 6;
	
	private int date;
	private int hour;
	private int min;
	
	// constructor
	public TimePoint() {
		this.date = this.hour = this.min = -1;
	}
	
	// constructor
	public TimePoint(int date, int hour, int min) {
		this.date = date;
		this.hour = hour;
		this.min  = min;
	}
	
	// constructor with time point and interval
	// return a new time point, which equals parameter time point + parameter duration
	// out of bound values can be set to:
	// Monday, 00:00 or Sunday, 24:00
	public TimePoint(TimePoint tPoint, Duration interval) {
		this.date = tPoint.getDate() + interval.getDay();
		this.hour = tPoint.getHour() + interval.getHour();
		this.min = tPoint.getMin() + interval.getMin();
		if(this.min >= 60) {
			this.min -= 60;
			this.hour++;
		} else if(this.min < 0) {
			this.min += 60;
			this.hour--;
		}
		if(this.hour >= 24) {
			this.hour -= 24;
			this.date++;
		} else if(this.hour < 0) {
			this.hour += 24;
			this.date--;
		}
		if(this.date > TimePoint.SUNDAY) {
			this.date = TimePoint.SUNDAY;
			this.hour = 24;
			this.min = 0;
		} else if (this.date < TimePoint.MONDAY) {
			this.date = TimePoint.MONDAY;
			this.hour = 0;
			this.min = 0;
		}
	}
	
	// time compare with another time point
	// return -1 if this time point happens earlier
	// return  0 if 2 time point happen at the same time
	// return +1 if this time point happens later
	public int compareTime(TimePoint tp) {
		if(this.date < tp.getDate()) 
			return -1;
		else if(this.date > tp.getDate()) 
			return +1;
		else if(this.hour < tp.getHour())
			return -1;
		else if(this.hour > tp.getHour())
			return +1;
		else if(this.min < tp.getMin())
			return -1;
		else if(this.min > tp.getMin())
			return +1;
		return 0;
	}
	
	// get and set methods
	// get this time point date
	public int getDate() {
		return this.date;
	}
	
	// get this time point hour
	public int getHour() {
		return this.hour;
	}
	
	// get this time point minute
	public int getMin() {
		return this.min;
	}

	// get text information of this time point
	public String toString() {
		String printStr = "";
		switch(this.date) {
		case -1:
			return "Empty TimePoint";
		case TimePoint.MONDAY:
			printStr +="Monday, ";
			break;
		case TimePoint.TUESDAY:
			printStr +="TUESDAY, ";
			break;
		case TimePoint.WEDNESDAY:
			printStr +="WEDNESDAY, ";
			break;
		case TimePoint.THURSDAY:
			printStr +="THURSDAY, ";
			break;
		case TimePoint.FRIDAY:
			printStr +="FRIDAY, ";
			break;
		case TimePoint.SATURDAY:
			printStr +="SATURDAY, ";
			break;
		case TimePoint.SUNDAY:
			printStr +="SUNDAY, ";
			break;
		}
		printStr += this.hour + "hour(s), " + this.min + "min(s)";
		return printStr;
	}
	
}


/*
 * Class: MonitorCliet
 * Purpose: hold information of a monitoring client
 * Including: client address, client port 
 * Including: time interval
 * */
public class MonitorClient {
	
	private String clientAddr;
	private int clientPort;
	private TimePoint endTime;
	
	// constructor
	public MonitorClient(InetAddress clientAddr, int clientPort, TimePoint endTime) 
			throws UnknownHostException {
		this.clientAddr = clientAddr.getHostName();
		this.clientPort = clientPort;
		this.endTime = new TimePoint(endTime.getDate(), endTime.getHour(), endTime.getMin());
	}
	
	// get text client address
	public String getClientAddress() {
		return this.clientAddr;
	}
	
	// get client port
	public int getClientPort(){
		return this.clientPort;
	}
	
	public boolean finishMonitor(TimePoint current) {
		int timeCompare = this.endTime.compareTime(current);
		if(timeCompare <= 0) {
			return true;
		} else return false;
	}
}

/*
 * Class Facility
 * Purpose: hold information of a facility
 * Including: ID and description
 * Including: list of booking slots
 * Including: list of clients who monitor this facility
 * */
public class Facility {
	
	private int id;
	private String desc;
	private ArrayList<BookingSlot> slots;
	private ArrayList<MonitorClient> monitorList;
	private int confirmId = 0;
	
	// constructor
	public Facility(int id, String desc) {
		this.id = id;
		this.desc = desc;
		slots = new ArrayList<BookingSlot>();
		monitorList = new ArrayList<MonitorClient>();
	}
	
	// get text ID and Description of this facility
	public String toString() {
		return "Id:" + this.id + ":" + this.desc;
	}
	
	// get description of this facility
	public String getDesc() {
		return this.desc;
	}
	
	// get a string of all booking slots
	public String getBookSchedule() {
		if(this.slots.size() == 0) 
			return "There is no booking slots for " + this.desc;
		String str = "";
		for(int i = 0;  i < this.slots.size(); i++) {
			str += "Slot " + i + ": \n" + slots.get(i).toString() + "\n";
		}
		return str;
	}
	
	// return client list object
	// used in BookingServer class
	public ArrayList<MonitorClient> getClientList() {
		return this.monitorList;
	}
	
	// return booking slot list object
	// used in BookingServer class
	public ArrayList<BookingSlot> getBookSlots() {
		return this.slots;
	} 
	
	// query Availability of a service
	// output = true (mean available at startTime), 
	// output = false(mean not available at startTime)
	public boolean queryAvailibility(TimePoint startTime) {
		// return true of there is no booking available
		if(slots.size() == 0)
			return true;
		int index = 0;
		// find the slot that happens later and is nearest to parameter startTime
		while(index < slots.size()) {
			BookingSlot currentSlot = slots.get(index);
			if(currentSlot.compareTime(startTime) > 0) {
				break;
			}
			index++;
		}
		// check if previous slot's end time violates with parameter startTime
		// return false if violated
		if(index > 0 && slots.get(index-1).getEndTime().compareTime(startTime) > 0) {
			return false;
		}
		// otherwise return true
		return true;
	}
	
	// get the next available / occupied TimePoint from the parameter startTime
	// this method is similar to above queryAvailability
	public TimePoint getNextTime(TimePoint startTime) {
		if(slots.size() == 0) {
			return null;
		}
		TimePoint nextTime;
		int index = 0;
		while(index < slots.size()) {
			BookingSlot currentSlot = slots.get(index);
			if(currentSlot.compareTime(startTime) > 0) {
				break;
			}
			index++;
		}
		System.out.println("Index = " + index);
		if(index > 0 && slots.get(index-1).getEndTime().compareTime(startTime) > 0) {
			nextTime = slots.get(index - 1).getEndTime();
		} else if(index == 0 || index < slots.size()) {
			BookingSlot nextSlot = slots.get(index);
			nextTime =  new TimePoint(
					nextSlot.getStartDate(), 
					nextSlot.getStartHour(), 
					nextSlot.getStartMin());
		} else { 
			nextTime = null;
		}
		return nextTime;
	}
	
	// add slot into the booking slot
	// return: new confirmation id if the new slot could be added
	// return: -1 else
	public int addSlot(BookingSlot newSlot) {
		int index = 0;
		// find ordering index to add slot
		while(index < slots.size()) {
			BookingSlot currentSlot = slots.get(index);
			if(newSlot.compareTime(currentSlot) <= 0) {
				break;
			} else {
				index++;
			}
		}
		// check violation
		// before violation: start time of new slot violates end time of previous
		if(index > 0) {
			BookingSlot prevSlot = slots.get(index - 1);
			if(newSlot.compareTime(prevSlot.getEndTime()) < 0) {
				System.out.println("Time Violation: before");
				return -1;
			}
		}
		// after violation: end time of new slot violates start time of the next
		if(index < slots.size()) {
			BookingSlot currSlot = slots.get(index);
			if(currSlot.compareTime(newSlot.getEndTime()) < 0) {
				System.out.println("Time Violation: after");
				return -1;
			}
		}
		// increase confirmation id and set to the added slot
		this.confirmId ++;
		newSlot.setConfirmationId(confirmId);
		System.out.println("ConfirmId = " + confirmId);
		slots.add(index, newSlot);
		System.out.println("Slotsize = " + this.slots.size());
		return confirmId;
	}
	
	// add new booking slot with out change the confirmation id
	// used in Booking Change service
	public int addSlotWithoutChangeConfirmId(BookingSlot newSlot) {
		int index = 0;
		// find ordering index to add slot
		while(index < slots.size()) {
			//System.out.println("index: " + index + ", size = " + slots.size());
			BookingSlot currentSlot = slots.get(index);
			if(newSlot.compareTime(currentSlot) <= 0) {
				break;
			} else {
				index++;
			}
		}
		// check violation
		// before violation: start time of new slot violates end time of previous
		if(index > 0) {
			BookingSlot prevSlot = slots.get(index - 1);
			if(newSlot.compareTime(prevSlot.getEndTime()) < 0) {
				System.out.println("Time Violation: before");
				return -1;
			}
		}
		// after violation: end time of new slot violates start time of the next
		if(index < slots.size()) {
			BookingSlot currSlot = slots.get(index);
			if(currSlot.compareTime(newSlot.getEndTime()) < 0) {
				System.out.println("Time Violation: after");
				return -1;
			}
		}
		
		slots.add(index, newSlot);
		return confirmId;
	}
		
	// remove a slot
	// return the removed booking slot
	public BookingSlot removeSlot(int index) {
		if(this.slots.size() == 0 || this.slots.size() > 0 && index >= this.slots.size())
			return null;
		return this.slots.remove(index);
	}
	
	// remove all slots that are booked by parameter client address
	public void removeAllSlot(InetAddress clientAddr, int clientPort) {
		System.out.println("Size = " + this.slots.size());		
		for (Iterator i = slots.iterator(); i.hasNext(); ) {
		    BookingSlot slot = (BookingSlot)i.next();
		    if(slot.compareClient(clientAddr, clientPort))
		    	i.remove();
		}
		System.out.println("Size = " + this.slots.size());
	}
	
	// remove the latest booking slot that is made by a client
	// the latest booking slot has the biggest confirmation id
	public BookingSlot removeLastSlot(InetAddress clientAddr, int clientPort) {
		int latestConfirmId = -1;
		int latestIndex = -1;
		for(int i = 0; i < slots.size(); i++) {
			BookingSlot slot = slots.get(i);
			if(slot.compareClient(clientAddr, clientPort)) {
				if(slot.getConfirmationId() > latestConfirmId) {
					latestConfirmId = slot.getConfirmationId();
					latestIndex = i;
				}
			}
		}
		if(latestIndex != -1)
			return slots.remove(latestIndex);
		else return null;
	}
	
	// search booking slot by confirmation id
	public int searchBookSlot(int confirmId) {
		int index = -1;
		if(this.slots.size() > 0) {
			for(int i = 0; i < this.slots.size(); i++) {
				BookingSlot slot = this.slots.get(i);
				System.out.println("Slot CID " + slot.getConfirmationId() );
				if(slot.getConfirmationId() == confirmId) {
					index = i;
					break;
				}
			}
		}
		return index;
	}
	
	// change a booking slot with its confirmation id and a duration
	// return -1 if failed
	// otherwise return the new confirmation id
	public int bookChange(int confirmId, Duration dr) throws UnknownHostException {
		int index = this.searchBookSlot(confirmId);
		System.out.println("ConfirmId: " + confirmId + ", Index" + index);
		if(index == -1)
			return -1;
		BookingSlot currSlot = this.removeSlot(index);
		BookingSlot updateSlot = currSlot.getUpdateSlot(dr);
		// use add slot without change confirmation id for this
		int addResult = this.addSlotWithoutChangeConfirmId(updateSlot);
		if(addResult == -1)
			this.addSlotWithoutChangeConfirmId(currSlot);
		return addResult;
	}

	// add a new client into monitor list
	public void addMonitorClient(MonitorClient client) {
		if(client != null)
			this.monitorList.add(client);
	}
	
}

/*
 * Class: Duration
 * Purpose: express a time duration / interval
 * Including: duration's day, hour, minute
 * */
public class Duration {
	
	private int day;
	private int hour;
	private int min; 
	
	// constructor
	public Duration() {
		this.day = this.hour = this.min = -1;
	}
	
	// constructor
	public Duration(int day, int hour, int min) {
		this.day = day;
		this.hour = hour;
		this.min = min;
	}
	
	// get and set method
	// get this duration day
	public int getDay(){
		return this.day;
	}
	
	// get this duration hour
	public int getHour(){
		return this.hour;
	}
	
	// get this duration minute
	public int getMin(){
		return this.min;
	}
	
	// get this duration's text information
	public String toString() {
		return this.day + " day(s), " + this.hour + " hour(s), " + this.min + " min(s)";
	}
}

/* Class: Booking Slot
 * Purpose: contain data of a booking slot
 * Including: start time, duration, confirmation id, 
 * address of client who book this slot
 * */

public class BookingSlot {
	
	private TimePoint startTime;
	private Duration interval;
	private int confirmationId;
	private InetAddress clientAddr;
	private int clientPort;
	
	//constructor
	public BookingSlot(TimePoint tp, Duration dr) {
		startTime = new TimePoint(tp.getDate(), tp.getHour(), tp.getMin());
		interval = new Duration(dr.getDay(), dr.getHour(), dr.getMin());
	}
	
	// constructor with client address and port
	public BookingSlot(TimePoint tp, Duration dr, InetAddress clientAddr, int clientPort) 
			throws UnknownHostException {
		this(tp, dr);
		this.clientAddr = InetAddress.getByName(clientAddr.getHostAddress());
		this.clientPort = clientPort;
	}
	
	// compare time with a TimePoint
	// return: -1 if this booking slot is earlier
	//		    0 if happens at the same time
	//          1 if this booking slot is later
	public int compareTime(TimePoint tp) {
		return this.startTime.compareTime(tp);
	}
	
	// compare time with a TimePoint
		// return: -1 if this booking slot is earlier
		//		    0 if happens at the same time
		//          1 if this booking slot is later
	public int compareTime(BookingSlot slot) {
		return this.startTime.compareTime(slot.startTime);
	}
	
	// check if owner and confirmation is correct
	// return: true of match client address, client port and confirmation id
	// else    false
	public boolean compareClient(InetAddress clientAddr, int clientPort, int confirmationId) {
		return this.clientAddr.equals(clientAddr) 
				&& (this.clientPort == clientPort) 
				&& (this.confirmationId == confirmationId);
	}
	
	// check if owner and confirmation is correct
		// return: true of match client address, client port
		// else    false
	public boolean compareClient(InetAddress clientAddr, int clientPort) {
		if(this.clientAddr == null)
			System.out.println("Null Client Address");
		return this.clientAddr.equals(clientAddr) 
				&& (this.clientPort == clientPort);
	}
	
	// get and set methods
	// get an update slot from this slot
	// return booking slot, which equals to this booking slot, 
	//  but shifted by a time interval
	public BookingSlot getUpdateSlot(Duration dr) 
			throws UnknownHostException {
		BookingSlot updateSlot = null;
		TimePoint updateTP = new TimePoint(this.startTime, dr);
		updateSlot = new BookingSlot(updateTP, this.interval);
		// set existed client address, client port and confirm id to updated slot
		updateSlot.setClientAddress(this.clientAddr.getHostAddress());
		updateSlot.setClientPort(this.clientPort);
		updateSlot.setConfirmationId(this.confirmationId);
		return updateSlot;
	}
	
	// get text information of this booking slot
	public String toString() {
		String printStr = "Start at: " + this.startTime.toString() + "\n";
		printStr += "Duration: " + this.interval.getDay() + " day(s), " 
		         + this.interval.getHour() + " hour(s),"
		         + this.interval.getMin() + " min(s)";
		return printStr;
	}
	
	// get EndTime Method 
	// return (this BookingSlot.startTime + this BookingSlot.interval)
	public TimePoint getEndTime() {
		return new TimePoint(startTime, interval);
	}
	
	// get start date of booking slot
	public int getStartDate() {
		return this.startTime.getDate();
	}
	
	// get start hour of booking slot
	public int getStartHour() {
		return this.startTime.getHour();
	}
	
	// get start minute of booking slot
	public int getStartMin() {
		return this.startTime.getMin();
	}
	
	// get interval day of booking slot
	public int getIntervalDay() {
		return this.interval.getDay();
	}

	// get interval hour of booking slot
	public int getIntervalHour() {
		return this.interval.getHour();
	}
	
	// get interval minute of booking slot
	public int getIntervalMin() {
		return this.interval.getMin();
	}
	
	// get confirmation id of this booking slot
	public int getConfirmationId() {
		return this.confirmationId;
	}
	
	// set new confirmation id to this booking slot
	public void setConfirmationId(int confirmationId) {
		this.confirmationId = confirmationId;
	}
	
	// get text client address of this booking slot
	public String getClientAddress() {
		return this.clientAddr.getHostAddress();
	}
	
	// get client port of this booking slot
	public int getClientPort() {
		return this.clientPort;
	}
	
	// set new client address to this booking address
	public void setClientAddress(String hostAddress) throws UnknownHostException {
		this.clientAddr = InetAddress.getByName(hostAddress);
	}
	
	// set new client port to this address
	public void setClientPort(int port) {
		this.clientPort = port;
	}
	
}
