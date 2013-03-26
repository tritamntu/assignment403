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

import data.RequestPackage;

import booking.TimePoint;

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
