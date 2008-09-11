package jd;

import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.plugins.PluginForDecrypt;

public class DecryptPluginWrapper extends PluginWrapper {
private static final ArrayList<DecryptPluginWrapper> DECRYPT_WRAPPER = new ArrayList<DecryptPluginWrapper>();
    public static ArrayList<DecryptPluginWrapper> getDecryptWrapper() {
    return DECRYPT_WRAPPER;
}

    public DecryptPluginWrapper(String name, String host, String className, String patternSupported, int flags) {
        super(name, host, "jd.plugins.decrypt." + className, patternSupported, flags);
        DECRYPT_WRAPPER.add(this);
    }

    public DecryptPluginWrapper(String host, String className, String patternSupported, int flags) {
        this(host, host, className, patternSupported, flags);
    }

    public DecryptPluginWrapper(String host, String className, String patternSupported) {
        this(host, host, className, patternSupported, 0);
    }

    public DecryptPluginWrapper(String name, String host, String className, Pattern patternSupported, int flags) {
        super(name, host, "jd.plugins.decrypt." + className, patternSupported.pattern(), flags);
        super.setPattern(patternSupported);
        DECRYPT_WRAPPER.add(this);
    }

    public DecryptPluginWrapper(String host, String className, Pattern patternSupported, int flags) {
        this(host, host, className, patternSupported, flags);
    }

    public DecryptPluginWrapper(String host, String className, Pattern patternSupported) {
        this(host, host, className, patternSupported, 0);
    }

    public PluginForDecrypt getPlugin() {
        return (PluginForDecrypt) super.getPlugin();
    }

}
