package jd.gui.skins.simple.components.Linkgrabber;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Vector;

import jd.OptionalPluginWrapper;
import jd.config.Configuration;
import jd.config.Property;
import jd.gui.skins.simple.components.ComboBrowseFile;
import jd.nutils.io.JDIO;
import jd.plugins.DownloadLink;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class LinkGrabberV2FilePackage extends Property {

    /**
     * 
     */
    private static final long serialVersionUID = 5865820033205069205L;
    private ComboBrowseFile brwSaveTo;
    private String downloadDirectory;
    private Vector<DownloadLink> downloadLinks = new Vector<DownloadLink>();
    private String name = "";
    private boolean extractAfterDownload = true;
    private boolean useSubDir = true;
    private String comment = "";
    private String password = "";
    private long size = -1;

    private UpdateBroadcaster upc = new UpdateBroadcaster();
    private long lastSizeCalc = 0;
    private String dlpassword = "";
    private boolean lastSort = false;

    public LinkGrabberV2FilePackage() {
        downloadDirectory = JDUtilities.getConfiguration().getDefaultDownloadDirectory();
        name = JDUtilities.removeEndingPoints(JDLocale.L("controller.packages.defaultname", "various"));
        useSubDir = JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_PACKETNAME_AS_SUBFOLDER, false);
        for (OptionalPluginWrapper wrapper : OptionalPluginWrapper.getOptionalWrapper()) {
            if (wrapper.isEnabled() && wrapper.getPlugin().getClass().getName().endsWith("JDUnrar")) {
                extractAfterDownload = wrapper.getPluginConfig().getBooleanProperty("ACTIVATED", true);
            }
        }
    }

    public LinkGrabberV2FilePackage(String name, UpdateListener listener) {
        this(name);
        upc.addUpdateListener(listener);
    }

    public synchronized long getDownloadSize() {
        if (System.currentTimeMillis() - lastSizeCalc < 5000) return size;
        long newsize = 0;
        synchronized (downloadLinks) {
            for (DownloadLink element : downloadLinks) {
                newsize += element.getDownloadSize();
            }
        }
        lastSizeCalc = System.currentTimeMillis();
        size = newsize;
        return size;
    }

    public UpdateBroadcaster getUpdateBroadcaster() {
        return upc;
    }

    public String getDownloadDirectory() {
        return downloadDirectory;
    }

    public void setUseSubDir(boolean b) {
        useSubDir = b;
        upc.fireUpdateEvent(new UpdateEvent(this, UpdateEvent.UPDATE_EVENT));
    }

    public boolean useSubDir() {
        return useSubDir;
    }

    public void setDownloadDirectory(String dir) {
        downloadDirectory = dir;
        upc.fireUpdateEvent(new UpdateEvent(this, UpdateEvent.UPDATE_EVENT));
    }

    public LinkGrabberV2FilePackage(String name) {
        this();
        this.setName(name);
    }

    public String getName() {
        return name;
    }

    public ComboBrowseFile getComboBrowseFile() {
        return brwSaveTo;
    }

    public void add(DownloadLink link) {
        synchronized (downloadLinks) {
            if (!downloadLinks.contains(link)) {
                downloadLinks.add(link);
            }
        }
    }

    public void updateData() {
        synchronized (downloadLinks) {
            String password = this.password;
            StringBuilder comment = new StringBuilder(this.comment);

            String[] pws = JDUtilities.passwordStringToArray(password);
            Vector<String> pwList = new Vector<String>();
            for (String element : pws) {
                pwList.add(element);
            }

            Vector<String> dlpwList = new Vector<String>();

            for (DownloadLink element : downloadLinks) {
                pws = JDUtilities.passwordStringToArray(element.getSourcePluginPassword());

                String dlpw = element.getStringProperty("pass", null);
                if (dlpw != null && !dlpwList.contains(dlpw)) dlpwList.add(dlpw);
                for (String element2 : pws) {
                    if (pwList.indexOf(element2) < 0) {
                        pwList.add(element2);
                    }
                }

                String newComment = element.getSourcePluginComment();
                if (newComment != null && comment.indexOf(newComment) < 0) {
                    comment.append("|");
                    comment.append(newComment);
                }
            }

            String cmt = comment.toString();
            if (cmt.startsWith("|")) {
                cmt = cmt.substring(1);
            }
            this.comment = cmt;
            this.password = JDUtilities.passwordArrayToString(pwList.toArray(new String[pwList.size()]));
            this.dlpassword = JDUtilities.passwordArrayToString(dlpwList.toArray(new String[dlpwList.size()]));
        }
    }

    public void add(int index, DownloadLink link) {
        synchronized (downloadLinks) {
            if (downloadLinks.contains(link)) {
                downloadLinks.remove(link);
            }
            downloadLinks.add(index, link);
        }
    }

    public void addAll(Vector<DownloadLink> links) {
        synchronized (downloadLinks) {
            for (int i = 0; i < links.size(); i++) {
                add(links.get(i));
            }
        }
    }

    public boolean isExtractAfterDownload() {
        return extractAfterDownload;
    }

    public void setExtractAfterDownload(boolean extractAfterDownload) {
        this.extractAfterDownload = extractAfterDownload;
        upc.fireUpdateEvent(new UpdateEvent(this, UpdateEvent.UPDATE_EVENT));
    }

    public void addAllAt(Vector<DownloadLink> links, int index) {
        for (int i = 0; i < links.size(); i++) {
            add(index + i, links.get(i));
        }
    }

    public boolean contains(DownloadLink link) {
        synchronized (downloadLinks) {
            return downloadLinks.contains(link);
        }
    }

    public DownloadLink get(int index) {
        synchronized (downloadLinks) {
            return downloadLinks.get(index);
        }
    }

    public Vector<DownloadLink> getDownloadLinks() {
        synchronized (downloadLinks) {
            return downloadLinks;
        }
    }

    public String getPassword() {
        updateData();
        return password;
    }

    public String getDLPassword() {
        updateData();
        return dlpassword;
    }

    public String getComment() {
        updateData();
        return comment;
    }

    public boolean remove(DownloadLink link) {
        synchronized (downloadLinks) {
            boolean ret = downloadLinks.remove(link);
            if (downloadLinks.size() == 0) upc.fireUpdateEvent(new UpdateEvent(this, UpdateEvent.EMPTY_EVENT));
            return ret;
        }
    }

    public DownloadLink remove(int index) {
        synchronized (downloadLinks) {
            DownloadLink link = downloadLinks.remove(index);
            if (downloadLinks.size() == 0) upc.fireUpdateEvent(new UpdateEvent(this, UpdateEvent.EMPTY_EVENT));
            return link;
        }
    }

    public void setComment(String comment) {
        if (comment == null) comment = "";
        this.comment = comment;
        upc.fireUpdateEvent(new UpdateEvent(this, UpdateEvent.UPDATE_EVENT));
    }

    public void setDLPassword(String pass) {
        if (pass == null) pass = "";
        this.dlpassword = pass;
        upc.fireUpdateEvent(new UpdateEvent(this, UpdateEvent.UPDATE_EVENT));
    }

    public void setDownloadLinks(Vector<DownloadLink> downloadLinks) {
        synchronized (downloadLinks) {
            this.downloadLinks = new Vector<DownloadLink>(downloadLinks);
            if (downloadLinks.size() == 0) upc.fireUpdateEvent(new UpdateEvent(this, UpdateEvent.EMPTY_EVENT));
        }
    }

    public void setName(String name) {
        if (name == null || name.length() == 0) {
            this.name = JDUtilities.removeEndingPoints(JDLocale.L("controller.packages.defaultname", "various"));
        } else
            this.name = JDUtilities.removeEndingPoints(JDIO.validateFileandPathName(name));
        upc.fireUpdateEvent(new UpdateEvent(this, UpdateEvent.UPDATE_EVENT));
    }

    public void setPassword(String password) {
        if (password == null) password = "";
        this.password = password;
        upc.fireUpdateEvent(new UpdateEvent(this, UpdateEvent.UPDATE_EVENT));
    }

    public int size() {
        synchronized (downloadLinks) {
            return downloadLinks.size();
        }
    }

    public void sort(final int col) {
        if (!(col >= 1 && col <= 3)) return;
        lastSort = !lastSort;
        synchronized (downloadLinks) {

            Collections.sort(downloadLinks, new Comparator<DownloadLink>() {

                public int compare(DownloadLink a, DownloadLink b) {
                    if (a.getName().endsWith(".sfv")) { return -1; }
                    if (b.getName().endsWith(".sfv")) { return 1; }
                    DownloadLink aa = a;
                    DownloadLink bb = b;
                    if (lastSort) {
                        aa = b;
                        bb = a;
                    }
                    switch (col) {
                    case 1:
                        return aa.getName().compareToIgnoreCase(bb.getName());
                    case 2:
                        return aa.getDownloadSize() > bb.getDownloadSize() ? 1 : -1;
                    case 3:
                        return aa.getHost().compareToIgnoreCase(bb.getHost());
                    default:
                        return -1;
                    }

                }

            });
        }
    }

    public String getHoster() {
        LinkedList<DownloadLink> dLinks = new LinkedList<DownloadLink>(downloadLinks);
        StringBuilder hoster = new StringBuilder();
        String curHost;
        while (!dLinks.isEmpty()) {
            curHost = dLinks.removeFirst().getHost();
            if (hoster.length() > 0) hoster.append(", ");
            hoster.append(curHost);
            for (int i = dLinks.size() - 1; i >= 0; --i) {
                if (dLinks.get(i).getHost().equals(curHost)) dLinks.remove(i);
            }
        }
        return hoster.toString();
    }

}
