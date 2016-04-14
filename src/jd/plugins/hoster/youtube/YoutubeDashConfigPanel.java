package jd.plugins.hoster.youtube;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JSeparator;

import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.storage.config.handler.StringKeyHandler;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.settings.Pair;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.plugins.components.youtube.BlackOrWhitelistEntry;
import org.jdownloader.plugins.components.youtube.YoutubeConfig;
import org.jdownloader.plugins.components.youtube.YoutubeConfig.GroupLogic;
import org.jdownloader.plugins.components.youtube.YoutubeConfig.IfUrlisAPlaylistAction;
import org.jdownloader.plugins.components.youtube.YoutubeConfig.IfUrlisAVideoAndPlaylistAction;
import org.jdownloader.plugins.components.youtube.YoutubeHelper;
import org.jdownloader.plugins.components.youtube.variants.AbstractVariant;
import org.jdownloader.plugins.components.youtube.variants.AudioVariant;
import org.jdownloader.plugins.components.youtube.variants.FileContainer;
import org.jdownloader.plugins.components.youtube.variants.ImageVariant;
import org.jdownloader.plugins.components.youtube.variants.VariantBase;
import org.jdownloader.plugins.components.youtube.variants.VideoVariant;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.settings.staticreferences.CFG_YOUTUBE;

import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.ComboBox;
import jd.gui.swing.jdgui.views.settings.components.MultiComboBox;
import jd.gui.swing.jdgui.views.settings.components.ProxyInput;
import jd.gui.swing.jdgui.views.settings.components.TextInput;
import jd.gui.swing.jdgui.views.settings.panels.advanced.AdvancedConfigTableModel;
import jd.plugins.PluginConfigPanelNG;

public class YoutubeDashConfigPanel extends PluginConfigPanelNG {

    private AdvancedConfigTableModel        model;
    private YoutubeConfig                   cf;
    private Pair<MultiVariantBoxForVideo>   videoMp4;
    private Pair<MultiVariantBoxForVideo>   videoWebm;
    private Pair<MultiVariantBoxForVideo>   videoFlv;
    private Pair<MultiVariantBoxForVideo>   videoGp3;
    private Pair<MultiVariantBoxFor3DVideo> video3D;
    private Pair<MultiVariantBoxForAudio>   audioPair;
    private Pair<MultiVariantBoxForImage>   imagePair;
    private Pair<Checkbox>                  subtitles;
    private Pair<MultiVariantBoxForVideo>   extraVideoMp4;
    private Pair<MultiVariantBoxForVideo>   extraVideoWebm;
    private Pair<MultiVariantBoxForVideo>   extraVideoFlv;
    private Pair<MultiVariantBoxForVideo>   extraVideoGp3;
    private Pair<MultiVariantBoxFor3DVideo> extraVideo3D;
    private Pair<MultiVariantBoxForAudio>   extraAudio;
    private Pair<MultiVariantBoxForImage>   extraImage;
    private boolean                         setting = false;
    private Pair<Checkbox>                  bestVideo;
    private Pair<Checkbox>                  bestAudio;
    private Pair<Checkbox>                  best3D;
    private Pair<Checkbox>                  bestSubtitle;
    private Pair<Checkbox>                  bestImage;
    private Pair<Checkbox>                  bestLimit;

    private final class MultiVariantBoxForImage extends MultiVariantBox<ImageVariant> {
        private MultiVariantBoxForImage(YoutubeDashConfigPanel youtubeDashConfigPanel, List<ImageVariant> list) {
            super(youtubeDashConfigPanel, list);
        }

        @Override
        protected String variantToName(ImageVariant sc, boolean popup) {

            return sc._getName(this);
        }
    }

    private final class MultiVariantBoxForAudio extends MultiVariantBox<AudioVariant> {
        private MultiVariantBoxForAudio(YoutubeDashConfigPanel youtubeDashConfigPanel, List<AudioVariant> list) {
            super(youtubeDashConfigPanel, list);
        }

