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

package jd.config;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.Vector;

import jd.utils.JDLocale;
import jd.utils.JDUtilities;

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
     * ConfigElement ist ein Browser für eine Datei public ConfigEntry(int type,
     * Property propertyInstance, String propertyName, String label) {
     */
    public static final int TYPE_BROWSEFILE = 7;
    /**
     * ConfigElement ist ein Browser für ein Verzeichnis public ConfigEntry(int
     * type, Property propertyInstance, String propertyName, String label) {
     */
    public static final int TYPE_BROWSEFOLDER = 9;
    /**
     * ConfigElement ist ein Button ConfigEntry(int type, ActionListener
     * listener, String label)
     */
    public static final int TYPE_BUTTON = 2;
    /**
     * ConfigElement ist eine Checkbox
     */
    public static final int TYPE_CHECKBOX = 3;
    /**
     * ConfigElement ist ein Combobox ConfigEntry(int type, Property
     * propertyInstance, String propertyName, Object[] list, String label)
     */
    public static final int TYPE_COMBOBOX = 1;
    public static final int TYPE_CONTAINER = 13;
    /**
     * ConfigElement ist ein Label ConfigEntry(int type, String label)
     */
    public static final int TYPE_LABEL = 4;
    public static final int TYPE_LINK = 12;
    /**
     * ConfigElement ist ein Textbereich public ConfigEntry(int type, Property
     * propertyInstance, String propertyName, String label) {
     */
    public static final int TYPE_PASSWORDFIELD = 11;
    /**
     * ConfigElement ist ein Radiobutton ConfigEntry(int type, Property
     * propertyInstance, String propertyName, Object[] list, String label)
     */
    public static final int TYPE_RADIOFIELD = 5;

    /**
     * ConfigElement ist eine Trennlinie ConfigEntry(int type)
     */
    public static final int TYPE_SEPARATOR = 6;
    /**
     * ConfigElement ist eine Zahlenkomponente (Spinner) ConfigEntry(int type,
     * Property propertyInstance, String propertyName, String label, int start,
     * int end)
     */
    public static final int TYPE_SPINNER = 8;
    /**
     * ConfigElement ist ein Textbereich public ConfigEntry(int type, Property
     * propertyInstance, String propertyName, String label) {
     */
    public static final int TYPE_TEXTAREA = 10;
    /**
     * ConfigElement ist ein Textfeld public ConfigEntry(int type, Property
     * propertyInstance, String propertyName, String label) {
     */
    public static final int TYPE_TEXTFIELD = 0;

    private ActionListener actionListener;

    private int containers = 0;

    private Vector<ConfigEntry> content = new Vector<ConfigEntry>();
    @SuppressWarnings("unused")
    private Object instance;
    private Property propertyInstance;
    private String title;

    public ConfigContainer(Object instance) {
        this.instance = instance;
        title = JDLocale.L("config.container.defaultname", "Allgemein");
        propertyInstance = JDUtilities.getConfiguration();
    }

    public ConfigContainer(Object instance, String title) {
        this.instance = instance;
        this.title = title;
        propertyInstance = JDUtilities.getConfiguration();
    }

    /**
     * Fügt einen Konfigurationseintrag hinzu
     * 
     * @param entry
     *            Der Eintrag, der hinzugefügt werden soll
     */
    public void addEntry(ConfigEntry entry) {
        if (entry.getContainer() != null) {
            containers++;
        }
        if (entry.getPropertyInstance() == null) {
            entry.setPropertyInstance(propertyInstance);
        }
        content.add(entry);
    }

    public int getContainerNum() {
        return containers;
    }

    /**
     * Gibt eine Liste aller gespeicherten Konfigurationseinträge zurück
     * 
     * @return Liste aller gespeicherten Konfigurationseinträge
     */
    public Vector<ConfigEntry> getEntries() {
        return content;
    }

    /**
     * Gibt den Konfigurationseintrag an der Stelle i zurück
     * 
     * @param i
     *            Index des Eintrags
     * @return ConfigEntry
     */
    public ConfigEntry getEntryAt(int i) {
        if (content.size() <= i) { return null; }
        return content.elementAt(i);
    }

    /**
     * Gibt die Propertyinstanz zurück die dieser container zum speichern
     * verwendet(Es werden nur die einstellungen überdeckt bei denen die
     * propertyinstanz bei den COnfigEntries null ist Default ist die
     * configuration
     * 
     * @return
     */
    public Property getPropertyInstance() {
        return propertyInstance;
    }

    public String getTitle() {
        return title;
    }

    public void requestSave() {
        if (actionListener == null) { return; }
        actionListener.actionPerformed(new ActionEvent(this, ACTION_REQUEST_SAVE, "save"));

    }

    public void setActionListener(ActionListener listener) {
        actionListener = listener;

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

}