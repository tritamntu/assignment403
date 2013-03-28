package bookingclient.bookingclientUI;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.SocketException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import bookingclient.BookingClient;
import data.RequestPackage;

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
