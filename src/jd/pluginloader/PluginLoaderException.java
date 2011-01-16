package jd.pluginloader;

public class PluginLoaderException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public PluginLoaderException(String string) {
        super(string);
        this.setStackTrace(new StackTraceElement[] {});
    }

}
