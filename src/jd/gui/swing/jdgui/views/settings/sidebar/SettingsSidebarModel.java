package jd.gui.swing.jdgui.views.settings.sidebar;

import java.util.List;

import javax.swing.DefaultListModel;

import jd.gui.swing.jdgui.views.downloads.DownloadControll;
import jd.gui.swing.jdgui.views.settings.panels.BasicAuthentication;
import jd.gui.swing.jdgui.views.settings.panels.ConfigPanelGeneral;
import jd.gui.swing.jdgui.views.settings.panels.GUISettings;
import jd.gui.swing.jdgui.views.settings.panels.ReconnectSettings;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.AccountManagerSettings;
import jd.gui.swing.jdgui.views.settings.panels.advanced.AdvancedSettings;
import jd.gui.swing.jdgui.views.settings.panels.downloadandnetwork.ProxyConfig;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.Linkgrabber;
import jd.gui.swing.jdgui.views.settings.panels.packagizer.Packagizer;
import jd.gui.swing.jdgui.views.settings.panels.pluginsettings.PluginSettings;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.controlling.filter.LinkFilterSettings;
import org.jdownloader.controlling.packagizer.PackagizerSettings;
import org.jdownloader.extensions.AbstractExtensionWrapper;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.jdtrayicon.TrayExtension;

public class SettingsSidebarModel extends DefaultListModel<Object> implements GenericConfigEventListener<Boolean> {

    private static final long      serialVersionUID = -204494527404304349L;
    private Object                 LOCK             = new Object();
    private ConfigPanelGeneral     cfg;
    private DownloadControll       dlc;
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
        LinkFilterSettings.LINK_FILTER_ENABLED.getEventSender().addListener(this);
        PackagizerSettings.ENABLED.getEventSender().addListener(this);
    }

    public void fill() {
        synchronized (LOCK) {
            removeAllElements();
            AbstractExtensionWrapper extract = ExtensionController.getInstance().getExtension(ExtractionExtension.class);
            AbstractExtensionWrapper tray = ExtensionController.getInstance().getExtension(TrayExtension.class);

            if (cfg == null) cfg = new ConfigPanelGeneral();
            addElement(cfg);
            if (dlc == null) dlc = new DownloadControll();
            addElement(dlc);

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
            List<AbstractExtensionWrapper> pluginsOptional = ExtensionController.getInstance().getExtensions();

            for (final AbstractExtensionWrapper plg : pluginsOptional) {
                if (contains(plg)) continue;
                if (CrossSystem.isWindows() && !plg.isWindowsRunnable()) continue;
                if (CrossSystem.isLinux() && !plg.isLinuxRunnable()) continue;
                if (CrossSystem.isMac() && !plg.isMacRunnable()) continue;

                if (first) {
                    if (eh == null) eh = new ExtensionHeader();
                    addElement(eh);
                }
                first = false;
                addElement(plg);
            }
        }
    }

    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        fireContentsChanged(this, 0, size() - 1);
    }

}
