package org.jdownloader.plugins.components.youtube.choosevariantdialog;

import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.appwork.storage.config.annotations.IntegerInterface;
import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.storage.config.handler.ObjectKeyHandler;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.dimensor.RememberLastDialogDimension;
import org.appwork.utils.swing.dialog.locator.RememberRelativeDialogLocator;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.youtube.Projection;
import org.jdownloader.plugins.components.youtube.VariantIDStorable;
import org.jdownloader.plugins.components.youtube.YoutubeClipData;
import org.jdownloader.plugins.components.youtube.configpanel.AbstractVariantWrapper;
import org.jdownloader.plugins.components.youtube.configpanel.EnumMultiComboBox;
import org.jdownloader.plugins.components.youtube.itag.AudioBitrate;
import org.jdownloader.plugins.components.youtube.itag.AudioCodec;
import org.jdownloader.plugins.components.youtube.itag.VideoCodec;
import org.jdownloader.plugins.components.youtube.itag.VideoFrameRate;
import org.jdownloader.plugins.components.youtube.itag.VideoResolution;
import org.jdownloader.plugins.components.youtube.variants.AbstractVariant;
import org.jdownloader.plugins.components.youtube.variants.AudioInterface;
import org.jdownloader.plugins.components.youtube.variants.FileContainer;
import org.jdownloader.plugins.components.youtube.variants.ImageVariant;
import org.jdownloader.plugins.components.youtube.variants.VariantGroup;
import org.jdownloader.plugins.components.youtube.variants.VariantInfo;
import org.jdownloader.plugins.components.youtube.variants.VideoVariant;
import org.jdownloader.settings.staticreferences.CFG_YOUTUBE;

import jd.controlling.linkcrawler.CrawledLink;
import jd.gui.swing.jdgui.JDGui;
import jd.plugins.PluginConfigPanelNG;
import net.miginfocom.swing.MigLayout;

