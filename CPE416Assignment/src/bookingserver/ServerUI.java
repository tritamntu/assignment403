package bookingserver;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class ServerUI extends JFrame{

	JTextArea textArea;
	JComboBox semanticsCombo;
	JTextArea ackLossRate;
	JTextArea dataLossRate;
	String [] semantics = {"AT_LEAST_ONCE", "AT_MOST_ONCE"};
	
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
	
	public void appendText(String str) {
		this.textArea.append(str);
	}
	
	public void appendTextLine(String str) {
		this.textArea.append(str + "\n");
	}
	
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