        @Override
        protected String variantToName(AudioVariant sc, boolean popup) {
            if (PluginJsonConfig.get(YoutubeConfig.class).isAdvancedVariantNamesEnabled() && popup) {
                return sc.createAdvancedName();
            }

            return sc._getName(YoutubeDashConfigPanel.class);
        }
    }

    private final class MultiVariantBoxFor3DVideo extends MultiVariantBox<VideoVariant> {
        private MultiVariantBoxFor3DVideo(YoutubeDashConfigPanel youtubeDashConfigPanel, List<VideoVariant> list) {
            super(youtubeDashConfigPanel, list);
        }

        @Override
        protected String variantToName(VideoVariant sc, boolean popup) {

            if (PluginJsonConfig.get(YoutubeConfig.class).isAdvancedVariantNamesEnabled()) {
                return sc.createAdvancedName();
            }

            return sc._getName(YoutubeDashConfigPanel.class);

        }
    }

    private final class MultiVariantBoxForVideo extends MultiVariantBox<VideoVariant> {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        private MultiVariantBoxForVideo(YoutubeDashConfigPanel youtubeDashConfigPanel, List<VideoVariant> list) {
            super(youtubeDashConfigPanel, list);
        }

        @Override
        protected String variantToName(VideoVariant sc, boolean popup) {
            if (PluginJsonConfig.get(YoutubeConfig.class).isAdvancedVariantNamesEnabled() && popup) {
                return sc.createAdvancedName();

            }
            return sc._getName(YoutubeDashConfigPanel.class);

        }
    }

    public abstract class MultiVariantBox<Type extends AbstractVariant> extends MultiComboBox<Type> {

        private YoutubeDashConfigPanel panel;

        public MultiVariantBox(YoutubeDashConfigPanel youtubeDashConfigPanel, List<Type> list) {
            super(list);
            this.panel = youtubeDashConfigPanel;

        }

        @Override
        public void onChanged() {
            super.onChanged();
            panel.save();
        }

        //
        @Override
        protected String getLabel(int i, Type sc) {
            String name = variantToName(sc, true);

            if (i == 0) {
                return _GUI.T.YoutubeDashConfigPanel_MultiVariantBox_getLabel_(name) + " (" + _GUI.T.YoutubeDashConfigPanel_getLabel_best() + ")";
            } else if (i == getValues().size() - 1) {

                return _GUI.T.YoutubeDashConfigPanel_MultiVariantBox_getLabel_(name) + " (" + _GUI.T.YoutubeDashConfigPanel_getLabel_worst() + ")";
            }
            return _GUI.T.YoutubeDashConfigPanel_MultiVariantBox_getLabel_(name);
        }

        protected abstract String variantToName(Type sc, boolean popup);

        @Override
        protected String getLabel(Type sc) {
            return variantToName(sc, true);
        }

        @Override
        protected String getLabel(List<Type> list) {
            if (list.size() == 0) {
                return super.getLabel(list);
            }
            StringBuilder sb = new StringBuilder();
            HashSet<String> dupes = new HashSet<String>();
            for (int i = 0; i < list.size(); i++) {
                String name = (variantToName(list.get(i), false));
                if (!dupes.add(name)) {
                    continue;
                }
                if (sb.length() > 0) {
                    if (i == list.size() - 1) {
                        sb.append(" & ");
                    } else {
                        sb.append(", ");
                    }
                }

                sb.append(name);

            }

            return "(" + list.size() + "/" + getValues().size() + ") " + sb.toString();
        }

    }

