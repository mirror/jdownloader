package jd.gui;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
/**
 * Mit dieser Klasse wird ein Captcha Bild angezeigt
 * 
 * @author astaldo
 */
public class CaptchaDialog extends JDialog implements ActionListener{

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 5880899982952719438L;
    /**
     * In dieses Textfeld wird der Code eingegeben
     */
    private JTextField textField;
    /**
     * Bestätigungsknopf
     */
    private JButton btnOK;
    /**
     * Das ist der eingegebene captcha Text
     */
    private String captchaText=null;

    public CaptchaDialog(Frame owner, String imageAddress){ 
        super(owner);
        setModal(true);
        setLayout(new GridBagLayout());
        ImageIcon imageIcon=null;
        try {
            imageIcon = new ImageIcon(getToolkit().createImage(new URL(imageAddress)));
        }
        catch (MalformedURLException e) { e.printStackTrace(); }
        JLabel label = new JLabel(imageIcon);
        textField    = new JTextField(10);
        btnOK        = new JButton("OK");
        
        btnOK.addActionListener(this);
        getRootPane().setDefaultButton(btnOK);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        Utilities.addToGridBag(this, label,     0, 0, 2, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.CENTER);
        Utilities.addToGridBag(this, textField, 0, 1, 1, 1, 1, 1, null, GridBagConstraints.NONE, GridBagConstraints.EAST);
        Utilities.addToGridBag(this, btnOK,     1, 1, 1, 1, 1, 1, null, GridBagConstraints.NONE, GridBagConstraints.WEST);

        pack();
        setLocation(Utilities.getCenterOfComponent(null, this));
    }
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == btnOK){
            captchaText = textField.getText();
            dispose();
        }
    }
    public String getCaptchaText() {
        return captchaText;
    }
}
