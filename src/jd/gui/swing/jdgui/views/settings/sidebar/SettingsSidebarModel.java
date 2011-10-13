package jd.gui.swing.jdgui.views.settings.sidebar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.DefaultListModel;

import jd.gui.swing.jdgui.views.downloads.DownloadControll;
import jd.gui.swing.jdgui.views.settings.panels.BasicAuthentication;
import jd.gui.swing.jdgui.views.settings.panels.ConfigPanelGeneral;
import jd.gui.swing.jdgui.views.settings.panels.GUISettings;
import jd.gui.swing.jdgui.views.settings.panels.ReconnectSettings;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.AccountManagerSettings;
import jd.gui.swing.jdgui.views.settings.panels.advanced.AdvancedSettings;
import jd.gui.swing.jdgui.views.settings.panels.downloadandnetwork.ProxyConfig;
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

public class SettingsSidebarModel extends DefaultListModel implements GenericConfigEventListener<Boolean> {

    private static final long serialVersionUID = -204494527404304349L;

    public SettingsSidebarModel() {
        super();
        fill();
    }

    private void fill() {
        removeAllElements();
        AbstractExtensionWrapper extract = ExtensionController.getInstance().getExtension(ExtractionExtension.class);
        AbstractExtensionWrapper tray = ExtensionController.getInstance().getExtension(TrayExtension.class);
        addElement(new ConfigPanelGeneral());
        addElement(new DownloadControll());

        // addElement(new ToolbarController());

        addElement(new ReconnectSettings());
        addElement(new ProxyConfig());
        addElement(new AccountManagerSettings());
        addElement(new BasicAuthentication());

        // addElement(new Premium());

        addElement(new PluginSettings());
        // addElement(new ExtensionManager());
        addElement(new GUISettings());
        // modules
        addElement(new jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.Linkgrabber());
        addElement(new Packagizer());
        addElement(extract);
        addElement(tray);
        addElement(new AdvancedSettings());

        LinkFilterSettings.LINK_FILTER_ENABLED.getEventSender().addListener(this);
        PackagizerSettings.ENABLED.getEventSender().addListener(this);
        boolean first = true;
        ArrayList<AbstractExtensionWrapper> pluginsOptional = ExtensionController.getInstance().getExtensions();
        Collections.sort(pluginsOptional, new Comparator<AbstractExtensionWrapper>() {

            public int compare(AbstractExtensionWrapper o1, AbstractExtensionWrapper o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        for (final AbstractExtensionWrapper plg : pluginsOptional) {
            if (plg == extract || plg == tray) continue;
            if (CrossSystem.isWindows() && !plg.isWindowsRunnable()) continue;
            if (CrossSystem.isLinux() && !plg.isLinuxRunnable()) continue;
            if (CrossSystem.isMac() && !plg.isMacRunnable()) continue;

            if (first) {
                addElement(new ExtensionHeader());
            }
            first = false;
            addElement(plg);

        }
    }

    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        fireContentsChanged(this, 0, size() - 1);
    }

}
