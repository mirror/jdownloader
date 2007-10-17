package jd.unrar;

import java.io.File;
import java.io.Serializable;

public class Config implements Serializable{
    private static final long serialVersionUID = 1L;
    /**
     * Homepfad zu unrarit
     */
    public static final File unraritHome = new File(System.getProperty("user.home"),".unrarit");
     
    /**
     * pfad zum unrarBefehl
     */
    public String unrar = null;
    /**
     * Loescht automatisch die Rar-Archive bei erfolgreichem entpacken
     */
    public boolean autoDelete = true;
    /**
     * wenn overwriteFiles true ist werden vorhandene Dateien automatisch
     * ueberschrieben
     */
    public boolean overwriteFiles = false;
    /**
     * absoluter Pfad zur Passwordliste
     */
    public File passwordList = new File(unraritHome, "passwordlist.xml");
    /**
     * maximale Dateigroesse fuer fie Passwortsuche
     */
    public int maxFilesize = 500000;
    /**
     * Ob neue Passwoerter automatisch zur Passwortliste hinzugefuegt werden sollen
     */
    public boolean addToPasswordList=true;

}
