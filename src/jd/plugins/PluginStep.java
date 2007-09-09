package jd.plugins;
/**
 * Diese Klasse bildet jeden einzelnen Schritt ab, die ein Plugin nacheinander abarbeiten muß
 *
 * @author astaldo
 */
public class PluginStep {
    /**
     * ID für eine gewisse Wartezeit. Der dazugehörige Wert ist die Zeit in Millisekunden
     */
    public final static int STEP_WAIT_TIME = 1 << 1;
    /**
     * Das Captcha Bild soll erkannt werden. Als Rückgabe wird der erkannte Text als Parameter übergeben
     */
    public final static int STEP_CAPTCHA   = 1 << 2;
    /**
     * Der Download soll durchgeführt werden. Als Parameter wird der DownloadLink übergeben
     */
    public final static int STEP_DOWNLOAD  = 1 << 3;
    /**
     * Ein Link soll entschlüsselt werden
     */
    public final static int STEP_DECRYPT   = 1 << 4;
    /**
     * Dieser Schritt muß erst noch stattfinden
     */
    public final static int STATUS_TODO        = 0;
    /**
     * Dieser Schritt wurde fehlerhaft abgearbeitet
     */
    public final static int STATUS_ERROR       = 1;
    /**
     * Dieser Schritt wurde erfolgreich durchgeführt
     */
    public final static int STATUS_DONE        = 2;
    /**
     * Dieser Schritt erfordert eine Interaktion
     */
    public final static int STATUS_USER_INPUT  = 3;
    /**
     * Dieser Schritt sollte nochmal wiederholt werden
     */
    public final static int STATUS_RETRY       = 4;
    /**
     * Status dieses Schrittes
     */
    private int status = 0;
    /**
     * ID dieses Schrittes
     */
    private int step;
    /**
     * Ein optionaler Parameter
     */
    private Object parameter;

    public PluginStep (int step, Object parameter){
        this.step      = step;
        this.parameter = parameter;
    }
    public int getStep()                       { return step;                }
    public Object getParameter()               { return parameter;           }
    public void setParameter(Object parameter) { this.parameter = parameter; }
    public int getStatus()                     { return status;              }
    public void setStatus(int status)          { this.status = status;       }
    public String toString(){
        StringBuffer buffer = new StringBuffer();
        buffer.append("");
        switch(step){
            case STEP_WAIT_TIME: buffer.append("Warten "); break;
            case STEP_CAPTCHA:   buffer.append("Captcha ");   break;
            case STEP_DOWNLOAD:  buffer.append("Download ");  break;
            case STEP_DECRYPT:   buffer.append("Decrypt ");   break;
        }
        buffer.append("status: ");
        switch(status){
            case STATUS_DONE:       buffer.append("Fertig");      break;
            case STATUS_ERROR:      buffer.append("Fehler");     break;
            case STATUS_TODO:       buffer.append("");      break;
            case STATUS_USER_INPUT: buffer.append("Eingabe"); break;
        }
        return buffer.toString();
    }
}
