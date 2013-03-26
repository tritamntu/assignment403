package bookingclient;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import data.RequestPackage;

import booking.Duration;
import booking.TimePoint;

public class BookChangeForm extends JFrame{
	
	JButton submitBtn;
	JButton cancelBtn;
	JFrame frame = this;
	JComboBox fCombo;
	JTextField cText;
	JComboBox durDayCombo;
	JComboBox durHourCombo;
	JComboBox durMinCombo;
	
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
	    durMinCombo = new JComboBox(BookingClient.minList);
	    panel.add(durMinCombo);
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
		int drMin = Integer.parseInt((String)durMinCombo.getSelectedItem());
		Duration dr = new Duration(drDay, drHour, drMin);
		BookingClient.sendRequest(RequestPackage.SERVICE_CHANGE, fIndex, cIndex, null, dr);
	}
	
	public static void main(String [] args) {
		BookChangeForm form = new BookChangeForm();
		form.setVisible(true);
	} 
}
