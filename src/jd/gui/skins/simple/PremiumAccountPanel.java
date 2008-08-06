package jd.gui.skins.simple;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import net.miginfocom.swing.MigLayout;


public class PremiumAccountPanel {

	public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		JFrame frame = new JFrame("Rapidshare Premium Accounts");
		frame.setContentPane(getPremiumAccountPanel());
		frame.setPreferredSize(new Dimension(550,700));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setAlwaysOnTop(true);
        frame.setVisible(true);
	}



	private static JPanel getPremiumAccountPanel() {
		JPanel panel = new JPanel(new MigLayout("ins 22", "[right]10[grow,fill]15[right][grow,fill]"));
		for (int i = 1; i <= 5; i++) addPremiumAccount(panel, i);
		return panel;
	}
	
	

	private static void addPremiumAccount(JPanel panel, final int accountNo) {
		final JCheckBox active = new JCheckBox("<html><b>Premium Account #" + accountNo +"</b></html>");
		active.setForeground(Color.red);
		active.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (active.isSelected()) {
					active.setForeground(Color.green.darker());
				}
				else {
					active.setForeground(Color.red);
				} 
			}
		});
		active.setSelected(accountNo % 2 != 0 ? true : false);
		panel.add(active, "alignleft");
		panel.add(new JSeparator(), "spanx, pushx, growx");
		
		panel.add(new JLabel("Premium User"), "gaptop 12");
		panel.add(new JTextField("coalado"));
		
		panel.add(new JLabel("Passwort"));
		panel.add(new JPasswordField("1234565789"), "wrap");

		panel.add(new JLabel("Account Status"));
		JTextField status = new JTextField("tuxla sold me into slavery :(");
		status.setEditable(false);
		panel.add(status ,          "span, gapbottom :1000:push");
	}
}
