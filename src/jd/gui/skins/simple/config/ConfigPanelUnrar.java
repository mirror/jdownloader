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
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.controlling.interaction.Unrar;
import jd.gui.UIInterface;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.Link.JLinkButton;
import jd.unrar.JUnrar;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
/**
 * Konfigurationspanel f�r Unrar
 *
 * @author DwD
 *
 */

public class ConfigPanelUnrar extends ConfigPanel implements ActionListener {

    /**
     *
     */
    private static final long serialVersionUID = -1543456288909278519L;
    /**
     * Instanz zum speichern der parameter
     */
    private Unrar unrar;
    private Configuration configuration;
    public   ConfigPanelUnrar(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);
        this.unrar = Unrar.getInstance();
        initPanel();
        this.configuration = configuration;
        load();
    }

    public void save() {

        this.saveConfigEntries();

        configuration.setProperty(Configuration.PARAM_UNRAR_INSTANCE, unrar);

    }

    public void load() {
        this.loadConfigEntries();
    }

    
    public void initPanel() {
        GUIConfigEntry ce;
        configuration = JDUtilities.getConfiguration();
        String unrarcmd = JDUtilities.getConfiguration().getStringProperty("GUNRARCOMMAND");
        if (unrarcmd == null) {
            unrarcmd = new JUnrar(false).getUnrarCommand();
            if (unrarcmd == null)
                configuration.setProperty("GUNRARCOMMAND", "NOT FOUND");
            else
                configuration.setProperty("GUNRARCOMMAND", unrarcmd);
            JDUtilities.saveConfig();
        } else if (unrarcmd.matches("NOT FOUND"))
            unrarcmd = null;

        // ce = new GUIConfigEntry(new
        // ConfigEntry(ConfigContainer.TYPE_COMBOBOX, configuration,
        // Unrar.PROPERTY_ENABLED_TYPE,new String[] {
        // Unrar.ENABLED_TYPE_ALWAYS,Unrar.ENABLED_TYPE_LINKGRABBER,Unrar.ENABLED_TYPE_NEVER
        // },"Unrar
        // aktivieren:").setDefaultValue(Unrar.ENABLED_TYPE_LINKGRABBER));
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Unrar.PROPERTY_ENABLED, JDLocale.L("gui.config.unrar.enabled", "automatisches entpacken aktivieren")).setDefaultValue(true).setInstantHelp(JDLocale.L("modules.unrar.enabled.instanthelp","http://jdownloader.org/wiki/index.php?title=JDownloader_Wiki:Portal#Module")));
        addGUIConfigEntry(ce);
        if(unrarcmd==null)
        {

            try {
            	JLinkButton bb;
            	if(System.getProperty("os.name").toLowerCase().indexOf("mac") > -1)
            		bb = new JLinkButton(JDLocale.L("gui.config.unrar.download.osx", "Bitte laden sie Unrar/RaR 3.7 oder hoeher herunter"), new URL("http://www.rarlab.com/download.htm"));
            	else
            		bb = new JLinkButton(JDLocale.L("gui.config.unrar.download", "Bitte laden sie Unrar/RaR 3.7 oder hoeher herunter"), new URL("http://www.rarlab.com/rar_add.htm"));
                JDUtilities.addToGridBag(panel, bb, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.NORTHWEST);
            } catch (MalformedURLException e) {
                
                e.printStackTrace();
            }

        }
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_BROWSEFILE, configuration, Unrar.PROPERTY_UNRARCOMMAND, JDLocale.L("gui.config.unrar.cmd")).setDefaultValue(unrarcmd));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Unrar.PROPERTY_AUTODELETE, JDLocale.L("gui.config.unrar.delete")).setDefaultValue(true));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Unrar.PROPERTY_OVERWRITE_FILES, JDLocale.L("gui.config.unrar.overwrite")).setDefaultValue(false));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Unrar.PROPERTY_WAIT_FOR_TERMINATION, JDLocale.L("gui.config.unrar.thread", "Erst nach dem Entpacken mit dem Download fortfahren")).setDefaultValue(false));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_BROWSEFOLDER, configuration, Unrar.PROPERTY_EXTRACTFOLDER, JDLocale.L("gui.config.unrar.extractfolder", "Zielordner: ")).setDefaultValue(null));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Unrar.PROPERTY_ENABLE_EXTRACTFOLDER, JDLocale.L("gui.config.unrar.extractfolderenabled", "Im Zielordner entpacken (Wenn die Dateien nicht im Downloadordner entpackt werden sollen)")).setDefaultValue(false));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Unrar.PROPERTY_DELETE_INFOFILE, JDLocale.L("gui.config.unrar.deleteinfofile", "Info-Datei nach erfolgreichem Entpacken l�schen")).setDefaultValue(true));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, this, JDLocale.L("gui.menu.action.passwordlist.name")));
        addGUIConfigEntry(ce);
        addGUIConfigEntry(new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR)));
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Unrar.PROPERTY_USE_HJMERGE, JDLocale.L("gui.config.unrar.usehjmerge", "HJSplit Dateien automatisch zusammenf�gen")).setDefaultValue(true));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Unrar.PROPERTY_DELETE_MERGEDFILES, JDLocale.L("gui.config.unrar.deletemergedfiles", "Dateiteile nach dem zusammenf�gen l�schen")).setDefaultValue(true));
        addGUIConfigEntry(ce);
        add(panel, BorderLayout.NORTH);

    }

    
    public String getName() {

        return JDLocale.L("modules.unrar.name");
    }

    public void actionPerformed(ActionEvent e) {
        new jdUnrarPasswordListDialog(((SimpleGUI) this.uiinterface).getFrame()).setVisible(true);
    }
}
