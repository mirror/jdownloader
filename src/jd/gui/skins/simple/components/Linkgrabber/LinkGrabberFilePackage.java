package jd.gui.skins.simple.components.Linkgrabber;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import jd.OptionalPluginWrapper;
import jd.config.Configuration;
import jd.config.Property;
import jd.controlling.LinkGrabberController;
import jd.event.JDBroadcaster;
import jd.gui.skins.simple.components.ComboBrowseFile;
import jd.nutils.io.JDIO;
import jd.plugins.DownloadLink;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

class LinkGrabberFilePackageBroadcaster extends JDBroadcaster<LinkGrabberFilePackageListener, LinkGrabberFilePackageEvent> {

    @Override
    protected void fireEvent(LinkGrabberFilePackageListener listener, LinkGrabberFilePackageEvent event) {
        listener.handle_LinkGrabberFilePackageEvent(event);
    }

}

public class LinkGrabberFilePackage extends Property {

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

    private long lastSizeCalc = 0;
    private String dlpassword = "";
    private boolean lastSort = false;
    private int lastfail = 0;
    private long lastFailCount = 0;
    private transient LinkGrabberFilePackageBroadcaster broadcaster = new LinkGrabberFilePackageBroadcaster();

    public LinkGrabberFilePackage() {
        downloadDirectory = JDUtilities.getConfiguration().getDefaultDownloadDirectory();
        name = JDUtilities.removeEndingPoints(JDLocale.L("controller.packages.defaultname", "various"));
        useSubDir = JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_PACKETNAME_AS_SUBFOLDER, false);
        for (OptionalPluginWrapper wrapper : OptionalPluginWrapper.getOptionalWrapper()) {
            if (wrapper.isEnabled() && wrapper.getPlugin().getClass().getName().endsWith("JDUnrar")) {
                extractAfterDownload = wrapper.getPluginConfig().getBooleanProperty("ACTIVATED", true);
            }
        }
    }

    public synchronized JDBroadcaster<LinkGrabberFilePackageListener, LinkGrabberFilePackageEvent> getBroadcaster() {
        if (broadcaster == null) broadcaster = new LinkGrabberFilePackageBroadcaster();
        return this.broadcaster;
    }

    public LinkGrabberFilePackage(String name, LinkGrabberFilePackageListener listener) {
        this(name);
        getBroadcaster().addListener(listener);
    }

    public synchronized long getDownloadSize(boolean forceUpdate) {
        if (!forceUpdate && System.currentTimeMillis() - lastSizeCalc < 5000) return size;
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

    public synchronized int countFailedLinks(boolean forceUpdate) {
        if (!forceUpdate && System.currentTimeMillis() - lastFailCount < 5000) return lastfail;
        int newfail = 0;
        synchronized (downloadLinks) {
            for (DownloadLink dl : downloadLinks) {
                if ((dl.isAvailabilityChecked() && !dl.isAvailable())) {
                    newfail++;
                }
            }
        }
        lastFailCount = System.currentTimeMillis();
        lastfail = newfail;
        return lastfail;
    }

    public void keepHostersOnly(Set<String> hoster) {
        Vector<DownloadLink> remove = new Vector<DownloadLink>();
        synchronized (downloadLinks) {
            for (DownloadLink dl : downloadLinks) {
                if (!hoster.contains(dl.getPlugin().getHost())) remove.add(dl);
            }
            this.remove(remove);
            countFailedLinks(true);
        }
    }

    public int indexOf(DownloadLink link) {
        synchronized (downloadLinks) {
            return downloadLinks.indexOf(link);
        }
    }

    public String getDownloadDirectory() {
        return downloadDirectory;
    }

    public void setUseSubDir(boolean b) {
        useSubDir = b;
        getBroadcaster().fireEvent(new LinkGrabberFilePackageEvent(this, LinkGrabberFilePackageEvent.UPDATE_EVENT));
    }

    public boolean useSubDir() {
        return useSubDir;
    }

    public void setDownloadDirectory(String dir) {
        downloadDirectory = dir;
        getBroadcaster().fireEvent(new LinkGrabberFilePackageEvent(this, LinkGrabberFilePackageEvent.UPDATE_EVENT));
    }

    public LinkGrabberFilePackage(String name) {
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
                LinkGrabberFilePackage fp = LinkGrabberController.getInstance().getFPwithLink(link);                
                downloadLinks.add(link);
                getBroadcaster().fireEvent(new LinkGrabberFilePackageEvent(this, LinkGrabberFilePackageEvent.ADD_LINK));
                if (fp != null && fp != this) fp.remove(link);
            }
        }
    }

    public void removeOffline() {
        Vector<DownloadLink> remove = new Vector<DownloadLink>();
        synchronized (downloadLinks) {
            for (DownloadLink dl : downloadLinks) {
                if ((dl.isAvailabilityChecked() && !dl.isAvailable())) {
                    remove.add(dl);
                }
            }
            this.remove(remove);
            countFailedLinks(true);
        }
    }

    public void remove(Vector<DownloadLink> links) {
        synchronized (downloadLinks) {
            for (DownloadLink dl : links) {
                this.remove(dl);
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
                if (index > downloadLinks.size() - 1) {
                    downloadLinks.add(link);
                } else if (index < 0) {
                    downloadLinks.add(0, link);
                } else
                    downloadLinks.add(index, link);
                getBroadcaster().fireEvent(new LinkGrabberFilePackageEvent(this, LinkGrabberFilePackageEvent.UPDATE_EVENT));
            } else {
                LinkGrabberFilePackage fp = LinkGrabberController.getInstance().getFPwithLink(link);
                if (index > downloadLinks.size() - 1) {
                    downloadLinks.add(link);
                } else if (index < 0) {
                    downloadLinks.add(0, link);
                } else
                    downloadLinks.add(index, link);                
                getBroadcaster().fireEvent(new LinkGrabberFilePackageEvent(this, LinkGrabberFilePackageEvent.ADD_LINK));
                if (fp != null && fp != this) fp.remove(link);
            }
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
        getBroadcaster().fireEvent(new LinkGrabberFilePackageEvent(this, LinkGrabberFilePackageEvent.UPDATE_EVENT));
    }

    public void addAllAt(Vector<DownloadLink> links, int index) {
        synchronized (downloadLinks) {
            for (int i = 0; i < links.size(); i++) {
                add(index + i, links.get(i));
            }
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
            if (ret) getBroadcaster().fireEvent(new LinkGrabberFilePackageEvent(this, LinkGrabberFilePackageEvent.REMOVE_LINK));
            if (downloadLinks.size() == 0) getBroadcaster().fireEvent(new LinkGrabberFilePackageEvent(this, LinkGrabberFilePackageEvent.EMPTY_EVENT));
            return ret;
        }
    }

    public DownloadLink remove(int index) {
        synchronized (downloadLinks) {
            DownloadLink link = downloadLinks.remove(index);
            if (link != null) getBroadcaster().fireEvent(new LinkGrabberFilePackageEvent(this, LinkGrabberFilePackageEvent.REMOVE_LINK));
            if (downloadLinks.size() == 0) getBroadcaster().fireEvent(new LinkGrabberFilePackageEvent(this, LinkGrabberFilePackageEvent.EMPTY_EVENT));
            return link;
        }
    }

    public void setComment(String comment) {
        if (comment == null) comment = "";
        this.comment = comment;
        getBroadcaster().fireEvent(new LinkGrabberFilePackageEvent(this, LinkGrabberFilePackageEvent.UPDATE_EVENT));
    }

    public void setDLPassword(String pass) {
        if (pass == null) pass = "";
        this.dlpassword = pass;
        getBroadcaster().fireEvent(new LinkGrabberFilePackageEvent(this, LinkGrabberFilePackageEvent.UPDATE_EVENT));
    }

    public void setDownloadLinks(Vector<DownloadLink> downloadLinks) {
        synchronized (downloadLinks) {
            this.downloadLinks = new Vector<DownloadLink>(downloadLinks);
            getBroadcaster().fireEvent(new LinkGrabberFilePackageEvent(this, LinkGrabberFilePackageEvent.UPDATE_EVENT));
            if (downloadLinks.size() == 0) getBroadcaster().fireEvent(new LinkGrabberFilePackageEvent(this, LinkGrabberFilePackageEvent.EMPTY_EVENT));
        }
    }

    public void setName(String name) {
        if (name == null || name.length() == 0) {
            this.name = JDUtilities.removeEndingPoints(JDLocale.L("controller.packages.defaultname", "various"));
        } else
            this.name = JDUtilities.removeEndingPoints(JDIO.validateFileandPathName(name));
        getBroadcaster().fireEvent(new LinkGrabberFilePackageEvent(this, LinkGrabberFilePackageEvent.UPDATE_EVENT));
    }

    public void setPassword(String password) {
        if (password == null) password = "";
        this.password = password;
        getBroadcaster().fireEvent(new LinkGrabberFilePackageEvent(this, LinkGrabberFilePackageEvent.UPDATE_EVENT));
    }

    public int size() {
        synchronized (downloadLinks) {
            return downloadLinks.size();
        }
    }

    public void sort(final int col) {
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
                    case 4:
                        if (aa.isAvailabilityChecked() && bb.isAvailabilityChecked()) {
                            return (aa.isAvailable() && !bb.isAvailable()) ? 1 : -1;
                        } else
                            return -1;
                    default:
                        return -1;
                    }

                }

            });
        }
        getBroadcaster().fireEvent(new LinkGrabberFilePackageEvent(this, LinkGrabberFilePackageEvent.UPDATE_EVENT));
    }

    public String getHoster() {
        Set<String> hosterList = new HashSet<String>();
        synchronized (downloadLinks) {
            for (DownloadLink dl : downloadLinks) {
                hosterList.add(dl.getHost());
            }
        }
        return hosterList.toString();
    }

}
