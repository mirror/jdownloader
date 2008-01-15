package jd.gui.skins.simple.components;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Diese Klasse ist wie die Optionspane mit textfeld nur mit textarea
 * 
 * @author JD-Team
 */
public class HTMLDialog extends JDialog implements ActionListener {
    /**
	 * 
	 */
	private static final long serialVersionUID = -7741748123426268439L;

	/**
     * 
     */

    protected Insets    insets = new Insets(0, 0, 0, 0);

    protected Logger    logger = JDUtilities.getLogger();

    private JButton     btnOk;

    private JButton     btnCancel;

    private JTextPane   htmlArea;

   // private JLabel      lblText;

    private JScrollPane scrollPane;

   // private String text=null;

    private boolean success=false;

    private HTMLDialog(JFrame frame, String title, String html) {
        super(frame);
        
        setLayout(new BorderLayout());

        btnCancel = new JButton(JDLocale.L("gui.btn_cancel","Cancel"));
        btnCancel.addActionListener(this);
        btnOk = new JButton(JDLocale.L("gui.btn_ok","OK"));
        btnOk.addActionListener(this);
        setTitle(title);
        htmlArea = new JTextPane();
        scrollPane = new JScrollPane(htmlArea);
        htmlArea.setEditable(false);
        htmlArea.setContentType("text/html"); 
        htmlArea.setText(html);
        htmlArea.requestFocusInWindow();
     
       
        this.add(scrollPane,BorderLayout.CENTER);
        JPanel p= new JPanel();
        p.add(btnOk);
        p.add(btnCancel);
        this.add(p,BorderLayout.SOUTH);
        
       pack();
        
      
        //setLocation(JDUtilities.getCenterOfComponent(null, this));
        getRootPane().setDefaultButton(btnOk);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
       // this.setLocationRelativeTo(null);
        this.setVisible(true);
        this.setLocation(JDUtilities.getCenterOfComponent(frame, this));
        this.setVisible(false);
        setModal(true);
        this.setVisible(true);
      
    }
    public static boolean showDialog(JFrame frame, String title, String question){
        HTMLDialog  tda= new HTMLDialog( frame,  title,  question);
        return tda.success;
        
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnOk) {
            this.success=true;
            dispose();
        }else{
            dispose();
        }
    }

}
