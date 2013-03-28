package bookingclient.bookingclientUI;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import booking.Duration;
import bookingclient.BookingClient;

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
	JComboBox durMinCombo;
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
	    durMinCombo = new JComboBox(BookingClient.minList);
	    panel.add(durMinCombo);
	    // add buttons
	    submitBtn = new JButton("Submit");
	    submitBtn.addActionListener(new ActionListener() {
	           public void actionPerformed(ActionEvent event) {
	        	   try {
					getFormValues();
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
		int drMin = Integer.parseInt((String)durMinCombo.getSelectedItem());
		MonitorThread thread = new MonitorThread(fIndex, drDay, drHour, drMin);
		thread.start();
	}
	
	
	public static void main(String [] args) {
		MonitorCallForm form = new MonitorCallForm();
		form.setVisible(true);
	}
}
