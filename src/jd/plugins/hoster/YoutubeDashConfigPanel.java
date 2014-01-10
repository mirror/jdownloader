package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JScrollPane;

import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.ComboBox;
import jd.gui.swing.jdgui.views.settings.components.ProxyInput;
import jd.gui.swing.jdgui.views.settings.components.TextInput;
import jd.gui.swing.jdgui.views.settings.panels.advanced.AdvancedConfigTableModel;
import jd.gui.swing.jdgui.views.settings.panels.advanced.AdvancedTable;
import jd.plugins.PluginConfigPanelNG;
import jd.plugins.hoster.YoutubeDashV2.YoutubeConfig;
import jd.plugins.hoster.YoutubeDashV2.YoutubeConfig.GroupLogic;
import jd.plugins.hoster.YoutubeDashV2.YoutubeConfig.IfUrlisAVideoAndPlaylistAction;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.storage.config.handler.StringKeyHandler;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.settings.Pair;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.settings.advanced.AdvancedConfigEntry;

public class YoutubeDashConfigPanel extends PluginConfigPanelNG {

    private AdvancedConfigTableModel model;
    private YoutubeConfig            cf;

    public YoutubeDashConfigPanel(String description) {
        ;
        cf = PluginJsonConfig.get(YoutubeConfig.class);

        addStartDescription(description);

        addPair(_GUI._.YoutubeDashConfigPanel_YoutubeDashConfigPanel_if_link_contains_video_and_playlist(), null, null, new ComboBox<IfUrlisAVideoAndPlaylistAction>(cf._getStorageHandler().getKeyHandler("LinkIsVideoAndPlaylistUrlAction", KeyHandler.class), IfUrlisAVideoAndPlaylistAction.values(), null));
        addPair(_GUI._.YoutubeDashConfigPanel_YoutubeDashConfigPanel_grouping(), null, null, new ComboBox<GroupLogic>(cf._getStorageHandler().getKeyHandler("GroupLogic", KeyHandler.class), GroupLogic.values(), null));
        addPair(_GUI._.YoutubeDashConfigPanel_YoutubeDashConfigPanel_https(), null, null, new Checkbox(cf._getStorageHandler().getKeyHandler("PreferHttpsEnabled", BooleanKeyHandler.class), null));

        addHeader(_GUI._.YoutubeDashConfigPanel_YoutubeDashConfigPanel_filename_pattern_header(), NewTheme.I().getIcon(IconKey.ICON_FILE, 18));
        addHtmlDescription(_GUI._.YoutubeDashConfigPanel_YoutubeDashConfigPanel_filename_desc(), false);
        addPair(_GUI._.YoutubeDashConfigPanel_YoutubeDashConfigPanel_filename_pattern(), null, null, new TextInput(cf._getStorageHandler().getKeyHandler("FilenamePattern", StringKeyHandler.class)));

        addHeader(_GUI._.YoutubeDashConfigPanel_YoutubeDashConfigPanel_proxy_header(), NewTheme.I().getIcon(IconKey.ICON_PROXY, 18));
        Pair<Checkbox> checkbox = addPair(_GUI._.YoutubeDashConfigPanel_YoutubeDashConfigPanel_userproxy(), null, null, new Checkbox(cf._getStorageHandler().getKeyHandler("ProxyEnabled", BooleanKeyHandler.class), null));

        Pair<ProxyInput> proxy = addPair("", null, null, new ProxyInput(cf._getStorageHandler().getKeyHandler("Proxy", KeyHandler.class)));
        checkbox.getComponent().setDependencies(proxy.getComponent());

        addHeader("DEBUG ONLY... This Plugin is not finished yet", NewTheme.I().getIcon(IconKey.ICON_WARNING, 18));
        add(new JScrollPane(new AdvancedTable(model = new AdvancedConfigTableModel("YoutubeConfig") {
            @Override
            public void refresh(String filterText) {
                _fireTableStructureChanged(register(), true);
            }
        })));
        model.refresh("Youtube");
    }

    public static void main(String[] args) {

    }

    public ArrayList<AdvancedConfigEntry> register() {
        ArrayList<AdvancedConfigEntry> configInterfaces = new ArrayList<AdvancedConfigEntry>();
        HashMap<KeyHandler, Boolean> map = new HashMap<KeyHandler, Boolean>();

        for (KeyHandler m : cf._getStorageHandler().getMap().values()) {

            if (map.containsKey(m)) continue;

            if (m.getAnnotation(AboutConfig.class) != null) {
                if (m.getSetter() == null) {
                    throw new RuntimeException("Setter for " + m.getGetter().getMethod() + " missing");
                } else if (m.getGetter() == null) {
                    throw new RuntimeException("Getter for " + m.getSetter().getMethod() + " missing");
                } else {
                    synchronized (configInterfaces) {
                        configInterfaces.add(new AdvancedConfigEntry(cf, m));
                    }
                    map.put(m, true);
                }
            }

        }

        return configInterfaces;
    }

    @Override
    public void reset() {
    }

    @Override
    public void save() {

    }

    @Override
    public void updateContents() {
        // ifVideoAndPlaylistCombo.getComponent().setSelectedItem(cf.getLinkIsVideoAndPlaylistUrlAction());
    }

}
