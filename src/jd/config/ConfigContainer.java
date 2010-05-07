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

package jd.config;

import java.io.Serializable;
import java.util.ArrayList;

import javax.swing.ImageIcon;

import jd.controlling.ListController;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

/**
 * Diese Klasse speichert die GUI-Dialog Informationen. Jede GUI kann diese
 * Infos Abfragen und Entsprechend verarbeiten
 * 
 * @author JD-Team
 * 
 */
public class ConfigContainer implements Serializable {
    public static final int ACTION_REQUEST_SAVE = 0;
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 6583843494325603616L;
    /**
     * ConfigElement ist ein Textfeld
     * 
     * @see ConfigEntry#ConfigEntry(int, Property, String, String)
     */
    public static final int TYPE_TEXTFIELD = 0;
    /**
     * ConfigElement ist ein Combobox
     * 
     * @see ConfigEntry#ConfigEntry(int, Property, String, Object[], String)
     */
    public static final int TYPE_COMBOBOX = 1;
    /**
     * ConfigElement ist ein Button ConfigEntry(int type, ActionListener
     * listener, String label)
     * 
     * @see ConfigEntry#ConfigEntry(int, ActionListener, String, String,
     *      ImageIcon)
     */
    public static final int TYPE_BUTTON = 2;
    /**
     * ConfigElement ist eine Checkbox
     * 
     * @see ConfigEntry#ConfigEntry(int, Property, String, String)
     */
    public static final int TYPE_CHECKBOX = 3;
    /**
     * ConfigElement ist ein Label
     * 
     * @see ConfigEntry#ConfigEntry(int, String)
     */
    public static final int TYPE_LABEL = 4;
    /**
     * ConfigElement ist ein Radiobutton <br>
     * <b>TODO:</b> Unused for now!
     * 
     * @see ConfigEntry#ConfigEntry(int, Property, String, Object[], String)
     */
    public static final int TYPE_RADIOFIELD = 5;
    /**
     * ConfigElement ist eine Trennlinie ConfigEntry(int type)
     * 
     * @see ConfigEntry#ConfigEntry(int)
     */
    public static final int TYPE_SEPARATOR = 6;
    /**
     * ConfigElement ist ein Browser für eine Datei
     * 
     * @see ConfigEntry#ConfigEntry(int, Property, String, String)
     */
    public static final int TYPE_BROWSEFILE = 7;
    /**
     * ConfigElement ist eine Zahlenkomponente (Spinner)
     * 
     * @see ConfigEntry#ConfigEntry(int, Property, String, String, int, int)
     */
    public static final int TYPE_SPINNER = 8;
    /**
     * ConfigElement ist ein Browser für ein Verzeichnis
     * 
     * @see ConfigEntry#ConfigEntry(int, Property, String, String)
     */
    public static final int TYPE_BROWSEFOLDER = 9;
    /**
     * ConfigElement ist ein Textbereich
     * 
     * @see ConfigEntry#ConfigEntry(int, Property, String, String)
     */
    public static final int TYPE_TEXTAREA = 10;
    /**
     * ConfigElement ist ein Textbereich
     * 
     * @see ConfigEntry#ConfigEntry(int, Property, String, String)
     */
    public static final int TYPE_PASSWORDFIELD = 11;

    /**
     * <b>TODO:</b> Unused for now!
     */
    public static final int TYPE_LINK = 12;

    public static final int TYPE_CONTAINER = 13;

    /**
     * ConfigElement ist ein Textbereich, welcher von einem eigenen Controller
     * verwaltet wird (siehe PasswordListController und HTAccessController)
     * 
     * @see ConfigEntry#ConfigEntry(int, ListController, String)
     */
    public static final int TYPE_LISTCONTROLLED = 110;
    /**
     * ConfigElement ist ein Combobox dessen Index (und nicht Text) gespeichert
     * und geladen wird
     * 
     * @see ConfigEntry#ConfigEntry(int, Property, String, Object[], String)
     */
    public static final int TYPE_COMBOBOX_INDEX = 15;

    private ArrayList<ConfigEntry> content;
    private Property propertyInstance;
    private String title;
    private ConfigGroup group;
    private ImageIcon icon;

    public ConfigContainer() {
        this(JDL.L("config.container.defaultname", "General"));
    }

    public ConfigContainer(String title) {
        this.title = title;
        propertyInstance = JDUtilities.getConfiguration();
        content = new ArrayList<ConfigEntry>();
    }

    /**
     * Fügt einen Konfigurationseintrag hinzu
     * 
     * @param entry
     *            Der Eintrag, der hinzugefügt werden soll
     */
    public void addEntry(ConfigEntry entry) {
        if (entry.getGroup() == null) entry.setGroup(group);

        if (entry.getPropertyInstance() == null) {
            entry.setPropertyInstance(propertyInstance);
        }
        content.add(entry);
    }

    /**
     * Gibt eine Liste aller gespeicherten Konfigurationseinträge zurück
     * 
     * @return Liste aller gespeicherten Konfigurationseinträge
     */
    public ArrayList<ConfigEntry> getEntries() {
        return content;
    }

    /**
     * Gibt die Propertyinstanz zurück die dieser container zum speichern
     * verwendet(Es werden nur die einstellungen überdeckt bei denen die
     * propertyinstanz bei den COnfigEntries null ist Default ist die
     * CONFIGURATION
     * 
     * @return
     */
    public Property getPropertyInstance() {
        return propertyInstance;
    }

    public String getTitle() {
        return title;
    }

    /**
     * Setzt die Propertyinstanz zurück, die dieser Container zum Speichern
     * verwendet(Es werden nur die einstellungen überdeckt bei denen die
     * propertyinstanz bei den ConfigEntries null ist
     */
    public void setPropertyInstance(Property propertInstance) {
        propertyInstance = propertInstance;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Sets a configgroup for this container. the containers add method will add
     * this configgroup to every new entry
     * 
     * @param configGroup
     */
    public void setGroup(ConfigGroup configGroup) {
        this.group = configGroup;
    }

    public ConfigGroup getGroup() {
        return group;
    }

    /**
     * Sets the icon for the ConfigContainer. It can be used for the tab-header
     * or the config-info-panel.
     * 
     * @param icon
     */
    public void setIcon(ImageIcon icon) {
        this.icon = icon;
    }

    public ImageIcon getIcon() {
        return icon;
    }

}