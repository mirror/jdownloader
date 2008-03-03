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
    private Action action1;
    private Action action2;
    private Action action3;
    private JFrame    parentFrame;

    private JHelpDialog(JFrame frame, String title, String html) {
        super(frame);
        this.parentFrame = frame;
        setLayout(new GridBagLayout());

        btn1 = new JButton("UNSET");
        btn2 = new JButton("UNSET");
        btn3 = new JButton("UNSET");
  
        btn1.addActionListener(this);
        btn2.addActionListener(this);
        btn3.addActionListener(this);
        setTitle(title);
        htmlArea = new JTextPane();

        htmlArea.setEditable(false);
        htmlArea.setContentType("text/html");

        htmlArea.setText(html);
        htmlArea.setOpaque(false);
        htmlArea.requestFocusInWindow();
        this.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        Icon imageIcon = new ImageIcon(JDUtilities.getImage(JDTheme.I("gui.images.config.tip")));

        JDUtilities.addToGridBag(this, new JLabel(imageIcon), 0, 0, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.NORTHWEST);

        JDUtilities.addToGridBag(this, htmlArea, 1, 0, 3, 1, 1, 1, insets, GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);

        JDUtilities.addToGridBag(this, btn1, 1, 1, 1, 1, 1, 0, insets, GridBagConstraints.NONE, GridBagConstraints.NORTHEAST);
        JDUtilities.addToGridBag(this, btn2, 2, 1, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.NORTHEAST);

        JDUtilities.addToGridBag(this, btn3, 3, 1, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.NORTHEAST);

        pack();

        // setLocation(JDUtilities.getCenterOfComponent(null, this));
        getRootPane().setDefaultButton(btn1);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        // this.setLocationRelativeTo(null);

    }

    public static int showHelpMessage(JFrame parent, String title, String message,final URL url) {

        title = title == null ? JDLocale.L("gui.dialogs.helpDialog.defaultTitle", "jDownloader Soforthilfe") : title;
//        int buttons = JOptionPane.YES_NO_OPTION;
//        int messageType = JOptionPane.INFORMATION_MESSAGE;
//        String[] options = { JDLocale.L("gui.dialogs.helpDialog.btn.ok", "OK"), JDLocale.L("gui.dialogs.helpDialog.btn.help", "Hilfe anzeigen") };
        JHelpDialog d = new JHelpDialog(parent, title, message);
        d.btn3.setVisible(false);
        d.btn1.setText(JDLocale.L("gui.dialogs.helpDialog.btn.help", "Hilfe anzeigen"));
        d.btn2.setText(JDLocale.L("gui.dialogs.helpDialog.btn.ok", "OK"));      
        d.action1= d.new Action(){
            public boolean doAction(){               
                        JLinkButton.OpenURL(url);
                return true;
            }
        };
        d.showDialog();
        return d.status;
    }

    private void showDialog() {
        this.setVisible(true);
        this.setLocation(JDUtilities.getCenterOfComponent(parentFrame, this));
        this.setVisible(false);
        setModal(true);
        this.setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == btn1) {
            this.status = STATUS_ANSWER_1;
            if(action1!=null)action1.doAction();
            else dispose();
        }
        else if (e.getSource() == btn2) {
            this.status = STATUS_ANSWER_2;
            if(action2!=null)action2.doAction();
            else dispose();
        }
        else if (e.getSource() == btn3) {
            this.status = STATUS_ANSWER_3;
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
    abstract class Action{        
        public abstract boolean doAction();
    }

}
