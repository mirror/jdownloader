package org.jdownloader.plugins.components.youtube.choosevariantdialog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.SwingConstants;

import org.appwork.storage.config.handler.KeyHandler;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.youtube.configpanel.AbstractVariantWrapper;
import org.jdownloader.plugins.components.youtube.configpanel.VariantsMapTableModel;
import org.jdownloader.plugins.components.youtube.variants.AbstractVariant;
import org.jdownloader.settings.staticreferences.CFG_YOUTUBE;

public class CustomVariantsMapTableModel extends VariantsMapTableModel {

    private AbstractVariant selected;

    public CustomVariantsMapTableModel(ArrayList<AbstractVariantWrapper> sorted, AbstractVariant selected) {
        super("ChooseYoutubeVariantDialog", sorted);
        this.selected = selected;
        super.init("ChooseYoutubeVariantDialog");
        super.initHighLighter();
    }

    @Override
    protected void init(String id) {

    }

    @Override
    protected void initHighLighter() {

    }

    @Override
    public void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {

        _fireTableStructureChanged(new ArrayList<AbstractVariantWrapper>(all), true);

    }

    @Override
    protected void addGroupingColumn() {

    }

    @Override
    protected void initColumns() {

        super.initColumns();

    }

    private List<Filter> filters = new ArrayList<Filter>();

    public void addFilter(Filter typeSel) {
        filters.add(typeSel);
    }

    @Override
    protected void filter(List<AbstractVariantWrapper> newtableData) {

        main: for (final Iterator<AbstractVariantWrapper> it = newtableData.iterator(); it.hasNext();) {
            final AbstractVariantWrapper next = it.next();

            for (Filter f : filters) {
                if (f.isBlacklisted(next.variant)) {
                    it.remove();
                    continue main;
                }
            }

        }
    }

    protected void addFileTypeColumn() {
        addColumn(new AutoResizingTextColumn(_GUI.T.YOUTUBE_CONFIG_PANEL_TABLE_FILETYPE()) {
            {
                rendererField.setHorizontalAlignment(SwingConstants.LEFT);
            }

            @Override
            public boolean isAutoWidthEnabled() {
                return false;
            }

            @Override
            public String getStringValue(AbstractVariantWrapper value) {

                return value.variant.getContainer().getLabel();
            }
        });
    }

    @Override
    protected void onStructureChanged(List<AbstractVariantWrapper> newtableData) {
        super.onStructureChanged(newtableData);
        if (typeColumn != null) {
            boolean showType = false;
            showType |= hasSubtitle;
            showType |= hasImage;
            int groups = 0;
            if (hasSubtitle) {
                groups++;
            }
            if (hasVideo) {
                groups++;
            }
            if (hasAudio) {
                groups++;
            }
            if (hasDescription) {
                groups++;
            }
            if (hasImage) {
                groups++;
            }
            showType |= groups > 1;
            setColumnVisible(typeColumn, showType);
        }

    }

    @Override
    public void save() {

    }

    @Override
    protected void initListeners() {
        CFG_YOUTUBE.CHOOSE_VARIANT_DIALOG_BLACKLISTED_AUDIO_BITRATES.getEventSender().addListener(this, true);
        CFG_YOUTUBE.CHOOSE_VARIANT_DIALOG_BLACKLISTED_AUDIO_CODECS.getEventSender().addListener(this, true);
        CFG_YOUTUBE.CHOOSE_VARIANT_DIALOG_BLACKLISTED_FILE_CONTAINERS.getEventSender().addListener(this, true);
        CFG_YOUTUBE.CHOOSE_VARIANT_DIALOG_BLACKLISTED_GROUPS.getEventSender().addListener(this, true);
        CFG_YOUTUBE.CHOOSE_VARIANT_DIALOG_BLACKLISTED_PROJECTIONS.getEventSender().addListener(this, true);
        CFG_YOUTUBE.CHOOSE_VARIANT_DIALOG_BLACKLISTED_RESOLUTIONS.getEventSender().addListener(this, true);
        CFG_YOUTUBE.CHOOSE_VARIANT_DIALOG_BLACKLISTED_VIDEO_CODECS.getEventSender().addListener(this, true);
        CFG_YOUTUBE.CHOOSE_VARIANT_DIALOG_BLACKLISTED_VIDEO_FRAMERATES.getEventSender().addListener(this, true);

        onConfigValueModified(null, null);
    }

    @Override
    protected void addCheckBoxColumn() {

    }

}