    public YoutubeDashConfigPanel(String description) {
        cf = PluginJsonConfig.get(YoutubeConfig.class);

        addStartDescription(description);

        addPair(_GUI.T.YoutubeDashConfigPanel_YoutubeDashConfigPanel_if_link_contains_video_and_playlist(), null, null, new ComboBox<IfUrlisAVideoAndPlaylistAction>(cf._getStorageHandler().getKeyHandler("LinkIsVideoAndPlaylistUrlAction", KeyHandler.class), IfUrlisAVideoAndPlaylistAction.values(), null));
        addPair(_GUI.T.YoutubeDashConfigPanel_YoutubeDashConfigPanel_if_link_equals_playlist(), null, null, new ComboBox<IfUrlisAPlaylistAction>(cf._getStorageHandler().getKeyHandler("LinkIsPlaylistUrlAction", KeyHandler.class), IfUrlisAPlaylistAction.values(), null));

        // ComboBox<GroupLogic> box = new ComboBox<GroupLogic>(cf._getStorageHandler().getKeyHandler("GroupLogic", KeyHandler.class),
        // GroupLogic.values(), null) {
        // @Override
        // public void onConfigValueModified(KeyHandler<GroupLogic> keyHandler, GroupLogic newValue) {
        // super.onConfigValueModified(keyHandler, newValue);
        // updateBest();
        // }
        // };
        //
        // addPair(_GUI.T.YoutubeDashConfigPanel_YoutubeDashConfigPanel_grouping(), null, null, box);
        addDescriptionPlain(_GUI.T.YoutubeDashConfigPanel_YoutubeDashConfigPanel_fastcrawling_desc());
        addPair(_GUI.T.YoutubeDashConfigPanel_YoutubeDashConfigPanel_fastcrawling(), null, null, new Checkbox(cf._getStorageHandler().getKeyHandler("FastLinkCheckEnabled", BooleanKeyHandler.class), null));

        HashSet<String> dupe = new HashSet<String>();
        ArrayList<VideoVariant> videoFLV = new ArrayList<VideoVariant>();
        ArrayList<VideoVariant> videoMP4 = new ArrayList<VideoVariant>();
        ArrayList<VideoVariant> videoWEBM = new ArrayList<VideoVariant>();
        ArrayList<VideoVariant> videoGP3 = new ArrayList<VideoVariant>();
        ArrayList<VideoVariant> video3D = new ArrayList<VideoVariant>();

        ArrayList<AudioVariant> audio = new ArrayList<AudioVariant>();
        ArrayList<ImageVariant> image = new ArrayList<ImageVariant>();
        VariantBase[] variants = VariantBase.values();
        Comparator<VariantBase> comp = new Comparator<VariantBase>() {

            @Override
            public int compare(VariantBase o1, VariantBase o2) {
                return new Double(o2.getQualityRating()).compareTo(new Double(o1.getQualityRating()));
            }
        };
        ArrayList<VariantBase> sorted = new ArrayList<VariantBase>();
        for (VariantBase ytv : variants) {
            sorted.add(ytv);
        }
        Collections.sort(sorted, comp);
        for (VariantBase ytv : sorted) {

            AbstractVariant variant = AbstractVariant.get(ytv);
            if (!dupe.add(variant.getTypeId())) {
                continue;
            }

            switch (ytv.getGroup()) {
            case AUDIO:
                audio.add((AudioVariant) variant);
                break;
            case IMAGE:
                image.add((ImageVariant) variant);
                break;
            case VIDEO:
                FileContainer videoTag = ytv.getContainer();

                switch (videoTag) {
                case FLV:
                    videoFLV.add((VideoVariant) variant);

                    break;
                case MP4:
                    videoMP4.add((VideoVariant) variant);

                    break;
                case THREEGP:
                    videoGP3.add((VideoVariant) variant);

                    break;
                case WEBM:
                    videoWEBM.add((VideoVariant) variant);

                    break;
                }
                VideoVariant threeD = (VideoVariant) variant;
                threeD = (VideoVariant) AbstractVariant.get(ytv);
                threeD.getGenericInfo().setThreeD(true);
                video3D.add(threeD);
                break;
            case VIDEO_3D:
                video3D.add((VideoVariant) AbstractVariant.get(ytv));
                break;

            }
        }

        // Collections.sort(videoMP4, comp);
        // Collections.sort(video3D, comp);
        // Collections.sort(videoFLV, comp);
        // Collections.sort(videoGP3, comp);
        // Collections.sort(videoMP4, comp);
        // Collections.sort(videoWEBM, comp);
        // Collections.sort(audio, comp);
        // Collections.sort(image, comp);
        // Collections.sort(videoWEBM,comp );
        addHeader(_GUI.T.YoutubeDashConfigPanel_allowedtypoes(), NewTheme.I().getIcon(IconKey.ICON_MEDIAPLAYER, 18));
        videoMp4 = addPair(_GUI.T.YoutubeDashConfigPanel_allowedtypoes_mp4(), null, null, new MultiVariantBoxForVideo(this, videoMP4));
        videoWebm = addPair(_GUI.T.YoutubeDashConfigPanel_allowedtypoes_webm(), null, null, new MultiVariantBoxForVideo(this, videoWEBM));
        videoFlv = addPair(_GUI.T.YoutubeDashConfigPanel_allowedtypoes_flv(), null, null, new MultiVariantBoxForVideo(this, videoFLV));
        videoGp3 = addPair(_GUI.T.YoutubeDashConfigPanel_allowedtypoes_gp3(), null, null, new MultiVariantBoxForVideo(this, videoGP3));
        this.video3D = addPair(_GUI.T.YoutubeDashConfigPanel_allowedtypoes_3D(), null, null, new MultiVariantBoxFor3DVideo(this, video3D));

        audioPair = addPair(_GUI.T.YoutubeDashConfigPanel_allowedtypoes_audio(), null, null, new MultiVariantBoxForAudio(this, audio));

        imagePair = addPair(_GUI.T.YoutubeDashConfigPanel_allowedtypoes_image(), null, null, new MultiVariantBoxForImage(this, image));
        subtitles = addPair(_GUI.T.YoutubeDashConfigPanel_allowedtypoes_subtitles(), null, null, new Checkbox(cf._getStorageHandler().getKeyHandler("SubtitlesEnabled", BooleanKeyHandler.class), null));

        addHeader(_GUI.T.YoutubeDashConfigPanel_YoutubeDashConfigPanel_filename_or_package_pattern_header(), NewTheme.I().getIcon(IconKey.ICON_FILE, 18));
        addDescriptionPlain(_GUI.T.YoutubeDashConfigPanel_YoutubeDashConfigPanel_tags());
        for (org.jdownloader.plugins.components.youtube.YoutubeReplacer r : YoutubeHelper.REPLACER) {

            StringBuilder sb = new StringBuilder();
            for (String s : r.getTags()) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append("*").append(s).append("*");
            }
            JLabel txt = new JLabel("<html>" + sb.toString().replace("\r\n", "<br>").replace("\r", "<br>").replace("\n", "<br>") + "<html>");
            SwingUtils.setOpaque(txt, false);
            txt.setEnabled(false);

            add(txt, "gaptop 0,growx,pushx,gapleft 10,gapbottom 0,gaptop 0,aligny top");

            txt = new JLabel("<html>" + r.getDescription().replace("\r\n", "<br>").replace("\r", "<br>").replace("\n", "<br>") + "<html>");
            SwingUtils.setOpaque(txt, false);
            txt.setEnabled(false);
            add(txt, "gaptop 0,spanx,growx,pushx,wmin 10,gapbottom 0,gaptop 0,aligny top");

        }
        {
            addDescriptionPlain(_GUI.T.YoutubeHelper_getDescription_upperlowercasereplacetags_header());

            JLabel txt = new JLabel("<html>" + _GUI.T.YoutubeHelper_getDescription_upperlowercase().replace("\r\n", "<br>").replace("\r", "<br>").replace("\n", "<br>") + "<html>");
            SwingUtils.setOpaque(txt, false);
            txt.setEnabled(false);
            add(txt, "gaptop 0,growx,pushx,spanx,gapleft 10,gapbottom 0,gaptop 0,aligny top");

            txt = new JLabel("<html>" + _GUI.T.YoutubeHelper_getDescription_replacetags().replace("\r\n", "<br>").replace("\r", "<br>").replace("\n", "<br>") + "<html>");
            SwingUtils.setOpaque(txt, false);
            txt.setEnabled(false);
            add(txt, "gaptop 0,growx,pushx,spanx,gapleft 10,gapbottom 0,gaptop 0,aligny top");

            // TODO: Coalado place some type of help guide in here for
            // title:
            // Optional in tag components: to be used within *tags* above. Must be presented after standard tag name
            // Body:
            // [UC] UpperCase or [LC] LowerCase, you can only use ONE of these.
            // [SU] Space to Underscore

        }
        addPair(_GUI.T.YoutubeDashConfigPanel_YoutubeDashConfigPanel_package_pattern(), null, null, new TextInput(cf._getStorageHandler().getKeyHandler("PackagePattern", StringKeyHandler.class)));

