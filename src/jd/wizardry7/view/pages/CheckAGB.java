package jd.wizardry7.view.pages;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.io.File;

import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.border.EmptyBorder;

import jd.wizardry7.view.DefaultWizardPage;


public class CheckAGB extends DefaultWizardPage {
	
	private static class RTFView extends JScrollPane {

	    private static final long serialVersionUID = 1L;
	    
	    JEditorPane rtf;

	    public RTFView(File file) {
	        try {
	            this.rtf = new JEditorPane(file.toURL());
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        this.rtf.setEditable(false);

	        JViewport vp = this.getViewport();
	        vp.add(rtf);
	    }
	    
	    @Override
        public Dimension getPreferredSize() {
	        if (this.getParent()!=null) {
	            Dimension newSize = new Dimension(-1, this.getParent().getSize().height-70);
	            System.out.println("newSize: " + newSize);
	            this.setPreferredSize(newSize);
	        }
	        return super.getPreferredSize();
	    }
	}
	
	private static final CheckAGB INSTANCE = new CheckAGB();
	
	public static CheckAGB getInstance() {
		return INSTANCE;
	}
	
	JCheckBox checkbox;
	private RTFView rtfView;

	
	private CheckAGB() {
		super();
	}
	
	@Override
    protected Component createBody() {
		initComponents();

		int n = 10;
        JPanel panel = new JPanel(new BorderLayout(n ,n));
        panel.setBorder(new EmptyBorder(n,n,n,n));
        panel.add(rtfView);
        panel.add(checkbox, BorderLayout.SOUTH);
        
		return panel;
	}
	
	
	// Validation ##########################################################

	@Override
    public void enterWizardPageAfterForward() {
		this.checkbox.setSelected(false);
	}
	
	
	@Override
    public String forwardValidation() {
		if (checkbox.isSelected()) {
			checkbox.setSelected(false);
			return "";
		}
		else return "Before you are allowed to continue you have to read and agree to our AGBs";
	}
	
	
	
	@Override
    protected void initComponents() {
		checkbox = new JCheckBox();
		checkbox.setText(" I have read and agree with the JDownloader AGBs");
		rtfView = new RTFView(new File("res/agbs.rtf"));
	}

}





