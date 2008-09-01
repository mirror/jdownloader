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

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
import javax.swing.JTextPane;
import javax.swing.WindowConstants;

import jd.gui.skins.simple.LocationListener;
import jd.gui.skins.simple.SimpleGUI;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Diese Klasse ist wie die Optionspane mit textfeld nur mit textarea
 * 
 * @author JD-Team
 */
public class TextAreaDialog extends JDialog implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = -655039113948925165L;

    public static String showDialog(JFrame frame, String title, String question, String def) {
        TextAreaDialog tda = new TextAreaDialog(frame, title, question, def);
        return tda.getText();
    }

    public static String[] showDialog(JFrame frame, String title, String questionOne, String questionTwo, String defaultOne, String defaultTwo) {
        TextAreaDialog tda = new TextAreaDialog(frame, title, questionOne, questionTwo, defaultOne, defaultTwo);
        return tda.getTextArray();
    }

    private JButton btnCancel;

    private JButton btnOk;

    protected Insets insets = new Insets(0, 0, 0, 0);

    protected Logger logger = JDUtilities.getLogger();

    private JScrollPane scrollPane;
    private JScrollPane optScrollPane;
    private String text = null;
    private String[] text2 = new String[2];

    private JTextPane textArea;
    private JTextPane optTextArea;

    private TextAreaDialog(JFrame frame, String title, String question, String def) {
        super(frame);

        setModal(false);
        setLayout(new BorderLayout());
        setName(title);
        btnCancel = new JButton(JDLocale.L("gui.btn_cancel", "Cancel"));
        btnCancel.addActionListener(this);
        btnOk = new JButton(JDLocale.L("gui.btn_ok", "OK"));
        btnOk.addActionListener(this);
        setTitle(title);
        textArea = new JTextPane();
        scrollPane = new JScrollPane(textArea);

        setResizable(true);

        textArea.setEditable(true);
        textArea.requestFocusInWindow();
        if (question != null) {
            this.add(new JLabel(question), BorderLayout.NORTH);
        }
        if (def != null) {
            textArea.setText(def);
        }
        this.add(scrollPane, BorderLayout.CENTER);
        JPanel p = new JPanel();
        p.add(btnOk);
        p.add(btnCancel);
        pack();

        getRootPane().setDefaultButton(btnOk);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.add(p, BorderLayout.SOUTH);
        addWindowListener(new LocationListener());

        SimpleGUI.restoreWindow(null, this);
        setModal(true);
        setVisible(true);

    }

    private TextAreaDialog(JFrame frame, String title, String questionField1, String questionField2, String defField1, String defField2) {
        super(frame);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;

        setModal(false);
        setLayout(new GridBagLayout());
        setName(title);
        btnCancel = new JButton(JDLocale.L("gui.btn_cancel", "Cancel"));
        btnCancel.addActionListener(this);
        btnOk = new JButton(JDLocale.L("gui.btn_ok", "OK"));
        btnOk.addActionListener(this);
        setTitle(title);
        textArea = new JTextPane();
        optTextArea = new JTextPane();

        scrollPane = new JScrollPane(textArea);
        optScrollPane = new JScrollPane(optTextArea);

        setResizable(true);

        textArea.setEditable(true);
        optTextArea.setEditable(true);
        textArea.requestFocusInWindow();

        c.weighty = 1.0;
        c.weightx = 1.0;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 0;
        this.add(new JLabel(questionField1), c);

        c.weighty = 1.0;
        c.weightx = 1.0;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 1;
        this.add(textArea, c);

        if (defField1 != null) {
            textArea.setText(defField1);
        }

        if (defField2 != null) {
            optTextArea.setText(defField2);
        }

        c.weighty = 1.0;
        c.weightx = 1.0;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 2;
        this.add(new JLabel(questionField2), c);

        c.weighty = 1.0;
        c.weightx = 1.0;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 3;
        this.add(optScrollPane, c);

        c.weighty = 1.0;
        c.weightx = 1.0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 4;
        this.add(btnOk, c);

        c.weighty = 1.0;
        c.weightx = 1.0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 1;
        c.gridy = 4;
        this.add(btnCancel, c);

        pack();

        getRootPane().setDefaultButton(btnOk);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new LocationListener());

        SimpleGUI.restoreWindow(null, this);
        setModal(true);
        setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnOk && optTextArea == null) {
            text = textArea.getText();
        } else if (e.getSource() == btnOk && optTextArea != null) {
            text2[0] = textArea.getText();
            text2[1] = optTextArea.getText();
        }
        dispose();
    }

    private String getText() {
        return text;
    }

    private String[] getTextArray() {
        return text2;
    }
}
