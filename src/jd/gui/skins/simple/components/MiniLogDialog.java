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

package jd.gui.skins.simple.components;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import jd.gui.skins.simple.SimpleGUI;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class MiniLogDialog extends JFrame implements ActionListener {
    @SuppressWarnings("unused")
    private static Logger logger = JDUtilities.getLogger();

    /**
     * 
     */
    private static final long serialVersionUID = -1749561448228487759L;

    public static void main(String args[]) {

        // MiniLogDialog mld = new MiniLogDialog(new JFrame(), "String message",
        // Thread.currentThread(), true, true);
        // String tmp[] = new String[args.length - 1];
        // for(int i = 1; i < args.length; i++)
        // tmp[i - 1] = args[i];
        //
        // runCommand(args[0], tmp, null);
    }

    private JButton btnNOTOK;

    private JButton btnOK;

    private JTextArea htmlArea;

    private JLabel lblMessage;

    // private Thread thread;

    private JProgressBar progress;

    private JScrollPane scrollPane;

    // private static int REL = GridBagConstraints.RELATIVE;

    // private static int REM = GridBagConstraints.REMAINDER;

    public MiniLogDialog(String message) {
        super();

        setLayout(new GridBagLayout());
        setVisible(true);
        // this.thread = ob;
        setTitle(message);
        setAlwaysOnTop(true);
        btnOK = new JButton(JDLocale.L("gui.btn_ok", "OK"));
        btnNOTOK = new JButton(JDLocale.L("gui.btn_cancel", "Abbrechen"));
        lblMessage = new JLabel(message);
        htmlArea = new JTextArea();
        scrollPane = new JScrollPane(htmlArea);
        htmlArea.setEditable(false);
        htmlArea.setLineWrap(false);
        // htmlArea.setContentType("text/html");
        htmlArea.setText("");
        // htmlArea.requestFocusInWindow();

        setPreferredSize(new Dimension(400, 300));
        progress = new JProgressBar();

        // this.setAlwaysOnTop(true);
        setTitle(JDLocale.L("gui.dialogs.progress.title", "Fortschritt...bitte warten"));
        btnOK.addActionListener(this);
        btnNOTOK.addActionListener(this);
        getRootPane().setDefaultButton(btnOK);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        JDUtilities.addToGridBag(this, lblMessage, 0, 0, 4, 1, 0, 0, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTHWEST);
        JDUtilities.addToGridBag(this, progress, 0, 1, 4, 1, 1, 0, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(this, scrollPane, 0, 2, 4, 1, 1, 1, null, GridBagConstraints.BOTH, GridBagConstraints.EAST);

        JDUtilities.addToGridBag(this, btnOK, 3, 3, 1, 1, 1, 0, null, GridBagConstraints.NONE, GridBagConstraints.EAST);
        // if (cancel) JDUtilities.addToGridBag(this, btnNOTOK, 3, 3, 1, 1, ok ?
        // 0 : 1, 0, null, GridBagConstraints.NONE, GridBagConstraints.EAST);
        try {
            SimpleGUI.restoreWindow(null, null, this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        pack();

    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnOK) {
            setVisible(false);
            dispose();
        }
        if (e.getSource() == btnNOTOK) {
            setVisible(false);
            dispose();
        }
    }

    public JButton getBtnNOTOK() {
        return btnNOTOK;
    }

    public JButton getBtnOK() {
        return btnOK;
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

    public JProgressBar getProgress() {
        return progress;
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    public String getString() {
        return progress.getString();
    }

    public String getText() {
        return htmlArea.getText();
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

    public void setText(String text) {
        htmlArea.setText(text);
    }

    public void setValue(int value) {
        progress.setValue(value);
    }

}
