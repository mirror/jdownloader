package org.jdownloader.plugins.components.youtube.configpanel;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.JTableHeader;

import jd.gui.swing.jdgui.AlternateHighlighter;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtComponentRowHighlighter;
import org.appwork.swing.exttable.ExtDefaultRowSorter;
import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtCheckColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.CompareUtils;
import org.appwork.utils.CounterMap;
import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.ffmpeg.AbstractFFmpegBinary;
import org.jdownloader.controlling.ffmpeg.AbstractFFmpegBinary.FLAG;
import org.jdownloader.controlling.ffmpeg.FFmpeg;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.plugins.components.youtube.VariantIDStorable;
import org.jdownloader.plugins.components.youtube.itag.VideoResolution;
import org.jdownloader.plugins.components.youtube.variants.AudioInterface;
import org.jdownloader.plugins.components.youtube.variants.ImageVariant;
import org.jdownloader.plugins.components.youtube.variants.SubtitleVariant;
import org.jdownloader.plugins.components.youtube.variants.VariantBase;
import org.jdownloader.plugins.components.youtube.variants.VideoVariant;
import org.jdownloader.settings.staticreferences.CFG_YOUTUBE;
import org.jdownloader.translate._JDT;
import org.jdownloader.updatev2.gui.LAFOptions;

public class VariantsMapTableModel extends ExtTableModel<AbstractVariantWrapper> implements GenericConfigEventListener<Object> {
    protected static int globalCompare(int ret, AbstractVariantWrapper o1, AbstractVariantWrapper o2, boolean b) {
        if (ret != 0) {
            return ret;
        }

        if (!b) {
            AbstractVariantWrapper tmp = o1;
            o1 = o2;
            o2 = tmp;
        }

        ret = o1.variant.getGroup().compareTo(o2.variant.getGroup());
        if (ret != 0) {
            return ret;
        }

        ret = CompareUtils.compare(o1.getProjection(), o2.getProjection());
        if (ret != 0) {
            return ret;
        }
        ret = o1.variant.getContainer().compareTo(o2.variant.getContainer());
        if (ret != 0) {
            return ret;
        }

        ret = CompareUtils.compare(o1.getHeight(), o2.getHeight());
        if (ret != 0) {
            return ret;
        }

        ret = CompareUtils.compare(o1.getWidth(), o2.getWidth());
        if (ret != 0) {
            return ret;
        }
        ret = CompareUtils.compare(o1.getFramerate(), o2.getFramerate());
        if (ret != 0) {
            return ret;
        }

        ret = o1.getVideoCodec().compareToIgnoreCase(o2.getVideoCodec());
        if (ret != 0) {
            return ret;
        }
        ret = o1.getAudioCodec().compareToIgnoreCase(o2.getAudioCodec());
        if (ret != 0) {
            return ret;
        }
        ret = CompareUtils.compare(o1.getAudioBitrate(), o2.getAudioBitrate());
        if (ret != 0) {
            return ret;
        }
        return ret;

    }

    private class EnabledColumn extends ExtCheckColumn<AbstractVariantWrapper> {
        private EnabledColumn(String string) {
            super(string);

            this.setRowSorter(new ExtDefaultRowSorter<AbstractVariantWrapper>() {
                @Override
                public int compare(final AbstractVariantWrapper o1, final AbstractVariantWrapper o2) {
                    final boolean b1 = getBooleanValue(o1);
                    final boolean b2 = getBooleanValue(o2);

                    int ret;
                    if (b1 == b2) {
                        ret = 0;
                    } else {
                        if (this.getSortOrderIdentifier() == ExtColumn.SORT_ASC) {
                            ret = b1 && !b2 ? -1 : 1;
                        } else {
                            ret = !b1 && b2 ? -1 : 1;
                        }
                    }

                    return globalCompare(ret, o1, o2, this.getSortOrderIdentifier() == ExtColumn.SORT_ASC);
                }

            });

        }

        public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

            final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {
                private final Icon        ok               = NewTheme.I().getIcon(IconKey.ICON_OK, 14);
                private static final long serialVersionUID = 3224931991570756349L;

                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    setIcon(ok);
                    setHorizontalAlignment(CENTER);
                    setText(null);
                    return this;
                }

            };

