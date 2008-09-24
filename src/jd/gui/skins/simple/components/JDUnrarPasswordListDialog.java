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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import jd.parser.Regex;
import jd.unrar.UnrarPassword;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Ein Dialog, der Passwort-Output anzeigen kann.
 * 
 * @author DwD
 */
public class JDUnrarPasswordListDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;

    /**
     * Knopf zum schliessen des Fensters
     */
    private JButton btnCancel;

    /**
     * Knopf zum scheichern der Passwörter
     */
    private JButton btnSave;

    /**
     * JTextField wo der Passwort Output eingetragen wird
     */
    private JTextArea pwField;

    /**
     * Primary Constructor
     * 
     * @param owner
     *            The owning Frame
     */
    public JDUnrarPasswordListDialog(Frame owner) {
        super(owner, JDLocale.L("gui.config.unrar.passwordlist", "Password List"));
        this.setModal(true);
        int n = 10;

        btnCancel = new JButton(JDLocale.L("gui.btn_cancel", "Abbrechen"));
        btnCancel.addActionListener(this);

        btnSave = new JButton(JDLocale.L("gui.btn_save", "Speichern"));
        btnSave.addActionListener(this);

        pwField = new JTextArea();
        pwField.setEditable(true);
        for (String element : UnrarPassword.returnPasswords()) {
            pwField.append(element + System.getProperty("line.separator"));
        }
        pwField.setCaretPosition(0);

        JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.CENTER, n, 0));
        bpanel.add(btnSave);
        bpanel.add(btnCancel);

        JPanel panel = new JPanel(new BorderLayout(n, n));
        panel.setBorder(new EmptyBorder(n, n, n, n));
        panel.add(new JScrollPane(pwField), BorderLayout.CENTER);
        panel.add(bpanel, BorderLayout.SOUTH);

        this.getRootPane().setDefaultButton(btnSave);
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.setContentPane(panel);
        this.setPreferredSize(new Dimension(400, 300));
        this.pack();
        this.setLocation(JDUtilities.getCenterOfComponent(null, this));
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnSave) {
            UnrarPassword.editPasswordlist(Regex.getLines(pwField.getText()));
        }
        dispose();
    }

}