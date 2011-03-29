package org.jdownloader.extensions.jdfeedme;

import java.util.ArrayList;
import java.util.HashMap;

import org.jdownloader.extensions.jdfeedme.posts.JDFeedMePost;

import jd.gui.swing.components.table.JDTable;

public class JDFeedMeTable extends JDTable {
	
	private static final long serialVersionUID = 2767333885884748758L;

    public JDFeedMeTable(ArrayList<JDFeedMeFeed> settings, HashMap<String, ArrayList<JDFeedMePost>> posts) {
        super(new JDFeedMeTableModel("jdfeedmeview", settings, posts));
    }

    @Override
    public JDFeedMeTableModel getModel() {
        return (JDFeedMeTableModel) super.getModel();
    }

}
