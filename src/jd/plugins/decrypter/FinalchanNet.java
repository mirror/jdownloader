package jd.plugins.decrypter;

import jd.PluginWrapper;
import jd.plugins.DecrypterPlugin;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "finalchan.net" }, urls = { "https?://(www\\.)?finalchan\\.net/.+(/res/\\d+\\.html|/\\d+\\.html|/index\\.html)" })
public class FinalchanNet extends AbstractChan {
    public FinalchanNet(PluginWrapper wrapper) {
        super(wrapper);
    }
}
