package bookingclient;

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

import data.RequestPackage;


public class ClientUI extends JFrame {
	
	JTextArea textArea;
	JButton [] btn = new JButton[7];
	BookChangeForm bookChangeForm;
	BookRequestForm bookRequestForm;
	MonitorCallForm monitorCallForm;
	QueryAvailForm queryAvailForm;
	RunProgramForm programForm;
	RemoveSlotForm removeForm;
	
	public ClientUI() {
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
        // add button list
        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new GridLayout(7,1));
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
        		btn[i] = new JButton("Monitor Callback");
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
        	}
        	btnPanel.add(btn[i]);
        }
        this.add(btnPanel, BorderLayout.WEST);
        // set property
		this.setTitle("Client User Interface");
		this.setSize(800, 600);
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
	}
	
	public void appendText(String str) {
		this.textArea.append(str);
	}
	
	public void appendTextLine(String str) {
		this.textArea.append(str + "\n");
	}
	
	public void updateFacilityList() {
		System.out.println("Update FList");
		this.queryAvailForm.updateFList();
		this.bookRequestForm.updateFList();
		this.monitorCallForm.updateFList();
	}
	
	public static void main(String[] args) {
	    ClientUI graphics = new ClientUI();
	    graphics.setVisible(true);
	}
	  
}


