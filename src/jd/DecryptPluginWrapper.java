package jd;

import jd.plugins.PluginForDecrypt;

public class DecryptPluginWrapper extends PluginWrapper {



    public DecryptPluginWrapper(String name, String host, String className, String patternSupported, int flags) {
        super(name, host, "jd.plugins.decrypt."+className, patternSupported, flags);
    }

    public DecryptPluginWrapper(String host, String className, String patternSupported, int flags) {
        this(host, host, className, patternSupported, flags);
    }

    public DecryptPluginWrapper(String host, String className, String patternSupported) {
        this(host, host, className, patternSupported, 0);
    }

    public PluginForDecrypt getPlugin() {
        return (PluginForDecrypt) super.getPlugin();
    }

  



}
