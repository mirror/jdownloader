//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.optional.jdfeedme;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.actions.ThreadedAction;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.views.ViewToolbar;
import jd.nutils.JDFlags;
import jd.plugins.optional.jdfeedme.dialogs.AddFeedDialog;
import jd.plugins.optional.jdfeedme.dialogs.ComboDialog;
import jd.plugins.optional.jdfeedme.posts.JDFeedMePost;
import net.miginfocom.swing.MigLayout;

public class JDFeedMeGui extends SwitchPanel implements KeyListener, ActionListener {

    private static final long serialVersionUID = 7508784076121700378L;

    /// stop using config and use XML instead
    //private final SubConfiguration config;
    
    private JDFeedMeTable table;
    private JCheckBox startDownloadsCheckbox;
    private JSpinner syncIntervalSpinner;

    public JDFeedMeGui() {
        
        initActions();
        initGUI();
    }

    private void initGUI() {
        this.setLayout(new MigLayout("ins 0, wrap 1", "[grow,fill]", "[][grow,fill][]"));
        this.add(new ViewToolbar("Add Feed", "Remove Feed" , "Reset Feed", "Sync All Now"));
        
        /// stop using config and use XML instead
        //this.add(new JScrollPane(table = new JDFeedMeTable(config.getGenericProperty(JDFeedMe.PROPERTY_SETTINGS, new ArrayList<JDFeedMeSetting>()))));
        ArrayList<JDFeedMeFeed> feeds = JDFeedMeFeed.loadXML(JDFeedMe.STORAGE_FEEDS);
        HashMap<String, ArrayList<JDFeedMePost>> posts = JDFeedMePost.loadXML(JDFeedMe.STORAGE_POSTS);
        this.add(new JScrollPane(table = new JDFeedMeTable(feeds, posts)), "grow");
        
        // config panel appears on the bottom
        JDFeedMeConfig config = JDFeedMeConfig.loadXML(JDFeedMe.STORAGE_CONFIG);
        JPanel bottom = new JPanel(new MigLayout("ins 0", "[][]15[]"));
        bottom.setOpaque(true);
        
        bottom.add(new JLabel("Sync Interval (hours):"));
        syncIntervalSpinner = new JSpinner(new SpinnerNumberModel(config.getSyncintervalhours(),1,Integer.MAX_VALUE,1));
        bottom.add(syncIntervalSpinner,"w 50!");
        startDownloadsCheckbox = new JCheckBox("Start downloads on updates",config.getStartdownloads());
        bottom.add(startDownloadsCheckbox);
        
        this.add(bottom);
    }