        addPair(_GUI.T.YoutubeDashConfigPanel_YoutubeDashConfigPanel_filename_pattern_video(), null, null, new TextInput(CFG_YOUTUBE.VIDEO_FILENAME_PATTERN));
        addPair(_GUI.T.YoutubeDashConfigPanel_YoutubeDashConfigPanel_filename_pattern_video3D(), null, null, new TextInput(CFG_YOUTUBE.VIDEO3DFILENAME_PATTERN));
        addPair(_GUI.T.YoutubeDashConfigPanel_YoutubeDashConfigPanel_filename_pattern_audio(), null, null, new TextInput(CFG_YOUTUBE.AUDIO_FILENAME_PATTERN));
        addPair(_GUI.T.YoutubeDashConfigPanel_YoutubeDashConfigPanel_filename_pattern_image(), null, null, new TextInput(CFG_YOUTUBE.IMAGE_FILENAME_PATTERN));
        addPair(_GUI.T.YoutubeDashConfigPanel_YoutubeDashConfigPanel_filename_pattern_subtitle(), null, null, new TextInput(CFG_YOUTUBE.SUBTITLE_FILENAME_PATTERN));

        addHeader(_GUI.T.YoutubeDashConfigPanel_YoutubeDashConfigPanel_proxy_header(), NewTheme.I().getIcon(IconKey.ICON_PROXY, 18));
        Pair<Checkbox> checkbox = addPair(_GUI.T.YoutubeDashConfigPanel_YoutubeDashConfigPanel_userproxy(), null, null, new Checkbox(cf._getStorageHandler().getKeyHandler("ProxyEnabled", BooleanKeyHandler.class), null));

