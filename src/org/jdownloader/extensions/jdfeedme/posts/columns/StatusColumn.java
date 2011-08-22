package org.jdownloader.extensions.jdfeedme.posts.columns;

import java.awt.Component;

import javax.swing.SwingConstants;

import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;

import org.jdesktop.swingx.renderer.JRendererLabel;
import org.jdownloader.extensions.jdfeedme.posts.JDFeedMePost;
import org.jdownloader.images.NewTheme;

public class StatusColumn extends JDTableColumn {

    private static final long serialVersionUID = 7784119536615940150L;

    private JRendererLabel    labelRend;

    public StatusColumn(String name, JDTableModel table) {
        super(name, table);

        labelRend = new JRendererLabel();
        labelRend.setBorder(null);
        labelRend.setHorizontalAlignment(SwingConstants.CENTER);
        labelRend.setIcon(null);
        labelRend.setOpaque(false);
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public boolean isEditable(Object obj) {
        return false;
    }

    @Override
    public boolean isEnabled(Object obj) {
        return true;
    }

    @Override
    public boolean isSortable(Object obj) {
        return false;
    }

    @Override
    public Component myTableCellEditorComponent(JDTableModel table, Object value, boolean isSelected, int row, int column) {
        return null;
    }

    @Override
    public Component myTableCellRendererComponent(JDTableModel table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        String added = ((JDFeedMePost) value).getAdded();

        if (added.equalsIgnoreCase(JDFeedMePost.ADDED_NO)) {
            labelRend.setIcon(null);
            labelRend.setToolTipText(null);
        } else if (added.equalsIgnoreCase(JDFeedMePost.ADDED_YES)) {
            labelRend.setIcon(NewTheme.I().getIcon("true", 16));
            labelRend.setToolTipText("Downloaded successfully");
        } else if (added.equalsIgnoreCase(JDFeedMePost.ADDED_YES_NO_FILES)) {
            labelRend.setIcon(NewTheme.I().getIcon("false", 16));
            labelRend.setToolTipText("Downloaded but no files found");
        } else if (added.equalsIgnoreCase(JDFeedMePost.ADDED_YES_OTHER_FEED)) {
            labelRend.setIcon(NewTheme.I().getIcon("warning", 16));
            labelRend.setToolTipText("Downloaded in another feed");
        }

        return labelRend;
    }

    @Override
    public void setValue(Object value, Object object) {
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
    }

    @Override
    public int getMaxWidth() {
        return 60;
    }

}
