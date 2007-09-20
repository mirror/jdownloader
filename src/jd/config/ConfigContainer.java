package jd.config;

import java.util.Vector;

import jd.plugins.Plugin;

/**
 * Diese Klasse speichert die GUI-Dialog Informationen. Jede GUI kann diese
 * Infos Abfragen und Entsprechend verarbeiten
 * 
 * @author coalado
 * 
 */
public class ConfigContainer {
    public static final int     TYPE_SPINNER    = 8;

    public static final int     TYPE_BROWSEFILE = 7;

    public static final int     TYPE_SEPERATOR  = 6;

    public static final int     TYPE_RADIOFIELD = 5;

    public static final int     TYPE_LABEL      = 4;

    public static final int     TYPE_CHECKBOX   = 3;

    public static final int     TYPE_BUTTON     = 2;

    public static final int     TYPE_COMBOBOX   = 1;

    public static final int     TYPE_TEXTFIELD  = 0;

    @SuppressWarnings("unused")
    private Object              instance;

    private Vector<ConfigEntry> content         = new Vector<ConfigEntry>();

    public ConfigContainer(Object instance) {
        this.instance = instance;
    }
/**
 * Fügt einen Konfigurationseintrag hinzu
 * @param entry
 */
    public void addEntry(ConfigEntry entry) {
        content.add(entry);
    }
/**
 * Gibt den KOnfigurationseintrag an der stelle i zurück
 * @param i
 * @return ConfigEntry
 */
    public ConfigEntry getEntryAt(int i) {
        if (content.size() <= i) return null;
        return content.elementAt(i);
    }
/**
 * Gibt eine Liste aller gespeicherten ConfigEinträge zurück
 * @return
 */
    public Vector<ConfigEntry> getEntries() {
        return content;
    }

}