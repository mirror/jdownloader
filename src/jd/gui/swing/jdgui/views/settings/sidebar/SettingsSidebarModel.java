package jd.gui.swing.jdgui.views.settings.sidebar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;

import jd.SecondLevelLaunch;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.settings.panels.BasicAuthentication;
import jd.gui.swing.jdgui.views.settings.panels.GUISettings;
import jd.gui.swing.jdgui.views.settings.panels.GeneralSettingsConfigPanel;
import jd.gui.swing.jdgui.views.settings.panels.MyJDownloaderSettingsPanel;
import jd.gui.swing.jdgui.views.settings.panels.ReconnectConfigPanel;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.AccountManagerSettings;
import jd.gui.swing.jdgui.views.settings.panels.advanced.AdvancedSettings;
import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.CaptchaConfigPanel;
import jd.gui.swing.jdgui.views.settings.panels.extensionmanager.OptionalExtensionSettings;
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
import org.jdownloader.gui.notify.BubbleNotify;
import org.jdownloader.gui.notify.gui.BubbleNotifyConfigPanel;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class SettingsSidebarModel extends DefaultListModel implements GenericConfigEventListener<Object>, ExtensionControllerListener {
    private static final long            serialVersionUID = -204494527404304349L;
    private GeneralSettingsConfigPanel   cfg;
    private ReconnectConfigPanel         rcs;
    private ProxyConfig                  pc;
    private AccountManagerSettings       ams;
    private BasicAuthentication          ba;
    private PluginSettings               ps;
    private GUISettings                  gs;
    private Packagizer                   pz;
    private AdvancedSettings             ads;
    private Linkgrabber                  lg;
    private OptionalExtensionSettings            es;
    private final Object                 lock             = new Object();
    private SingleReachableState         TREE_COMPLETE    = new SingleReachableState("TREE_COMPLETE");
    private final JList                  list;
    protected MyJDownloaderSettingsPanel myJDownloader;
    protected BubbleNotifyConfigPanel    notifierPanel;
    protected CaptchaConfigPanel         ac;

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
        SecondLevelLaunch.EXTENSIONS_LOADED.executeWhenReached(new Runnable() {
            @Override
            public void run() {
                ExtensionController.getInstance().getEventSender().addListener(SettingsSidebarModel.this);
                fill(true);
            }
        });
    }

    private GeneralSettingsConfigPanel getConfigPanelGeneral() {
        if (cfg != null) {
            return cfg;
        } else {
            return new EDTHelper<GeneralSettingsConfigPanel>() {
                public GeneralSettingsConfigPanel edtRun() {
                    if (cfg != null) {
                        return cfg;
                    } else {
                        cfg = new GeneralSettingsConfigPanel();
                        return cfg;
                    }
                }
            }.getReturnValue();
        }
    }

    private ReconnectConfigPanel getReconnectSettings() {
        if (rcs != null) {
            return rcs;
        } else {
            return new EDTHelper<ReconnectConfigPanel>() {
                public ReconnectConfigPanel edtRun() {
                    if (rcs != null) {
                        return rcs;
                    } else {
                        rcs = new ReconnectConfigPanel();
                        return rcs;
                    }
                }
            }.getReturnValue();
        }
    }

    private ProxyConfig getProxyConfig() {
        if (pc != null) {
            return pc;
        } else {
            return new EDTHelper<ProxyConfig>() {
                public ProxyConfig edtRun() {
                    if (pc != null) {
                        return pc;
                    } else {
                        pc = new ProxyConfig();
                        return pc;
                    }
                }
            }.getReturnValue();
        }
    }

    private AccountManagerSettings getAccountManagerSettings() {
        if (ams != null) {
            return ams;
        } else {
            return new EDTHelper<AccountManagerSettings>() {
                public AccountManagerSettings edtRun() {
                    if (ams != null) {
                        return ams;
                    } else {
                        ams = new AccountManagerSettings();
                        return ams;
                    }
                }
            }.getReturnValue();
        }
    }

    private BasicAuthentication getBasicAuthentication() {
        if (ba != null) {
            return ba;
        } else {
            return new EDTHelper<BasicAuthentication>() {
                public BasicAuthentication edtRun() {
                    if (ba != null) {
                        return ba;
                    } else {
                        ba = new BasicAuthentication();
                        return ba;
                    }
                }
            }.getReturnValue();
        }
    }

    private CaptchaConfigPanel getAntiCaptchaConfigPanel() {
        if (ac != null) {
            return ac;
        } else {
            return new EDTHelper<CaptchaConfigPanel>() {
                public CaptchaConfigPanel edtRun() {
                    if (ac != null) {
                        return ac;
                    } else {
                        ac = new CaptchaConfigPanel();
                        return ac;
                    }
                }
            }.getReturnValue();
        }
    }

    private PluginSettings getPluginSettings() {
        if (ps != null) {
            return ps;
        } else {
            return new EDTHelper<PluginSettings>() {
                public PluginSettings edtRun() {
                    if (ps != null) {
                        return ps;
                    } else {
                        ps = new PluginSettings();
                        return ps;
                    }
                }
            }.getReturnValue();
        }
    }

    private GUISettings getGUISettings() {
        if (gs != null) {
            return gs;
        } else {
            return new EDTHelper<GUISettings>() {
                public GUISettings edtRun() {
                    if (gs != null) {
                        return gs;
                    } else {
                        gs = new GUISettings();
                        return gs;
                    }
                }
            }.getReturnValue();
        }
    }

    private BubbleNotifyConfigPanel getNotifierConfigPanel() {
        if (notifierPanel != null) {
            return notifierPanel;
        } else {
            return new EDTHelper<BubbleNotifyConfigPanel>() {
                public BubbleNotifyConfigPanel edtRun() {
                    if (notifierPanel != null) {
                        return notifierPanel;
                    } else {
                        notifierPanel = BubbleNotify.getInstance().getConfigPanel();
                        return notifierPanel;
                    }
                }
            }.getReturnValue();
        }
    }

    private MyJDownloaderSettingsPanel getMyJDownloaderPanel() {
        if (myJDownloader != null) {
            return myJDownloader;
        } else {
            return new EDTHelper<MyJDownloaderSettingsPanel>() {
                public MyJDownloaderSettingsPanel edtRun() {
                    if (myJDownloader != null) {
                        return myJDownloader;
                    } else {
                        myJDownloader = new MyJDownloaderSettingsPanel();
                        return myJDownloader;
                    }
                }
            }.getReturnValue();
        }
    }

    private Packagizer getPackagizer() {
        if (pz != null) {
            return pz;
        } else {
            return new EDTHelper<Packagizer>() {
                public Packagizer edtRun() {
                    if (pz != null) {
                        return pz;
                    } else {
                        pz = new Packagizer();
                        return pz;
                    }
                }
            }.getReturnValue();
        }
    }

    private Linkgrabber getLinkgrabber() {
        if (lg != null) {
            return lg;
        } else {
            return new EDTHelper<Linkgrabber>() {
                public Linkgrabber edtRun() {
                    if (lg != null) {
                        return lg;
                    } else {
                        lg = new Linkgrabber();
                        return lg;
                    }
                }
            }.getReturnValue();
        }
    }

    private AdvancedSettings getAdvancedSettings() {
        if (ads != null) {
            return ads;
        } else {
            return new EDTHelper<AdvancedSettings>() {
                public AdvancedSettings edtRun() {
                    if (ads != null) {
                        return ads;
                    } else {
                        ads = new AdvancedSettings();
                        return ads;
                    }
                }
            }.getReturnValue();
        }
    }

    private OptionalExtensionSettings getExtensionSettings() {
        if (es != null) {
            return es;
        } else {
            return new EDTHelper<OptionalExtensionSettings>() {
                public OptionalExtensionSettings edtRun() {
                    if (es != null) {
                        return es;
                    } else {
                        es = new OptionalExtensionSettings();
                        return es;
                    }
                }
            }.getReturnValue();
        }
    }

    private void edtAllElement(final Object element) {
        if (element != null) {
            new EDTRunner() {
                @Override
                protected void runInEDT() {
                    addElement(element);
                }
            };
        }
    }

    public void fill(final boolean finalWithExtensions) {
        new Thread("FillSettingsSideBarModel") {
            @Override
            public void run() {
                try {
                    synchronized (lock) {
                        boolean withExtensions = finalWithExtensions;
                        if (SecondLevelLaunch.EXTENSIONS_LOADED.isReached()) {
                            withExtensions = true;
                        }
                        LazyExtension extractionExtension = null;
                        try {
                            if (withExtensions) {
                                extractionExtension = ExtensionController.getInstance().getExtension("org.jdownloader.extensions.extraction.ExtractionExtension");
                            }
                        } catch (final Throwable e) {
                            /* plugin not loaded yet */
                        }
                        new EDTRunner() {
                            @Override
                            protected void runInEDT() {
                                removeAllElements();
                            }
                        };
                        edtAllElement(getConfigPanelGeneral());
                        edtAllElement(getReconnectSettings());
                        edtAllElement(getProxyConfig());
                        edtAllElement(getAccountManagerSettings());
                        edtAllElement(getBasicAuthentication());
                        edtAllElement(getPluginSettings());
                        edtAllElement(getAntiCaptchaConfigPanel());
                        edtAllElement(getGUISettings());
                        edtAllElement(getNotifierConfigPanel());
                        edtAllElement(getMyJDownloaderPanel());
                        edtAllElement(getLinkgrabber());
                        edtAllElement(getPackagizer());
                        if (extractionExtension != null) {
                            edtAllElement(extractionExtension);
                        }
                        edtAllElement(JDGui.getInstance().getTray());
                        edtAllElement(getAdvancedSettings());
                        if (withExtensions) {
                            edtAllElement(getExtensionSettings());
                            final List<LazyExtension> pluginsOptional = new ArrayList<LazyExtension>();
                            pluginsOptional.addAll(ExtensionController.getInstance().getExtensions());
                            Collections.sort(pluginsOptional, new Comparator<LazyExtension>() {
                                @Override
                                public int compare(LazyExtension a, LazyExtension b) {
                                    final String namea = a.getName();
                                    final String nameb = b.getName();
                                    return namea.compareTo(nameb);
                                }
                            });
                            if (pluginsOptional != null) {
                                for (final LazyExtension plg : pluginsOptional) {
                                    if ("org.jdownloader.extensions.extraction.ExtractionExtension".equals(plg.getClassname())) {
                                        continue;
                                    } else if ("org.jdownloader.extensions.jdtrayicon.TrayExtension".equals(plg.getClassname())) {
                                        // avoid that old TrayExtension Jars will get loaded
                                        continue;
                                    } else if (contains(plg)) {
                                        continue;
                                    } else if (CrossSystem.isWindows() && !plg.isWindowsRunnable()) {
                                        continue;
                                    } else if (CrossSystem.isUnix() && !plg.isLinuxRunnable()) {
                                        /* TODO: add own isBSD runnable or better provide the CrossSytem enum */
                                        continue;
                                    } else if (CrossSystem.isMac() && !plg.isMacRunnable()) {
                                        continue;
                                    } else {
                                        plg._getSettings()._getStorageHandler().getEventSender().addListener(SettingsSidebarModel.this, true);
                                        new EDTRunner() {
                                            @Override
                                            protected void runInEDT() {
                                                addElement(plg);
                                            }
                                        };
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    new EDTRunner() {
                        @Override
                        protected void runInEDT() {
                            if (list != null) {
                                list.repaint();
                            }
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
        if (JsonConfig.create(GraphicalUserInterfaceSettings.class).isConfigViewVisible()) {
            fill(true);
        }
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
