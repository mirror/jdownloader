package jd.plugins.optional.jdfeedme;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JFrame;

import jd.HostPluginWrapper;
import jd.Main;
import jd.config.ConfigContainer;
import jd.controlling.AccountController;
import jd.controlling.DistributeData;
import jd.controlling.DownloadWatchDog;
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.controlling.LinkGrabberController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.interfaces.SwitchPanelEvent;
import jd.gui.swing.jdgui.interfaces.SwitchPanelListener;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.AddonPanel;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.optional.ExtensionConfigPanel;
import jd.plugins.optional.PluginOptional;
import jd.plugins.optional.interfaces.RemoteSupport;
import jd.plugins.optional.jdfeedme.posts.JDFeedMePost;
import jd.plugins.optional.remotecontrol.helppage.HelpPage;
import jd.plugins.optional.remotecontrol.helppage.Table;

import org.appwork.shutdown.ShutdownVetoException;
import org.appwork.utils.Regex;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;

// notes: this module lets you download content automatically from RSS feeds
// the module was developed after stable JDownloader 0.9.580 was released (SVN revision 9580)
// since we wanted to release both a version for stable and a version for nightly (latest beta)
// the code supports both
// if you want to compile a version for 9580 (interface version 5), change the following comments:
// enable CODE_FOR_INTERFACE_5-START-END and disable CODE_FOR_INTERFACE_7-START-END
// don't forget to change interface version from 7 to 5

public class FeedMeExtension extends PluginOptional implements RemoteSupport, ActionListener, ControlListener {
    // / stop using config and use XML instead
    // public static final String PROPERTY_SETTINGS = "FEEDS";
    public static final String     STORAGE_FEEDS  = "cfg/jdfeedme/feeds.xml";
    public static final String     STORAGE_CONFIG = "cfg/jdfeedme/config.xml";
    public static final String     STORAGE_POSTS  = "cfg/jdfeedme/posts.xml";

    public static final int        MAX_POSTS      = 100;                      // max
    // number
    // of
    // last
    // posts
    // we
    // save
    // per
    // feed
    // (history)
    public static final boolean    VERBOSE        = false;                    // should
    // we
    // spit
    // up
    // lots
    // of
    // log
    // messages

    private JDFeedMeView           view;
    private JDFeedMeGui            gui            = null;
    private MenuAction             showAction;

    private static FeedMeExtension INSTANCE       = null;
    private static JDFeedMeThread  thread         = null;
    private boolean                running        = false;

    public ExtensionConfigPanel getConfigPanel() {
        return null;
    }

    public boolean hasConfigPanel() {
        return false;
    }

    public FeedMeExtension() throws StartException {
        super("Feedme RSS Reader");
        INSTANCE = this;
        initConfig();
    }

    public static JDFeedMeGui getGui() {
        if (INSTANCE == null) return null;
        return INSTANCE.gui;
    }

    public static void syncNowEvent() {
        if (thread != null && thread.isSleeping()) thread.interrupt();
    }