    private void initActions() {
        new ThreadedAction("Add Feed", "gui.images.add") {
            private static final long serialVersionUID = 2902582806883565245L;

            @Override
            public void initDefaults() {
                this.setToolTipText("Add a new feed");
            }

            /* CODE_FOR_INTERFACE_5_START
            @Override
            public void init() {}
            CODE_FOR_INTERFACE_5_END */

            @Override
            public void threadedActionPerformed(final ActionEvent e) {
                table.editingStopped(null);
                new GuiRunnable<Object>() {

                    @Override
                    public Object runSave() {
                    	
                        /* CODE_FOR_INTERFACE_5_START
                        int flags = UserIO.NO_COUNTDOWN;
                        CODE_FOR_INTERFACE_5_END */
                        /* CODE_FOR_INTERFACE_7_START */
                        int flags = 0;
                        /* CODE_FOR_INTERFACE_7_END */
                        
                    	AddFeedDialog dialog = new AddFeedDialog(flags);
                    	
                    	/* CODE_FOR_INTERFACE_7_START */
                    	dialog.displayDialog();
                    	/* CODE_FOR_INTERFACE_7_END */
                    	
                    	if (dialog.isResultOK())
                    	{
                    		JDFeedMeFeed new_feed = new JDFeedMeFeed(dialog.getResultAddress());
                    		new_feed.setUniqueid(JDFeedMeFeed.allocateUniqueid());
                    		new_feed.setTimestampFromGetOld(dialog.getResultGetOld());
                    		new_feed.setDoFilters(dialog.getResultDofilters());
                    		table.getModel().getFeeds().add(new_feed);
                            table.getModel().refreshModel();
                            table.getModel().fireTableDataChanged();
                    	}
                        return null;
                    }
                }.start();

            }
        };

        new ThreadedAction("Remove Feed", "gui.images.delete") {
            private static final long serialVersionUID = -961227173418839351L;

            @Override
            public void initDefaults() {
                this.setToolTipText("Remove selected feed(s)");
            }

            /* CODE_FOR_INTERFACE_5_START
            @Override
            public void init() {}
            CODE_FOR_INTERFACE_5_END */

            @Override
            public void threadedActionPerformed(ActionEvent e) {
                int[] rows = table.getSelectedRows();
                table.editingStopped(null);
                if (rows.length == 0) return;
                if (JDFlags.hasSomeFlags(UserIO.getInstance().requestConfirmDialog(0, "Remove selected Feed(s)?"), UserIO.RETURN_OK, UserIO.RETURN_DONT_SHOW_AGAIN)) {
                    ArrayList<JDFeedMeFeed> feeds = table.getModel().getFeeds();
                    for (int i = rows.length - 1; i >= 0; --i) 
                    {
                    	JDFeedMeFeed feed = feeds.get(rows[i]);
                        clearPostsFromFeed(feed);
                        feeds.remove(rows[i]);
                    }
                }
                table.getModel().refreshModel();
                table.getModel().fireTableDataChanged();
            }
        };
        
        new ThreadedAction("Reset Feed", "gui.images.restart") {
            private static final long serialVersionUID = -961227173618834351L;

            @Override
            public void initDefaults() {
                this.setToolTipText("Reset selected feed(s)");
            }

            /* CODE_FOR_INTERFACE_5_START
            @Override
            public void init() {}
            CODE_FOR_INTERFACE_5_END */

            @Override
            public void threadedActionPerformed(ActionEvent e) {
                table.editingStopped(null);
                
                new GuiRunnable<Object>() {

                    @Override
                    public Object runSave() {
                
                        int[] rows = table.getSelectedRows();
                        if (rows.length == 0) return null;
                
                        /* CODE_FOR_INTERFACE_5_START
                        int flags = UserIO.NO_COUNTDOWN;
                        CODE_FOR_INTERFACE_5_END */
                        /* CODE_FOR_INTERFACE_7_START */
                        int flags = 0;
                        /* CODE_FOR_INTERFACE_7_END */
                        
                        ComboDialog dialog = new ComboDialog(flags, "Reset Feed", "Get Old Posts:", JDFeedMeFeed.GET_OLD_OPTIONS, 0, null, "Reset", "Cancel", null);
                        
                        /* CODE_FOR_INTERFACE_7_START */
                        dialog.displayDialog();
                        /* CODE_FOR_INTERFACE_7_END */
                        
                    	if (dialog.isResultOK())
                    	{
                    		ArrayList<JDFeedMeFeed> feeds = table.getModel().getFeeds();
                            for (int i = rows.length - 1; i >= 0; --i) 
                            {
                            	JDFeedMeFeed feed = feeds.get(rows[i]);
                                feed.setTimestampFromGetOld(dialog.getResultCombo());
                                clearPostsFromFeed(feed);
                            }
                            table.getModel().refreshModel();
                            table.getModel().fireTableDataChanged();
                    	}
                    	return null;
                    }
                }.start();
            }
        };
        
        new ThreadedAction("Sync All Now", "gui.images.taskpanes.download") {
            private static final long serialVersionUID = -911247173617834351L;

            @Override
            public void initDefaults() {
                this.setToolTipText("Sync all feeds now");
            }

            /* CODE_FOR_INTERFACE_5_START
            @Override
            public void init() {}
            CODE_FOR_INTERFACE_5_END */

            @Override
            public void threadedActionPerformed(ActionEvent e) {
                table.editingStopped(null);
                new GuiRunnable<Object>() {

                    @Override
                    public Object runSave() {
                        JDFeedMe.syncNowEvent();
                        return null;
                    }
                }.start();
            }
        };
    }
    
