package org.jdownloader.extensions.jdfeedme;

import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import jd.nutils.io.JDIO;
import jd.utils.JDUtilities;

public class JDFeedMeFeed implements Serializable {

    private static final long    serialVersionUID            = 3295435612660256840L;

    public static final String   STATUS_OK                   = "OK";
    public static final String   STATUS_ERROR                = "Error";
    public static final String   STATUS_NEW                  = "New";
    public static final String   STATUS_RUNNING              = "Running";

    public static final String   HOSTER_ANY_PREMIUM          = "My Premium";
    public static final String   HOSTER_ANY_HOSTER           = "Any Hoster";

    public static final String[] HOSTER_EXCLUDE              = { "DirectHTTP", "imagehost.org" };

    public static final String   GET_OLD_OPTIONS_ALL         = "All";
    public static final String   GET_OLD_OPTIONS_LASTWEEK    = "Last week";
    public static final String   GET_OLD_OPTIONS_LAST24HOURS = "Last 24 hours";
    public static final String   GET_OLD_OPTIONS_NONE        = "None";
    public static final String[] GET_OLD_OPTIONS             = { GET_OLD_OPTIONS_ALL, GET_OLD_OPTIONS_LASTWEEK, GET_OLD_OPTIONS_LAST24HOURS, GET_OLD_OPTIONS_NONE };

    private String               address;
    private String               description;
    private String               timestamp;
    private String               hoster;
    private String               status;
    private boolean              enabled;
    private boolean              skiplinkgrabber;
    private String               filters;
    private boolean              filtersearchtitle;
    private boolean              filtersearchdesc;
    private boolean              newposts;
    private String               uniqueid;
    private boolean              dofilters;

    public JDFeedMeFeed() {
        this.address = "";
        this.enabled = true;
        this.status = STATUS_NEW;
        this.hoster = JDFeedMeFeed.HOSTER_ANY_HOSTER;
        this.skiplinkgrabber = true;
        this.filters = "";
        this.filtersearchtitle = true;
        this.filtersearchdesc = true;
        this.newposts = false;
        this.uniqueid = "";
        this.dofilters = false;
    }

    public JDFeedMeFeed(String address) {
        this();
        this.address = address.trim().toLowerCase();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean getSkiplinkgrabber() {
        return skiplinkgrabber;
    }

    public void setSkiplinkgrabber(boolean skiplinkgrabber) {
        this.skiplinkgrabber = skiplinkgrabber;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setTimestampFromGetOld(String get_old_options) {
        if (get_old_options == null) return;

        Calendar calendar = Calendar.getInstance();
        DateFormat formatter = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z");

        if (get_old_options.equals(GET_OLD_OPTIONS_ALL)) {
            this.timestamp = null;
            return;
        }

        if (get_old_options.equals(GET_OLD_OPTIONS_NONE)) {
            this.timestamp = formatter.format(calendar.getTime());
            return;
        }

        if (get_old_options.equals(GET_OLD_OPTIONS_LASTWEEK)) {
            calendar.add(Calendar.HOUR, -24 * 7);
            this.timestamp = formatter.format(calendar.getTime());
            return;
        }

        if (get_old_options.equals(GET_OLD_OPTIONS_LAST24HOURS)) {
            calendar.add(Calendar.HOUR, -24);
            this.timestamp = formatter.format(calendar.getTime());
            return;
        }
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setHoster(String hoster) {
        this.hoster = hoster;
    }

    public String getHoster() {
        return hoster;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getUniqueid() {
        return uniqueid;
    }

    public void setUniqueid(String uniqueid) {
        this.uniqueid = uniqueid;
    }

    public String getFilters() {
        return filters;
    }

    public void setFilters(String filters) {
        this.filters = filters;
    }

    public boolean getFiltersearchtitle() {
        return filtersearchtitle;
    }

    public boolean getDoFilters() {
        return dofilters;
    }

    public void setDoFilters(boolean dofilters) {
        this.dofilters = dofilters;
    }

    public void setFiltersearchtitle(boolean filtersearchtitle) {
        this.filtersearchtitle = filtersearchtitle;
    }

    public boolean getFiltersearchdesc() {
        return filtersearchdesc;
    }

    public void setFiltersearchdesc(boolean filtersearchdesc) {
        this.filtersearchdesc = filtersearchdesc;
    }

    public boolean getNewposts() {
        return newposts;
    }

    public void setNewposts(boolean newposts) {
        this.newposts = newposts;
    }

    public static String allocateUniqueid() {
        return Long.toString(System.currentTimeMillis());
    }

    // location is something like "cfg/jdfeedme/feeds.xml"
    public synchronized static void saveXML(ArrayList<JDFeedMeFeed> settings, String location) {
        // Thread.currentThread().setContextClassLoader(JDUtilities.getJDClassLoader());

        /*
         * CODE_FOR_INTERFACE_5_START JDIO.saveObject(null, settings,
         * JDUtilities.getResourceFile(location), null, null, true);
         * CODE_FOR_INTERFACE_5_END
         */
        /* CODE_FOR_INTERFACE_7_START */
        JDIO.saveObject(settings, JDUtilities.getResourceFile(location), true);
        /* CODE_FOR_INTERFACE_7_END */

        /*
         * try { String path =
         * JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() +
         * "/" + location; File f = new File(path); f.mkdirs();
         * 
         * XMLEncoder encoder = new XMLEncoder( new BufferedOutputStream( new
         * FileOutputStream
         * (JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() +
         * "/" + location))); encoder.writeObject(settings); encoder.close(); }
         * catch (Exception e) {
         * JDLogger.getLogger().severe("JDFeedMe cannot save settings XML");
         * JDLogger.exception(e); }
         */
    }

    // location is something like "cfg/jdfeedme/feeds.xml"
    @SuppressWarnings("unchecked")
    public synchronized static ArrayList<JDFeedMeFeed> loadXML(String location) {
        // Thread.currentThread().setContextClassLoader(JDUtilities.getJDClassLoader());

        File xmlFile = JDUtilities.getResourceFile(location);
        if (xmlFile.exists()) {
            /*
             * CODE_FOR_INTERFACE_5_START Object loaded = JDIO.loadObject(null,
             * xmlFile, true); CODE_FOR_INTERFACE_5_END
             */
            /* CODE_FOR_INTERFACE_7_START */
            Object loaded = JDIO.loadObject(xmlFile, true);
            /* CODE_FOR_INTERFACE_7_END */

            if (loaded != null) return (ArrayList<JDFeedMeFeed>) loaded;
        }
        return new ArrayList<JDFeedMeFeed>();

        /*
         * try { XMLDecoder decoder = new XMLDecoder( new BufferedInputStream(
         * new FileInputStream(JDUtilities.getJDHomeDirectoryFromEnvironment().
         * getAbsolutePath() + "/" + location))); ArrayList<JDFeedMeSetting>
         * settings = (ArrayList<JDFeedMeSetting>)decoder.readObject();
         * decoder.close(); return settings; } catch (Exception e) {
         * JDLogger.getLogger().severe("JDFeedMe cannot load settings XML");
         * JDLogger.exception(e); }
         * 
         * // if here then we were unable to load return new
         * ArrayList<JDFeedMeSetting>();
         */
    }
}
