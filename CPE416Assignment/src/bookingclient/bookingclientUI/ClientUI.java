package bookingclient.bookingclientUI;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import bookingclient.BookingClient;

import data.RequestPackage;

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


