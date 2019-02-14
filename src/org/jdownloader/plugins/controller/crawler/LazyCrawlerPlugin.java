package org.jdownloader.plugins.controller.crawler;

import jd.plugins.PluginForDecrypt;

import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.storage.config.annotations.TooltipInterface;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.plugins.controller.LazyPluginClass;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;
import org.jdownloader.plugins.controller.UpdateRequiredClassNotFoundException;
import org.jdownloader.translate._JDT;

public class LazyCrawlerPlugin extends LazyPlugin<PluginForDecrypt> {
    public static enum FEATURE implements LabelInterface, TooltipInterface {
        GENERIC {
            @Override
            public String getLabel() {
                return _JDT.T.LazyHostPlugin_FEATURE_GENERIC();
            }

            @Override
            public String getTooltip() {
                return _JDT.T.LazyHostPlugin_FEATURE_GENERIC_TOOLTIP();
            }
        };
        public static final long CACHEVERSION = 25082016l; // change when you add/change enums!

        public boolean isSet(FEATURE[] features) {
            if (features != null) {
                for (final FEATURE feature : features) {
                    if (this.equals(feature)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public LazyCrawlerPlugin(LazyPluginClass lazyPluginClass, String pattern, String displayName, Class<PluginForDecrypt> pluginClass, PluginClassLoaderChild classLoaderChild) {
        super(lazyPluginClass, pattern, displayName, pluginClass, classLoaderChild);
    }

    private volatile long decrypts               = 0;
    private volatile long decryptsRuntime        = 0;
    private volatile long averageRuntime         = 0;
    private volatile long pluginUsage            = 0;
    private int           maxConcurrentInstances = Integer.MAX_VALUE;
    private boolean       hasConfig              = false;
    private String        configInterface        = null;
    private FEATURE[]     features               = null;

    public FEATURE[] getFeatures() {
        return features;
    }

    public long getPluginUsage() {
        return decrypts + pluginUsage;
    }

    public boolean hasFeature(FEATURE feature) {
        return feature != null && feature.isSet(getFeatures());
    }

    protected void setFeatures(FEATURE[] features) {
        this.features = features;
    }

    public void setPluginUsage(long pluginUsage) {
        this.pluginUsage = Math.max(0, pluginUsage);
    }

    public boolean isHasConfig() {
        return hasConfig;
    }

    @Override
    public String getClassName() {
        return "jd.plugins.decrypter.".concat(getLazyPluginClass().getClassName());
    }

    protected void setHasConfig(boolean hasConfig) {
        this.hasConfig = hasConfig;
    }

    public String getConfigInterface() {
        return configInterface;
    }

    public void setConfigInterface(String configInterface) {
        this.configInterface = configInterface;
    }

    public long getAverageCrawlRuntime() {
        return averageRuntime;
    }

    public void updateCrawlRuntime(long r) {
        synchronized (this) {
            decrypts++;
            if (r >= 0) {
                decryptsRuntime += r;
            }
            averageRuntime = decryptsRuntime / decrypts;
        }
    }

    @Override
    public PluginForDecrypt newInstance(PluginClassLoaderChild classLoader) throws UpdateRequiredClassNotFoundException {
        try {
            final PluginForDecrypt ret = super.newInstance(classLoader);
            ret.setLazyC(this);
            return ret;
        } catch (UpdateRequiredClassNotFoundException e) {
            this.setHasConfig(false);
            throw e;
        }
    }

    @Override
    public PluginForDecrypt getPrototype(PluginClassLoaderChild classLoader) throws UpdateRequiredClassNotFoundException {
        try {
            final PluginForDecrypt ret = super.getPrototype(classLoader);
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
        this.maxConcurrentInstances = Math.max(1, maxConcurrentInstances);
    }
}
