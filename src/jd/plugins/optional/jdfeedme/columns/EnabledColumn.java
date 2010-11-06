package jd.plugins.optional.jdfeedme.columns;

import jd.gui.swing.components.table.JDCheckBoxTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.plugins.optional.jdfeedme.JDFeedMeFeed;

public class EnabledColumn extends JDCheckBoxTableColumn {

	private static final long serialVersionUID = 2684119530915940150L;

    public EnabledColumn(String name, JDTableModel table) {
        super(name, table);
    }

    @Override
    public boolean isEditable(Object obj) {
        return true;
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
    protected boolean getBooleanValue(Object value) {
        return ((JDFeedMeFeed) value).isEnabled();
    }

    @Override
    protected void setBooleanValue(boolean value, Object object) {
        ((JDFeedMeFeed) object).setEnabled(value);
    }

}
