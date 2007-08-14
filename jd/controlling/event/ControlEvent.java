package jd.controlling.event;
/**
 * Diese Klasse realisiert Ereignisse, die zum Steuern des Programmes dienen
 * 
 * @author astaldo
 */
public class ControlEvent {
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
     * Die ID des Ereignisses
     */
    private int controlID;
    /**
     * Ein optionaler Parameter
     */
    private Object parameter;
    
    public ControlEvent(int controlID){ this(controlID,null); }
    
    public ControlEvent(int controlID, Object parameter){
        this.controlID = controlID;
        this.parameter = parameter;
    } 
    public int getID()          { return controlID; }
    public Object getParameter(){ return parameter; }
}