        Pair<ProxyInput> proxy = addPair("", null, null, new ProxyInput(cf._getStorageHandler().getKeyHandler("Proxy", KeyHandler.class)));
        checkbox.getComponent().setDependencies(proxy.getComponent());
        addHeader(_GUI.T.YoutubeDashConfigPanel_links(), NewTheme.I().getIcon(IconKey.ICON_LIST, 18));

        BooleanKeyHandler videoKeyhandler;
        bestVideo = addPair(_GUI.T.YoutubeDashConfigPanel_allowedtypoes_best_video(), null, null, new Checkbox(videoKeyhandler = cf._getStorageHandler().getKeyHandler("CreateBestVideoVariantLinkEnabled", BooleanKeyHandler.class), null));
        addDescriptionPlain(_GUI.T.YoutubeDashConfigPanel_YoutubeDashConfigPanel_best_explain());
        bestLimit = addPair(_GUI.T.YoutubeDashConfigPanel_allowedtypoes_best_limitation(), null, null, new Checkbox(cf._getStorageHandler().getKeyHandler("BestVideoVariant1080pLimitEnabled", BooleanKeyHandler.class), null));
        bestVideo.getComponent().setDependencies(bestLimit.getComponent());
        add(new JSeparator(), "pushx,growx,spanx");
        bestImage = addPair(_GUI.T.YoutubeDashConfigPanel_allowedtypoes_best_image(), null, null, new Checkbox(cf._getStorageHandler().getKeyHandler("CreateBestImageVariantLinkEnabled", BooleanKeyHandler.class), null));

        bestAudio = addPair(_GUI.T.YoutubeDashConfigPanel_allowedtypoes_best_audio(), null, null, new Checkbox(cf._getStorageHandler().getKeyHandler("CreateBestAudioVariantLinkEnabled", BooleanKeyHandler.class), null));
        best3D = addPair(_GUI.T.YoutubeDashConfigPanel_allowedtypoes_best_3d(), null, null, new Checkbox(cf._getStorageHandler().getKeyHandler("CreateBest3DVariantLinkEnabled", BooleanKeyHandler.class), null));
        bestSubtitle = addPair(_GUI.T.YoutubeDashConfigPanel_allowedtypoes_best_subtitle(), null, null, new Checkbox(cf._getStorageHandler().getKeyHandler("CreateBestSubtitleVariantLinkEnabled", BooleanKeyHandler.class), null));

