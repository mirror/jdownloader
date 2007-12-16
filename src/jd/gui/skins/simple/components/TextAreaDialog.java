package jd.gui.skins.simple.components;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Diese Klasse ist wie die Optionspane mit textfeld nur mit textarea
 * 
 * @author coalado
 */
public class TextAreaDialog extends JDialog implements ActionListener {
    /**
     * 
     */

    protected Insets    insets = new Insets(0, 0, 0, 0);

    protected Logger    logger = JDUtilities.getLogger();

    private JButton     btnOk;

    private JButton     btnCancel;

    private JTextArea   textArea;

    private JLabel      lblText;

    private JScrollPane scrollPane;

    private String text=null;

    private TextAreaDialog(JFrame frame, String title, String question, String def) {
        super(frame);
        setModal(true);
        setLayout(new BorderLayout());

        btnCancel = new JButton(JDLocale.L("gui.btn_cancel","Cancel"));
        btnCancel.addActionListener(this);
        btnOk = new JButton(JDLocale.L("gui.btn_ok","OK"));
        btnOk.addActionListener(this);
        setTitle(title);
        textArea = new JTextArea(10, 60);
        scrollPane = new JScrollPane(textArea);
        textArea.setEditable(true);
        textArea.requestFocusInWindow();
        if (question != null) {
            this.add(new JLabel(question),BorderLayout.NORTH);
        }
        if (def != null) {
            textArea.setText(def);
        }
        this.add(scrollPane,BorderLayout.CENTER);
        JPanel p= new JPanel();
        p.add(btnOk);
        p.add(btnCancel);
        this.add(p,BorderLayout.SOUTH);
       pack();
        this.setVisible(true);
        
        //setLocation(JDUtilities.getCenterOfComponent(null, this));
        getRootPane().setDefaultButton(btnOk);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.setLocationRelativeTo(null);

    }
    public static String showDialog(JFrame frame, String title, String question, String def){
        TextAreaDialog  tda= new TextAreaDialog( frame,  title,  question,  def);
        return tda.getText();
        
    }
private String getText(){
    return text;
}
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnOk) {
            this.text=textArea.getText();
            dispose();
        }else{
            dispose();
        }
    }

}
