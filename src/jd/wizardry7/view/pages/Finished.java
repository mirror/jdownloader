package jd.wizardry7.view.pages;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import jd.wizardry7.view.DefaultWizardPage;


public class Finished extends DefaultWizardPage {
	
	private static final Finished INSTANCE = new Finished();
	
	public static Finished getInstance() {
		return INSTANCE;
	}
	
	private JLabel label;
	
    private Finished() {
		super();
		setPreferredSize(new Dimension(500,600));
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
    public void exitWizardPage() {
	    JOptionPane.showMessageDialog(null, "You finsished the Wizard!");
	}
	
	
	@Override
    protected void initComponents() {
	    label = new JLabel("You did it! Press the finish button to use JDownloader.");
	}

}
