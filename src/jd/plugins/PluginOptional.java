package jd.plugins;

import java.io.File;
import java.util.regex.Pattern;

public abstract class PluginOptional extends Plugin{

    @Override
    public boolean doBotCheck(File file) { return false; }

    @Override public PluginStep doStep(PluginStep step, Object parameter) { return null; }
    @Override public String getHost()            { return null; }
    @Override public String getLinkName()        { return null; }
    @Override public Pattern getSupportedLinks() { return null; }
    public abstract void enable(boolean enable);
}
