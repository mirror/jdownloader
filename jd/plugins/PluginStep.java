package jd.plugins;
/**
 * Diese Klasse bildet jeden einzelnen Schritt ab, die ein Plugin nacheinander abarbeiten muß
 * 
 * @author astaldo
 */
public class PluginStep {
    
    public final static long WAIT_TIME = 1l << 1;
    public final static long CAPTCHA   = 1l << 2;
    public final static long DOWNLOAD  = 1l << 3;
    public final static long DECRYPT   = 1l << 4;
    
    private long   step;
    private Object parameter;
    
    public PluginStep (long step, Object parameter){
        this.step      = step;
        this.parameter = parameter;
    }
}
