package jd.plugins.optional.jdfeedme.columns;

import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.components.table.JDTextEditorTableColumn;
import jd.plugins.optional.jdfeedme.JDFeedMeFeed;

public class StatusColumn extends JDTextEditorTableColumn {

	private static final long serialVersionUID = 4030351626645232509L;

    public StatusColumn(String status, JDTableModel table) {
        super(status, table);
    }

    @Override
    public boolean isEditable(Object obj) {
        return false;
    }

    @Override
    public boolean isEnabled(Object obj) {
        return ((JDFeedMeFeed) obj).isEnabled();
    }

    @Override
    public boolean isSortable(Object obj) {
        return false;
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
    }

    @Override
    protected String getStringValue(Object value) {
        return ((JDFeedMeFeed) value).getStatus();
    }

    @Override
    protected void setStringValue(String value, Object object) {
        ((JDFeedMeFeed) object).setStatus(value);
    }



}