    public void initConfig() {
        /*
         * SubConfiguration subConfig = getPluginConfig(); ConfigEntry ce;
         * 
         * config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX,
         * subConfig, "Helo", "Delete archive after merging"));
         * ce.setDefaultValue(true); config.addEntry(ce = new
         * ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, "Helo2",
         * "Overwrite existing files")); ce.setDefaultValue(true);
         */
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == showAction) {
            /* save current selection state */
            getPluginConfig().setProperty("selected", showAction.isSelected());
            getPluginConfig().save();
            if (showAction.isSelected()) {
                showGui();
            } else {
                if (view != null) view.close();
            }
        }
    }

    private void showGui() {
        if (view == null) {
            view = new JDFeedMeView();
            view.getBroadcaster().addListener(new SwitchPanelListener() {

                @Override
                public void onPanelEvent(SwitchPanelEvent event) {

                    /*
                     * CODE_FOR_INTERFACE_5_START if (event.getID() ==
                     * SwitchPanelEvent.ON_REMOVE)
                     * showAction.setSelected(false); CODE_FOR_INTERFACE_5_END
                     */
                    /* CODE_FOR_INTERFACE_7_START */
                    if (event.getEventID() == SwitchPanelEvent.ON_REMOVE) showAction.setSelected(false);
                    /* CODE_FOR_INTERFACE_7_END */

                }

            });

            // gui = new JDFeedMeGui(getPluginConfig());
            gui = new JDFeedMeGui();

            view.setContent(gui);
        }
        showAction.setSelected(true);
        JDGui.getInstance().setContent(view);
    }

    public void setGuiEnable(boolean b) {
        if (b) {
            showGui();
        } else {
            if (view != null) view.close();
        }
    }

    @Override
    public String getIconKey() {

        /*
         * CODE_FOR_INTERFACE_5_START return "gui.images.reconnect";
         * CODE_FOR_INTERFACE_5_END
         */
        /* CODE_FOR_INTERFACE_7_START */
        return "gui.images.rss";
        /* CODE_FOR_INTERFACE_7_END */

    }

    private void syncRss() {
        // get the feed list
        ArrayList<JDFeedMeFeed> feeds = gui.getFeeds();

        // go over all the feeds
        for (JDFeedMeFeed feed : feeds) {
            if (!feed.isEnabled()) continue;
            String feed_address = feed.getAddress();
            if (feed_address.length() == 0) continue;

            // sync each feed
            String response = "not downloaded yet";
            try {
                // get the feed last update timestamp
                String timestamp = null;
                if ((feed.getTimestamp() != null) && (feed.getTimestamp().length() > 0)) timestamp = feed.getTimestamp();

                logger.info("JDFeedMe syncing feed: " + feed_address + " [" + timestamp + "]");
                gui.setFeedStatus(feed, JDFeedMeFeed.STATUS_RUNNING);

                // get the feed content from the web
                Browser browser = new Browser();
                browser.setFollowRedirects(true); // support redirects since
                // some feeds have them
                response = browser.getPage(feed_address);

                // parse the feed
                String new_timestamp = parseFeed(feed, timestamp, response);

                // set the new timestamp if needed
                if ((new_timestamp != null) && (new_timestamp != timestamp)) {
                    gui.setFeedTimestamp(feed, new_timestamp);
                }

                // all ok
                gui.setFeedStatus(feed, JDFeedMeFeed.STATUS_OK);
            } catch (Exception e) {
                // shorten the response so we can show it in the log
                int max_reponse_length = 1000;
                String response_short = response;
                if (response_short != null) {
                    if (response_short.length() > max_reponse_length) {
                        response_short = response_short.substring(0, max_reponse_length / 2) + "..." + response_short.substring(response_short.length() - max_reponse_length / 2);
                    }
                }

                e.printStackTrace();
                logger.severe("JDFeedMe cannot sync feed: " + feed_address + " (" + e.toString() + "), feed response: " + response_short);
                gui.setFeedStatus(feed, JDFeedMeFeed.STATUS_ERROR);
            }
        }

        // save our xml with any updates from what we just downloaded
        gui.saveFeeds(); // maybe do this only if we had updates
        gui.savePosts(); // maybe do this only if we had updates
    }

    private String parseFeed(JDFeedMeFeed feed, String timestamp, String content) throws Exception {
        String new_timestamp = timestamp;
        boolean found_new_posts = false;

        // parse the rss xml
        RssParser feed_parser = new RssParser(feed);
        feed_parser.parseContent(content);
        int feed_item_number = 0;
        JDFeedMePost post = null;
        while ((post = feed_parser.getPost()) != null) {
            feed_item_number++;

            // get the original description
            String post_description = post.getDescription();

            // originally we removed the description from the posts since posts
            // are saved in xml and this could become large
            // new feature: let's do save the description, but make it somewhat
            // shorter (extract links from it)
            post.setDescription(extractLinksFromHtml(post_description, JDFeedMeFeed.HOSTER_ANY_HOSTER, JDFeedMeFeed.HOSTER_EXCLUDE));

            // handle the rss item
            if (post.isValid()) {
                boolean is_new = handlePost(feed, post, post_description, timestamp);
                if (is_new) found_new_posts = true;
                if (post.isTimestampNewer(new_timestamp)) new_timestamp = post.getTimestamp();
            } else {
                logger.severe("JDFeedMe rss item " + Integer.toString(feed_item_number) + " is invalid for feed: " + feed.getAddress());
            }
        }

        // if found new posts, update the feed
        if (found_new_posts) gui.setFeedNewposts(feed, true);

        return new_timestamp;
    }

    // return true if the post is new, false if old
    private boolean handlePost(JDFeedMeFeed feed, JDFeedMePost post, String post_description, String timestamp) {
        // make sure this rss item is indeed newer than what we have
        if (post.isTimestampNewer(timestamp)) {
            if (FeedMeExtension.VERBOSE) logger.info("JDFeedMe found new item with timestamp: [" + post.getTimestamp() + "]");
            post.setNewpost(true);

            // check for filters on this item (see if it passes download
            // filters)
            boolean need_to_add = runFilterOnPost(feed, post, post_description);

            // if we don't need to add, we can return now
            if (!need_to_add) {
                if (FeedMeExtension.VERBOSE) logger.info("JDFeedMe new item title: [" + post.getTitle() + "] description: [" + post_description + "] did not pass filters");
                post.setAdded(JDFeedMePost.ADDED_NO);
            } else {
                // start processing this rss item - we're going to download it
                downloadPost(feed, post, post_description);
            }

            // update the post history
            gui.addPostToFeed(post, feed);

            return true;
        } else {
            if (FeedMeExtension.VERBOSE) logger.info("JDFeedMe ignoring item with old timestamp: [" + post.getTimestamp() + "]");
            return false;
        }
    }

    // downloads a post in a new thread, since post may be updated after,
    // optionally receives the table that shows it
    public static void downloadPostThreaded(final JDFeedMeFeed feed, final JDFeedMePost post, final String post_description, final JDTableModel table) {
        // make sure we have an active instance
        if (INSTANCE == null) return;

        new Thread() {
            public void run() {
                INSTANCE.downloadPost(feed, post, post_description);

                // since post was probably updates, show changes in the table
                if (table != null) {
                    new GuiRunnable<Object>() {

                        @Override
                        public Object runSave() {

                            table.refreshModel();
                            table.fireTableDataChanged();
                            return null;

                        }
                    }.start();
                }
            }
        }.start();
    }

    public void downloadPost(JDFeedMeFeed feed, JDFeedMePost post, String post_description) {
        String link_list_to_download = null;

        // check if we have a valid files field
        if ((link_list_to_download == null) && (post.hasValidFiles())) {
            // just take the files field
            link_list_to_download = extractLinksFromHtml(post.getFiles(), feed.getHoster(), JDFeedMeFeed.HOSTER_EXCLUDE);
        }

        // no file fields, maybe we have a good link
        if ((link_list_to_download == null) && (post.hasValidLink())) {
            // try to follow this link
            try {
                Browser browser = new Browser();
                browser.setFollowRedirects(true); // support redirects since
                // some feeds have them
                String response = browser.getPage(post.getLink());
                link_list_to_download = extractLinksFromHtml(response, feed.getHoster(), JDFeedMeFeed.HOSTER_EXCLUDE);
            } catch (Exception e) {
                logger.severe("JDFeedMe could not follow feed item link: " + post.getLink());
            }
        }

        // no good link, try the description
        if ((link_list_to_download == null) && (post_description != null) && (post_description.trim().length() > 0)) {
            link_list_to_download = extractLinksFromHtml(post_description, feed.getHoster(), JDFeedMeFeed.HOSTER_EXCLUDE);
        }

        // nothing, exit
        if (link_list_to_download == null) {
            post.setAdded(JDFeedMePost.ADDED_YES_NO_FILES);
            return;
        }

        // JOptionPane.showMessageDialog(new JFrame(),
        // "JDFeedMe says we need to download link: "+rssitem.link);
        logger.info("JDFeedMe attempting to download: " + link_list_to_download);

        // let's download the links.. finally..
        boolean anything_downloaded = downloadLinks(link_list_to_download, feed, post);
        if (anything_downloaded) {
            post.setAdded(JDFeedMePost.ADDED_YES);
            gui.notifyPostAddedInOtherFeed(post, feed);
        } else
            post.setAdded(JDFeedMePost.ADDED_YES_NO_FILES);
    }

    // returns whether this item needs to be added (passes filters) or not
    private boolean runFilterOnPost(JDFeedMeFeed feed, JDFeedMePost post, String post_description) {
        // see if we even need to run filters
        if (!feed.getDoFilters()) return true;

        // let's run our filters
        if (feed.getFiltersearchtitle() || feed.getFiltersearchdesc()) {
            // we need to check something, prepare the checker
            FilterChecker filter = new FilterChecker(feed.getFilters());

            // check title if needed
            if (feed.getFiltersearchtitle()) {
                if (filter.match(post.getTitle())) {
                    if (FeedMeExtension.VERBOSE) logger.info("JDFeedMe new item title: [" + post.getTitle() + "] passed filters");
                    return true;
                }
            }

            // check description if needed
            if (feed.getFiltersearchdesc()) {
                if (filter.match(post_description)) {
                    if (FeedMeExtension.VERBOSE) logger.info("JDFeedMe new item description: [" + post_description + "] passed filters");
                    return true;
                }
            }
        }

        // if here then nothing passed
        return false;
    }

    // this function takes a page containing links or a description and cleans
    // it so only interesting file links remain
    private String extractLinksFromHtml(String html, String limit_to_host, String[] exclude_hosters) {
        String result = "";

        // get all links in the html

        // there is a bug in HTMLParser.getHttpLinks() - it does not handle
        // <br/> correctly, so we must take action ourselves
        html = html.replace("http://", " http://");

        // use HTMLParser.getHttpLinks to get links, maybe make our own link
        // extractor some day
        String[] links = HTMLParser.getHttpLinks(html, null);

        // go over all links
        for (String link : links) {
            // go over all host plugins
            try {
                // HostPluginWrapper.readLock.lock();
                for (final HostPluginWrapper pHost : HostPluginWrapper.getHostWrapper()) {
                    // make sure this hoster is enabled
                    if (!pHost.isEnabled()) continue;

                    // check if we need to ignore certain hosters
                    if (exclude_hosters != null) {
                        boolean in_ignore_list = false;
                        for (final String exclude_hoster : exclude_hosters) {
                            if (pHost.getHost().equalsIgnoreCase(exclude_hoster)) in_ignore_list = true;
                        }
                        if (in_ignore_list) continue;
                    }

                    // check if we limit to certain hosters
                    if ((limit_to_host != null) && (!limit_to_host.equals(JDFeedMeFeed.HOSTER_ANY_HOSTER))) {
                        if (limit_to_host.equals(JDFeedMeFeed.HOSTER_ANY_PREMIUM)) {
                            // make sure we have a premium account for this
                            // hoster

                            if (!pHost.isPremiumEnabled()) continue;
                            Account account = AccountController.getInstance().getValidAccount(pHost.getPlugin());
                            if (account == null) continue;
                        } else // regular hoster limit
                        {
                            if (!pHost.getHost().equalsIgnoreCase(limit_to_host)) continue;
                        }
                    }
                    if (!pHost.canHandle(link)) continue;

                    // if here than this link is ok
                    result += link + "\r\n";
                }
            } finally {
                // HostPluginWrapper.readLock.unlock();
            }
        }

        return result;
    }

    // returns true if downloaded something, false if didn't
    // old implementation - using DistributeDate, works ok, but no control over
    // package name
    @SuppressWarnings("unused")
    private boolean downloadLinksOld(String linksText, boolean skipGrabber) {
        // make sure we have something to download
        if (linksText.trim().length() == 0) return false;

        // lots of cool code in
        // jd/plugins/optional/remotecontrol/Serverhandler.java

        // LinkGrabberController.getInstance().addLinks(downloadLinks,
        // skipGrabber, autostart);
        new DistributeData(linksText, skipGrabber).start();

        // see if we need to start downloads
        if (DownloadWatchDog.getInstance().getDownloadStatus() != DownloadWatchDog.STATE.RUNNING) {
            // check if we need to start downloads right now
            if (gui.getConfig().getStartdownloads()) {
                DownloadWatchDog.getInstance().startDownloads();
            }
        }

        return true;
    }

    // returns true if downloaded something, false if didn't
    private boolean downloadLinks(String linksText, JDFeedMeFeed feed, JDFeedMePost post) {
        // make sure we have something to download
        if (linksText.trim().length() == 0) return false;

        boolean skip_grabber = feed.getSkiplinkgrabber();
        boolean autostart = gui.getConfig().getStartdownloads();

        // handle a direct download
        if (skip_grabber) {
            // get all the links from the text
            ArrayList<DownloadLink> links = new DistributeData(linksText).findLinks();

            // create a new package for the data
            FilePackage fp = FilePackage.getInstance();
            fp.setName(post.getTitle() + " [JDFeedMe]");
            fp.addLinks(links);

            // download the package
            LinkGrabberController.getInstance().addLinks(links, skip_grabber, autostart);

            // restart the downloads if needed
            if (autostart) DownloadWatchDog.getInstance().startDownloads();
        } else // throw into the link grabber using the old code
        {
            new DistributeData(linksText, skip_grabber).start();
        }

        return true;
    }

    // thread to sync the feeds periodically
    public class JDFeedMeThread extends Thread {
        private boolean sleeping = false;

        public JDFeedMeThread() {
            super("JDFeedMeThread");
        }

        public boolean isSleeping() {
            synchronized (this) {
                return sleeping;
            }
        }

        @Override
        public void run() {
            try {
                logger.info("JDFeedMe thread: started");
                while (running) {
                    // sleep for the required interval
                    synchronized (this) {
                        sleeping = true;
                    }
                    try {
                        int syncIntervalHours = 1;
                        if (gui != null) syncIntervalHours = gui.getConfig().getSyncintervalhours();
                        sleep(syncIntervalHours * 60 * 60000);
                    } catch (InterruptedException e) {
                    }
                    synchronized (this) {
                        sleeping = false;
                    }

                    // sync the rss feeds if needed
                    if (running) {
                        try {
                            new GuiRunnable<Object>() {

                                @Override
                                public Object runSave() {
                                    syncRss();
                                    return null;
                                }
                            }.start();
                        } catch (Exception e) {
                        }
                    }
                }
                logger.info("JDFeedMe thread: terminated");

            } catch (Exception e) {
                logger.severe("JDFeedMe thread: died!!");
                JDLogger.exception(e);
            }
        }
    }

    // integrate JD FeedMe commands into RemoteControl addon
    public Object handleRemoteCmd(String cmd) {
        if (cmd.matches("(?is).*/addon/feedme/action/sync")) {
            syncRss();
            return "All RSS feeds have been synced.";
        } else if (cmd.matches("(?is).*/addon/feedme/action/add(/(all|lastweek|last24hours|none))?(/(true|false))?/.+")) {
            JDFeedMeTable table = getGui().getTable();

            // defaults
            String url = "http://";
            String oldoption = JDFeedMeFeed.GET_OLD_OPTIONS_ALL;
            boolean filteroption = false;

            url = Encoding.urlDecode(new Regex(cmd, ".*/addon/feedme/action/add.*/(.+)").getMatch(0), false);

            if (cmd.matches(".*/(all|lastweek|last24hours|none)/.*")) {
                String optionstr = new Regex(cmd, ".*/(all|lastweek|last24hours|none)/.*").getMatch(0);

                for (String option : JDFeedMeFeed.GET_OLD_OPTIONS) {
                    String newstr = option.replaceAll(" ", "").toLowerCase();
                    if (newstr.equals(optionstr)) {
                        oldoption = option;
                        break;
                    }
                }
            }

            if (cmd.matches(".*/(true|false)/.*")) {
                filteroption = Boolean.parseBoolean(new Regex(cmd, ".*/(true|false)/.*").getMatch(0));
            }

            JDFeedMeFeed new_feed = new JDFeedMeFeed(url);
            new_feed.setUniqueid(JDFeedMeFeed.allocateUniqueid());
            new_feed.setTimestampFromGetOld(oldoption);
            new_feed.setDoFilters(filteroption);
            table.getModel().getFeeds().add(new_feed);
            table.getModel().refreshModel();
            table.getModel().fireTableDataChanged();

            return "RSS feed has been added.";
        } else if (cmd.matches("(?is).*/addon/feedme/action/remove")) {
            return "RSS feed has been removed.";
        } else if (cmd.matches("(?is).*/addon/feedme/action/reset")) { return "RSS feed has been resetted."; }

        return null;
    }

    // RemoteControl addon command description
    public void initCmdTable() {
        Table t = HelpPage.createTable(new Table(this.getName()));

        t.setCommand("/addon/feedme/action/sync");
        t.setInfo("Syncs all RSS feeds");

        t.setCommand("/addon/feedme/action/add/(all|lastweek|last24hours|none)/(true|false)/%X%");
        t.setInfo("Add RSS feed. Where %X% is the (urlencoded) URL. The two previous parameters are optional");

        // TODO:
        /*
         * t.setCommand("/addon/feedme/action/remove");
         * t.setInfo("Remove RSS feed");
         * 
         * t.setCommand("/addon/feedme/action/reset");
         * t.setInfo("Reset RSS feed");
         */
    }

    public void onShutdown() {
    }

    public boolean onShutdownRequest() throws ShutdownVetoException {
        return false;
    }

    public void onShutdownVeto(ArrayList<ShutdownVetoException> vetos) {
    }

    @Override
    protected void stop() throws StopException {
        running = false;
        if (thread != null && thread.isSleeping()) thread.interrupt();
        thread = null;
        logger.info("JDFeedMe has exited");
        JDController.getInstance().removeControlListener(this);
    }

    @Override
    protected void start() throws StartException {
        showAction = new MenuAction("feedme", 0);

        /*
         * CODE_FOR_INTERFACE_5_START showAction.setTitle("JD FeedMe");
         * CODE_FOR_INTERFACE_5_END
         */
        /* CODE_FOR_INTERFACE_7_START */
        showAction.putValue(javax.swing.Action.NAME, "JD FeedMe");
        /* CODE_FOR_INTERFACE_7_END */

        showAction.setIcon(this.getIconKey());
        /* restore selection state */
        showAction.setSelected(this.getPluginConfig().getBooleanProperty("selected", false));
        showAction.setActionListener(this);
        logger.info("JDFeedMe is running");
        running = true;
        thread = new JDFeedMeThread();
        thread.start();
        JDController.getInstance().addControlListener(this);
    }

    @Override
    protected void initSettings(ConfigContainer config) {
    }

    @Override
    public String getConfigID() {
        return "jdfeedme";
    }

    @Override
    public String getAuthor() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public AddonPanel getGUI() {
        return null;
    }

    @Override
    public ArrayList<MenuAction> getMenuAction() {
        // add main menu items.. this item is used to show/hide GUi
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();

        menu.add(showAction);

        return menu;
    }

    public void controlEvent(ControlEvent event) {
        /* we must wait till the gui is initiated before we can show the tab */
        if (event.getEventID() == ControlEvent.CONTROL_INIT_COMPLETE && event.getCaller() instanceof Main) {
            if (showAction.isSelected()) {
                JFrame guiFrame = JDGui.getInstance().getMainFrame();
                if (guiFrame == null) return;
                new GuiRunnable<Object>() {

                    @Override
                    public Object runSave() {
                        showGui();
                        return null;
                    }
                }.start();
            }
        }
    }

    @Override
    protected void initExtension() throws StartException {
    }

}
