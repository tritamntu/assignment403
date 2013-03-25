package bookingclient;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

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
	JComboBox durMinCombo;
	
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
	    durMinCombo = new JComboBox(BookingClient.minList);
	    panel.add(durMinCombo);
	    // add buttons
	    submitBtn = new JButton("Submit");
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
	
	public static void main(String [] args) {
		BookRequestForm form = new BookRequestForm();
		form.setVisible(true);
	}
}
