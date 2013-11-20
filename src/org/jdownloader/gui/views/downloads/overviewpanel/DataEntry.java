package org.jdownloader.gui.views.downloads.overviewpanel;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.appwork.swing.MigPanel;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class DataEntry {

    private JLabel total;

    public JLabel getTotal() {
        return total;
    }

    public JLabel getFiltered() {
        return filtered;
    }

    public JLabel getSelected() {
        return selected;
    }

    private JLabel       filtered;
    private JLabel       selected;
    final private String label;

    public String getLabel() {
        return label;
    }

    public DataEntry(String label) {
        this.label = label;
        total = new JLabel();
        filtered = new JLabel();
        selected = new JLabel();
        total.setToolTipText(_GUI._.DownloadOverview_DownloadOverview_tooltip1());
        filtered.setToolTipText(_GUI._.DownloadOverview_DownloadOverview_tooltip2());
        selected.setToolTipText(_GUI._.DownloadOverview_DownloadOverview_tooltip3());
        updateVisibility(false);
    }

    private JComponent createHeaderLabel(String label) {
        JLabel lbl = new JLabel(label);
        SwingUtils.toBold(lbl);
        lbl.setEnabled(false);
        return lbl;
    }

    public void addTo(MigPanel info) {
        addTo(info, null);
    }

    public void addTo(MigPanel info, String constrains) {
        info.add(createHeaderLabel(label), "alignx right" + (constrains == null ? "" : constrains));
        info.add(total, "hidemode 3");
        info.add(filtered, "hidemode 3");
        info.add(selected, "hidemode 3");
    }

    public void setTotal(Object string) {
        total.setText(string.toString());
    }

    public void setSelected(Object string) {
        selected.setText(string.toString());
    }

    public void setFiltered(Object string) {
        filtered.setText(string.toString());
    }

    public void updateVisibility(boolean hasSelectedObjects) {
        if (CFG_GUI.OVERVIEW_PANEL_SMART_INFO_VISIBLE.isEnabled() || (!CFG_GUI.OVERVIEW_PANEL_SELECTED_INFO_VISIBLE.isEnabled() && !CFG_GUI.OVERVIEW_PANEL_TOTAL_INFO_VISIBLE.isEnabled() && !CFG_GUI.OVERVIEW_PANEL_VISIBLE_ONLY_INFO_VISIBLE.isEnabled())) {
            if (hasSelectedObjects) {
                filtered.setVisible(false);
                total.setVisible(false);
                selected.setVisible(true);
            } else {
                filtered.setVisible(true);
                total.setVisible(false);
                selected.setVisible(false);
            }
        } else {
            filtered.setVisible(CFG_GUI.OVERVIEW_PANEL_VISIBLE_ONLY_INFO_VISIBLE.isEnabled());
            total.setVisible(CFG_GUI.OVERVIEW_PANEL_TOTAL_INFO_VISIBLE.isEnabled());
            selected.setVisible(CFG_GUI.OVERVIEW_PANEL_SELECTED_INFO_VISIBLE.isEnabled());
        }
    }

}
