//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.


package jd.gui.skins.simple.components;

import java.awt.ComponentOrientation;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextPane;

import jd.gui.skins.simple.Link.JLinkButton;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

public class JHelpDialog extends JDialog implements ActionListener {

    protected Insets  insets            = new Insets(5, 5, 5, 5);

    protected Logger  logger            = JDUtilities.getLogger();

    private JButton   btn1;

    private JButton   btn2;

    private JButton   btn3;

    public static int STATUS_UNANSWERED = 0;

    public static int STATUS_ANSWER_1   = 1;

    public static int STATUS_ANSWER_2   = 2;

    public static int STATUS_ANSWER_3   = 3;

    private JTextPane htmlArea;

    private int       status            = STATUS_UNANSWERED;
    public Action action1;
    private Action action2;
    private Action action3;
    private JFrame    parentFrame;

    public JHelpDialog(JFrame frame, String title, String html) {
        super(frame);
        this.parentFrame = frame;
        setLayout(new GridBagLayout());

        setBtn1(new JButton("UNSET"));
        setBtn2(new JButton("UNSET"));
        setBtn3(new JButton("UNSET"));
  
        getBtn1().addActionListener(this);
        getBtn2().addActionListener(this);
        getBtn3().addActionListener(this);
        setTitle(title);
        htmlArea = new JTextPane();

        htmlArea.setEditable(false);
        htmlArea.setContentType("text/html");

        htmlArea.setText(html);
        htmlArea.setOpaque(false);
        htmlArea.requestFocusInWindow();
        this.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        Icon imageIcon = new ImageIcon(JDUtilities.getImage(JDTheme.V("gui.images.config.tip")));

        JDUtilities.addToGridBag(this, new JLabel(imageIcon), 0, 0, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.NORTHWEST);

        JDUtilities.addToGridBag(this, htmlArea, 1, 0, 3, 1, 1, 1, insets, GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);

        JDUtilities.addToGridBag(this, getBtn1(), 1, 1, 1, 1, 1, 0, insets, GridBagConstraints.NONE, GridBagConstraints.NORTHEAST);
        JDUtilities.addToGridBag(this, getBtn2(), 2, 1, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.NORTHEAST);

        JDUtilities.addToGridBag(this, getBtn3(), 3, 1, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.NORTHEAST);

        pack();

        // setLocation(JDUtilities.getCenterOfComponent(null, this));
        getRootPane().setDefaultButton(getBtn1());
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        // this.setLocationRelativeTo(null);

    }

    public static int showHelpMessage(JFrame parent, String title, String message,final URL url) {
        return showHelpMessage(parent, title, message, url,JDLocale.L("gui.dialogs.helpDialog.btn.help", "Hilfe anzeigen"));
      
    }
    /**
     * @param status the status to set
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * @return the status
     */
    public int getStatus() {
        return status;
    }

    public static int showHelpMessage(JFrame parent, String title, String message,final URL url,String helpText) {

        title = title == null ? JDLocale.L("gui.dialogs.helpDialog.defaultTitle", "jDownloader Soforthilfe") : title;
//        int buttons = JOptionPane.YES_NO_OPTION;
//        int messageType = JOptionPane.INFORMATION_MESSAGE;
//        String[] options = { JDLocale.L("gui.dialogs.helpDialog.btn.ok", "OK"), JDLocale.L("gui.dialogs.helpDialog.btn.help", "Hilfe anzeigen") };
        JHelpDialog d = new JHelpDialog(parent, title, message);
        d.getBtn3().setVisible(false);
        d.getBtn1().setText(helpText);
        d.getBtn2().setText(JDLocale.L("gui.dialogs.helpDialog.btn.ok", "OK"));      
        d.action1= d.new Action(){
            public boolean doAction(){               
                        JLinkButton.openURL(url);
                return true;
            }
        };
        d.showDialog();
        return d.getStatus();
    }
    

    
    public void showDialog() {
        this.setVisible(true);
        this.setLocation(JDUtilities.getCenterOfComponent(parentFrame, this));
        this.setVisible(false);
        setModal(true);
        this.setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == getBtn1()) {
            this.setStatus(STATUS_ANSWER_1);
            if(action1!=null)action1.doAction();
            else dispose();
        }
        else if (e.getSource() == getBtn2()) {
            this.setStatus(STATUS_ANSWER_2);
            if(action2!=null)action2.doAction();
            else dispose();
        }
        else if (e.getSource() == getBtn3()) {
            this.setStatus(STATUS_ANSWER_3);
            if(action3!=null)action3.doAction();
            else dispose();
        }
        else {
            dispose();
        }
    }

    public static void main(String[] argv) throws MalformedURLException {
        JDTheme.setTheme("default");
        JDLocale.setLocale("german");
        showHelpMessage(null, "Test", "Messagef sdfdsa fda f sdafjklsdhafdfhsafhdsahd jahfjkldhasfjkdhsajkfhdhfs afhsdal fhdfhjksadhfdjjfhsda fdjs hfasdjfljkshfkjshfdsa",new URL("http://www.google.de"));
    }
    /**
     * @param btn3 the btn3 to set
     */
    public void setBtn3(JButton btn3) {
        this.btn3 = btn3;
    }

    /**
     * @return the btn3
     */
    public JButton getBtn3() {
        return btn3;
    }
    /**
     * @param btn1 the btn1 to set
     */
    public void setBtn1(JButton btn1) {
        this.btn1 = btn1;
    }

    /**
     * @return the btn1
     */
    public JButton getBtn1() {
        return btn1;
    }
    /**
     * @param btn2 the btn2 to set
     */
    public void setBtn2(JButton btn2) {
        this.btn2 = btn2;
    }

    /**
     * @return the btn2
     */
    public JButton getBtn2() {
        return btn2;
    }
    public abstract class Action{        
        public abstract boolean doAction();
    }

}
