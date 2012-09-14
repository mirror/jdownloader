package org.jdownloader.plugins.controller.crawler;

import jd.plugins.PluginForDecrypt;

import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;
import org.jdownloader.plugins.controller.UpdateRequiredClassNotFoundException;

public class LazyCrawlerPlugin extends LazyPlugin<PluginForDecrypt> {

    private static final String JD_PLUGINS_DECRYPTER = "jd.plugins.decrypter.";

    public LazyCrawlerPlugin(AbstractCrawlerPlugin ap, Class<PluginForDecrypt> class1, PluginClassLoaderChild classLoader) {
        super(ap.getPattern(), JD_PLUGINS_DECRYPTER + ap.getClassname(), ap.getDisplayName(), ap.getVersion(), class1, classLoader);
        hasConfig = ap.isHasConfig();
        maxConcurrentInstances = ap.getMaxConcurrentInstances();
    }

    private long    decrypts               = 0;
    private long    decryptsRuntime        = 0;
    private int     maxConcurrentInstances = Integer.MAX_VALUE;
    private boolean hasConfig              = false;

    public boolean isHasConfig() {
        return hasConfig;
    }

    protected void setHasConfig(boolean hasConfig) {
        this.hasConfig = hasConfig;
    }

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

    @Override
    public PluginForDecrypt newInstance(PluginClassLoaderChild classLoader) throws UpdateRequiredClassNotFoundException {
        PluginForDecrypt ret = null;
        try {
            ret = super.newInstance(classLoader);
            ret.setLazyC(this);
            return ret;
        } catch (UpdateRequiredClassNotFoundException e) {
            this.setHasConfig(false);
            throw e;
        }
    }

    @Override
    public PluginForDecrypt getPrototype(PluginClassLoaderChild classLoader) throws UpdateRequiredClassNotFoundException {
        PluginForDecrypt ret = null;
        try {
            ret = super.getPrototype(classLoader);
            ret.setLazyC(this);
            return ret;
        } catch (UpdateRequiredClassNotFoundException e) {
            this.setHasConfig(false);
            throw e;
        }
    }

    /**
     * @return the maxConcurrentInstances
     */
    public int getMaxConcurrentInstances() {
        return maxConcurrentInstances;
    }

    /**
     * @param maxConcurrentInstances
     *            the maxConcurrentInstances to set
     */
    public void setMaxConcurrentInstances(int maxConcurrentInstances) {
        this.maxConcurrentInstances = maxConcurrentInstances;
    }
}
