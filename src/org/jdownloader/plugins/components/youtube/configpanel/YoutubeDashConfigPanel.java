package org.jdownloader.plugins.components.youtube.configpanel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.ComboBox;
import jd.gui.swing.jdgui.views.settings.components.MultiComboBox;
import jd.gui.swing.jdgui.views.settings.components.TextInput;
import jd.gui.swing.jdgui.views.settings.panels.advanced.AdvancedConfigTableModel;
import jd.plugins.Plugin;
import jd.plugins.PluginConfigPanelNG;

import org.appwork.storage.config.annotations.IntegerInterface;
import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.storage.config.handler.StringKeyHandler;
import org.appwork.utils.CounterMap;
import org.appwork.utils.logging2.extmanager.Log;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.PerfectHeightScrollPane;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.plugins.components.youtube.Projection;
import org.jdownloader.plugins.components.youtube.VariantIDStorable;
import org.jdownloader.plugins.components.youtube.YoutubeConfig;
import org.jdownloader.plugins.components.youtube.YoutubeConfig.IfUrlisAPlaylistAction;
import org.jdownloader.plugins.components.youtube.YoutubeConfig.IfUrlisAVideoAndPlaylistAction;
import org.jdownloader.plugins.components.youtube.YoutubeHelper;
import org.jdownloader.plugins.components.youtube.itag.AudioBitrate;
import org.jdownloader.plugins.components.youtube.itag.AudioCodec;
import org.jdownloader.plugins.components.youtube.itag.VideoCodec;
import org.jdownloader.plugins.components.youtube.itag.VideoFrameRate;
import org.jdownloader.plugins.components.youtube.itag.VideoResolution;
import org.jdownloader.plugins.components.youtube.variants.AbstractVariant;
import org.jdownloader.plugins.components.youtube.variants.FileContainer;
import org.jdownloader.plugins.components.youtube.variants.VariantGroup;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.settings.staticreferences.CFG_YOUTUBE;

