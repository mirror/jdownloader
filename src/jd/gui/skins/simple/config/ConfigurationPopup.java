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

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jd.config.Configuration;
import jd.gui.UIInterface;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Parentklasse für ein 2. Popupfenster. Wird momentan zur Konfiguration der Interactions verwendet
 * @author JD-Team
 *
 */
public class ConfigurationPopup extends JDialog implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 3815946152967454931L;

    private JButton       btnCancel;

    private JButton       btnSave;

    @SuppressWarnings("unused")
    private Configuration configuration;

    private ConfigPanel   panel;

    @SuppressWarnings("unused")
    private UIInterface   uiinterface;
/**
 * Erstellt einen Neuen Dialog
 * @param parent (Parent FEnster)
 * @param panel   (ConfigPanel (panel inkl. ok/close buttons etc)
 * @param jpanel  (panel des eigentlichen Konfigfenster)
 * @param uiinterface
 * @param configuration
 */
    public ConfigurationPopup(Frame parent, ConfigPanel panel,JPanel jpanel, UIInterface uiinterface, Configuration configuration) {
        super(parent);
        this.uiinterface = uiinterface;
        setTitle(JDLocale.L("gui.config.popup.title","Konfiguration"));
        setModal(true);
        setLayout(new GridBagLayout());
        this.configuration = configuration;

        this.panel = panel;
        btnSave = new JButton(JDLocale.L("gui.config.popup.btn_ok","OK"));
        btnSave.addActionListener(this);
        btnCancel = new JButton(JDLocale.L("gui.config.popup.btn_cancel","Abbrechen"));
        btnCancel.addActionListener(this);

        Insets insets = new Insets(5, 5, 5, 5);

        JDUtilities.addToGridBag(this, new JScrollPane(jpanel), 0, 0, 2, 1, 1, 1, null, GridBagConstraints.BOTH, GridBagConstraints.FIRST_LINE_START);
        JDUtilities.addToGridBag(this, btnSave, 0, 1, 1, 1, 1, 0, insets, GridBagConstraints.NONE, GridBagConstraints.NORTHEAST);
        JDUtilities.addToGridBag(this, btnCancel, 1, 1, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.NORTHEAST);
        setLocation(JDUtilities.getCenterOfComponent(parent, this));
       
        pack();
        
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnSave) {
            if(panel!=null)panel.save();
        }
        setVisible(false);
    }

}
