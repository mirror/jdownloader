package jd.plugins;
/**
 * Diese Klasse bildet jeden einzelnen Schritt ab, die ein Plugin nacheinander abarbeiten muß
 * 
 * @author astaldo
 */
public class PluginStep {
    
    public final static int WAIT_TIME = 1 << 1;
    public final static int CAPTCHA   = 1 << 2;
    public final static int DOWNLOAD  = 1 << 3;
    public final static int DECRYPT   = 1 << 4;
    
    private int   step;
    private Object parameter;
    
    public PluginStep (int step, Object parameter){
        this.step      = step;
        this.parameter = parameter;
    }

    public int getStep()                       { return step;                }
    public Object getParameter()               { return parameter;           }
    public void setParameter(Object parameter) { this.parameter = parameter; }
}
