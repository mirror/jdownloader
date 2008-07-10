package jd.wizardry7.view;

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

public class _WizardDialog extends JFrame {

	public _WizardDialog() {
		super("JDownloader Settings Wizard");
	    setPreferredSize(new Dimension(550,700));
		this.pack();
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}
}
