package jd.gui.skins.simple;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

import jd.captcha.JAntiCaptcha;
import jd.config.Configuration;
import jd.plugins.Plugin;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class ProgressDialog extends JDialog implements ActionListener {

    @SuppressWarnings("unused")
    private static Logger logger = JDUtilities.getLogger();

    private JButton       btnOK;

    private JButton       btnNOTOK;

    private JLabel        lblMessage;

    private JProgressBar  progress;

    private Thread        thread;

    private static int    REL    = GridBagConstraints.RELATIVE;

    private static int    REM    = GridBagConstraints.REMAINDER;

    public ProgressDialog(Frame owner, String message, Thread ob, boolean ok, boolean cancel) {
        super(owner);
        setModal(true);
        setLayout(new GridBagLayout());

        this.thread = ob;
        btnOK = new JButton(JDLocale.L("gui.btn_ok", "OK"));
        btnNOTOK = new JButton(JDLocale.L("gui.btn_cancel", "Abbrechen"));
        lblMessage = new JLabel(message);
        progress = new JProgressBar();
        this.setAlwaysOnTop(true);
this.setTitle(JDLocale.L("gui.dialogs.progress.title","Fortschritt...bitte warten"));
        btnOK.addActionListener(this);
        btnNOTOK.addActionListener(this);
        getRootPane().setDefaultButton(btnOK);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        JDUtilities.addToGridBag(this, lblMessage, REL, REL, REM, 1, 0, 0, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTHWEST);
        JDUtilities.addToGridBag(this, progress, REL, REL, REM, 1, 1, 1, null, GridBagConstraints.BOTH, GridBagConstraints.WEST);
        if (ok) JDUtilities.addToGridBag(this, btnOK, REL, REL, cancel ? REL : REM, 1, 1, 0, null, GridBagConstraints.NONE, GridBagConstraints.EAST);
        if (cancel) JDUtilities.addToGridBag(this, btnNOTOK, REL, REL, REM, 1, ok ? 0 : 1, 0, null, GridBagConstraints.NONE, GridBagConstraints.EAST);
        setLocation(JDUtilities.getLastLocation(owner, "DIALOGS", this));
       
        pack();
        
    }

    public void setString(String txt) {

        progress.setString(txt);

    }

    public void setStringPainted(boolean v) {
        progress.setStringPainted(v);
    }

    public String getString() {
        return progress.getString();
    }

    public void setMaximum(int value) {
        progress.setMaximum(value);

    }

    public void setMinimum(int value) {
        progress.setMinimum(value);
    }

    public int getMaximum() {
        return progress.getMaximum();
    }

    public int getMinimum() {
        return progress.getMinimum();
    }

    public void setValue(int value) {
        progress.setValue(value);
    }

    public int getValue() {
        return progress.getValue();
    }

    public void setMessage(String txt) {
        this.lblMessage.setText(txt);
    }

    public String getMessage() {
        return lblMessage.getText();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnOK) {
            if (this.thread != null && thread.isAlive()) thread.interrupt();
            setVisible(false);
            dispose();
        }
        if (e.getSource() == btnNOTOK) {
            if (this.thread != null && thread.isAlive()) thread.interrupt();
            setVisible(false);
            dispose();
        }
    }

    public void setThread(Thread th) {
       this.thread=th;
        
    }

}
