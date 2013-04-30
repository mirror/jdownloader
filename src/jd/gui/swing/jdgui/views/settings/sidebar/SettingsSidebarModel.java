package jd.gui.swing.jdgui.views.settings.sidebar;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.DefaultListModel;

import jd.SecondLevelLaunch;
import jd.gui.swing.jdgui.views.settings.panels.BasicAuthentication;
import jd.gui.swing.jdgui.views.settings.panels.ConfigPanelGeneral;
import jd.gui.swing.jdgui.views.settings.panels.GUISettings;
import jd.gui.swing.jdgui.views.settings.panels.ReconnectSettings;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.AccountManagerSettings;
import jd.gui.swing.jdgui.views.settings.panels.advanced.AdvancedSettings;
import jd.gui.swing.jdgui.views.settings.panels.extensionmanager.ExtensionManager;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.Linkgrabber;
import jd.gui.swing.jdgui.views.settings.panels.packagizer.Packagizer;
import jd.gui.swing.jdgui.views.settings.panels.pluginsettings.PluginSettings;
import jd.gui.swing.jdgui.views.settings.panels.proxy.ProxyConfig;

import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.Application;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.ExtensionControllerListener;
import org.jdownloader.extensions.LazyExtension;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class SettingsSidebarModel extends DefaultListModel implements GenericConfigEventListener<Object>, ExtensionControllerListener {

    private static final long      serialVersionUID = -204494527404304349L;
    private ConfigPanelGeneral     cfg;

    private ReconnectSettings      rcs;
    private ProxyConfig            pc;
    private AccountManagerSettings ams;
    private BasicAuthentication    ba;
    private PluginSettings         ps;
    private GUISettings            gs;
    private Packagizer             pz;
    private AdvancedSettings       ads;
    private Linkgrabber            lg;
    private ExtensionHeader        eh;
    private ExtensionManager       extm;
    private Object                 lock             = new Object();

    public SettingsSidebarModel() {
        super();
        GenericConfigEventListener<Boolean> listener = new GenericConfigEventListener<Boolean>() {

            @Override
            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        fireContentsChanged(this, 0, size() - 1);
                    }
                };
            }

            @Override
            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }
        };
        org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINK_FILTER_ENABLED.getEventSender().addListener(listener);
        org.jdownloader.settings.staticreferences.CFG_PACKAGIZER.PACKAGIZER_ENABLED.getEventSender().addListener(listener);
        SecondLevelLaunch.EXTENSIONS_COMPLETE.executeWhenReached(new Runnable() {

            @Override
            public void run() {
                ExtensionController.getInstance().getEventSender().addListener(SettingsSidebarModel.this);
                fill(true);
            }
        });
    }

    private ConfigPanelGeneral getConfigPanelGeneral() {
        if (cfg != null) return cfg;

        return new EDTHelper<ConfigPanelGeneral>() {
            public ConfigPanelGeneral edtRun() {
                if (cfg != null) return cfg;
                cfg = new ConfigPanelGeneral();
                return cfg;
            }
        }.getReturnValue();
    }

    private ReconnectSettings getReconnectSettings() {
        if (rcs != null) return rcs;

        return new EDTHelper<ReconnectSettings>() {
            public ReconnectSettings edtRun() {
                if (rcs != null) return rcs;
                rcs = new ReconnectSettings();
                return rcs;
            }
        }.getReturnValue();
    }

    private ProxyConfig getProxyConfig() {
        if (pc != null) return pc;

        return new EDTHelper<ProxyConfig>() {
            public ProxyConfig edtRun() {
                if (pc != null) return pc;
                pc = new ProxyConfig();
                return pc;
            }
        }.getReturnValue();
    }

    private AccountManagerSettings getAccountManagerSettings() {
        if (ams != null) return ams;

        return new EDTHelper<AccountManagerSettings>() {
            public AccountManagerSettings edtRun() {
                if (ams != null) return ams;
                ams = new AccountManagerSettings();
                return ams;
            }
        }.getReturnValue();
    }

    private BasicAuthentication getBasicAuthentication() {
        if (ba != null) return ba;

        return new EDTHelper<BasicAuthentication>() {
            public BasicAuthentication edtRun() {
                if (ba != null) return ba;
                ba = new BasicAuthentication();
                return ba;
            }
        }.getReturnValue();
    }

    private PluginSettings getPluginSettings() {
        if (ps != null) return ps;

        return new EDTHelper<PluginSettings>() {
            public PluginSettings edtRun() {
                if (ps != null) return ps;
                ps = new PluginSettings();
                return ps;
            }
        }.getReturnValue();
    }

    private GUISettings getGUISettings() {
        if (gs != null) return gs;

        return new EDTHelper<GUISettings>() {
            public GUISettings edtRun() {
                if (gs != null) return gs;
                gs = new GUISettings();
                return gs;
            }
        }.getReturnValue();
    }

    private Packagizer getPackagizer() {
        if (pz != null) return pz;

        return new EDTHelper<Packagizer>() {
            public Packagizer edtRun() {
                if (pz != null) return pz;
                pz = new Packagizer();
                return pz;
            }
        }.getReturnValue();
    }

    private Linkgrabber getLinkgrabber() {
        if (lg != null) return lg;

        return new EDTHelper<Linkgrabber>() {
            public Linkgrabber edtRun() {
                if (lg != null) return lg;
                lg = new Linkgrabber();
                return lg;
            }
        }.getReturnValue();
    }

    private AdvancedSettings getAdvancedSettings() {
        if (ads != null) return ads;

        return new EDTHelper<AdvancedSettings>() {
            public AdvancedSettings edtRun() {
                if (ads != null) return ads;
                ads = new AdvancedSettings();
                return ads;
            }
        }.getReturnValue();
    }

    private ExtensionManager getExtensionManager() {
        if (extm != null) return extm;

        return new EDTHelper<ExtensionManager>() {
            public ExtensionManager edtRun() {
                if (extm != null) return extm;
                extm = new ExtensionManager();
                return extm;
            }
        }.getReturnValue();
    }

    private ExtensionHeader getExtensionHeader() {
        if (eh != null) return eh;

        return new EDTHelper<ExtensionHeader>() {
            public ExtensionHeader edtRun() {
                if (eh != null) return eh;
                eh = new ExtensionHeader();
                return eh;
            }
        }.getReturnValue();
    }

    public void fill(final boolean finalWithExtensions) {
        new Thread("FillSettingsSideBarModel") {
            @Override
            public void run() {
                synchronized (lock) {
                    boolean withExtensions = finalWithExtensions;
                    if (SecondLevelLaunch.EXTENSIONS_COMPLETE.isReached()) withExtensions = true;
                    LazyExtension extract = null;
                    try {
                        if (withExtensions) extract = ExtensionController.getInstance().getExtension("org.jdownloader.extensions.extraction.ExtractionExtension");
                    } catch (final Throwable e) {
                        /* plugin not loaded yet */
                    }
                    LazyExtension tray = null;
                    try {
                        if (withExtensions) tray = ExtensionController.getInstance().getExtension("org.jdownloader.extensions.jdtrayicon.TrayExtension");
                    } catch (final Throwable e) {
                        /* plugin not loaded yet */
                    }
                    final LazyExtension finalExtract = extract;
                    final LazyExtension finalTray = tray;
                    getConfigPanelGeneral();
                    getReconnectSettings();
                    getProxyConfig();
                    getAccountManagerSettings();
                    getBasicAuthentication();
                    getPluginSettings();
                    getGUISettings();
                    getLinkgrabber();
                    getPackagizer();
                    if (!Application.isJared(Application.class)) {
                        getExtensionManager();
                    }
                    getAdvancedSettings();
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            removeAllElements();
                            addElement(getConfigPanelGeneral());
                            addElement(getReconnectSettings());
                            addElement(getProxyConfig());
                            addElement(getAccountManagerSettings());
                            addElement(getBasicAuthentication());
                            addElement(getPluginSettings());
                            addElement(getGUISettings());
                            addElement(getLinkgrabber());
                            addElement(getPackagizer());
                            if (finalExtract != null) addElement(finalExtract);
                            if (finalTray != null) addElement(finalTray);
                            if (!Application.isJared(Application.class)) {
                                addElement(getExtensionManager());
                            }
                            addElement(getAdvancedSettings());
                        }
                    };
                    if (withExtensions) {
                        final AtomicBoolean firstExtension = new AtomicBoolean(true);
                        List<LazyExtension> pluginsOptional = ExtensionController.getInstance().getExtensions();
                        if (pluginsOptional != null) {
                            for (final LazyExtension plg : pluginsOptional) {
                                System.out.println(plg.getClassname());
                                if (contains(plg)) continue;
                                if (CrossSystem.isWindows() && !plg.isWindowsRunnable()) continue;
                                if (CrossSystem.isLinux() && !plg.isLinuxRunnable()) continue;
                                if (CrossSystem.isMac() && !plg.isMacRunnable()) continue;
                                plg._getSettings().getStorageHandler().getEventSender().addListener(SettingsSidebarModel.this, true);
                                new EDTRunner() {

                                    @Override
                                    protected void runInEDT() {
                                        if (firstExtension.get()) {
                                            addElement(getExtensionHeader());
                                            firstExtension.set(false);
                                        }
                                        addElement(plg);
                                    }
                                };
                            }
                        }
                    }
                }
            }
        }.start();
    }

    public void onConfigValidatorError(KeyHandler<Object> keyHandler, Object invalidValue, ValidationException validateException) {
    }

    public void onUpdated() {
        if (!JsonConfig.create(GraphicalUserInterfaceSettings.class).isConfigViewVisible()) return;
        fill(true);
    }

    @Override
    public void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                fireContentsChanged(this, 0, size() - 1);
            }
        };
    }

}
