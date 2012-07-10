package jd.gui.swing.jdgui.views.settings.sidebar;

import java.util.List;

import javax.swing.DefaultListModel;

import jd.gui.swing.jdgui.views.settings.panels.BasicAuthentication;
import jd.gui.swing.jdgui.views.settings.panels.ConfigPanelGeneral;
import jd.gui.swing.jdgui.views.settings.panels.GUISettings;
import jd.gui.swing.jdgui.views.settings.panels.ReconnectSettings;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.AccountManagerSettings;
import jd.gui.swing.jdgui.views.settings.panels.advanced.AdvancedSettings;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.Linkgrabber;
import jd.gui.swing.jdgui.views.settings.panels.packagizer.Packagizer;
import jd.gui.swing.jdgui.views.settings.panels.pluginsettings.PluginSettings;
import jd.gui.swing.jdgui.views.settings.panels.proxy.ProxyConfig;

import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.ExtensionControllerListener;
import org.jdownloader.extensions.LazyExtension;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class SettingsSidebarModel extends DefaultListModel implements GenericConfigEventListener<Boolean>, ExtensionControllerListener {

    private static final long      serialVersionUID = -204494527404304349L;
    private Object                 LOCK             = new Object();
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

    public SettingsSidebarModel() {
        super();
        org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINK_FILTER_ENABLED.getEventSender().addListener(this);
        org.jdownloader.settings.staticreferences.CFG_PACKAGIZER.PACKAGIZER_ENABLED.getEventSender().addListener(this);
        ExtensionController.getInstance().getEventSender().addListener(this);
    }

    public void fill() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                synchronized (LOCK) {
                    removeAllElements();
                    LazyExtension extract = null;
                    try {
                        extract = ExtensionController.getInstance().getExtension(org.jdownloader.extensions.extraction.ExtractionExtension.class);
                    } catch (final Throwable e) {
                        /* plugin not loaded yet */
                    }
                    LazyExtension tray = null;
                    try {
                        tray = ExtensionController.getInstance().getExtension(org.jdownloader.extensions.jdtrayicon.TrayExtension.class);
                    } catch (final Throwable e) {
                        /* plugin not loaded yet */
                    }

                    if (cfg == null) cfg = new ConfigPanelGeneral();
                    addElement(cfg);

                    // addElement(new ToolbarController());
                    if (rcs == null) rcs = new ReconnectSettings();
                    addElement(rcs);
                    if (pc == null) pc = new ProxyConfig();
                    addElement(pc);
                    if (ams == null) ams = new AccountManagerSettings();
                    addElement(ams);
                    if (ba == null) ba = new BasicAuthentication();
                    addElement(ba);

                    // addElement(new Premium());
                    if (ps == null) ps = new PluginSettings();
                    addElement(ps);
                    // addElement(new ExtensionManager());
                    if (gs == null) gs = new GUISettings();
                    addElement(gs);
                    // modules

                    if (lg == null) lg = new jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.Linkgrabber();
                    addElement(lg);
                    if (pz == null) pz = new Packagizer();
                    addElement(pz);
                    if (extract != null) addElement(extract);
                    if (tray != null) addElement(tray);
                    if (ads == null) ads = new AdvancedSettings();
                    addElement(ads);
                    boolean first = true;
                    List<LazyExtension> pluginsOptional = ExtensionController.getInstance().getExtensions();
                    if (pluginsOptional != null) {
                        for (final LazyExtension plg : pluginsOptional) {

                            if (contains(plg)) continue;
                            if (CrossSystem.isWindows() && !plg.isWindowsRunnable()) continue;
                            if (CrossSystem.isLinux() && !plg.isLinuxRunnable()) continue;
                            if (CrossSystem.isMac() && !plg.isMacRunnable()) continue;
                            final int index = getSize();
                            plg._getSettings().getStorageHandler().getEventSender().addListener(new GenericConfigEventListener<Object>() {

                                public void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {
                                    fireContentsChanged(this, index, index);
                                }

                                public void onConfigValidatorError(KeyHandler<Object> keyHandler, Object invalidValue, ValidationException validateException) {
                                }
                            });
                            if (first) {
                                if (eh == null) eh = new ExtensionHeader();
                                addElement(eh);
                            }
                            first = false;
                            addElement(plg);
                        }
                    }
                }
            }
        };

    }

    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        fireContentsChanged(this, 0, size() - 1);
    }

    public void onUpdated() {
        new Thread() {
            @Override
            public void run() {
                /*
                 * extra thread, because we dont want to block the eventsender
                 */
                if (JsonConfig.create(GraphicalUserInterfaceSettings.class).isConfigViewVisible()) {
                    fill();
                }
            }

        }.start();

    }

}
