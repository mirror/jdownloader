package jd.gui.skins.simple.components.Linkgrabber;

public class UpdateEvent {

    /* Eine Update-Event soll an alle Listener weitergegeben werden */
    /* Der Listener weiss hier selbst welche Informationen für ihn wichtig sind */
    public static final int UPDATE_EVENT = 1;

    /* Ein Empty-Event soll an alle Listener weitergegeben werden */
    /*
     * Ein Empty-Zustand wurde erreicht, der Listener weiss wie er hierauf
     * reagieren soll
     */
    public static final int EMPTY_EVENT = 999;

    private int controlID;
    private Object source;
    private Object parameter;

    public UpdateEvent(Object source, int controlID) {
        this.source = source;
        this.controlID = controlID;
    }

    public UpdateEvent(Object source, int controlID, Object parameter) {
        this(source, controlID);
        this.parameter = parameter;
    }

    public int getID() {
        return controlID;
    }

    public Object getParameter() {
        return parameter;
    }

    public Object getSource() {
        return source;
    }

    public String toString() {
        return "[source:" + source + ", controlID:" + controlID + ", parameter:" + parameter + "]";
    }
}
