package jd.plugins.decrypter;

import jd.PluginWrapper;
import jd.plugins.DecrypterPlugin;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ponychan.net" }, urls = { "https?://(www\\.)?ponychan\\.net/.+(/res/\\d+\\.html|/\\d+\\.html|/index\\.html)" })
public class PonychanNet extends AbstractChan {
    public PonychanNet(PluginWrapper wrapper) {
        super(wrapper);
    }
}
