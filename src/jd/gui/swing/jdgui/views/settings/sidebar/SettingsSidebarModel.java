package jd.gui.swing.jdgui.views.settings.sidebar;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.DefaultListModel;
import javax.swing.JList;

import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.settings.panels.BasicAuthentication;
import jd.gui.swing.jdgui.views.settings.panels.GUISettings;
import jd.gui.swing.jdgui.views.settings.panels.GeneralSettingsConfigPanel;
import jd.gui.swing.jdgui.views.settings.panels.MyJDownloaderSettingsPanel;
import jd.gui.swing.jdgui.views.settings.panels.ReconnectSettings;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.AccountManagerSettings;
import jd.gui.swing.jdgui.views.settings.panels.advanced.AdvancedSettings;
import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.AntiCaptchaConfigPanel;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.Linkgrabber;
import jd.gui.swing.jdgui.views.settings.panels.packagizer.Packagizer;
import jd.gui.swing.jdgui.views.settings.panels.pluginsettings.PluginSettings;
import jd.gui.swing.jdgui.views.settings.panels.proxy.ProxyConfig;

import org.appwork.controlling.SingleReachableState;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.ExtensionControllerListener;
import org.jdownloader.extensions.LazyExtension;
import org.jdownloader.gui.notify.gui.BubbleNotifyConfigPanel;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class SettingsSidebarModel extends DefaultListModel implements GenericConfigEventListener<Object>, ExtensionControllerListener {

    private static final long            serialVersionUID = -204494527404304349L;
    private GeneralSettingsConfigPanel   cfg;

    private ReconnectSettings            rcs;
    private ProxyConfig                  pc;
    private AccountManagerSettings       ams;
    private BasicAuthentication          ba;
    private PluginSettings               ps;
    private GUISettings                  gs;
    private Packagizer                   pz;
    private AdvancedSettings             ads;
    private Linkgrabber                  lg;
    private ExtensionHeader              eh;

    private Object                       lock             = new Object();

    private SingleReachableState         TREE_COMPLETE    = new SingleReachableState("TREE_COMPLETE");
    private final JList                  list;
    protected MyJDownloaderSettingsPanel myJDownloader;
    protected BubbleNotifyConfigPanel    notifierPanel;
    protected AntiCaptchaConfigPanel     ac;

    public SettingsSidebarModel(JList list) {
        super();
        this.list = list;
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
        ExtensionController.EXTENSIONS_COMPLETE.executeWhenReached(new Runnable() {

            @Override
            public void run() {
                ExtensionController.getInstance().getEventSender().addListener(SettingsSidebarModel.this);
                fill(true);
            }
        });
    }

    private GeneralSettingsConfigPanel getConfigPanelGeneral() {
        if (cfg != null) return cfg;

        return new EDTHelper<GeneralSettingsConfigPanel>() {
            public GeneralSettingsConfigPanel edtRun() {
                if (cfg != null) return cfg;
                cfg = new GeneralSettingsConfigPanel();
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

    private AntiCaptchaConfigPanel getAntiCaptchaConfigPanel() {
        if (ac != null) return ac;

        return new EDTHelper<AntiCaptchaConfigPanel>() {
            public AntiCaptchaConfigPanel edtRun() {
                if (ac != null) return ac;
                ac = new AntiCaptchaConfigPanel();
                return ac;
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

    private BubbleNotifyConfigPanel getNotifierConfigPanel() {
        if (notifierPanel != null) return notifierPanel;

        return new EDTHelper<BubbleNotifyConfigPanel>() {
            public BubbleNotifyConfigPanel edtRun() {
                if (notifierPanel != null) return notifierPanel;
                notifierPanel = new BubbleNotifyConfigPanel();
                return notifierPanel;
            }
        }.getReturnValue();
    }

    private MyJDownloaderSettingsPanel getMyJDownloaderPanel() {
        if (myJDownloader != null) return myJDownloader;

        return new EDTHelper<MyJDownloaderSettingsPanel>() {
            public MyJDownloaderSettingsPanel edtRun() {
                if (myJDownloader != null) return myJDownloader;
                myJDownloader = new MyJDownloaderSettingsPanel();
                return myJDownloader;
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
                try {
                    synchronized (lock) {
                        boolean withExtensions = finalWithExtensions;
                        if (ExtensionController.EXTENSIONS_COMPLETE.isReached()) withExtensions = true;
                        LazyExtension extract = null;
                        try {
                            if (withExtensions) extract = ExtensionController.getInstance().getExtension("org.jdownloader.extensions.extraction.ExtractionExtension");
                        } catch (final Throwable e) {
                            /* plugin not loaded yet */
                        }

                        final LazyExtension finalExtract = extract;
                        getConfigPanelGeneral();
                        getReconnectSettings();
                        getProxyConfig();
                        getAccountManagerSettings();
                        getBasicAuthentication();
                        getPluginSettings();
                        getAntiCaptchaConfigPanel();
                        getGUISettings();
                        getNotifierConfigPanel();
                        getMyJDownloaderPanel();
                        getLinkgrabber();
                        getPackagizer();

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
                                addElement(getAntiCaptchaConfigPanel());
                                addElement(getGUISettings());
                                addElement(getNotifierConfigPanel());
                                addElement(getMyJDownloaderPanel());
                                addElement(getLinkgrabber());
                                addElement(getPackagizer());
                                if (finalExtract != null) addElement(finalExtract);
                                addElement(JDGui.getInstance().getTray());

                                addElement(getAdvancedSettings());
                            }
                        }.waitForEDT();
                        if (withExtensions) {
                            final AtomicBoolean firstExtension = new AtomicBoolean(true);
                            List<LazyExtension> pluginsOptional = ExtensionController.getInstance().getExtensions();
                            if (pluginsOptional != null) {
                                for (final LazyExtension plg : pluginsOptional) {
                                    if ("org.jdownloader.extensions.extraction.ExtractionExtension".equals(plg.getClassname())) continue;
                                    // avoid that old TrayExtension Jars will get loaded
                                    if ("org.jdownloader.extensions.jdtrayicon.TrayExtension".equals(plg.getClassname())) continue;
                                    if (contains(plg)) continue;
                                    if (CrossSystem.isWindows() && !plg.isWindowsRunnable()) continue;
                                    if (CrossSystem.isLinux() && !plg.isLinuxRunnable()) continue;
                                    if (CrossSystem.isMac() && !plg.isMacRunnable()) continue;
                                    plg._getSettings()._getStorageHandler().getEventSender().addListener(SettingsSidebarModel.this, true);
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
                } finally {
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            if (list != null) list.repaint();
                        }
                    };
                    TREE_COMPLETE.setReached();
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

    /**
     * @return the tREE_COMPLETE
     */
    public SingleReachableState getTreeCompleteState() {
        return TREE_COMPLETE;
    }

}