        addDescriptionPlain(_GUI.T.YoutubeDashConfigPanel_YoutubeDashConfigPanel_extra_desc());
        extraVideoMp4 = addPair(_GUI.T.YoutubeDashConfigPanel_allowedtypoes_mp4(), null, null, new MultiVariantBoxForVideo(this, videoMP4));
        extraVideoWebm = addPair(_GUI.T.YoutubeDashConfigPanel_allowedtypoes_webm(), null, null, new MultiVariantBoxForVideo(this, videoWEBM));
        extraVideoFlv = addPair(_GUI.T.YoutubeDashConfigPanel_allowedtypoes_flv(), null, null, new MultiVariantBoxForVideo(this, videoFLV));
        extraVideoGp3 = addPair(_GUI.T.YoutubeDashConfigPanel_allowedtypoes_gp3(), null, null, new MultiVariantBoxForVideo(this, videoGP3));
        extraVideo3D = addPair(_GUI.T.YoutubeDashConfigPanel_allowedtypoes_3D(), null, null, new MultiVariantBoxFor3DVideo(this, video3D));
        extraAudio = addPair(_GUI.T.YoutubeDashConfigPanel_allowedtypoes_audio(), null, null, new MultiVariantBoxForAudio(this, audio));
        extraImage = addPair(_GUI.T.YoutubeDashConfigPanel_allowedtypoes_image(), null, null, new MultiVariantBoxForImage(this, image));

