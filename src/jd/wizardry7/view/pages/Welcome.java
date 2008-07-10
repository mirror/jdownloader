package jd.wizardry7.view.pages;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import jd.wizardry7.view.DefaultWizardPage;


public class Welcome extends DefaultWizardPage {
	
	private static final Welcome INSTANCE = new Welcome();
	
	public static Welcome getInstance() {
		return INSTANCE;
	}
	
	private Welcome() {
		super();
	}
	
    private JLabel label;

	
	protected void initComponents() {
		label = new JLabel("Welcome to the JDownloader configuration Wizard");
	}
	
	protected Component createBody() {
		initComponents();

		int n = 10;
        JPanel panel = new JPanel(new BorderLayout(n ,n));
        panel.setBorder(new EmptyBorder(n,n,n,n));
        panel.add(label, BorderLayout.NORTH);
        
		return panel;
	}


}