    // TODO: add locking here
    @SuppressWarnings("unchecked")
	public ArrayList<JDFeedMeFeed> getFeeds()
    {
    	return (ArrayList<JDFeedMeFeed>) table.getModel().getFeeds().clone();
    }
    
    // TODO: add locking here
    public void setFeedTimestamp(JDFeedMeFeed feed, String timestamp)
    {
    	String uniqueid = feed.getUniqueid();
    	for (JDFeedMeFeed ifeed : table.getModel().getFeeds())
    	{
    		if (ifeed.getUniqueid().equalsIgnoreCase(uniqueid))
    		{
    			ifeed.setTimestamp(timestamp);
    			break;
    		}
    	}
    	
    	table.getModel().refreshModel();
        table.getModel().fireTableDataChanged();
    }
    
    // TODO: add locking here
    public void setFeedStatus(JDFeedMeFeed feed, String status)
    {
    	String uniqueid = feed.getUniqueid();
    	for (JDFeedMeFeed ifeed : table.getModel().getFeeds())
    	{
    		if (ifeed.getUniqueid().equalsIgnoreCase(uniqueid))
    		{
    			ifeed.setStatus(status);
    			break;
    		}
    	}
    	
    	table.getModel().refreshModel();
        table.getModel().fireTableDataChanged();
    }
    
    // TODO: add locking here
    public void setFeedNewposts(JDFeedMeFeed feed, boolean newposts)
    {
    	String uniqueid = feed.getUniqueid();
    	for (JDFeedMeFeed ifeed : table.getModel().getFeeds())
    	{
    		if (ifeed.getUniqueid().equalsIgnoreCase(uniqueid))
    		{
    			ifeed.setNewposts(newposts);
    			break;
    		}
    	}
    	
    	table.getModel().refreshModel();
        table.getModel().fireTableDataChanged();
    	
    	// update the posts too if no more new posts
    	if (!newposts)
    	{
    		ArrayList<JDFeedMePost> feed_posts = table.getModel().getPosts().get(feed.getUniqueid());
    		if (feed_posts != null)
    		{
    			for (JDFeedMePost feed_post : feed_posts)
    			{
    				feed_post.setNewpost(false);
    			}
    		}
    	}
    }
    
    // TODO: add locking here
    public void saveFeeds()
    {
    	JDFeedMeFeed.saveXML(table.getModel().getFeeds(), JDFeedMe.STORAGE_FEEDS);
    }
    
    public JDFeedMeConfig getConfig()
    {
    	JDFeedMeConfig result = new JDFeedMeConfig();
    	result.setSyncintervalhours(((SpinnerNumberModel)syncIntervalSpinner.getModel()).getNumber().intValue());
    	result.setStartdownloads(startDownloadsCheckbox.isSelected());
    	return result;
    }
    
    public void saveConfig()
    {
    	JDFeedMeConfig.saveXML(getConfig(), JDFeedMe.STORAGE_CONFIG);
    }
    
    // TODO: add locking here
    public void addPostToFeed(JDFeedMePost post, JDFeedMeFeed feed)
    {
    	// first check if the post was added in another feed
    	if (post.getAdded().equalsIgnoreCase(JDFeedMePost.ADDED_NO))
    	{
    		if (wasPostAddedInOtherFeed(post, feed))
    		{
    			post.setAdded(JDFeedMePost.ADDED_YES_OTHER_FEED);
    		}
    	}
    	
    	// now add the post
    	HashMap<String, ArrayList<JDFeedMePost>> posts = table.getModel().getPosts();
    	if (posts.containsKey(feed.getUniqueid()))
    	{
    		// we have an array for this feed, add to it
    		ArrayList<JDFeedMePost> feed_posts = posts.get(feed.getUniqueid());
    		feed_posts.add(post);
    		// make sure the posts are sorted (according to timestamp)
    		Collections.sort(feed_posts);
    		// remove the oldest post if needed
    		if (feed_posts.size() > JDFeedMe.MAX_POSTS) feed_posts.remove(feed_posts.size()-1);
    	}
    	else
    	{
    		// a new feed, let's create an array
    		ArrayList<JDFeedMePost> feed_posts = new ArrayList<JDFeedMePost>();
    		feed_posts.add(post);
    		posts.put(feed.getUniqueid(), feed_posts);
    	}
    }
    
