package jd.plugins;

/**
 * Diese Klasse bildet jeden einzelnen Schritt ab, die ein Plugin nacheinander
 * abarbeiten muß
 * 
 * @author astaldo
 */
public class PluginStep {
    /**
     * ID für eine gewisse Wartezeit. Der dazugehörige Wert ist die Zeit in
     * Millisekunden
     */
    public final static int STEP_WAIT_TIME        = 1;
    /**
     * Das Captcha Bild soll erkannt werden. Als Rückgabe wird der erkannte Text
     * als Parameter übergeben
     */
    public final static int STEP_GET_CAPTCHA_FILE = 2;
    /**
     * Der Download soll durchgeführt werden. Als Parameter wird der
     * DownloadLink übergeben
     */
    public final static int STEP_DOWNLOAD         = 3;
    /**
     * Ein Link soll entschlüsselt werden
     */
    public final static int STEP_DECRYPT          = 4;
    /**
     * Eine Seite soll geladen werden. Zum auswerten von Parametern etc
     */
    public final static int STEP_PAGE             = 5;
    /**
     * Eine Containerdatei soll geöffnet werden
     */
    public final static int STEP_OPEN_CONTAINER   = 6;
    /**
     * Warten. Diese Wartezeit muss sein. Reconnecten bringt nichts und wird
     * auch nicht ausgeführt
     */
    public final static int STEP_PENDING          = 7;
    public static final int STEP_SEARCH           = 8;
    public static final int STEP_LOGIN            = 9;
    /**
     * Dieser Schritt muß erst noch stattfinden
     */
    public final static int STATUS_TODO           = 1;
    /**
     * Dieser Schritt wurde fehlerhaft abgearbeitet
     */
    public final static int STATUS_ERROR          = 2;
    /**
     * Dieser Schritt wurde erfolgreich durchgeführt
     */
    public final static int STATUS_DONE           = 3;
    /**
     * Dieser Schritt erfordert eine Interaktion
     */
    public final static int STATUS_USER_INPUT     = 4;
    /**
     * Dieser Schritt sollte nochmal wiederholt werden
     */
    public final static int STATUS_RETRY          = 5;
    /**
     * Schritt wird übersprungen
     */
    public final static int STATUS_SKIP           = 6;
    /**
     * Status dieses Schrittes
     */
    private int             status                = 0;
    /**
     * ID dieses Schrittes
     */
    private int             step;
    /**
     * Ein optionaler Parameter
     */
    private Object          parameter;
    public PluginStep(int step, Object parameter) {
        this.step = step;
        this.parameter = parameter;
    }
    public int getStep() {
        return step;
    }
    public Object getParameter() {
        return parameter;
    }
    public void setParameter(Object parameter) {
        this.parameter = parameter;
    }
    public int getStatus() {
        return status;
    }
    public void setStatus(int status) {
        this.status = status;
    }
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("");
        switch (step) {
            case STEP_WAIT_TIME:
                buffer.append("Warten ");
                break;
            case STEP_GET_CAPTCHA_FILE:
                buffer.append("Captcha ");
                break;
            case STEP_DOWNLOAD:
                buffer.append("Download ");
                break;
            case STEP_DECRYPT:
                buffer.append("Decrypt ");
                break;
            case STEP_PAGE:
                buffer.append("Laden... ");
                break;
        }
        switch (status) {
            case STATUS_DONE:
                buffer.append("status: Fertig");
                break;
            case STATUS_ERROR:
                buffer.append("status: Fehler");
                break;
            case STATUS_TODO:
                buffer.append("");
                break;
            case STATUS_USER_INPUT:
                buffer.append("status: Eingabe");
                break;
        }
        return buffer.toString();
    }
}
