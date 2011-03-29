package org.jdownloader.extensions.jdfeedme;

import java.util.ArrayList;
import java.util.HashMap;

import org.jdownloader.extensions.jdfeedme.columns.AddressColumn;
import org.jdownloader.extensions.jdfeedme.columns.DescriptionColumn;
import org.jdownloader.extensions.jdfeedme.columns.EnabledColumn;
import org.jdownloader.extensions.jdfeedme.columns.FiltersColumn;
import org.jdownloader.extensions.jdfeedme.columns.HosterColumn;
import org.jdownloader.extensions.jdfeedme.columns.PostsColumn;
import org.jdownloader.extensions.jdfeedme.columns.SkipColumn;
import org.jdownloader.extensions.jdfeedme.columns.StatusColumn;
import org.jdownloader.extensions.jdfeedme.columns.TimestampColumn;
import org.jdownloader.extensions.jdfeedme.posts.JDFeedMePost;

import jd.gui.swing.components.table.JDTableModel;


public class JDFeedMeTableModel extends JDTableModel {

	private static final long serialVersionUID = -8877812370684343642L;

    private ArrayList<JDFeedMeFeed> feeds;
    private HashMap<String, ArrayList<JDFeedMePost>> posts;

    public JDFeedMeTableModel(String configname, ArrayList<JDFeedMeFeed> feeds, HashMap<String, ArrayList<JDFeedMePost>> posts) {
        super(configname);

        this.feeds = feeds;
        this.posts = posts;
    }

    protected void initColumns() {
        this.addColumn(new EnabledColumn("Enabled", this));
        this.addColumn(new AddressColumn("Feed Address", this));
        this.addColumn(new DescriptionColumn("Description", this));
        this.addColumn(new PostsColumn("Posts", this));
        this.addColumn(new FiltersColumn("Filters", this));
        this.addColumn(new HosterColumn("Limit To Hoster", this));
        this.addColumn(new SkipColumn("Skip Linkgrabber", this));
        this.addColumn(new TimestampColumn("Last Update", this));
        this.addColumn(new StatusColumn("Status", this));
    }

    @Override
    public void refreshModel() {
        synchronized (list) {
            list.clear();
            list.addAll(feeds);
        }
    }

    // TODO: add LOCKS on all code that reads or writes feed data
    public ArrayList<JDFeedMeFeed> getFeeds() {
        return feeds;
    }

    public void setFeeds(ArrayList<JDFeedMeFeed> feeds) {
        this.feeds = feeds;
    }
    
 // TODO: add LOCKS on all code that reads or writes posts data
    public HashMap<String, ArrayList<JDFeedMePost>> getPosts() {
        return posts;
    }

    public void setPosts(HashMap<String, ArrayList<JDFeedMePost>> posts) {
        this.posts = posts;
    }

}