        updateBest();
    }

    private void updateBest() {
        bestLimit.getComponent().setEnabled(cf.getGroupLogic() != GroupLogic.NO_GROUP);
        best3D.getComponent().setEnabled(cf.getGroupLogic() != GroupLogic.NO_GROUP);
        bestAudio.getComponent().setEnabled(cf.getGroupLogic() != GroupLogic.NO_GROUP);
        bestImage.getComponent().setEnabled(cf.getGroupLogic() != GroupLogic.NO_GROUP);
        bestSubtitle.getComponent().setEnabled(cf.getGroupLogic() != GroupLogic.NO_GROUP);
        bestVideo.getComponent().setEnabled(cf.getGroupLogic() != GroupLogic.NO_GROUP);
        extraVideoMp4.getComponent().setEnabled(cf.getGroupLogic() != GroupLogic.NO_GROUP);
        extraVideoWebm.getComponent().setEnabled(cf.getGroupLogic() != GroupLogic.NO_GROUP);
        extraVideoFlv.getComponent().setEnabled(cf.getGroupLogic() != GroupLogic.NO_GROUP);
        extraVideoGp3.getComponent().setEnabled(cf.getGroupLogic() != GroupLogic.NO_GROUP);
        extraVideo3D.getComponent().setEnabled(cf.getGroupLogic() != GroupLogic.NO_GROUP);
        extraAudio.getComponent().setEnabled(cf.getGroupLogic() != GroupLogic.NO_GROUP);
        extraImage.getComponent().setEnabled(cf.getGroupLogic() != GroupLogic.NO_GROUP);

    }

    public static void main(String[] args) {

    }

    @Override
    public void reset() {

        for (KeyHandler m : cf._getStorageHandler().getMap().values()) {

            m.setValue(m.getDefaultValue());
        }
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                updateContents();
            }
        };

    }

    @Override
    public void save() {
        if (setting) {
            return;
        }
        List<BlackOrWhitelistEntry> blacklistSet = new ArrayList<BlackOrWhitelistEntry>();
        getBlacklist(blacklistSet, video3D.getComponent());
        getBlacklist(blacklistSet, audioPair.getComponent());
        getBlacklist(blacklistSet, videoFlv.getComponent());
        getBlacklist(blacklistSet, videoGp3.getComponent());
        getBlacklist(blacklistSet, imagePair.getComponent());
        getBlacklist(blacklistSet, videoMp4.getComponent());
        getBlacklist(blacklistSet, videoWebm.getComponent());

        List<BlackOrWhitelistEntry> whitelist = new ArrayList<BlackOrWhitelistEntry>();

        getWhitelist(whitelist, extraVideo3D.getComponent());
        getWhitelist(whitelist, extraAudio.getComponent());
        getWhitelist(whitelist, extraVideoFlv.getComponent());
        getWhitelist(whitelist, extraVideoGp3.getComponent());
        getWhitelist(whitelist, extraImage.getComponent());
        getWhitelist(whitelist, extraVideoMp4.getComponent());
        getWhitelist(whitelist, extraVideoWebm.getComponent());

        cf.setBlacklisted(blacklistSet);
        cf.setExtra(whitelist);

    }

    /**
     * @param list
     * @param blacklistSet
     */
    public void getWhitelist(List<BlackOrWhitelistEntry> whitelist, MultiVariantBox<?> list) {
        for (AbstractVariant v : list.getValues()) {
            if (((MultiVariantBox<AbstractVariant>) list).isItemSelected(v)) {
                whitelist.add(new BlackOrWhitelistEntry(v));
            }
        }
    }

    /**
     * @param list
     * @param blacklistSet
     */
    public void getBlacklist(List<BlackOrWhitelistEntry> blacklistSet, MultiVariantBox<?> list) {

        for (AbstractVariant v : list.getValues()) {
            if (!((MultiVariantBox<AbstractVariant>) list).isItemSelected(v)) {
                blacklistSet.add(new BlackOrWhitelistEntry(v));
            }
        }

    }

    @Override
    public void updateContents() {
        // ifVideoAndPlaylistCombo.getComponent().setSelectedItem(cf.getLinkIsVideoAndPlaylistUrlAction());
        setting = true;
        try {

            List<BlackOrWhitelistEntry> blacklistSet = YoutubeHelper.readBlacklist();

            setBlacklist(blacklistSet, video3D.getComponent());
            setBlacklist(blacklistSet, audioPair.getComponent());
            setBlacklist(blacklistSet, videoFlv.getComponent());
            setBlacklist(blacklistSet, videoGp3.getComponent());
            setBlacklist(blacklistSet, imagePair.getComponent());
            setBlacklist(blacklistSet, videoMp4.getComponent());
            setBlacklist(blacklistSet, videoWebm.getComponent());
            List<BlackOrWhitelistEntry> extraSet = YoutubeHelper.readExtraList();

            setWhitelist(extraSet, extraVideo3D.getComponent());
            setWhitelist(extraSet, extraAudio.getComponent());
            setWhitelist(extraSet, extraVideoFlv.getComponent());
            setWhitelist(extraSet, extraVideoGp3.getComponent());
            setWhitelist(extraSet, extraImage.getComponent());
            setWhitelist(extraSet, extraVideoMp4.getComponent());
            setWhitelist(extraSet, extraVideoWebm.getComponent());
        } finally {
            setting = false;
        }
        //
    }

    public void setWhitelist(List<BlackOrWhitelistEntry> whitelist, MultiVariantBox<?> list) {

        ArrayList<AbstractVariant> vList = new ArrayList<AbstractVariant>();

        for (AbstractVariant v : list.getValues()) {
            if (whitelist.contains(new BlackOrWhitelistEntry(v))) {
                vList.add(v);
            }

        }
        ((MultiVariantBox<AbstractVariant>) list).setSelectedItems(vList);
    }

    /**
     * @param blacklistSet
     * @param list
     */
    public void setBlacklist(List<BlackOrWhitelistEntry> blacklistSet, MultiVariantBox<?> list) {

        ArrayList<AbstractVariant> vList = new ArrayList<AbstractVariant>();

        for (AbstractVariant v : list.getValues()) {
            BlackOrWhitelistEntry be = new BlackOrWhitelistEntry(v);
            if (!blacklistSet.contains(be)) {
                vList.add(v);

            } else {

            }

        }
        ((MultiVariantBox<AbstractVariant>) list).setSelectedItems(vList);
    }

}
