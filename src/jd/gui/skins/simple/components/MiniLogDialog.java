//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

    private static final long serialVersionUID = -1749561448228487759L;

    private String lineSeparator = System.getProperty("line.separator");

    private JButton btnNotOK;

    private JButton btnOK;

    private JTextArea htmlArea;

    private JLabel lblMessage;

    private JProgressBar progress;

    private JScrollPane scrollPane;

    public MiniLogDialog(String message) {
        super();

        setLayout(new GridBagLayout());
        setVisible(true);
        setTitle(message);
        setAlwaysOnTop(true);
        setPreferredSize(new Dimension(400, 300));
        setTitle(JDLocale.L("gui.dialogs.progress.title", "Fortschritt...bitte warten"));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        btnOK = new JButton(JDLocale.L("gui.btn_ok", "OK"));
        btnOK.addActionListener(this);
        btnNotOK = new JButton(JDLocale.L("gui.btn_cancel", "Abbrechen"));
        btnNotOK.addActionListener(this);
        lblMessage = new JLabel(message);
        htmlArea = new JTextArea();
        htmlArea.setEditable(false);
        htmlArea.setLineWrap(false);
        htmlArea.setText("");
        scrollPane = new JScrollPane(htmlArea);
        progress = new JProgressBar();

        getRootPane().setDefaultButton(btnOK);

        JDUtilities.addToGridBag(this, lblMessage, 0, 0, 4, 1, 0, 0, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTHWEST);
        JDUtilities.addToGridBag(this, progress, 0, 1, 4, 1, 1, 0, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(this, scrollPane, 0, 2, 4, 1, 1, 1, null, GridBagConstraints.BOTH, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(this, btnOK, 3, 3, 1, 1, 1, 0, null, GridBagConstraints.NONE, GridBagConstraints.EAST);

        SimpleGUI.restoreWindow(null, this);

        pack();

    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnOK || e.getSource() == btnNotOK) {
            setVisible(false);
            dispose();
        }
    }

    public void appendLine(String text) {
        if (htmlArea.getText().equals("")) {
            htmlArea.setText(text);
        } else {
            htmlArea.append(lineSeparator + text);
        }
    }

    public JButton getBtnNOTOK() {
        return btnNotOK;
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
