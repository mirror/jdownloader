package jd.plugins.optional.jdfeedme;

import java.util.ArrayList;
import java.util.HashMap;

import jd.gui.swing.components.table.JDTable;
import jd.plugins.optional.jdfeedme.posts.JDFeedMePost;

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
