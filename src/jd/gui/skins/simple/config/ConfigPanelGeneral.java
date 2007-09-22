package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.event.UIEvent;
import jd.gui.UIInterface;
import jd.gui.skins.simple.components.BrowseFile;
import jd.plugins.Plugin;
import jd.utils.JDUtilities;

public class ConfigPanelGeneral extends ConfigPanel{
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 3383448498625377495L;



    private JLabel            lblHomeDir;



    private BrowseFile        brsHomeDir;


    ConfigPanelGeneral(Configuration configuration, UIInterface uiinterface) {
        super(configuration, uiinterface);
        initPanel();

        load();

    }

    public void save() {
        this.saveConfigEntries();

        Plugin.getLogger().setLevel((Level) configuration.getProperty(Configuration.PARAM_LOGGER_LEVEL));
        if (JDUtilities.getHomeDirectory() != null) {
            JDUtilities.writeJDHomeDirectoryToWebStartCookie(brsHomeDir.getText().trim());
        }
        uiinterface.fireUIEvent(new UIEvent(uiinterface, UIEvent.UI_INTERACT_UPDATE));
    }

    @Override
    public void initPanel() {

       
        
   
       

        GUIConfigEntry ce;

        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, configuration, Configuration.PARAM_LOGGER_LEVEL, new Level[] { Level.ALL, Level.FINEST, Level.FINER, Level.FINE, Level.INFO, Level.WARNING, Level.SEVERE, Level.OFF }, "Level für's Logging").setDefaultValue(Level.FINER));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_BROWSEFOLDER, configuration, Configuration.PARAM_DOWNLOAD_DIRECTORY, "Default Download Verzeichnis").setDefaultValue(JDUtilities.getJDHomeDirectory().getAbsolutePath()));
        addGUIConfigEntry(ce);
        
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, configuration, Configuration.PARAM_FINISHED_DOWNLOADS_ACTION, new String[] { Configuration.FINISHED_DOWNLOADS_REMOVE, Configuration.FINISHED_DOWNLOADS_REMOVE_AT_START,Configuration.FINISHED_DOWNLOADS_NO_REMOVE}, "Fertig gestellte Downloads ...").setDefaultValue(Configuration.FINISHED_DOWNLOADS_REMOVE_AT_START));
        addGUIConfigEntry(ce);

        int row = 0;

        if (JDUtilities.getHomeDirectory() != null) {
            brsHomeDir=new BrowseFile();
            brsHomeDir.setText(JDUtilities.getHomeDirectory());
            brsHomeDir.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            JDUtilities.addToGridBag(panel, lblHomeDir, 0, row, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);

            JDUtilities.addToGridBag(panel, brsHomeDir, 1, row, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
            row++;
        }

        add(panel, BorderLayout.NORTH);

    }

    @Override
    public void load() {
        this.loadConfigEntries();

    }

    @Override
    public String getName() {

        return "Allgemein";
    }

}