    // TODO: add locking here
    public boolean wasPostAddedInOtherFeed(JDFeedMePost post, JDFeedMeFeed feed)
    {
    	// go over all other feeds
    	HashMap<String, ArrayList<JDFeedMePost>> posts = table.getModel().getPosts();
    	for (final String post_uniqueid : posts.keySet())
    	{
    		// ignore our own feed
    		if (post_uniqueid.equalsIgnoreCase(feed.getUniqueid())) continue;
    		
    		// go over all posts in the other feed
    		ArrayList<JDFeedMePost> other_posts = posts.get(post_uniqueid);
    		for (JDFeedMePost other_post : other_posts)
    		{
    			// ignore other posts with different links
    			if (!post.getLink().equalsIgnoreCase(other_post.getLink())) continue;
    			
    			// if here, then we found an identical link in another post
    			if (other_post.getAdded().equalsIgnoreCase(JDFeedMePost.ADDED_YES)) return true;
    		}
    	}
    	
    	return false;
    }
    
    // TODO: add locking here
    public void notifyPostAddedInOtherFeed(JDFeedMePost post, JDFeedMeFeed feed)
    {
    	// make sure post was indeed added
    	if (!post.getAdded().equalsIgnoreCase(JDFeedMePost.ADDED_YES)) return;
    	
    	// go over all other feeds
    	HashMap<String, ArrayList<JDFeedMePost>> posts = table.getModel().getPosts();
    	for (final String post_uniqueid : posts.keySet())
    	{
    		// ignore our own feed
    		if (post_uniqueid.equalsIgnoreCase(feed.getUniqueid())) continue;
    		
    		// go over all posts in the other feed
    		ArrayList<JDFeedMePost> other_posts = posts.get(post_uniqueid);
    		for (JDFeedMePost other_post : other_posts)
    		{
    			// ignore other posts with different links
    			if (!post.getLink().equalsIgnoreCase(other_post.getLink())) continue;
    			
    			// if here, then we found an identical link in another post
    			if (other_post.getAdded().equalsIgnoreCase(JDFeedMePost.ADDED_NO))
    			{
    				other_post.setAdded(JDFeedMePost.ADDED_YES_OTHER_FEED);
    			}
    		}
    	}
    }
    
    // TODO: add locking here
    public void clearPostsFromFeed(JDFeedMeFeed feed)
    {
    	HashMap<String, ArrayList<JDFeedMePost>> posts = table.getModel().getPosts();
    	if (posts.containsKey(feed.getUniqueid()))
    	{
    		posts.remove(feed.getUniqueid());
    	}
    }
    
    // TODO: add locking here
    public void savePosts()
    {
    	JDFeedMePost.saveXML(table.getModel().getPosts(), JDFeedMe.STORAGE_POSTS);
    }
    
    @Override
    protected void onHide() {
        
    	/// stop using config and use XML instead
    	//config.setProperty(JDFeedMe.PROPERTY_SETTINGS, table.getModel().getSettings());
        //config.save();
        saveFeeds();
        saveConfig();
        savePosts();
    }

    @Override
    protected void onShow() {
    	
    	/// stop using config and use XML instead
        //table.getModel().setSettings(config.getGenericProperty(JDFeedMe.PROPERTY_SETTINGS, new ArrayList<JDFeedMeSetting>()));
    	table.getModel().setFeeds(JDFeedMeFeed.loadXML(JDFeedMe.STORAGE_FEEDS));
        
        table.getModel().refreshModel();
        table.getModel().fireTableDataChanged();
    }

    public void keyPressed(KeyEvent e) {
        table.getModel().fireTableDataChanged();
    }

    public void keyReleased(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {
    }
    
    public void actionPerformed(ActionEvent e) {
    }

}
