package jd.plugins.optional.jdfeedme.posts.columns;

import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.components.table.JDTextEditorTableColumn;
import jd.plugins.optional.jdfeedme.posts.JDFeedMePost;

public class TimestampColumn extends JDTextEditorTableColumn {

	private static final long serialVersionUID = 5530351626645232509L;

    public TimestampColumn(String timestamp, JDTableModel table) {
        super(timestamp, table);
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
    public void sort(Object obj, boolean sortingToggle) {
    }

    @Override
    protected String getStringValue(Object value) {
        return ((JDFeedMePost) value).getTimestamp();
    }

    @Override
    protected void setStringValue(String value, Object object) {
        ((JDFeedMePost) object).setTimestamp(value);
    }

    
    @Override
    protected int getMaxWidth() {
        return 190;
    }
    

}