public class YoutubeVariantSelectionDialog extends AbstractDialog<Object> implements KeyListener, ListSelectionListener, MouseListener {
    private YoutubeClipData          clip;
    protected CustomVariantsMapTable table;
    private AbstractVariantWrapper   current;

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getClickCount() == 2) {
            if (selectedVariant != null) {
                okButton.doClick();
            }
        }
    }

    @Override
    protected int getPreferredHeight() {
        return 600;
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        List<AbstractVariantWrapper> selectedObject = table.getModel().getSelectedObjects(1);
        if (selectedObject == null || selectedObject.size() == 0) {
            selectedVariant = null;
            okButton.setEnabled(false);
        } else {
            selectedVariant = selectedObject.get(0);
            okButton.setEnabled(true);
        }

    }

    @Override
    protected int getPreferredWidth() {
        return 800;
    }

    protected AbstractVariantWrapper            selectedVariant;
    private List<VariantInfo>                   variants;
    protected AbstractVariant                   selected;
    private CrawledLink                         link;
    private HashSet<VariantGroup>               allowedGroups;
    private HashSet<FileContainer>              allowedFileTypes;
    private HashSet<Projection>                 allowedProjections;
    private HashSet<VideoResolution>            allowedResolutions;
    private HashSet<VideoFrameRate>             allowedFps;
    private HashSet<VideoCodec>                 allowedVCodec;
    private HashSet<AudioCodec>                 allowedACodec;
    protected HashSet<AudioBitrate>             allowedABitrate;
    protected ArrayList<AbstractVariantWrapper> variantWrapperList;

    @Override
    public ModalityType getModalityType() {
        return ModalityType.MODELESS;
    }

    protected YoutubeVariantSelectionDialog(VariantGroup group, String title, String okText, List<VariantInfo> variants) {
        super(Dialog.STYLE_HIDE_ICON, title, null, okText, null);
        setDimensor(new RememberLastDialogDimension("YoutubeChooseVariantDialogDimension"));
        setLocator(new RememberRelativeDialogLocator("YoutubeChooseVariantDialogLocation8", JDGui.getInstance().getMainFrame()));

        this.variants = variants;
        initVariants(group);
    }

    public YoutubeVariantSelectionDialog(CrawledLink link, AbstractVariant selected, YoutubeClipData clipData, List<VariantInfo> variants) {
        super(Dialog.STYLE_HIDE_ICON, selected == null ? _GUI.T.youtube_variant_selection_dialog_title_additional(clipData.title) : _GUI.T.youtube_variant_selection_dialog_title(clipData.title), null, selected == null ? _GUI.T.lit_add() : _GUI.T.lit_choose(), null);

        setDimensor(new RememberLastDialogDimension("YoutubeChooseVariantDialogDimension"));
        setLocator(new RememberRelativeDialogLocator("YoutubeChooseVariantDialogLocation8", JDGui.getInstance().getMainFrame()));
        this.clip = clipData;
        this.selected = selected;
        this.variants = variants;
        this.link = link;
        initVariants(selected != null ? selected.getGroup() : null);
        String selectID = selected == null ? null : new VariantIDStorable(selected).createUniqueID();

        for (AbstractVariantWrapper vi : variantWrapperList) {
            if (current == null && StringUtils.equals(selectID, vi.getVariableIDStorable().createUniqueID())) {
                current = vi;
                break;
            }

        }
    }

    protected void initVariants(VariantGroup group) {
        List<String> dupe = new ArrayList<String>();
        Collections.sort(variants);
        variantWrapperList = new ArrayList<AbstractVariantWrapper>();

        allowedGroups = new HashSet<VariantGroup>();
        allowedFileTypes = new HashSet<FileContainer>();
        allowedProjections = new HashSet<Projection>();
        allowedResolutions = new HashSet<VideoResolution>();
        allowedFps = new HashSet<VideoFrameRate>();
        allowedVCodec = new HashSet<VideoCodec>();
        allowedACodec = new HashSet<AudioCodec>();
        allowedABitrate = new HashSet<AudioBitrate>();
        if (group != null) {
            allowedGroups.add(group);

        } else {
            allowedGroups.addAll(Arrays.asList(VariantGroup.values()));
        }
        for (VariantInfo vi : variants) {
            if (allowedGroups.contains(vi.getVariant().getGroup())) {
                VariantIDStorable stor = new VariantIDStorable(vi.getVariant());
                if (dupe.add(stor.createUniqueID())) {
                    AbstractVariantWrapper avw;
                    variantWrapperList.add(avw = new AbstractVariantWrapper(vi.getVariant()));

                    allowedFileTypes.add(vi.getVariant().getContainer());
                    if (vi.getVariant() instanceof VideoVariant) {
                        VideoVariant vVar = (VideoVariant) vi.getVariant();
                        allowedProjections.add(vVar.getProjection());
                        allowedResolutions.add(vVar.getVideoResolution());
                        allowedFps.add(vVar.getiTagVideo().getVideoFrameRate());
                        allowedVCodec.add(vVar.getVideoCodec());
                    }
                    if (vi.getVariant() instanceof AudioInterface) {
                        AudioInterface aVar = (AudioInterface) vi.getVariant();
                        allowedABitrate.add(aVar.getAudioBitrate());
                        allowedACodec.add(aVar.getAudioCodec());
                    }
                    if (vi.getVariant() instanceof ImageVariant) {
                        ImageVariant iVar = (ImageVariant) vi.getVariant();
                        allowedResolutions.add(VideoResolution.getByHeight(iVar.getHeight()));
                    }
                }

            }
        }
    }

    public CustomVariantsMapTable getTable() {
        return table;
    }

    @Override
    public JComponent layoutDialogContent() {
        PluginConfigPanelNG ret = new PluginConfigPanelNG() {

            @Override
            public void reset() {
            }

            @Override
            public void save() {
            }

            @Override
            public void updateContents() {
            }

        };
        okButton.setEnabled(false);
        int height = new JLabel("Test").getPreferredSize().height;
        ret.setLayout(new MigLayout("ins 0, wrap 2", "[][grow,fill]", "[]"));
        CustomVariantsMapTableModel model = createTableModel();
        table = createTable(model);
        JScrollPane sp;
        if (selected == null) {

            table.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        }
        ret.addDescriptionPlain(getDescriptionText());
        //

        addFilter(ret);

        //
        table.load();
        ret.add(sp = new JScrollPane(table), "pushx,growx,spanx,pushy,growy");

        table.getSelectionModel().addListSelectionListener(this);
        table.addKeyListener(this);
        table.addMouseListener(this);
        getDialog().setMinimumSize(new Dimension(200, 200));
        if (current != null) {
            table.getModel().setSelectedObject(current);
            table.scrollToSelection(0);
        }
        okButton.setEnabled(false);
        return ret;
    }

    protected CustomVariantsMapTable createTable(CustomVariantsMapTableModel model) {
        return new CustomVariantsMapTable(model);
    }

    protected CustomVariantsMapTableModel createTableModel() {
        return new CustomVariantsMapTableModel(variantWrapperList, selected);
    }

    protected String getDescriptionText() {
        if (selected == null) {
            // add new variant
            return (_GUI.T.youtube_add_variant_help());

        } else {
            // change varaint
            return (_GUI.T.youtube_coose_variant_help());

        }
    }

    private class CustEnumMultiComboBox<T> extends EnumMultiComboBox<T> implements Filter {

        private T[]        allValues;
        private HashSet<T> set = new HashSet<T>();

        public CustEnumMultiComboBox(T[] allValues, List<T> values, ObjectKeyHandler keyHandler) {
            super(values, keyHandler, true);
            this.allValues = allValues;
            set.addAll(values);
            onConfigValueModified(null, null);
        }

        @Override
        public void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {
            if (allValues == null) {
                return;
            }
            super.onConfigValueModified(keyHandler, newValue);
        }

        @Override
        protected void loadValuesFromKeyHandler() {

            ArrayList<T> selected = new ArrayList<T>(getValues());
            List<T> blacklisted = (List<T>) keyHandler.getValue();
            if (blacklisted != null) {
                selected.removeAll(blacklisted);
            }
            setSelectedItems(selected);
        }

        @Override
        protected void saveValuesToKeyHandler() {
            List<T> selected = getSelectedItems();

            ArrayList<T> all = new ArrayList<T>(Arrays.asList(allValues));
            all.removeAll(selected);
            keyHandler.setValue(all);
        }

        @Override
        public void onChanged() {
            set = new HashSet<T>(selectedItems);
            super.onChanged();

        }

        @Override
        public boolean isBlacklisted(AbstractVariant variant) {
            if (allValues[0] instanceof VariantGroup) {
                return !set.contains(variant.getGroup());
            }

            if (allValues[0] instanceof FileContainer) {
                return !set.contains(variant.getContainer());
            }
            if (variant instanceof AudioInterface) {
                if (allValues[0] instanceof AudioCodec) {

                    return !set.contains(((AudioInterface) variant).getAudioCodec());

                }

                if (allValues[0] instanceof AudioBitrate) {

                    return !set.contains(((AudioInterface) variant).getAudioBitrate());
                }

            }

            if (variant instanceof VideoVariant) {
                if (allValues[0] instanceof VideoCodec) {

                    return !set.contains(((VideoVariant) variant).getVideoCodec());

                }
                if (allValues[0] instanceof VideoFrameRate) {

                    return !set.contains(((VideoVariant) variant).getiTagVideo().getVideoFrameRate());

                }
                if (allValues[0] instanceof Projection) {

                    return !set.contains(((VideoVariant) variant).getProjection());

                }
                if (allValues[0] instanceof VideoResolution) {

                    return !set.contains(((VideoVariant) variant).getVideoResolution());

                }
            }
            if (variant instanceof ImageVariant) {
                if (allValues[0] instanceof VideoResolution) {

                    return !set.contains(VideoResolution.getByHeight(((ImageVariant) variant).getHeight()));

                }
            }
            return false;
        }

    }

    private void addFilter(PluginConfigPanelNG ret) {
        List<VariantGroup> groups = new ArrayList<VariantGroup>(allowedGroups);
        Collections.sort(groups, LabelInterface.COMPARATOR_ASC);
        CustEnumMultiComboBox<VariantGroup> typeSel = new CustEnumMultiComboBox<VariantGroup>(VariantGroup.values(), groups, CFG_YOUTUBE.CHOOSE_VARIANT_DIALOG_BLACKLISTED_GROUPS);

        List<FileContainer> container = new ArrayList<FileContainer>(allowedFileTypes);
        Collections.sort(container, LabelInterface.COMPARATOR_ASC);
        CustEnumMultiComboBox<FileContainer> containerSel = new CustEnumMultiComboBox<FileContainer>(FileContainer.values(), container, CFG_YOUTUBE.CHOOSE_VARIANT_DIALOG_BLACKLISTED_FILE_CONTAINERS) {
            @Override
            protected String getLabel(int i, FileContainer sc) {
                return sc.getTooltip();
            }
        };
        List<Projection> projections = new ArrayList<Projection>(allowedProjections);
        Collections.sort(projections, LabelInterface.COMPARATOR_ASC);
        CustEnumMultiComboBox<Projection> projectionSelect = new CustEnumMultiComboBox<Projection>(Projection.values(), projections, CFG_YOUTUBE.CHOOSE_VARIANT_DIALOG_BLACKLISTED_PROJECTIONS) {
            @Override
            protected String getLabel(int i, Projection sc) {
                return sc.getTooltip();
            }
        };

        List<VideoResolution> heights = new ArrayList<VideoResolution>(allowedResolutions);
        Collections.sort(heights, IntegerInterface.COMPARATOR_DESC);
        CustEnumMultiComboBox<VideoResolution> resolutionSelect = new CustEnumMultiComboBox<VideoResolution>(VideoResolution.values(), heights, CFG_YOUTUBE.CHOOSE_VARIANT_DIALOG_BLACKLISTED_RESOLUTIONS);

        List<VideoFrameRate> fpss = new ArrayList<VideoFrameRate>(allowedFps);
        Collections.sort(fpss, IntegerInterface.COMPARATOR_DESC);
        CustEnumMultiComboBox<VideoFrameRate> fpsSelect = new CustEnumMultiComboBox<VideoFrameRate>(VideoFrameRate.values(), fpss, CFG_YOUTUBE.CHOOSE_VARIANT_DIALOG_BLACKLISTED_VIDEO_FRAMERATES);

        List<VideoCodec> videoCodecs = new ArrayList<VideoCodec>(allowedVCodec);
        Collections.sort(videoCodecs, LabelInterface.COMPARATOR_ASC);
        CustEnumMultiComboBox<VideoCodec> vcodec = new CustEnumMultiComboBox<VideoCodec>(VideoCodec.values(), videoCodecs, CFG_YOUTUBE.CHOOSE_VARIANT_DIALOG_BLACKLISTED_VIDEO_CODECS) {
            @Override
            protected String getLabel(int i, VideoCodec sc) {
                return sc.getTooltip();
            }
        };

        List<AudioCodec> audioCodecs = new ArrayList<AudioCodec>(allowedACodec);
        Collections.sort(audioCodecs, LabelInterface.COMPARATOR_ASC);
        CustEnumMultiComboBox<AudioCodec> acodec = new CustEnumMultiComboBox<AudioCodec>(AudioCodec.values(), audioCodecs, CFG_YOUTUBE.CHOOSE_VARIANT_DIALOG_BLACKLISTED_AUDIO_CODECS) {
            @Override
            protected String getLabel(int i, AudioCodec sc) {
                return sc.getTooltip();
            }
        };

        List<AudioBitrate> bitrates = new ArrayList<AudioBitrate>(allowedABitrate);
        Collections.sort(bitrates, IntegerInterface.COMPARATOR_DESC);
        CustEnumMultiComboBox<AudioBitrate> aBitrate = new CustEnumMultiComboBox<AudioBitrate>(AudioBitrate.values(), bitrates, CFG_YOUTUBE.CHOOSE_VARIANT_DIALOG_BLACKLISTED_AUDIO_BITRATES);

        typeSel.setShrinkedMode(true);
        containerSel.setShrinkedMode(true);
        projectionSelect.setShrinkedMode(true);
        resolutionSelect.setShrinkedMode(true);
        fpsSelect.setShrinkedMode(true);
        vcodec.setShrinkedMode(true);
        acodec.setShrinkedMode(true);
        aBitrate.setShrinkedMode(true);

        if (typeSel.getValues().size() > 1) {
            table.addFilter(typeSel);
            ret.addPair(_GUI.T.YOUTUBE_CONFIG_PANEL_TABLE_TYPE(), null, typeSel);
        }
        if (containerSel.getValues().size() > 1) {
            table.addFilter(containerSel);
            ret.addPair(_GUI.T.YOUTUBE_CONFIG_PANEL_TABLE_FILETYPE(), null, containerSel);
        }
        if (projectionSelect.getValues().size() > 1) {
            table.addFilter(projectionSelect);
            ret.addPair(_GUI.T.YOUTUBE_CONFIG_PANEL_TABLE_PROJECTION(), null, projectionSelect);
        }
        if (resolutionSelect.getValues().size() > 1) {
            table.addFilter(resolutionSelect);
            ret.addPair(_GUI.T.YOUTUBE_CONFIG_PANEL_TABLE_RESOLUTION(), null, resolutionSelect);
        }
        if (fpsSelect.getValues().size() > 1) {
            table.addFilter(fpsSelect);
            ret.addPair(_GUI.T.YOUTUBE_CONFIG_PANEL_TABLE_FPS(), null, fpsSelect);
        }
        if (vcodec.getValues().size() > 1) {
            table.addFilter(vcodec);
            ret.addPair(_GUI.T.YOUTUBE_CONFIG_PANEL_TABLE_VIDEO_CODEC(), null, vcodec);
        }
        if (acodec.getValues().size() > 1) {
            table.addFilter(acodec);
            ret.addPair(_GUI.T.YOUTUBE_CONFIG_PANEL_TABLE_AUDIO_CODEC(), null, acodec);
        }
        if (aBitrate.getValues().size() > 1) {
            table.addFilter(aBitrate);
            ret.addPair(_GUI.T.YOUTUBE_CONFIG_PANEL_TABLE_AUDIO_BITRATE(), null, aBitrate);
        }
    }

    @Override
    protected void packed() {
        super.packed();

    }

    @Override
    protected void initFocus(JComponent focus) {
        super.initFocus(focus);
        table.requestFocus();
    }

    public List<LinkVariant> getVariants() {

        List<AbstractVariantWrapper> wrapper = table.getModel().getSelectedObjects();
        List<LinkVariant> ret = new ArrayList<LinkVariant>();
        for (AbstractVariantWrapper w : wrapper) {
            ret.add(w.variant);
        }
        return ret;
    }

    public LinkVariant getVariant() {
        if (selectedVariant == null) {
            return null;
        }
        return selectedVariant.variant;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            if (selectedVariant != null) {
                okButton.doClick();
            }
        }
    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

}
