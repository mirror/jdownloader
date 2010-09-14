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

/**
 * Diese Klasse speichert die GUI-Dialog Informationen. Jede GUI kann diese
 * Infos Abfragen und Entsprechend verarbeiten
 * 
 * @author JD-Team
 * 
 */
public class ConfigContainer implements Serializable {
    /**
     * serialVersionUID
     */
    private static final long            serialVersionUID    = 6583843494325603616L;
    /**
     * ConfigElement ist ein Textfeld
     * 
     * @see ConfigEntry#ConfigEntry(int, Property, String, String)
     */
    public static final int              TYPE_TEXTFIELD      = 0;
    /**
     * ConfigElement ist ein Combobox
     * 
     * @see ConfigEntry#ConfigEntry(int, Property, String, Object[], String)
     */
    public static final int              TYPE_COMBOBOX       = 1;
    /**
     * ConfigElement ist ein Button ConfigEntry(int type, ActionListener
     * listener, String label)
     * 
     * @see ConfigEntry#ConfigEntry(int, ActionListener, String, String,
     *      ImageIcon)
     */
    public static final int              TYPE_BUTTON         = 2;
    /**
     * ConfigElement ist eine Checkbox
     * 
     * @see ConfigEntry#ConfigEntry(int, Property, String, String)
     */
    public static final int              TYPE_CHECKBOX       = 3;
    /**
     * ConfigElement ist ein Label
     * 
     * @see ConfigEntry#ConfigEntry(int, String)
     */
    public static final int              TYPE_LABEL          = 4;
    /**
     * ConfigElement ist ein Radiobutton <br>
     * <b>TODO:</b> Unused for now!
     * 
     * @see ConfigEntry#ConfigEntry(int, Property, String, Object[], String)
     */
    public static final int              TYPE_RADIOFIELD     = 5;
    /**
     * ConfigElement ist eine Trennlinie
     * 
     * @see ConfigEntry#ConfigEntry(int)
     */
    public static final int              TYPE_SEPARATOR      = 6;
    /**
     * ConfigElement ist ein Browser für eine Datei
     * 
     * @see ConfigEntry#ConfigEntry(int, Property, String, String)
     */
    public static final int              TYPE_BROWSEFILE     = 7;
    /**
     * ConfigElement ist eine Zahlenkomponente (Spinner)
     * 
     * @see ConfigEntry#ConfigEntry(int, Property, String, String, int, int,
     *      int)
     */
    public static final int              TYPE_SPINNER        = 8;
    /**
     * ConfigElement ist ein Browser für ein Verzeichnis
     * 
     * @see ConfigEntry#ConfigEntry(int, Property, String, String)
     */
    public static final int              TYPE_BROWSEFOLDER   = 9;
    /**
     * ConfigElement ist ein Textbereich
     * 
     * @see ConfigEntry#ConfigEntry(int, Property, String, String)
     */
    public static final int              TYPE_TEXTAREA       = 10;
    /**
     * ConfigElement ist ein Textbereich
     * 
     * @see ConfigEntry#ConfigEntry(int, Property, String, String)
     */
    public static final int              TYPE_PASSWORDFIELD  = 11;

    /**
     * ConfigElement ist eine Swing-Komponente
     * 
     * @see ConfigEntry#ConfigEntry(int, javax.swing.JComponent, String)
     */
    public static final int              TYPE_COMPONENT      = 12;
    /**
     * ConfigElement ist ein Textbereich, welcher von einem eigenen Controller
     * verwaltet wird (siehe PasswordListController und HTAccessController)
     * 
     * @see ConfigEntry#ConfigEntry(int, ListController, String)
     */
    public static final int              TYPE_LISTCONTROLLED = 110;
    /**
     * ConfigElement ist ein Combobox dessen Index (und nicht Text) gespeichert
     * und geladen wird
     * 
     * @see ConfigEntry#ConfigEntry(int, Property, String, Object[], String)
     */
    public static final int              TYPE_COMBOBOX_INDEX = 15;

    private final ArrayList<ConfigEntry> content;
    private String                       title;
    private ConfigGroup                  group;
    private ImageIcon                    icon;

    public ConfigContainer() {
        this.content = new ArrayList<ConfigEntry>();
    }

    public ConfigContainer(final String title) {
        this();
        this.title = title;
    }

    /**
     * Fügt einen Konfigurationseintrag hinzu
     * 
     * @param entry
     *            Der Eintrag, der hinzugefügt werden soll
     */
    public void addEntry(final ConfigEntry entry) {
        if (entry.getGroup() == null) {
            entry.setGroup(group);
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

    public String getTitle() {
        return title;
    }

    /**
     * Sets a configgroup for this container. the containers add method will add
     * this configgroup to every new entry
     * 
     * @param configGroup
     */
    public void setGroup(final ConfigGroup configGroup) {
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
    public void setIcon(final ImageIcon icon) {
        this.icon = icon;
    }

    public ImageIcon getIcon() {
        return icon;
    }

}