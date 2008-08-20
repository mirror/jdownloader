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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

import jd.utils.JDLocale;
import net.miginfocom.swing.MigLayout;

/**
 * Parentklasse für ein 2. Popupfenster.
 * 
 * @author JD-Team
 */
public class ConfigurationPopup extends JDialog implements ActionListener {

    private static final long serialVersionUID = 3815946152967454931L;

    private JButton btnCancel;

    private JButton btnSave;

    private ConfigPanel panel;

    /**
     * Erstellt einen Neuen Dialog
     * 
     * @param parent
     *            (Parent Fenster)
     * @param panel
     *            (ConfigPanel)
     * @param jpanel
     *            (Panel des eigentlichen Konfigfenster)
     */
    public ConfigurationPopup(Frame parent, ConfigPanel panel, JPanel jpanel) {
        super(parent);
        this.panel = panel;

        this.setTitle(JDLocale.L("gui.config.popup.title", "Konfiguration"));
        this.setModal(true);
        this.setLayout(new GridBagLayout());

        btnSave = new JButton(JDLocale.L("gui.config.popup.btn_ok", "OK"));
        btnSave.addActionListener(this);
        btnCancel = new JButton(JDLocale.L("gui.config.popup.btn_cancel", "Abbrechen"));
        btnCancel.addActionListener(this);

        Container bpanel = new JPanel(new MigLayout("", "[grow][]"));
        bpanel.add(btnSave, "w pref!, align right, tag ok right");
        bpanel.add(btnCancel, "w pref!, tag cancel, wrap");

        Container cpanel = new JPanel(new BorderLayout());
        cpanel.add(new JSeparator());
        cpanel.add(bpanel, BorderLayout.SOUTH);

        Container dpanel = new JPanel(new BorderLayout());
        dpanel.add(jpanel);
        dpanel.add(cpanel, BorderLayout.SOUTH);

        setContentPane(new JScrollPane(dpanel));
        this.pack();
        Dimension ps = getPreferredSize();
        Dimension available = Toolkit.getDefaultToolkit().getScreenSize();
        this.setPreferredSize(new Dimension(Math.min(available.width, ps.width), Math.min(available.height, ps.height)));
        this.setMinimumSize(getPreferredSize());
        this.pack();
        panel.setPreferredSize(panel.getMinimumSize());
        this.setLocationRelativeTo(null);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnSave) {
            if (panel != null) {
                panel.save();
            }
        }
        setVisible(false);
    }

}