public class YoutubeDashConfigPanel extends PluginConfigPanelNG {
    private AdvancedConfigTableModel model;
    private YoutubeConfig            cf;
    private boolean                  setting = false;
    private VariantsMapTable         allowed;
    private CollectionsTable         collections;

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
    }

    @Override
    protected void initPluginSettings(Plugin protoType) {
        addPair(_GUI.T.YoutubeDashConfigPanel_YoutubeDashConfigPanel_if_link_contains_video_and_playlist(), null, null, new ComboBox<IfUrlisAVideoAndPlaylistAction>(cf._getStorageHandler().getKeyHandler("LinkIsVideoAndPlaylistUrlAction", KeyHandler.class), IfUrlisAVideoAndPlaylistAction.values(), null));
        addPair(_GUI.T.YoutubeDashConfigPanel_YoutubeDashConfigPanel_if_link_equals_playlist(), null, null, new ComboBox<IfUrlisAPlaylistAction>(cf._getStorageHandler().getKeyHandler("LinkIsPlaylistUrlAction", KeyHandler.class), IfUrlisAPlaylistAction.values(), null));
        addDescriptionPlain(_GUI.T.YoutubeDashConfigPanel_YoutubeDashConfigPanel_fastcrawling_desc());
        addPair(_GUI.T.YoutubeDashConfigPanel_YoutubeDashConfigPanel_fastcrawling(), null, null, new Checkbox(cf._getStorageHandler().getKeyHandler("FastLinkCheckEnabled", BooleanKeyHandler.class), null));
        // VariantBase[] variants = VariantBase.values();
        // Comparator<VariantBase> comp = new Comparator<VariantBase>() {
        //
        // @Override
        // public int compare(VariantBase o1, VariantBase o2) {
        // return new Double(o2.getQualityRating()).compareTo(new Double(o1.getQualityRating()));
        // }
        // };
        ArrayList<AbstractVariantWrapper> sorted = new ArrayList<AbstractVariantWrapper>();
        for (AbstractVariant v : AbstractVariant.listVariants()) {
            sorted.add(new AbstractVariantWrapper(v));
        }
        addHeader(_GUI.T.YoutubeDashConfigPanel_allowedtypoes(), NewTheme.I().getIcon(IconKey.ICON_MEDIAPLAYER, 18));
        addDescriptionPlain(_GUI.T.YoutubeDashConfigPanel_allowedtypoes_quick());
        collections = new CollectionsTable(new CollectionsTableModel() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            public void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {
                super.onConfigValueModified(keyHandler, newValue);
                refreshLayout();
            }
        });
        allowed = new VariantsMapTable(new VariantsMapTableModel(sorted) {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            public void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {
                super.onConfigValueModified(keyHandler, newValue);
                refreshLayout();
            }
        }) {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            @Override
            public void onEnabledMapUpdate(CounterMap<String> enabledMap) {
                collections.onEnabledMapUpdate(enabledMap);
            }
        };
        collections.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                try {
                    YoutubeVariantCollection link = collections.getModel().getObjectbyRow(collections.getSelectedRow());
                    allowed.setSelectionByLink(link);
                } catch (Throwable e1) {
                    Log.log(e1);
                }
            }
        });
        List<VariantGroup> groups = Arrays.asList(VariantGroup.values());
        Collections.sort(groups, LabelInterface.COMPARATOR_ASC);
        EnumMultiComboBox<VariantGroup> typeSel = new EnumMultiComboBox<VariantGroup>(groups, CFG_YOUTUBE.BLACKLISTED_GROUPS, true);
        List<FileContainer> container = Arrays.asList(FileContainer.values());
        Collections.sort(container, LabelInterface.COMPARATOR_ASC);
        EnumMultiComboBox<FileContainer> containerSel = new EnumMultiComboBox<FileContainer>(container, CFG_YOUTUBE.BLACKLISTED_FILE_CONTAINERS, true) {
            @Override
            protected String getLabel(int i, FileContainer sc) {
                return sc.getTooltip();
            }
        };
        ;
        List<Projection> projections = Arrays.asList(Projection.values());
        Collections.sort(projections, LabelInterface.COMPARATOR_ASC);
        EnumMultiComboBox<Projection> projectionSelect = new EnumMultiComboBox<Projection>(projections, CFG_YOUTUBE.BLACKLISTED_PROJECTIONS, true) {
            @Override
            protected String getLabel(int i, Projection sc) {
                return sc.getTooltip();
            }
        };
        List<VideoResolution> heights = Arrays.asList(VideoResolution.values());
        Collections.sort(heights, IntegerInterface.COMPARATOR_DESC);
        EnumMultiComboBox<VideoResolution> resolutionSelect = new EnumMultiComboBox<VideoResolution>(heights, CFG_YOUTUBE.BLACKLISTED_RESOLUTIONS, true);
        List<VideoFrameRate> fpss = Arrays.asList(VideoFrameRate.values());
        Collections.sort(fpss, IntegerInterface.COMPARATOR_DESC);
        EnumMultiComboBox<VideoFrameRate> fpsSelect = new EnumMultiComboBox<VideoFrameRate>(fpss, CFG_YOUTUBE.BLACKLISTED_VIDEO_FRAMERATES, true);
        List<VideoCodec> videoCodecs = Arrays.asList(VideoCodec.values());
        Collections.sort(videoCodecs, LabelInterface.COMPARATOR_ASC);
        EnumMultiComboBox<VideoCodec> vcodec = new EnumMultiComboBox<VideoCodec>(videoCodecs, CFG_YOUTUBE.BLACKLISTED_VIDEO_CODECS, true) {
            @Override
            protected String getLabel(int i, VideoCodec sc) {
                return sc.getTooltip();
            }
        };
        List<AudioCodec> audioCodecs = Arrays.asList(AudioCodec.values());
        Collections.sort(audioCodecs, LabelInterface.COMPARATOR_ASC);
        EnumMultiComboBox<AudioCodec> acodec = new EnumMultiComboBox<AudioCodec>(audioCodecs, CFG_YOUTUBE.BLACKLISTED_AUDIO_CODECS, true) {
            @Override
            protected String getLabel(int i, AudioCodec sc) {
                return sc.getTooltip();
            }
        };
        List<AudioBitrate> bitrates = Arrays.asList(AudioBitrate.values());
        Collections.sort(bitrates, IntegerInterface.COMPARATOR_DESC);
        EnumMultiComboBox<AudioBitrate> aBitrate = new EnumMultiComboBox<AudioBitrate>(bitrates, CFG_YOUTUBE.BLACKLISTED_AUDIO_BITRATES, true);
        addPair(_GUI.T.YOUTUBE_CONFIG_PANEL_TABLE_TYPE(), null, typeSel);
        addPair(_GUI.T.YOUTUBE_CONFIG_PANEL_TABLE_FILETYPE(), null, containerSel);
        addPair(_GUI.T.YOUTUBE_CONFIG_PANEL_TABLE_PROJECTION(), null, projectionSelect);
        addPair(_GUI.T.YOUTUBE_CONFIG_PANEL_TABLE_RESOLUTION(), null, resolutionSelect);
        addPair(_GUI.T.YOUTUBE_CONFIG_PANEL_TABLE_FPS(), null, fpsSelect);
        addPair(_GUI.T.YOUTUBE_CONFIG_PANEL_TABLE_VIDEO_CODEC(), null, vcodec);
        addPair(_GUI.T.YOUTUBE_CONFIG_PANEL_TABLE_AUDIO_CODEC(), null, acodec);
        addPair(_GUI.T.YOUTUBE_CONFIG_PANEL_TABLE_AUDIO_BITRATE(), null, aBitrate);
        addDescriptionPlain(_GUI.T.YoutubeDashConfigPanel_allowedtypoes_table());
        JScrollPane sp;
        add(new PerfectHeightScrollPane(allowed), "pushx,growx,spanx");
        // sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        addHeader(_GUI.T.YoutubeDashConfigPanel_collections_header(), NewTheme.I().getIcon(IconKey.ICON_LIST, 18));
        addDescriptionPlain(_GUI.T.YoutubeDashConfigPanel_links_description());
        add(new PerfectHeightScrollPane(collections), "pushx,growx,spanx");
        addHeader(_GUI.T.YoutubeDashConfigPanel_YoutubeDashConfigPanel_filename_or_package_pattern_header(), NewTheme.I().getIcon(IconKey.ICON_FILE, 18));
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
        // addPair(_GUI.T.YoutubeDashConfigPanel_YoutubeDashConfigPanel_filename_pattern_video3D(), null, null, new
        // TextInput(CFG_YOUTUBE.VIDEO3DFILENAME_PATTERN));
        addPair(_GUI.T.YoutubeDashConfigPanel_YoutubeDashConfigPanel_filename_pattern_audio(), null, null, new TextInput(CFG_YOUTUBE.AUDIO_FILENAME_PATTERN));
        addPair(_GUI.T.YoutubeDashConfigPanel_YoutubeDashConfigPanel_filename_pattern_image(), null, null, new TextInput(CFG_YOUTUBE.IMAGE_FILENAME_PATTERN));
        addPair(_GUI.T.YoutubeDashConfigPanel_YoutubeDashConfigPanel_filename_pattern_subtitle(), null, null, new TextInput(CFG_YOUTUBE.SUBTITLE_FILENAME_PATTERN));
        addPair(_GUI.T.YoutubeDashConfigPanel_YoutubeDashConfigPanel_filename_pattern_description(), null, null, new TextInput(CFG_YOUTUBE.DESCRIPTION_FILENAME_PATTERN));
        updateBest();
    }

    protected void refreshLayout() {
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                revalidate();
            }
        };
    }

    private void updateBest() {
    }

    @Override
    public void reset() {
        for (KeyHandler m : cf._getStorageHandler().getKeyHandler()) {
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
        // List<VariantIDStorable> blacklistSet = new ArrayList<VariantIDStorable>();
        // getBlacklist(blacklistSet, video3D.getComponent());
        // getBlacklist(blacklistSet, audioPair.getComponent());
        // getBlacklist(blacklistSet, videoFlv.getComponent());
        // getBlacklist(blacklistSet, videoGp3.getComponent());
        // getBlacklist(blacklistSet, imagePair.getComponent());
        // getBlacklist(blacklistSet, videoMp4.getComponent());
        // getBlacklist(blacklistSet, videoWebm.getComponent());
        // List<BlackListEntry> whitelist = new ArrayList<BlackListEntry>();
        //
        // getWhitelist(whitelist, extraVideo3D.getComponent());
        // getWhitelist(whitelist, extraAudio.getComponent());
        // getWhitelist(whitelist, extraVideoFlv.getComponent());
        // getWhitelist(whitelist, extraVideoGp3.getComponent());
        // getWhitelist(whitelist, extraImage.getComponent());
        // getWhitelist(whitelist, extraVideoMp4.getComponent());
        // getWhitelist(whitelist, extraVideoWebm.getComponent());
        allowed.save();
        // cf.setExtra(whitelist);
    }

    /**
     * @param list
     * @param blacklistSet
     */
    public void getWhitelist(List<VariantIDStorable> whitelist, MultiVariantBox<?> list) {
        for (AbstractVariant v : list.getValues()) {
            if (((MultiVariantBox<AbstractVariant>) list).isItemSelected(v)) {
                whitelist.add(new VariantIDStorable(v));
            }
        }
    }

    /**
     * @param list
     * @param blacklistSet
     */
    public void getBlacklist(List<VariantIDStorable> blacklistSet, MultiVariantBox<?> list) {
        for (AbstractVariant v : list.getValues()) {
            if (!((MultiVariantBox<AbstractVariant>) list).isItemSelected(v)) {
                blacklistSet.add(new VariantIDStorable(v));
            }
        }
    }

    @Override
    public void updateContents() {
        // ifVideoAndPlaylistCombo.getComponent().setSelectedItem(cf.getLinkIsVideoAndPlaylistUrlAction());
        setting = true;
        try {
            allowed.load();
            collections.load();
            // List<VariantIDStorable> blacklistSet = YoutubeHelper.readBlacklist();
            // setBlacklist(blacklistSet, video3D.getComponent());
            // setBlacklist(blacklistSet, audioPair.getComponent());
            // setBlacklist(blacklistSet, videoFlv.getComponent());
            // setBlacklist(blacklistSet, videoGp3.getComponent());
            // setBlacklist(blacklistSet, imagePair.getComponent());
            // setBlacklist(blacklistSet, videoMp4.getComponent());
            // setBlacklist(blacklistSet, videoWebm.getComponent());
            // List<VariantIDStorable> extraSet = YoutubeHelper.readExtraList();
            // setWhitelist(extraSet, extraVideo3D.getComponent());
            // setWhitelist(extraSet, extraAudio.getComponent());
            // setWhitelist(extraSet, extraVideoFlv.getComponent());
            // setWhitelist(extraSet, extraVideoGp3.getComponent());
            // setWhitelist(extraSet, extraImage.getComponent());
            // setWhitelist(extraSet, extraVideoMp4.getComponent());
            // setWhitelist(extraSet, extraVideoWebm.getComponent());
        } finally {
            setting = false;
        }
        //
    }

    public void setWhitelist(List<VariantIDStorable> whitelist, MultiVariantBox<?> list) {
        ArrayList<AbstractVariant> vList = new ArrayList<AbstractVariant>();
        for (AbstractVariant v : list.getValues()) {
            if (whitelist.contains(new VariantIDStorable(v))) {
                vList.add(v);
            }
        }
        ((MultiVariantBox<AbstractVariant>) list).setSelectedItems(vList);
    }

    /**
     * @param blacklistSet
     * @param list
     */
    public void setBlacklist(List<VariantIDStorable> blacklistSet, MultiVariantBox<?> list) {
        ArrayList<AbstractVariant> vList = new ArrayList<AbstractVariant>();
        for (AbstractVariant v : list.getValues()) {
            VariantIDStorable be = new VariantIDStorable(v);
            if (!blacklistSet.contains(be)) {
                vList.add(v);
            } else {
            }
        }
        ((MultiVariantBox<AbstractVariant>) list).setSelectedItems(vList);
    }
}
