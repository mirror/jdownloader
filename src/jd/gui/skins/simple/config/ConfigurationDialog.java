package jd.gui.skins.simple.config;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import jd.Configuration;
import jd.JDUtilities;

public class ConfigurationDialog extends JDialog implements ActionListener{
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 4046836223202290819L;
    private Configuration configuration;
    private JTabbedPane tabbedPane;
    private ConfigPanelGeneral configPanelGeneral;
    private ConfigPanelRouter configPanelRouter;
    private ConfigPanelAutomatic configPanelAutomatic;
    private JButton btnSave;
    private JButton btnCancel;
    private boolean configChanged = false;
    
    
    private ConfigurationDialog(JFrame parent){
        super(parent);
        setModal(true);
        setLayout(new GridBagLayout());
        configuration = JDUtilities.getConfiguration();
        configPanelGeneral   = new ConfigPanelGeneral(configuration);
        configPanelRouter    = new ConfigPanelRouter(configuration);
        configPanelAutomatic = new ConfigPanelAutomatic(configuration);
        btnSave = new JButton("Speichern");
        btnSave.addActionListener(this);
        btnCancel = new JButton("Abbrechen");
        btnCancel.addActionListener(this);
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Allgemein",     configPanelGeneral);
        tabbedPane.addTab("Router",        configPanelRouter);
        tabbedPane.addTab("Automatisches", configPanelAutomatic);
        
        Insets insets = new Insets(5,5,5,5);
        
        JDUtilities.addToGridBag(this, tabbedPane, 0, 0, 2, 1, 1, 1, null, GridBagConstraints.BOTH, GridBagConstraints.CENTER);
        JDUtilities.addToGridBag(this, btnSave,    0, 1, 1, 1, 1, 0, insets, GridBagConstraints.NONE, GridBagConstraints.CENTER);
        JDUtilities.addToGridBag(this, btnCancel,  1, 1, 1, 1, 1, 0, insets, GridBagConstraints.NONE, GridBagConstraints.CENTER);
        
        pack();
    }
    public static boolean showConfig(JFrame frame){
        ConfigurationDialog c = new ConfigurationDialog(frame);
        c.setLocation(JDUtilities.getCenterOfComponent(frame, c));
        c.setVisible(true);
        return c.configChanged;
    }

    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == btnSave){
            configPanelGeneral.save();
            configPanelRouter.save();
            configPanelAutomatic.save();
            configChanged=true;
            JDUtilities.setConfiguration(configuration);
            JDUtilities.saveObject(null, JDUtilities.getConfiguration(), JDUtilities.getJDHomeDirectory(), "jdownloader", ".config", true);
        }
        setVisible(false);
    }
    
}
