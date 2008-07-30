package jd.wizardry7.view.pages;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import jd.wizardry7.view.DefaultWizardPage;


public class Welcome extends DefaultWizardPage {
	
	private static final Welcome INSTANCE = new Welcome();
	
	public static Welcome getInstance() {
		return INSTANCE;
	}
	
	private JLabel label;
	
    private Welcome() {
		super();
	}

	
	@Override
    protected Component createBody() {
		initComponents();

		int n = 10;
        JPanel panel = new JPanel(new BorderLayout(n ,n));
        panel.setBorder(new EmptyBorder(n,n,n,n));
        panel.add(label, BorderLayout.NORTH);
        
		return panel;
	}
	
	@Override
    protected void initComponents() {
		label = new JLabel("Welcome to the JDownloader configuration Wizard");
	}


}
