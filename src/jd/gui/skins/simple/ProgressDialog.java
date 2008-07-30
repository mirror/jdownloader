//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.skins.simple;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.WindowConstants;

import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class ProgressDialog extends JDialog implements ActionListener {

    @SuppressWarnings("unused")
    private static Logger logger = JDUtilities.getLogger();

    private static int REL = GridBagConstraints.RELATIVE;

    private static int REM = GridBagConstraints.REMAINDER;

    /**
     * 
     */
    private static final long serialVersionUID = -1749561448228487759L;

    private JButton btnNOTOK;

    private JButton btnOK;

    private JLabel lblMessage;

    private JProgressBar progress;

    private Thread thread;

    public ProgressDialog(Frame owner, String message, Thread ob, boolean ok, boolean cancel) {
        super(owner);
        setModal(true);
        setLayout(new GridBagLayout());

        thread = ob;
        btnOK = new JButton(JDLocale.L("gui.btn_ok", "OK"));
        btnNOTOK = new JButton(JDLocale.L("gui.btn_cancel", "Abbrechen"));
        lblMessage = new JLabel(message);
        progress = new JProgressBar();
        setAlwaysOnTop(true);
        setTitle(JDLocale.L("gui.dialogs.progress.title", "Fortschritt...bitte warten"));
        btnOK.addActionListener(this);
        btnNOTOK.addActionListener(this);
        getRootPane().setDefaultButton(btnOK);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        JDUtilities.addToGridBag(this, lblMessage, REL, REL, REM, 1, 0, 0, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTHWEST);
        JDUtilities.addToGridBag(this, progress, REL, REL, REM, 1, 1, 1, null, GridBagConstraints.BOTH, GridBagConstraints.WEST);
        if (ok) {
            JDUtilities.addToGridBag(this, btnOK, REL, REL, cancel ? REL : REM, 1, 1, 0, null, GridBagConstraints.NONE, GridBagConstraints.EAST);
        }
        if (cancel) {
            JDUtilities.addToGridBag(this, btnNOTOK, REL, REL, REM, 1, ok ? 0 : 1, 0, null, GridBagConstraints.NONE, GridBagConstraints.EAST);
        }
        setLocation(SimpleGUI.getLastLocation(owner, "DIALOGS", this));

        pack();

    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnOK) {
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
            }
            setVisible(false);
            dispose();
        }
        if (e.getSource() == btnNOTOK) {
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
            }
            setVisible(false);
            dispose();
        }
    }

    public int getMaximum() {
        return progress.getMaximum();
    }

    public String getMessage() {
        return lblMessage.getText();
    }

    public int getMinimum() {
        return progress.getMinimum();
    }

    public String getString() {
        return progress.getString();
    }

    public int getValue() {
        return progress.getValue();
    }

    public void setMaximum(int value) {
        progress.setMaximum(value);

    }

    public void setMessage(String txt) {
        lblMessage.setText(txt);
    }

    public void setMinimum(int value) {
        progress.setMinimum(value);
    }

    public void setString(String txt) {

        progress.setString(txt);

    }

    public void setStringPainted(boolean v) {
        progress.setStringPainted(v);
    }

    public void setThread(Thread th) {
        thread = th;

    }

    public void setValue(int value) {
        progress.setValue(value);
    }

}
