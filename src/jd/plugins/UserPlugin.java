package jd.plugins;

import java.io.File;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.Property;
import jd.plugins.event.PluginEvent;
import jd.plugins.event.PluginListener;
import jd.utils.JDUtilities;

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
