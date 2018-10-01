package jd.plugins.decrypter;

import jd.PluginWrapper;
import jd.plugins.DecrypterPlugin;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uboachan.net" }, urls = { "https?://(www\\.)?uboachan\\.net/.+(/res/\\d+\\.html|/\\d+\\.html|/index\\.html)" })
public class UboachanNet extends AbstractChan {
    public UboachanNet(PluginWrapper wrapper) {
        super(wrapper);
    }
}
