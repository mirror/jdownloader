package org.jdownloader.plugins.components.youtube.choosevariantdialog;

import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingConstants;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.youtube.configpanel.AbstractVariantWrapper;
import org.jdownloader.plugins.components.youtube.configpanel.VariantsMapTableModel;

public class CustomVariantsMapTableModel extends VariantsMapTableModel {

    public CustomVariantsMapTableModel(ArrayList<AbstractVariantWrapper> sorted) {
        super("ChooseYoutubeVariantDialog", sorted);
        load();
    }

    @Override
    protected void addGroupingColumn() {

    }

    @Override
    protected void addPriorityColumn() {

    }

    @Override
    protected void initColumns() {
        super.initColumns();

    }

    @Override
    protected void filter(List<AbstractVariantWrapper> newtableData) {

    }

    protected void addContainerColumn() {
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
    protected void addTypeColumn() {

    }

    @Override
    public void save() {

    }

    @Override
    protected void initListeners() {

    }

    @Override
    protected void addCheckBoxColumn() {

    }
}
