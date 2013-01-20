package org.jdownloader.gui.views.linkgrabber.actions;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSeparator;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.MigPanel;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.jdownloader.gui.translate._GUI;

public class ResetLinkGrabberOptionDialog extends AbstractDialog<Object> {

    private JCheckBox                     cbLinks;
    private JCheckBox                     cbSort;
    private JCheckBox                     cbCrawler;
    private JCheckBox                     cbSearch;
    private ResetLinkGrabberDialogOptions settings;

    public ResetLinkGrabberOptionDialog() {
        super(0, _GUI._.ResetLinkGrabberOptionDialog_ResetLinkGrabberOptionDialog_title(), null, null, null);
    }

    @Override
    protected Object createReturnValue() {

        settings.setClearSearchFilter(cbSearch.isSelected());
        settings.setInterruptCrawler(cbCrawler.isSelected());
        settings.setRemoveLinks(cbLinks.isSelected());
        settings.setResetSorter(cbSort.isSelected());
        return null;
    }

    public ResetLinkGrabberDialogOptions getSettings() {
        return settings;
    }

    @Override
    public JComponent layoutDialogContent() {
        settings = JsonConfig.create(ResetLinkGrabberDialogOptions.class);
        MigPanel m = new MigPanel("ins 5, wrap 2", "[grow, fill][]", "");
        m.add(new JLabel(_GUI._.ResetLinkGrabberOptionDialog_layoutDialogContent_()), "spanx");
        m.add(new JSeparator(), "spanx");
        m.add(createLabel(_GUI._.ResetLinkGrabberOptionDialog_layoutDialogContent_remove_links()));
        m.add(cbLinks = new JCheckBox());
        cbLinks.setSelected(settings.isRemoveLinks());
        m.add(createLabel(_GUI._.ResetLinkGrabberOptionDialog_layoutDialogContent_sort()));
        m.add(cbSort = new JCheckBox());
        cbSort.setSelected(settings.isResetSorter());
        m.add(createLabel(_GUI._.ResetLinkGrabberOptionDialog_layoutDialogContent_search()));
        m.add(cbSearch = new JCheckBox());
        cbSearch.setSelected(settings.isClearSearchFilter());
        m.add(createLabel(_GUI._.ResetLinkGrabberOptionDialog_layoutDialogContent_interrup_crawler()));
        m.add(cbCrawler = new JCheckBox());
        cbCrawler.setSelected(settings.isInterruptCrawler());
        return m;
    }

    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setHorizontalAlignment(JLabel.RIGHT);
        return lbl;
    }
}
