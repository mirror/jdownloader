package org.jdownloader.plugins.controller.host;

import jd.plugins.PluginForHost;

import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.storage.config.annotations.TooltipInterface;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.plugins.controller.LazyPluginClass;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;
import org.jdownloader.plugins.controller.UpdateRequiredClassNotFoundException;
import org.jdownloader.translate._JDT;

public class LazyHostPlugin extends LazyPlugin<PluginForHost> {
    public static enum FEATURE implements LabelInterface, TooltipInterface {
        USENET {
            @Override
            public String getLabel() {
                return _JDT.T.LazyHostPlugin_FEATURE_USENET();
            }

            @Override
            public String getTooltip() {
                return _JDT.T.LazyHostPlugin_FEATURE_USENET_TOOLTIP();
            }
        },
        MULTIHOST {
            @Override
            public String getLabel() {
                return _JDT.T.LazyHostPlugin_FEATURE_MULTIHOST();
            }

            @Override
            public String getTooltip() {
                return _JDT.T.LazyHostPlugin_FEATURE_MULTIHOST_TOOLTIP();
            }
        },
        GENERIC {
            @Override
            public String getLabel() {
                return _JDT.T.LazyHostPlugin_FEATURE_GENERIC();
            }

            @Override
            public String getTooltip() {
                return _JDT.T.LazyHostPlugin_FEATURE_GENERIC_TOOLTIP();
            }
        },
        INTERNAL {
            @Override
            public String getLabel() {
                return "INTERNAL";
            }

            @Override
            public String getTooltip() {
                return "INTERNAL";
            }
        };
        public static final long CACHEVERSION = 04062017l; // change when you add/change enums!

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

    private static enum PROPERTY {
        CONFIG,
        PREMIUM,
        REWRITE,
        ALLOW,
        SITESUPPORT
    }

    private String        premiumUrl;
    private volatile byte properties     = 0;
    private volatile long parsesLifetime = 0;
    private FEATURE[]     features       = null;

    public FEATURE[] getFeatures() {
        return features;
    }

    public boolean isFallbackPlugin() {
        return "UpdateRequired".equalsIgnoreCase(getDisplayName());
    }

    public boolean isOfflinePlugin() {
        return getClassName().endsWith("r.Offline");
    }

    public boolean hasFeature(FEATURE feature) {
        return feature != null && feature.isSet(getFeatures());
    }

    protected void setFeatures(FEATURE[] features) {
        this.features = features;
    }

    public long getPluginUsage() {
        return parsesLifetime + parses;
    }

    public void setPluginUsage(long pluginUsage) {
        this.parsesLifetime = Math.max(0, pluginUsage);
    }

    private volatile long parses         = 0;
    private volatile long parsesRuntime  = 0;
    private volatile long averageRuntime = 0;

    public long getAverageParseRuntime() {
        return averageRuntime;
    }

    public synchronized void updateParseRuntime(long r) {
        parses++;
        if (r >= 0) {
            parsesRuntime += r;
        }
        averageRuntime = parsesRuntime / parses;
    }

    @Override
    public String getClassName() {
        return "jd.plugins.hoster.".concat(getLazyPluginClass().getClassName());
    }

    public boolean isHasConfig() {
        return getProperty(PROPERTY.CONFIG);
    }

    protected void setHasConfig(boolean hasConfig) {
        setProperty(hasConfig, PROPERTY.CONFIG);
    }

    public boolean isSitesSupported() {
        return getProperty(PROPERTY.SITESUPPORT);
    }

    protected void setSitesSupported(boolean b) {
        setProperty(b, PROPERTY.SITESUPPORT);
    }

    protected synchronized final void setProperty(final boolean b, final PROPERTY property) {
        if (b) {
            properties |= 1 << property.ordinal();
        } else {
            properties &= ~(1 << property.ordinal());
        }
    }

    protected final boolean getProperty(final PROPERTY property) {
        return (properties & 1 << property.ordinal()) != 0;
    }

    public String getHost() {
        return getDisplayName();
    }

    public String getPremiumUrl() {
        return premiumUrl;
    }

    protected void setPremiumUrl(String premiumUrl) {
        this.premiumUrl = premiumUrl;
    }

    public boolean isPremium() {
        return getProperty(PROPERTY.PREMIUM);
    }

    protected void setPremium(boolean premium) {
        setProperty(premium, PROPERTY.PREMIUM);
    }

    private String configInterface = null;

    public LazyHostPlugin(LazyPluginClass lazyPluginClass, String pattern, String displayName, Class<PluginForHost> pluginClass, PluginClassLoaderChild classLoaderChild) {
        super(lazyPluginClass, pattern, displayName, pluginClass, classLoaderChild);
    }

    public String getConfigInterface() {
        return configInterface;
    }

    public void setConfigInterface(String configInterface) {
        this.configInterface = configInterface;
    }

    public void setHasAllowHandle(boolean hasAllowHandle) {
        setProperty(hasAllowHandle, PROPERTY.ALLOW);
    }

    public boolean isHasAllowHandle() {
        return getProperty(PROPERTY.ALLOW);
    }

    public void setHasRewrite(boolean hasRewrite) {
        setProperty(hasRewrite, PROPERTY.REWRITE);
    }

    public boolean isHasRewrite() {
        return getProperty(PROPERTY.REWRITE);
    }

    @Override
    public PluginForHost newInstance(PluginClassLoaderChild classLoader) throws UpdateRequiredClassNotFoundException {
        try {
            final PluginForHost ret = super.newInstance(classLoader);
            ret.setLazyP(this);
            return ret;
        } catch (UpdateRequiredClassNotFoundException e) {
            final LazyHostPlugin lFallBackPlugin = getFallBackPlugin();
            if (lFallBackPlugin != null && lFallBackPlugin != this) {
                final PluginForHost ret = lFallBackPlugin.newInstance(classLoader);
                if (ret != null) {
                    ret.setLazyP(lFallBackPlugin);
                    return ret;
                }
            }
            throw e;
        }
    }

    private LazyHostPlugin getFallBackPlugin() {
        if ("UpdateRequired".equalsIgnoreCase(getDisplayName())) {
            return null;
        } else {
            return HostPluginController.getInstance().getFallBackPlugin();
        }
    }

    @Override
    public PluginForHost getPrototype(PluginClassLoaderChild classLoader) throws UpdateRequiredClassNotFoundException {
        try {
            final PluginForHost ret = super.getPrototype(classLoader);
            ret.setLazyP(this);
            return ret;
        } catch (UpdateRequiredClassNotFoundException e) {
            final LazyHostPlugin lFallBackPlugin = getFallBackPlugin();
            if (lFallBackPlugin != null && lFallBackPlugin != this) {
                final PluginForHost ret = lFallBackPlugin.getPrototype(classLoader);
                if (ret != null) {
                    ret.setLazyP(lFallBackPlugin);
                    return ret;
                }
            }
            throw e;
        }
    }
}
