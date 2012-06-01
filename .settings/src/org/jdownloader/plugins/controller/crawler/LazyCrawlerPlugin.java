package org.jdownloader.plugins.controller.crawler;

import jd.plugins.PluginForDecrypt;

import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;
import org.jdownloader.plugins.controller.UpdateRequiredClassNotFoundException;

public class LazyCrawlerPlugin extends LazyPlugin<PluginForDecrypt> {

    private static final String JD_PLUGINS_DECRYPTER = "jd.plugins.decrypter.";

    public LazyCrawlerPlugin(AbstractCrawlerPlugin ap, Class<PluginForDecrypt> class1, PluginClassLoaderChild classLoader) {
        super(ap.getPattern(), JD_PLUGINS_DECRYPTER + ap.getClassname(), ap.getDisplayName(), ap.getVersion(), class1, classLoader);
    }

    @Override
    public PluginForDecrypt newInstance() throws UpdateRequiredClassNotFoundException {
        PluginForDecrypt ret = super.newInstance();
        ret.setLazyC(this);
        return ret;
    }

    private long decrypts        = 0;
    private long decryptsRuntime = 0;

    public long getAverageCrawlRuntime() {
        synchronized (this) {
            if (decrypts == 0 || decryptsRuntime == 0) return 0;
            long ret = decryptsRuntime / decrypts;
            return ret;
        }
    }

    public void updateCrawlRuntime(long r) {
        synchronized (this) {
            if (r >= 0) {
                decrypts++;
                decryptsRuntime += r;
            }
        }
    }
}
