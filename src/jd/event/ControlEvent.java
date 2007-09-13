package jd.event;

import java.awt.AWTEvent;

/**
 * Diese Klasse realisiert Ereignisse, die zum Steuern des Programmes dienen
 * 
 * @author astaldo
 */
public class ControlEvent extends AWTEvent{
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 1639354503246054870L;
    /**
     * Alle Downloads wurden bearbeitet
     */
    public final static int CONTROL_ALL_DOWNLOADS_FINISHED   = 1;
    /**
     * Ein einzelner Download ist bearbeitet worden
     */
    public final static int CONTROL_SINGLE_DOWNLOAD_CHANGED  = 2;
    /**
     * Das Verteilen des Inhalts der Zwischenablage ist abgeschlossen
     * Als Parameter wird hier ein Vector mit DownloadLinks übergeben, die
     * herausgearbeitet wurden
     */
    public final static int CONTROL_DISTRIBUTE_FINISHED      = 3;
    /**
     * Ein Entschlüsselungs Plugin ist aktiv
     */
    public final static int CONTROL_PLUGIN_DECRYPT_INACTIVE  = 4;
    /**
     * Ein Entschlüsselungs Plugin ist nicht mehr aktiv
     */
    public final static int CONTROL_PLUGIN_DECRYPT_ACTIVE    = 5;
    /**
     * Ein Anbieter Plugin zum Downloaden ist aktiv
     */
    public final static int CONTROL_PLUGIN_HOST_ACTIVE       = 6;
    /**
     * Ein Anbieter Plugin zum Downloaden ist nicht mehr aktiv
     */
    public final static int CONTROL_PLUGIN_HOST_INACTIVE     = 7;
    /**
     * Interaction aktiv
     */
    public final static int CONTROL_PLUGIN_INTERACTION_ACTIVE     = 8;
    /**
     * INteraction inaktiv
     */
    public final static int CONTROL_PLUGIN_INTERACTION_INACTIVE     = 9;
    
    /**. Dieses Event ist unabhängig von inaktiv. eine Interaction die in einem thread läuft kann Aktiv sein und trotzdem schon zurückgekehrt
     */
    public final static int CONTROL_PLUGIN_INTERACTION_RETURNED    = 10;
    /**
     * Zeigt an dass ein Einzelner Download beendet wurde. Der Status kann mit Downloadlink.getStatus() abgefragt werden (parameter)
     */
    public static final int CONTROL_SINGLE_DOWNLOAD_FINISHED = 11;
    /**
     * Gibt an dass ein captcha geladen wurde. der Fiel-Pfad zum captchw wir als parameter erwartet
     */
    public static final int CONTROL_CAPTCHA_LOADED = 12;
    /**
     * Gibt an, dass der Downloadvorgang gestartet wurde
     */
    public static final int CONTROL_ALL_DOWNLOAD_START = 13;
    /**
     * Gibt an dass ein neuer downlaod begonnen hat. der link wir dals parameter erwartet
     */
    public static final int CONTROL_SINGLE_DOWNLOAD_STARTS = 14;
    /**
     * Die ID des Ereignisses
     */
    private int controlID;
    /**
     * Ein optionaler Parameter
     */
    private Object parameter;
    
    public ControlEvent(Object source,int controlID){ this(source, controlID,null); }
    
    public ControlEvent(Object source, int controlID, Object parameter){
        super(source, controlID);
        this.controlID = controlID;
        this.parameter = parameter;
    } 
    public int getID()          { return controlID; }
    public Object getParameter(){ return parameter; }
}
