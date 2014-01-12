package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.ComboBox;
import jd.gui.swing.jdgui.views.settings.components.MultiComboBox;
import jd.gui.swing.jdgui.views.settings.components.ProxyInput;
import jd.gui.swing.jdgui.views.settings.components.TextInput;
import jd.gui.swing.jdgui.views.settings.panels.advanced.AdvancedConfigTableModel;
import jd.gui.swing.jdgui.views.settings.panels.advanced.AdvancedTable;
import jd.plugins.PluginConfigPanelNG;
import jd.plugins.decrypter.YoutubeVariant;
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
    private Pair<MultiVariantBox>    videoMp4;
    private Pair<MultiVariantBox>    videoWebm;
    private Pair<MultiVariantBox>    videoFlv;
    private Pair<MultiVariantBox>    videoGp3;
    private Pair<MultiVariantBox>    video3D;
    private Pair<MultiVariantBox>    videoAudio;
    private Pair<MultiVariantBox>    videoImage;

    public class MultiVariantBox extends MultiComboBox<YoutubeVariant> {

        private YoutubeDashConfigPanel panel;

        public MultiVariantBox(YoutubeDashConfigPanel youtubeDashConfigPanel, ArrayList<YoutubeVariant> list) {
            super(list.toArray(new YoutubeVariant[] {}));
            this.panel = youtubeDashConfigPanel;

        }

        @Override
        public void onChanged() {
            super.onChanged();
            panel.save();
        }

        @Override
        protected String getLabel(YoutubeVariant sc) {
            return _GUI._.YoutubeDashConfigPanel_MultiVariantBox_getLabel_(sc.getName());
        }

        @Override
        protected String getLabel(List<YoutubeVariant> list) {
            if (list.size() == 0) return super.getLabel(list);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                if (sb.length() > 0) {
                    if (i == list.size() - 1) {
                        sb.append(" & ");
                    } else {
                        sb.append(", ");
                    }
                }

                sb.append(list.get(i).getQualityExtension());

            }

            sb.append(" ");
            sb.append(list.get(0).getFileExtension());
            sb.append("-");
            if (list.size() > 1) {
                sb.append("Videos");
            } else {
                sb.append("Video");
            }
            return sb.toString();
        }

    }

    public YoutubeDashConfigPanel(String description) {
        ;
        cf = PluginJsonConfig.get(YoutubeConfig.class);

        addStartDescription(description);

        addPair(_GUI._.YoutubeDashConfigPanel_YoutubeDashConfigPanel_if_link_contains_video_and_playlist(), null, null, new ComboBox<IfUrlisAVideoAndPlaylistAction>(cf._getStorageHandler().getKeyHandler("LinkIsVideoAndPlaylistUrlAction", KeyHandler.class), IfUrlisAVideoAndPlaylistAction.values(), null));
        addPair(_GUI._.YoutubeDashConfigPanel_YoutubeDashConfigPanel_grouping(), null, null, new ComboBox<GroupLogic>(cf._getStorageHandler().getKeyHandler("GroupLogic", KeyHandler.class), GroupLogic.values(), null));
        addPair(_GUI._.YoutubeDashConfigPanel_YoutubeDashConfigPanel_https(), null, null, new Checkbox(cf._getStorageHandler().getKeyHandler("PreferHttpsEnabled", BooleanKeyHandler.class), null));
        addHeader("Allowed Mediatypes", NewTheme.I().getIcon(IconKey.ICON_MEDIAPLAYER, 18));
        HashSet<String> dupe = new HashSet<String>();
        ArrayList<YoutubeVariant> videoFLV = new ArrayList<YoutubeVariant>();
        ArrayList<YoutubeVariant> videoMP4 = new ArrayList<YoutubeVariant>();
        ArrayList<YoutubeVariant> videoWEBM = new ArrayList<YoutubeVariant>();
        ArrayList<YoutubeVariant> videoGP3 = new ArrayList<YoutubeVariant>();
        ArrayList<YoutubeVariant> video3D = new ArrayList<YoutubeVariant>();

        ArrayList<YoutubeVariant> audio = new ArrayList<YoutubeVariant>();
        ArrayList<YoutubeVariant> image = new ArrayList<YoutubeVariant>();
        for (YoutubeVariant ytv : YoutubeVariant.values()) {
            if (!dupe.add(ytv.getTypeId())) continue;
            switch (ytv.getGroup()) {
            case AUDIO:
                audio.add(ytv);
                break;
            case IMAGE:
                image.add(ytv);
                break;
            case VIDEO:
                if (ytv.name().startsWith("MP4")) {
                    videoMP4.add(ytv);

                } else if (ytv.name().startsWith("FLV")) {
                    videoFLV.add(ytv);

                } else if (ytv.name().startsWith("THREEGP")) {
                    videoGP3.add(ytv);

                } else {
                    videoWEBM.add(ytv);

                }

                break;
            case VIDEO_3D:
                video3D.add(ytv);
                break;

            }
        }
        Comparator<YoutubeVariant> comp = new Comparator<YoutubeVariant>() {

            @Override
            public int compare(YoutubeVariant o1, YoutubeVariant o2) {
                return new Double(o1.getQualityRating()).compareTo(new Double(o2.getQualityRating()));
            }
        };
        Collections.sort(videoMP4, comp);
        Collections.sort(video3D, comp);
        Collections.sort(videoFLV, comp);
        Collections.sort(videoGP3, comp);
        Collections.sort(videoMP4, comp);
        Collections.sort(videoWEBM, comp);
        Collections.sort(audio, comp);
        Collections.sort(image, comp);
        // Collections.sort(videoWEBM,comp );

        videoMp4 = addPair("Mp4 Video", null, null, new MultiVariantBox(this, videoMP4));
        videoWebm = addPair("Webm Video", null, null, new MultiVariantBox(this, videoWEBM));
        videoFlv = addPair("FLV Video", null, null, new MultiVariantBox(this, videoFLV));
        videoGp3 = addPair("GP3 Video", null, null, new MultiVariantBox(this, videoGP3));
        this.video3D = addPair("3D-Video", null, null, new MultiVariantBox(this, video3D));
        videoAudio = addPair("Audio", null, null, new MultiVariantBox(this, audio));
        videoImage = addPair("Image", null, null, new MultiVariantBox(this, image));
        addHeader(_GUI._.YoutubeDashConfigPanel_YoutubeDashConfigPanel_filename_pattern_header(), NewTheme.I().getIcon(IconKey.ICON_FILE, 18));
        addHtmlDescription(_GUI._.YoutubeDashConfigPanel_YoutubeDashConfigPanel_filename_desc(), false);
        addPair(_GUI._.YoutubeDashConfigPanel_YoutubeDashConfigPanel_filename_pattern(), null, null, new TextInput(cf._getStorageHandler().getKeyHandler("FilenamePattern", StringKeyHandler.class)));

        addHeader(_GUI._.YoutubeDashConfigPanel_YoutubeDashConfigPanel_proxy_header(), NewTheme.I().getIcon(IconKey.ICON_PROXY, 18));
        Pair<Checkbox> checkbox = addPair(_GUI._.YoutubeDashConfigPanel_YoutubeDashConfigPanel_userproxy(), null, null, new Checkbox(cf._getStorageHandler().getKeyHandler("ProxyEnabled", BooleanKeyHandler.class), null));

        Pair<ProxyInput> proxy = addPair("", null, null, new ProxyInput(cf._getStorageHandler().getKeyHandler("Proxy", KeyHandler.class)));
        checkbox.getComponent().setDependencies(proxy.getComponent());

        addHeader("DEBUG ONLY... This Plugin is not finished yet", NewTheme.I().getIcon(IconKey.ICON_WARNING, 18));
        add(new AdvancedTable(model = new AdvancedConfigTableModel("YoutubeConfig") {
            @Override
            public void refresh(String filterText) {
                _fireTableStructureChanged(register(), true);
            }
        }));
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

        HashSet<String> blacklistSet = new HashSet<String>();
        get(blacklistSet, video3D.getComponent());
        get(blacklistSet, videoAudio.getComponent());
        get(blacklistSet, videoFlv.getComponent());
        get(blacklistSet, videoGp3.getComponent());
        get(blacklistSet, videoImage.getComponent());
        get(blacklistSet, videoMp4.getComponent());
        get(blacklistSet, videoWebm.getComponent());
        cf.setBlacklistedVariants(blacklistSet.toArray(new String[] {}));
    }

    /**
     * @param list
     * @param blacklistSet
     */
    public void get(HashSet<String> blacklistSet, MultiVariantBox list) {
        for (YoutubeVariant v : list.getValues()) {
            if (!list.isItemSelected(v)) {
                blacklistSet.add(v.getUniqueId());
            }
        }
    }

    @Override
    public void updateContents() {
        // ifVideoAndPlaylistCombo.getComponent().setSelectedItem(cf.getLinkIsVideoAndPlaylistUrlAction());

        String[] blacklist = cf.getBlacklistedVariants();
        HashSet<String> blacklistSet = new HashSet<String>();
        if (blacklist != null) {
            for (String b : blacklist) {

                blacklistSet.add(b);
            }
        }

        setList(blacklistSet, video3D.getComponent());
        setList(blacklistSet, videoAudio.getComponent());
        setList(blacklistSet, videoFlv.getComponent());
        setList(blacklistSet, videoGp3.getComponent());
        setList(blacklistSet, videoImage.getComponent());
        setList(blacklistSet, videoMp4.getComponent());
        setList(blacklistSet, videoWebm.getComponent());

        //
    }

    /**
     * @param blacklistSet
     * @param list
     */
    public void setList(HashSet<String> blacklistSet, MultiVariantBox list) {
        for (YoutubeVariant v : list.getValues()) {

            list.setItemSelected(v, !blacklistSet.contains(v.getUniqueId()));

        }
    }

}