            return ret;
        }

        @Override
        public int getMaxWidth() {
            return 30;
        }

        @Override
        public boolean isEditable(AbstractVariantWrapper obj) {
            return true;
        }

        @Override
        protected boolean getBooleanValue(AbstractVariantWrapper value) {
            return value.isEnabled();
        }

        @Override
        protected void setBooleanValue(boolean value, AbstractVariantWrapper object) {
            object.setEnabled(value);
        }
    }

    public abstract class AutoResizingTextColumn extends ExtTextColumn<AbstractVariantWrapper> {
        public AutoResizingTextColumn(String name) {
            super(name);
            rendererField.setHorizontalAlignment(SwingConstants.RIGHT);
            this.setRowSorter(new ExtDefaultRowSorter<AbstractVariantWrapper>() {

                @Override
                public int compare(final AbstractVariantWrapper o1, final AbstractVariantWrapper o2) {
                    String o1s = getStringValue(o1);
                    String o2s = getStringValue(o2);
                    if (o1s == null) {
                        o1s = "";
                    }
                    if (o2s == null) {
                        o2s = "";
                    }
                    if (this.getSortOrderIdentifier() == ExtColumn.SORT_ASC) {
                        int ret = o1s.compareToIgnoreCase(o2s);

                        return globalCompare(ret, o1, o2, this.getSortOrderIdentifier() == ExtColumn.SORT_ASC);

                    } else {
                        int ret = o2s.compareToIgnoreCase(o1s);

                        return globalCompare(ret, o1, o2, this.getSortOrderIdentifier() == ExtColumn.SORT_ASC);

                    }

                }

            });
        }

        @Override
        public boolean isEnabled(AbstractVariantWrapper obj) {
            return obj.isEnabled();
        }

        @Override
        protected boolean isDefaultResizable() {
            return true;
        }

        @Override
        protected String getTooltipText(AbstractVariantWrapper obj) {
            return obj.variant.createAdvancedName();
        }

        @Override
        public boolean isResizable() {
            return true;
        }

        @Override
        public boolean isAutoWidthEnabled() {
            return false;
        }

        @Override
        public int getDefaultWidth() {
            return 0;
        }

        @Override
        protected int adjustWidth(int w) {

            return Math.max(w, this.calculateMinimumHeaderWidth());
        }

    }

    public abstract class AutoResizingIntColumn extends ExtTextColumn<AbstractVariantWrapper> {
        public AutoResizingIntColumn(String name) {
            super(name);
            rendererField.setHorizontalAlignment(SwingConstants.RIGHT);
            this.setRowSorter(new ExtDefaultRowSorter<AbstractVariantWrapper>() {

                @Override
                public int compare(final AbstractVariantWrapper o1, final AbstractVariantWrapper o2) {

                    final int _1 = AutoResizingIntColumn.this.getInt(o1);
                    final int _2 = AutoResizingIntColumn.this.getInt(o2);

                    int ret;
                    if (this.getSortOrderIdentifier() == ExtColumn.SORT_ASC) {
                        ret = _1 == _2 ? 0 : _1 < _2 ? -1 : 1;
                    } else {
                        ret = _1 == _2 ? 0 : _1 > _2 ? -1 : 1;
                    }

                    return globalCompare(ret, o1, o2, this.getSortOrderIdentifier() == ExtColumn.SORT_ASC);

                }

            });
        }

        @Override
        public boolean isEnabled(AbstractVariantWrapper obj) {
            return obj.isEnabled();
        }

        public abstract int getInt(AbstractVariantWrapper value);

        @Override
        public String getStringValue(AbstractVariantWrapper value) {
            int i = getInt(value);
            if (i <= 0) {
                return "";
            }
            return i + "";
        }

        @Override
        protected String getTooltipText(AbstractVariantWrapper obj) {
            return obj.variant.createAdvancedName();
        }

        @Override
        public boolean isAutoWidthEnabled() {
            return false;
        }

        @Override
        protected boolean isDefaultResizable() {
            return true;
        }

        @Override
        public boolean isResizable() {
            return true;
        }

        @Override
        public int getDefaultWidth() {
            return 0;
        }

        @Override
        protected int adjustWidth(int w) {

            return Math.max(w, this.calculateMinimumHeaderWidth());
        }

    }

    protected ArrayList<AbstractVariantWrapper>      all;
    private ExtTextColumn<AbstractVariantWrapper>    mergeIDColumn;
    private HashMap<AbstractVariantWrapper, Integer> alternateMergeIDMap;
    private AutoResizingTextColumn                   videoCodecColumn;
    private AutoResizingIntColumn                    fpsColumn;
    private AutoResizingIntColumn                    heightColumn;
    private AutoResizingIntColumn                    widthColumn;
    protected boolean                                hasImage;
    protected boolean                                hasVideo;
    protected boolean                                hasSubtitle;
    protected AutoResizingTextColumn                 typeColumn;
    protected boolean                                hasAudio;
    protected AutoResizingTextColumn                 projectionColumn;
    protected AutoResizingTextColumn                 audioCodecColumn;
    protected AutoResizingIntColumn                  audioBitrateColumn;
    protected boolean                                hasAudioSpatial;
    protected boolean                                hasDescription;
    private final Set<FLAG>                          supportedFlags;

    public VariantsMapTableModel(ArrayList<AbstractVariantWrapper> sorted) {
        this("VariantsMapTableModel", sorted);
    }

    public VariantsMapTableModel(String id, ArrayList<AbstractVariantWrapper> sorted) {
        super(id);
        this.all = sorted;
        initListeners();
        final FFmpeg ffmpeg = new FFmpeg();
        if (ffmpeg.isAvailable()) {
            this.supportedFlags = ffmpeg.getSupportedFlags();
        } else {
            this.supportedFlags = null;
        }
        initHighLighter();
    }

    protected void initHighLighter() {
        final AlternateHighlighter<AbstractVariantWrapper> alternate = new AlternateHighlighter<AbstractVariantWrapper>((LAFOptions.getInstance().getColorForTableAlternateRowForeground()), (LAFOptions.getInstance().getColorForTableAlternateRowBackground()), null);
        addExtComponentRowHighlighter(new ExtComponentRowHighlighter<AbstractVariantWrapper>(null, Color.BLACK, null) {

            @Override
            protected Color getBackground(Color current) {
                return LAFOptions.getInstance().getColorForTablePackageRowBackground();
            }

            @Override
            public boolean accept(ExtColumn<AbstractVariantWrapper> column, AbstractVariantWrapper value, boolean selected, boolean focus, int row) {
                if (alternateMergeIDMap == null || alternateMergeIDMap.get(value) == null) {
                    return alternate.accept(column, value, selected, focus, row);
                }
                return alternateMergeIDMap.get(value) % 2 == 0;
            }
        });
        if (supportedFlags != null) {
            addExtComponentRowHighlighter(new ExtComponentRowHighlighter<AbstractVariantWrapper>(Color.RED, null, null) {

                final boolean isOpusSupported   = supportedFlags.contains(AbstractFFmpegBinary.FLAG.OPUS);
                final boolean isVorbisSupported = supportedFlags.contains(AbstractFFmpegBinary.FLAG.VORBIS);

                @Override
                protected Color getBackground(Color current) {
                    return LAFOptions.getInstance().getColorForTablePackageRowBackground();
                }

                @Override
                public boolean accept(ExtColumn<AbstractVariantWrapper> column, AbstractVariantWrapper value, boolean selected, boolean focus, int row) {
                    if (StringUtils.equalsIgnoreCase("Opus", value.getAudioCodec())) {
                        return !isOpusSupported;
                    } else if (StringUtils.equalsIgnoreCase("Vorbis", value.getAudioCodec())) {
                        return !isVorbisSupported;
                    } else {
                        return false;
                    }
                }
            });
        }
    }

    protected void initListeners() {
        CFG_YOUTUBE.BLACKLISTED_AUDIO_BITRATES.getEventSender().addListener(this, true);
        CFG_YOUTUBE.BLACKLISTED_AUDIO_CODECS.getEventSender().addListener(this, true);
        CFG_YOUTUBE.BLACKLISTED_FILE_CONTAINERS.getEventSender().addListener(this, true);
        CFG_YOUTUBE.BLACKLISTED_GROUPS.getEventSender().addListener(this, true);
        CFG_YOUTUBE.BLACKLISTED_PROJECTIONS.getEventSender().addListener(this, true);
        CFG_YOUTUBE.BLACKLISTED_RESOLUTIONS.getEventSender().addListener(this, true);
        CFG_YOUTUBE.BLACKLISTED_VIDEO_CODECS.getEventSender().addListener(this, true);
        CFG_YOUTUBE.BLACKLISTED_VIDEO_FRAMERATES.getEventSender().addListener(this, true);
        CFG_YOUTUBE.DISABLED_VARIANTS.getEventSender().addListener(this, true);
        onConfigValueModified(null, null);
    }

    @Override
    protected void initColumns() {
        addCheckBoxColumn();

        addTypeColumn();

        addContainerColumn();
        addColumn(projectionColumn = new AutoResizingTextColumn(_GUI.T.YOUTUBE_CONFIG_PANEL_TABLE_PROJECTION()) {

            @Override
            public String getStringValue(AbstractVariantWrapper value) {
                String audioProjection = "";
                if (value.variant instanceof AudioInterface) {
                    switch (((AudioInterface) value.variant).getAudioCodec()) {
                    case AAC_SPATIAL:
                    case VORBIS_SPATIAL:
                        audioProjection = _JDT.T.youtube_spatial();
                    }
                }
                if (value.variant instanceof VideoVariant) {
                    switch (((VideoVariant) value.variant).getGenericInfo().getProjection()) {
                    case NORMAL:
                        return audioProjection;

                    default:
                        if (StringUtils.isNotEmpty(audioProjection)) {
                            return ((VideoVariant) value.variant).getGenericInfo().getProjection().getLabel() + " " + audioProjection;
                        }
                        return ((VideoVariant) value.variant).getGenericInfo().getProjection().getLabel();
                    }

                }
                return audioProjection;

            }
        });

        addWidthColumn();
        addHeightColumn();

        addFPSColumn();

        addVideoCodecColumn();

        addColumn(audioCodecColumn = new AutoResizingTextColumn(_GUI.T.YOUTUBE_CONFIG_PANEL_TABLE_AUDIO_CODEC()) {
            @Override
            public String getStringValue(AbstractVariantWrapper value) {
                if (value.variant instanceof AudioInterface) {
                    return ((AudioInterface) value.variant).getAudioCodec().getLabel();
                }
                return "";

            }

            @Override
            protected String getTooltipText(AbstractVariantWrapper value) {
                if (value.variant instanceof AudioInterface) {
                    return ((AudioInterface) value.variant).getAudioCodec().getLabelLong();
                }
                return "";
            }

        });

        addColumn(audioBitrateColumn = new AutoResizingIntColumn(_GUI.T.YOUTUBE_CONFIG_PANEL_TABLE_AUDIO_BITRATE()) {
            @Override
            public String getStringValue(AbstractVariantWrapper value) {
                String v = super.getStringValue(value);
                if (StringUtils.isEmpty(v)) {
                    return "";
                }
                return v + " kbit/s";
            }

            @Override
            public int getInt(AbstractVariantWrapper value) {
                if (value.variant instanceof AudioInterface) {
                    return ((AudioInterface) value.variant).getAudioBitrate().getKbit();
                }
                return -1;
            }

        });
        addGroupingColumn();

    }

    protected void addGroupingColumn() {
        addColumn(mergeIDColumn = new AutoResizingTextColumn(_GUI.T.YOUTUBE_CONFIG_PANEL_TABLE_GROUPING()) {
            {
                rendererField.setHorizontalAlignment(SwingConstants.LEFT);
            }

            @Override
            protected String getTooltipText(AbstractVariantWrapper obj) {
                return obj.variant.createAdvancedName();
            }

            @Override
            public boolean isDefaultVisible() {
                return false;
            }

            @Override
            public String getStringValue(AbstractVariantWrapper value) {
                return value.variant._getName(null);
            }

        });
    }

    protected void addVideoCodecColumn() {
        addColumn(videoCodecColumn = new AutoResizingTextColumn(_GUI.T.YOUTUBE_CONFIG_PANEL_TABLE_VIDEO_CODEC()) {

            @Override
            public String getStringValue(AbstractVariantWrapper value) {
                if (value.variant instanceof VideoVariant) {
                    return ((VideoVariant) value.variant).getVideoCodec().getLabel();
                }
                return "";

            }

            @Override
            protected String getTooltipText(AbstractVariantWrapper value) {
                if (value.variant instanceof VideoVariant) {
                    return ((VideoVariant) value.variant).getVideoCodec().getLabelLong();
                }
                return "";
            }
        });
    }

    protected void addFPSColumn() {
        addColumn(fpsColumn = new AutoResizingIntColumn(_GUI.T.YOUTUBE_CONFIG_PANEL_TABLE_FPS()) {

            @Override
            public int getInt(AbstractVariantWrapper value) {
                if (value.variant instanceof VideoVariant) {
                    return ((VideoVariant) value.variant).getVideoFrameRate();
                }
                ;
                return -1;
            }

        });
    }

    protected void addHeightColumn() {
        addColumn(heightColumn = new AutoResizingIntColumn(_GUI.T.YOUTUBE_CONFIG_PANEL_TABLE_HEIGHT()) {
            @Override
            public String getStringValue(AbstractVariantWrapper value) {
                if (value.variant instanceof ImageVariant) {
                    if (value.variant.getBaseVariant() == VariantBase.IMAGE_MAX) {
                        return "~" + ((ImageVariant) value.variant).getHeight();
                    }
                }

                return super.getStringValue(value);
            }

            @Override
            public int getInt(AbstractVariantWrapper value) {

                return value.getHeight();
            }

        });
    }

    protected void addWidthColumn() {
        addColumn(widthColumn = new AutoResizingIntColumn(_GUI.T.YOUTUBE_CONFIG_PANEL_TABLE_WIDTH()) {
            @Override
            public boolean isDefaultVisible() {
                return false;
            }

            @Override
            public String getStringValue(AbstractVariantWrapper value) {
                if (value.variant instanceof ImageVariant) {
                    if (value.variant.getBaseVariant() == VariantBase.IMAGE_MAX) {
                        return ">480";
                    }
                }
                return super.getStringValue(value);
            }

            @Override
            public int getInt(AbstractVariantWrapper value) {
                return value.getWidth();

            }

        });
    }

    protected void addContainerColumn() {
        addColumn(new AutoResizingTextColumn(_GUI.T.YOUTUBE_CONFIG_PANEL_TABLE_FILETYPE()) {

            @Override
            public String getStringValue(AbstractVariantWrapper value) {

                return value.variant.getContainer().getLabel();
            }
        });
    }

    protected void addTypeColumn() {
        addColumn(typeColumn = new AutoResizingTextColumn(_GUI.T.YOUTUBE_CONFIG_PANEL_TABLE_TYPE()) {

            @Override
            public boolean isAutoWidthEnabled() {
                return true;
            }

            @Override
            protected String getTooltipText(AbstractVariantWrapper obj) {
                return obj.variant.createAdvancedName();
            }

            @Override
            public String getStringValue(AbstractVariantWrapper value) {
                switch (value.variant.getGroup()) {
                case DESCRIPTION:
                    return _GUI.T.lit_desciption();
                case IMAGE:
                    return _GUI.T.YOUTUBE_CONFIG_PANEL_TABLE_IMAGES(value.variant.getFileNameQualityTag());
                case SUBTITLES:
                    if (((SubtitleVariant) value.variant).getGenericInfo()._getLocale() != null) {
                        return value.variant._getName(null);
                    }
                default:
                    return value.variant.getGroup().getLabel();
                }

            }

        });
    }

    protected void addCheckBoxColumn() {
        addColumn(new EnabledColumn(_GUI.T.lit_enabled()) {
            @Override
            protected void setBooleanValue(boolean value, AbstractVariantWrapper object) {
                super.setBooleanValue(value, object);
                updateEnabledMap();
            }
        });
    }

    public ExtTextColumn<AbstractVariantWrapper> getMergeIDColumn() {
        return mergeIDColumn;
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Object> keyHandler, Object invalidValue, ValidationException validateException) {
    }

    @Override
    public void _fireTableStructureChanged(java.util.List<AbstractVariantWrapper> newtableData, boolean refreshSort) {
        if (getTable() == null) {
            super._fireTableStructureChanged(newtableData, refreshSort);
            return;
        }
        filter(newtableData);

        HashMap<AbstractVariantWrapper, Integer> map = new HashMap<AbstractVariantWrapper, Integer>();
        int i = 0;
        AbstractVariantWrapper last = null;
        ExtColumn<AbstractVariantWrapper> sc = getSortColumn();
        if (sc != null && sc instanceof ExtTextColumn) {
            final ExtTextColumn txtCol = (ExtTextColumn) sc;
            Collections.sort(newtableData, txtCol.getRowSorter());

            for (AbstractVariantWrapper e : newtableData) {

                if (last == null || !txtCol.getStringValue(last).equals(txtCol.getStringValue(e))) {
                    i++;
                    last = e;
                }
                map.put(e, i);

            }
        }
        onStructureChanged(newtableData);
        this.alternateMergeIDMap = map;
        super._fireTableStructureChanged(newtableData, refreshSort);
        this.updateEnabledMap();

    }

    protected void onStructureChanged(List<AbstractVariantWrapper> newtableData) {
        hasDescription = false;
        hasImage = false;
        hasVideo = false;
        hasAudio = false;
        hasAudioSpatial = false;
        hasSubtitle = false;
        for (AbstractVariantWrapper e : newtableData) {
            switch (e.variant.getGroup()) {
            case AUDIO:
                hasAudio = true;
                break;
            case DESCRIPTION:
                hasDescription = true;
                break;
            case IMAGE:
                hasImage = true;
                break;
            case SUBTITLES:
                hasSubtitle = true;
                break;
            case VIDEO:
                hasVideo = true;
                break;

            }
            if (e.variant instanceof AudioInterface) {
                hasAudio = true;
                switch (((AudioInterface) e.variant).getAudioCodec()) {
                case AAC_SPATIAL:
                case VORBIS_SPATIAL:
                    hasAudioSpatial = true;
                }
            }
        }

        if (heightColumn != null) {
            setColumnVisible(heightColumn, hasVideo || hasImage);
        }
        if (widthColumn != null) {
            setColumnVisible(widthColumn, hasVideo || hasImage);
        }
        if (fpsColumn != null) {
            setColumnVisible(fpsColumn, hasVideo);
        }
        if (videoCodecColumn != null) {
            setColumnVisible(videoCodecColumn, hasVideo);
        }

        if (projectionColumn != null) {
            setColumnVisible(projectionColumn, hasVideo || hasAudioSpatial);
        }
        if (audioBitrateColumn != null) {
            setColumnVisible(audioBitrateColumn, hasAudio);
        }
        if (audioCodecColumn != null) {
            setColumnVisible(audioCodecColumn, hasAudio);
        }
    }

    protected void filter(java.util.List<AbstractVariantWrapper> newtableData) {
        HashSet<Object> blacklisted = new HashSet<Object>();
        for (Object o : list(CFG_YOUTUBE.CFG.getBlacklistedAudioBitrates())) {
            if (o != null) {
                blacklisted.add(o);
            }
        }

        for (Object o : list(CFG_YOUTUBE.CFG.getBlacklistedAudioCodecs())) {
            if (o != null) {
                blacklisted.add(o);
            }
        }

        for (Object o : list(CFG_YOUTUBE.CFG.getBlacklistedFileContainers())) {
            if (o != null) {
                blacklisted.add(o);
            }
        }

        for (Object o : list(CFG_YOUTUBE.CFG.getBlacklistedGroups())) {
            if (o != null) {
                blacklisted.add(o);
            }
        }

        for (Object o : list(CFG_YOUTUBE.CFG.getBlacklistedProjections())) {
            if (o != null) {
                blacklisted.add(o);
            }
        }

        for (Object o : list(CFG_YOUTUBE.CFG.getBlacklistedResolutions())) {
            if (o != null) {
                blacklisted.add(o);
            }
        }

        for (Object o : list(CFG_YOUTUBE.CFG.getBlacklistedVideoCodecs())) {
            if (o != null) {
                blacklisted.add(o);
            }
        }
        for (Object o : list(CFG_YOUTUBE.CFG.getBlacklistedVideoFramerates())) {
            if (o != null) {
                blacklisted.add(o);
            }
        }

        for (final Iterator<AbstractVariantWrapper> it = newtableData.iterator(); it.hasNext();) {
            final AbstractVariantWrapper next = it.next();
            if (blacklisted.contains(next.variant.getGroup())) {
                it.remove();
                continue;
            }
            if (blacklisted.contains(next.variant.getContainer())) {
                it.remove();
                continue;
            }
            if (next.variant instanceof AudioInterface) {
                if (blacklisted.contains(((AudioInterface) next.variant).getAudioBitrate())) {
                    it.remove();
                    continue;
                }
                if (blacklisted.contains(((AudioInterface) next.variant).getAudioCodec())) {
                    it.remove();
                    continue;
                }
            }
            if (next.variant instanceof VideoVariant) {
                if (blacklisted.contains(((VideoVariant) next.variant).getVideoCodec())) {
                    it.remove();
                    continue;
                }
                if (blacklisted.contains(((VideoVariant) next.variant).getVideoResolution())) {
                    it.remove();
                    continue;
                }
                if (blacklisted.contains(((VideoVariant) next.variant).getiTagVideo().getVideoFrameRate())) {
                    it.remove();
                    continue;
                }
                if (blacklisted.contains(((VideoVariant) next.variant).getProjection())) {
                    it.remove();
                    continue;
                }
            }
            if (next.variant instanceof ImageVariant) {
                if (blacklisted.contains(VideoResolution.getByHeight(((ImageVariant) next.variant).getHeight()))) {
                    it.remove();
                    continue;
                }
            }

        }
    }

    public void updateEnabledMap() {
        CounterMap<String> enabledMap = new CounterMap<String>();

        for (AbstractVariantWrapper e : getTableData()) {
            if (e.isEnabled()) {
                enabledMap.increment(e.getVariableIDStorable().createUniqueID());
                enabledMap.increment(e.variant.getStandardGroupingID());

                enabledMap.increment(e.variant.getContainer().name());

            }
        }
        if (getTable() != null) {
            ((VariantsMapTable) getTable()).onEnabledMapUpdate(enabledMap);
        }
    }

    public void onEnabledMapUpdate(HashSet<String> enabledMap) {
    }

    protected List<Object> list(List<?> list) {
        if (list == null) {
            return new ArrayList<Object>();
        }
        return (List<Object>) list;
    }

    @Override
    public void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {
        if (keyHandler == CFG_YOUTUBE.DISABLED_VARIANTS || keyHandler == null) {
            List<VariantIDStorable> disabled = CFG_YOUTUBE.CFG.getDisabledVariants();

            HashSet<String> ids = new HashSet<String>();
            if (disabled != null) {
                for (VariantIDStorable be : disabled) {
                    ids.add(be.createUniqueID());
                }
            }
            // if (ids.size() > 0) {
            for (AbstractVariantWrapper s : all) {
                s.setEnabled(!ids.contains(s.getVariableIDStorable().createUniqueID()));
            }
            // }

        }
        _fireTableStructureChanged(new ArrayList<AbstractVariantWrapper>(all), true);

    }

    public void load() {
        onConfigValueModified(null, null);

    }

    public void save() {
        ArrayList<VariantIDStorable> lst = new ArrayList<VariantIDStorable>();
        for (AbstractVariantWrapper s : all) {
            if (!s.isEnabled()) {
                lst.add(s.getVariableIDStorable());
            }
        }
        CFG_YOUTUBE.CFG.setDisabledVariants(lst);
    }

}
