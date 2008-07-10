package jd.wizardry7.view;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JPanel;


abstract public class DefaultWizardPage extends JPanel {
	
	/**
	 * Sometimes it is necessary to share data between WizardPages - e.g. one
	 * page creates a list of files, which have to be uploaded by the next page.
	 */
	protected static Object dataStoreBetweenWizardPages;
	
	private Component header;
	private Component footer;
	
	protected DefaultWizardPage() {
		super(new BorderLayout(0,0));
		initComponents();
		this.add(createBody(), BorderLayout.CENTER);
	}
	
	protected DefaultWizardPage(Object doNothing) {
		super(new BorderLayout(0,0));
	}
	
	protected void initComponents() {}
	protected abstract Component createBody();
	
	

	public Component getFooter() {
		return footer;
	}

	public Component getHeader() {
		return header;
	}
	
	
	public void setHeader(Component header) {
		this.header = header; 
		this.add(this.header, BorderLayout.NORTH);
	}
	
	public void setFooter(Component footer) {
		this.footer = footer;
		this.add(this.footer, BorderLayout.SOUTH);
	}
	
	
	public String forwardValidation() {System.out.println("forwardValidation"); return "";}
	
	public String backwardValidation() {System.out.println("backwardValidation"); return "";}
	
	public String cancelValidation() {System.out.println("cancelValidation"); return "";}


	public void exitWizardPage() {}

	public void enterWizardPageAfterForward() {}
	
}
