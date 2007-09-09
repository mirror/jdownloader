package jd.gui.skins.simple.config;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import jd.Configuration;
import jd.JDUtilities;
import jd.gui.UIInterface;


public class ConfigurationPopup extends JDialog implements ActionListener {

    private Configuration configuration;

    private ConfigPanel   panel;

    private JButton       btnSave;

    private JButton       btnCancel;

    private UIInterface   uiinterface;

    public ConfigurationPopup(JFrame parent, ConfigPanel panel,JPanel jpanel, UIInterface uiinterface, Configuration configuration) {
        super(parent);
        this.uiinterface = uiinterface;
        setTitle("Konfiguration");
        setModal(true);
        setLayout(new GridBagLayout());
        this.configuration = configuration;

        this.panel = panel;
        btnSave = new JButton("OK");
        btnSave.addActionListener(this);
        btnCancel = new JButton("Abbrechen");
        btnCancel.addActionListener(this);

        Insets insets = new Insets(5, 5, 5, 5);

        JDUtilities.addToGridBag(this, jpanel, 0, 0, 2, 1, 1, 1, null, GridBagConstraints.BOTH, GridBagConstraints.CENTER);
        JDUtilities.addToGridBag(this, btnSave, 0, 1, 1, 1, 1, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(this, btnCancel, 1, 1, 1, 1, 1, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        setLocation(JDUtilities.getCenterOfComponent(parent, this));
       
        pack();
        
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnSave) {
            panel.save();
        }
        setVisible(false);
    }

}
