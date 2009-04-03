package jd.event;

public abstract class JDEvent {
    
    private int ID;
    private Object source;
    private Object parameter;

    public JDEvent(Object source, int ID) {
        this.source = source;
        this.ID = ID;
    }

    public JDEvent(Object source, int ID, Object parameter) {
        this(source, ID);
        this.parameter = parameter;
    }

    public int getID() {
        return ID;
    }

    public Object getParameter() {
        return parameter;
    }

    public Object getSource() {
        return source;
    }

    public String toString() {
        return "[source:" + source + ", controlID:" + ID + ", parameter:" + parameter + "]";
    }

}
