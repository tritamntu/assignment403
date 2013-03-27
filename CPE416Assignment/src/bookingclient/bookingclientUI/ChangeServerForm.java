package bookingclient.bookingclientUI;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.net.UnknownHostException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;


import bookingclient.BookingClient;


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
