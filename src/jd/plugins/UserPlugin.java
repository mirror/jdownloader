package jd.plugins;

import java.io.File;
import java.util.regex.Pattern;

public abstract class UserPlugin extends Plugin {

    public UserPlugin() {

    }

    public abstract void enable(boolean value);

    // Müssen implementiert werden, werden aber nicht gebraucht

    @Override
    public boolean doBotCheck(File file) {

        return false;
    }

    @Override
    public PluginStep doStep(PluginStep step, Object parameter) {

        return null;
    }

    @Override
    public String getHost() {

        return null;
    }

    @Override
    public String getLinkName() {

        return null;
    }

    @Override
    public Pattern getSupportedLinks() {

        return null;
    }

}
