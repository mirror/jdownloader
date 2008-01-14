package jd.config;

import java.io.Serializable;
import java.util.Vector;
import jd.utils.JDUtilities;

/**
 * Diese Klasse speichert die GUI-Dialog Informationen. Jede GUI kann diese
 * Infos Abfragen und Entsprechend verarbeiten
 * 
 * @author coalado
 * 
 */
public class ConfigContainer implements Serializable {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 6583843494325603616L;
    /**
     * ConfigElement ist eine Zahlenkomponente (Spinner)
     * ConfigEntry(int type, Property propertyInstance, String propertyName, String label, int start, int end) 
     */
    public static final int     TYPE_SPINNER    = 8;
    /**
     * ConfigElement ist ein Browser für eine Datei
     * public ConfigEntry(int type, Property propertyInstance, String propertyName, String label) {
     */
    public static final int     TYPE_BROWSEFILE = 7;
    /**
     * ConfigElement ist eine Trennlinie
     * ConfigEntry(int type)
     */
    public static final int     TYPE_SEPARATOR  = 6;
    /**
     * ConfigElement ist ein Radiobutton
     * ConfigEntry(int type, Property propertyInstance, String propertyName, Object[] list, String label)
     */
    public static final int     TYPE_RADIOFIELD = 5;
    /**
     * ConfigElement ist ein Label
     * ConfigEntry(int type, String label)
     */
    public static final int     TYPE_LABEL      = 4;
    /**
     * ConfigElement ist eine Checkbox
     */
    public static final int     TYPE_CHECKBOX   = 3;
    /**
     * ConfigElement ist ein Button
     * ConfigEntry(int type, ActionListener listener, String label)
     */
    public static final int     TYPE_BUTTON     = 2;
    /**
     * ConfigElement ist ein Combobox
     * ConfigEntry(int type, Property propertyInstance, String propertyName, Object[] list, String label)
     */
    public static final int     TYPE_COMBOBOX   = 1;
    /**
     * ConfigElement ist ein Textfeld
     *     public ConfigEntry(int type, Property propertyInstance, String propertyName, String label) {
     */
    public static final int     TYPE_TEXTFIELD  = 0;
    /**
     * ConfigElement ist ein Browser für ein Verzeichnis
     * public ConfigEntry(int type, Property propertyInstance, String propertyName, String label) {
     */
    public static final int TYPE_BROWSEFOLDER = 9;
    /**
     * ConfigElement ist ein Textbereich
     * public ConfigEntry(int type, Property propertyInstance, String propertyName, String label) {
     */
    public static final int TYPE_TEXTAREA = 10;
    
    /**
     * ConfigElement ist ein Textbereich
     *     public ConfigEntry(int type, Property propertyInstance, String propertyName, String label) {
     */
    public static final int TYPE_PASSWORDFIELD = 11;
    public static final int TYPE_LINK = 12;
    

    private Property propertyInstance;

    @SuppressWarnings("unused")
    private Object              instance;

    private Vector<ConfigEntry> content         = new Vector<ConfigEntry>();

    public ConfigContainer(Object instance) {
        this.instance = instance;
        propertyInstance=JDUtilities.getConfiguration();
    }
    /**
     * Fügt einen Konfigurationseintrag hinzu
     * @param entry Der Eintrag, der hinzugefügt werden soll
     */
    public void addEntry(ConfigEntry entry) {
        if(entry.getPropertyInstance()==null){
            entry.setPropertyInstance(this.propertyInstance);
        }
        content.add(entry);
    }
    /**
     * Gibt den Konfigurationseintrag an der Stelle i zurück
     * @param i Index des Eintrags
     * @return ConfigEntry
     */
    public ConfigEntry getEntryAt(int i) {
        if (content.size() <= i) return null;
        return content.elementAt(i);
    }
    /**
     * Gibt eine Liste aller gespeicherten Konfigurationseinträge zurück
     * @return Liste aller gespeicherten Konfigurationseinträge
     */
    public Vector<ConfigEntry> getEntries() {
        return content;
    }
    /**
     * Gibt die Propertyinstanz zurück die dieser container zum speichern verwendet(Es werden nur die einstellungen überdeckt bei denen die propertyinstanz bei den COnfigEntries null ist
     * Default ist die configuration
     * @return
     */
    public Property getPropertyInstance() {
        return propertyInstance;
    }

    /**
     * Setzt die Propertyinstanz zurück, die dieser Container zum Speichern verwendet(Es werden nur die einstellungen überdeckt bei denen die propertyinstanz bei den ConfigEntries null ist
     */
    public void setPropertyInstance(Property propertInstance) {
        this.propertyInstance = propertInstance;
    }

}