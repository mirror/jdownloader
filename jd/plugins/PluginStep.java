package jd.plugins;
/**
 * Diese Klasse bildet jeden einzelnen Schritt ab, die ein Plugin nacheinander abarbeiten muß
 * 
 * @author astaldo
 */
public class PluginStep {
    
    public final static int STEP_WAIT_TIME = 1 << 1;
    public final static int STEP_CAPTCHA   = 1 << 2;
    public final static int STEP_DOWNLOAD  = 1 << 3;
    public final static int STEP_DECRYPT   = 1 << 4;
    
    public final static int STATUS_TODO             = 0;
    public final static int STATUS_ERROR            = 1;
    public final static int STATUS_DONE             = 2;
    public final static int STATUS_INPUT_FROM_USER  = 3;
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
}
