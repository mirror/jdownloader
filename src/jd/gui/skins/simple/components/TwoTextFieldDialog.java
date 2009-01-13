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
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import jd.gui.skins.simple.LocationListener;
import jd.gui.skins.simple.SimpleGUI;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Diese Klasse ist wie die Optionspane mit textfeld nur mit zwei textfeldern
 * 
 * @author gluewurm
 */
public class TwoTextFieldDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = -655039113948925165L;

    public static String[] showDialog(JFrame frame, String title, String questionOne, String questionTwo, String defaultOne, String defaultTwo) {
        TwoTextFieldDialog tda = new TwoTextFieldDialog(frame, title, questionOne, questionTwo, defaultOne, defaultTwo);
        return tda.getTextArray();
    }

    private JButton btnCancel;
    private JButton btnOk;

    protected Insets insets = new Insets(0, 0, 0, 0);
    protected Logger logger = JDUtilities.getLogger();

    private String[] text2 = new String[2];

    private JTextField textField;
    private JTextField textField2;

    private TwoTextFieldDialog(JFrame frame, String title, String questionField1, String questionField2, String defField1, String defField2) {
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
        textField = new JTextField(defField1);
        textField2 = new JTextField(defField2);

        setResizable(true);

        textField.setEditable(true);
        textField2.setEditable(true);
        textField.requestFocusInWindow();

        c.weighty = 1.0;
        c.weightx = 1.0;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 0;
        this.add(new JLabel(questionField1), c);

        c.gridx = 0;
        c.gridy = 1;
        this.add(textField, c);

        c.gridx = 0;
        c.gridy = 2;
        this.add(new JLabel(questionField2), c);

        c.gridx = 0;
        c.gridy = 3;
        this.add(textField2, c);

        c.weighty = 1.0;
        c.weightx = 1.0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 4;
        this.add(btnOk, c);

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
        if (e.getSource() == btnOk) {
            text2[0] = textField.getText();
            text2[1] = textField2.getText();
        }
        dispose();
    }

    private String[] getTextArray() {
        return text2;
    }
}
