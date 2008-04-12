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


package jd.gui.skins.simple.config;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import jd.unrar.JUnrar;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Ein Dialog, der Passwort-Output anzeigen kann.
 * 
 * @author DwD
 */
public class jdUnrarPasswordListDialog extends JDialog implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * JTextField wo der Passwort Output eingetragen wird
     */
    private JTextArea pwField;

    /**
     * JScrollPane fuer das pwField
     */
    private JScrollPane pwScrollPane;

    /**
     * Knopf zum schliessen des Fensters
     */
    private JButton btnCancel;
    /**
     * Knopf zum scheichern der Passwörter
     */
    private JButton btnSave;

    /**
     * Primary Constructor
     * 
     * @param owner
     *            The owning Frame
     */
    @SuppressWarnings("static-access")
	public jdUnrarPasswordListDialog(JFrame owner) {
        super(owner);
        setModal(true);
        setLayout(new GridBagLayout());
        
        btnCancel = new JButton(JDLocale.L("gui.config.unrar.passwordlist.btn_cancel.name"));
        btnCancel.setMnemonic(JDLocale.L("gui.config.unrar.passwordlist.btn_cancel.mnem").charAt(0));
        btnCancel.addActionListener(this);
        
        btnSave = new JButton(JDLocale.L("gui.config.unrar.passwordlist.btn_save.name"));
        btnSave.setMnemonic(JDLocale.L("gui.config.unrar.passwordlist.btn_save.mnem").charAt(0));
        
        btnSave.addActionListener(this);
        getRootPane().setDefaultButton(btnSave);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        pwField = new JTextArea(10, 60);
        pwScrollPane = new JScrollPane(pwField);
        pwField.setEditable(true);
        JUnrar unrar = new JUnrar(false);
        String[] pws = unrar.returnPasswords();
        for (int i = 0; i < pws.length; i++) {
            pwField.append(pws[i] + System.getProperty("line.separator"));
        }

        JDUtilities.addToGridBag(this, pwScrollPane, 0, 0, 2, 1, 1, 1, null, GridBagConstraints.BOTH, GridBagConstraints.CENTER);
        JDUtilities.addToGridBag(this, btnSave, 0, 1, 1, 1, 1, 0, null, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(this, btnCancel, 1, 1, 1, 1, 1, 0, null, GridBagConstraints.NONE, GridBagConstraints.WEST);

        pack();
        setLocation(JDUtilities.getCenterOfComponent(null, this));

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @SuppressWarnings("static-access")
	public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnSave) {
            JUnrar unrar = new JUnrar(false);
            unrar.editPasswordlist(JDUtilities.splitByNewline(pwField.getText()));
            dispose();

        } else {
            dispose();
        }
    }

}