package org.jdownloader.extensions.jdfeedme.posts.columns;

import org.jdownloader.extensions.jdfeedme.posts.JDFeedMePost;

import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.components.table.JDTextEditorTableColumn;

public class TitleColumn extends JDTextEditorTableColumn {

	private static final long serialVersionUID = 5830351666645022509L;

    public TitleColumn(String name, JDTableModel table) {
        super(name, table);
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
        return ((JDFeedMePost) value).getTitle();
    }

    @Override
    protected void setStringValue(String value, Object object) {
        ((JDFeedMePost) object).setTitle(value);
    }



}
