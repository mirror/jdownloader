package jd.controlling.reconnect.pluginsinc.liveheader.validate;

public class ScriptValidationExeption extends Exception {

    /**
     * 
     */
    public ScriptValidationExeption() {
        super();
    }

    public ScriptValidationExeption(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message
     */
    public ScriptValidationExeption(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public ScriptValidationExeption(Throwable cause) {
        super(cause);
    }

}
