package jd;

import java.util.regex.Pattern;

import jd.config.SubConfiguration;

import jd.plugins.PluginForHost;

public class HostPluginWrapper extends PluginWrapper {

    private static final String AGB_CHECKED = "AGB_CHECKED";

    public HostPluginWrapper(String name, String host, String className, String patternSupported, int flags) {
        super(name, host, "jd.plugins.host." + className, patternSupported, flags);
    }

    public HostPluginWrapper(String host, String className, String patternSupported, int flags) {
        this(host, host, className, patternSupported, flags);
    }

    public HostPluginWrapper(String host, String className, String patternSupported) {
        this(host, host, className, patternSupported, 0);
    }

    public HostPluginWrapper(String name, String host, String className, Pattern patternSupported, int flags) {
        super(name, host, "jd.plugins.host." + className, patternSupported.pattern(), flags);
        super.setPattern(patternSupported);
    }

    public HostPluginWrapper(String host, String className, Pattern patternSupported, int flags) {
        this(host, host, className, patternSupported, flags);
    }

    public HostPluginWrapper(String host, String className, Pattern patternSupported) {
        this(host, host, className, patternSupported, 0);
    }

    public PluginForHost getPlugin() {
        return (PluginForHost) super.getPlugin();
    }

    public boolean isAGBChecked() {
        return super.getPluginConfig().getBooleanProperty(AGB_CHECKED, false);
    }

    public void setAGBChecked(Boolean value) {
        super.getPluginConfig().setProperty(AGB_CHECKED, value);
        super.getPluginConfig().save();
    }

}
