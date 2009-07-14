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

package jd.gui.skins.simple.components.Linkgrabber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import jd.OptionalPluginWrapper;
import jd.config.Configuration;
import jd.config.Property;
import jd.controlling.LinkGrabberController;
import jd.event.JDBroadcaster;
import jd.nutils.io.JDIO;
import jd.plugins.DownloadLink;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

class LinkGrabberFilePackageBroadcaster extends JDBroadcaster<LinkGrabberFilePackageListener, LinkGrabberFilePackageEvent> {

    // @Override
    protected void fireEvent(LinkGrabberFilePackageListener listener, LinkGrabberFilePackageEvent event) {
        listener.handle_LinkGrabberFilePackageEvent(event);
    }

}

public class LinkGrabberFilePackage extends Property implements LinkGrabberFilePackageListener {

    /**
     * 
     */
    private static final long serialVersionUID = 5865820033205069205L;
    private String downloadDirectory;
    private ArrayList<DownloadLink> downloadLinks = new ArrayList<DownloadLink>();
    private String name = "";
    private boolean extractAfterDownload = true;
    private boolean useSubDir = true;
    private String comment = "";
    private String password = "";
    private long size = -1;

    private long lastSizeCalc = 0;
    private boolean sortasc = false;
    private int lastfail = 0;
    private long lastFailCount = 0;
    private String hosts;
    private boolean ignorePackage = false;
    private transient LinkGrabberFilePackageBroadcaster broadcaster = new LinkGrabberFilePackageBroadcaster();

    public boolean isIgnored() {
        return ignorePackage;
    }

    public void setIgnore(boolean b) {
        ignorePackage = b;
    }

    public LinkGrabberFilePackage() {
        downloadDirectory = JDUtilities.getConfiguration().getDefaultDownloadDirectory();
        name = JDUtilities.removeEndingPoints(JDL.L("controller.packages.defaultname", "various"));
        useSubDir = JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_PACKETNAME_AS_SUBFOLDER, false);
        for (OptionalPluginWrapper wrapper : OptionalPluginWrapper.getOptionalWrapper()) {
            if (wrapper.isEnabled() && wrapper.getPlugin() != null && wrapper.getPlugin().getClass().getName().endsWith("JDUnrar")) {
                extractAfterDownload = wrapper.getPluginConfig().getBooleanProperty("ACTIVATED", true);
            }
        }
        broadcaster = new LinkGrabberFilePackageBroadcaster();
        broadcaster.addListener(this);
    }

    public LinkGrabberFilePackage(String name, LinkGrabberFilePackageListener listener) {
        this(name);
        broadcaster.addListener(listener);
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
                if ((dl.isAvailabilityStatusChecked() && !dl.isAvailable())) {
                    newfail++;
                }
            }
        }
        lastFailCount = System.currentTimeMillis();
        lastfail = newfail;
        return lastfail;
    }

    public void keepHostersOnly(Set<String> hoster) {
        ArrayList<DownloadLink> remove = new ArrayList<DownloadLink>();
        synchronized (downloadLinks) {
            for (DownloadLink dl : downloadLinks) {
                if (!hoster.contains(dl.getPlugin().getHost())) remove.add(dl);
            }
        }
        this.remove(remove);
        countFailedLinks(true);
    }

    public int indexOf(DownloadLink link) {
        synchronized (downloadLinks) {
            return this.downloadLinks.indexOf(link);
        }
    }

    public String getDownloadDirectory() {
        return downloadDirectory;
    }

    public void setUseSubDir(boolean b) {
        useSubDir = b;
        broadcaster.fireEvent(new LinkGrabberFilePackageEvent(this, LinkGrabberFilePackageEvent.UPDATE_EVENT));
    }

    public boolean useSubDir() {
        return useSubDir;
    }

    public void setDownloadDirectory(String dir) {
        downloadDirectory = JDUtilities.removeEndingPoints(dir);
        broadcaster.fireEvent(new LinkGrabberFilePackageEvent(this, LinkGrabberFilePackageEvent.UPDATE_EVENT));
    }

    public LinkGrabberFilePackage(String name) {
        this();
        this.setName(name);
    }

    public String getName() {
        return name;
    }

    public void add(DownloadLink link) {
        if (link == null) return;
        synchronized (LinkGrabberController.ControllerLock) {
            synchronized (downloadLinks) {
                if (!downloadLinks.contains(link)) {
                    LinkGrabberFilePackage fp = LinkGrabberController.getInstance().getFPwithLink(link);
                    downloadLinks.add(link);
                    broadcaster.fireEvent(new LinkGrabberFilePackageEvent(this, LinkGrabberFilePackageEvent.ADD_LINK, link));
                    if (fp != null && fp != this) fp.remove(link);
                }
            }
        }
    }

    public void removeOffline() {
        ArrayList<DownloadLink> remove = new ArrayList<DownloadLink>();
        synchronized (downloadLinks) {
            for (DownloadLink dl : downloadLinks) {
                if ((dl.isAvailabilityStatusChecked() && !dl.isAvailable())) {
                    remove.add(dl);
                }
            }
        }
        this.remove(remove);
        countFailedLinks(true);
    }

    public void remove(ArrayList<DownloadLink> links) {
        for (DownloadLink dl : links) {
            this.remove(dl);
        }
    }

    public synchronized void updateData(String addpw) {
        String password = this.password == null ? "" : this.password;
        StringBuilder comment = new StringBuilder(this.comment == null ? "" : this.comment);

        ArrayList<String> pwList = new ArrayList<String>();
        if (password.length() > 0) {
            String[] pws = JDUtilities.passwordStringToArray(password);
            for (String element : pws) {
                pwList.add(element);
            }
        }

        synchronized (downloadLinks) {
            for (DownloadLink element : downloadLinks) {
                if (element.getSourcePluginPasswordList() != null) {
                    for (String pw : element.getSourcePluginPasswordList()) {
                        if (!pwList.contains(pw)) pwList.add(pw);
                    }
                }
                if (element.getSourcePluginComment() != null) {
                    String newComment = element.getSourcePluginComment();
                    if (newComment != null && comment.indexOf(newComment) < 0) {
                        comment.append("|");
                        comment.append(newComment);
                    }
                }
            }
        }
        String cmt = comment.toString();
        if (cmt.startsWith("|")) {
            cmt = cmt.substring(1);
        }
        this.comment = cmt;

        if (addpw != null && addpw.length() > 0) {
            String[] pws = JDUtilities.passwordStringToArray(addpw);
            for (String element : pws) {
                if (!pwList.contains(element)) pwList.add(element);
            }
        }
        this.password = JDUtilities.passwordArrayToString(pwList.toArray(new String[pwList.size()]));
    }

    public int add(int index, DownloadLink link, int repos) {
        if (link == null) return repos;
        synchronized (LinkGrabberController.ControllerLock) {
            synchronized (downloadLinks) {
                boolean newadded = false;
                LinkGrabberFilePackage fp = null;
                if (downloadLinks.contains(link)) {
                    int posa = this.indexOf(link);
                    if (posa < index) {
                        index -= ++repos;
                    }
                    downloadLinks.remove(link);
                    if (index > downloadLinks.size() - 1) {
                        downloadLinks.add(link);
                    } else if (index < 0) {
                        downloadLinks.add(0, link);
                    } else
                        downloadLinks.add(index, link);
                } else {
                    fp = LinkGrabberController.getInstance().getFPwithLink(link);
                    if (index > downloadLinks.size() - 1) {
                        downloadLinks.add(link);
                    } else if (index < 0) {
                        downloadLinks.add(0, link);
                    } else
                        downloadLinks.add(index, link);
                    newadded = true;
                }
                if (newadded) {
                    broadcaster.fireEvent(new LinkGrabberFilePackageEvent(this, LinkGrabberFilePackageEvent.ADD_LINK, link));
                    if (fp != null && fp != this) fp.remove(link);
                } else {
                    broadcaster.fireEvent(new LinkGrabberFilePackageEvent(this, LinkGrabberFilePackageEvent.UPDATE_EVENT));
                }
            }
        }
        return repos;
    }

    public void addAll(ArrayList<DownloadLink> links) {
        for (int i = 0; i < links.size(); i++) {
            add(links.get(i));
        }
    }

    public boolean isExtractAfterDownload() {
        return extractAfterDownload;
    }

    public void setExtractAfterDownload(boolean extractAfterDownload) {
        this.extractAfterDownload = extractAfterDownload;
        broadcaster.fireEvent(new LinkGrabberFilePackageEvent(this, LinkGrabberFilePackageEvent.UPDATE_EVENT));
    }

    public void addAllAt(ArrayList<DownloadLink> links, int index) {
        int repos = 0;
        for (int i = 0; i < links.size(); i++) {
            repos = add(index + i, links.get(i), repos);
        }
    }

    public boolean contains(DownloadLink link) {
        synchronized (downloadLinks) {
            return downloadLinks.contains(link);
        }
    }

    public DownloadLink get(int index) {
        synchronized (downloadLinks) {
            try {
                return downloadLinks.get(index);
            } catch (IndexOutOfBoundsException e) {
                return null;
            }
        }
    }

    public ArrayList<DownloadLink> getDownloadLinks() {
        return downloadLinks;
    }

    public String getPassword() {
        updateData(null);
        return password;
    }

    public String getComment() {
        updateData(null);
        return comment;
    }

    public boolean remove(DownloadLink link) {
        if (link == null) return false;
        synchronized (LinkGrabberController.ControllerLock) {
            synchronized (downloadLinks) {
                boolean ret = downloadLinks.remove(link);
                if (ret) broadcaster.fireEvent(new LinkGrabberFilePackageEvent(this, LinkGrabberFilePackageEvent.REMOVE_LINK, link));
                if (downloadLinks.size() == 0) broadcaster.fireEvent(new LinkGrabberFilePackageEvent(this, LinkGrabberFilePackageEvent.EMPTY_EVENT));
                return ret;
            }
        }
    }

    public DownloadLink remove(int index) {
        synchronized (LinkGrabberController.ControllerLock) {
            synchronized (downloadLinks) {
                DownloadLink link;
                try {
                    link = downloadLinks.remove(index);
                } catch (IndexOutOfBoundsException e) {
                    link = null;
                }
                if (link != null) downloadLinks.remove(link);
                if (link != null) broadcaster.fireEvent(new LinkGrabberFilePackageEvent(this, LinkGrabberFilePackageEvent.REMOVE_LINK, link));
                if (downloadLinks.size() == 0) broadcaster.fireEvent(new LinkGrabberFilePackageEvent(this, LinkGrabberFilePackageEvent.EMPTY_EVENT));
                return link;
            }
        }
    }

    public void setComment(String comment) {
        if (comment == null) comment = "";
        this.comment = comment;
        broadcaster.fireEvent(new LinkGrabberFilePackageEvent(this, LinkGrabberFilePackageEvent.UPDATE_EVENT));
    }

    public void clear() {
        this.setDownloadLinks(new ArrayList<DownloadLink>());
    }

    public void setDownloadLinks(ArrayList<DownloadLink> downloadLinks) {
        synchronized (LinkGrabberController.ControllerLock) {
            synchronized (downloadLinks) {
                this.downloadLinks = new ArrayList<DownloadLink>(downloadLinks);
                broadcaster.fireEvent(new LinkGrabberFilePackageEvent(this, LinkGrabberFilePackageEvent.UPDATE_EVENT));
                if (downloadLinks.size() == 0) broadcaster.fireEvent(new LinkGrabberFilePackageEvent(this, LinkGrabberFilePackageEvent.EMPTY_EVENT));
            }
        }
    }

    public void setName(String name) {
        if (name == null || name.length() == 0) {
            this.name = JDUtilities.removeEndingPoints(JDL.L("controller.packages.defaultname", "various"));
        } else
            this.name = JDUtilities.removeEndingPoints(JDIO.validateFileandPathName(name));
        broadcaster.fireEvent(new LinkGrabberFilePackageEvent(this, LinkGrabberFilePackageEvent.UPDATE_EVENT));
    }

    public void setPassword(String password) {
        if (password == null || password.length() == 0) return;
        updateData(password);
        broadcaster.fireEvent(new LinkGrabberFilePackageEvent(this, LinkGrabberFilePackageEvent.UPDATE_EVENT));
    }

    public int size() {
        synchronized (downloadLinks) {
            return downloadLinks.size();
        }
    }

    public void sort(final int col, boolean asc) {
        if (asc) {
            sortasc = true;
        } else {
            sortasc = !sortasc;
        }
        synchronized (downloadLinks) {

            Collections.sort(downloadLinks, new Comparator<DownloadLink>() {

                public int compare(DownloadLink a, DownloadLink b) {
                    if (a.getName().endsWith(".sfv")) { return -1; }
                    if (b.getName().endsWith(".sfv")) { return 1; }
                    DownloadLink aa = b;
                    DownloadLink bb = a;
                    if (sortasc) {
                        aa = a;
                        bb = b;
                    }
                    switch (col) {
                    case 0:
                        return aa.getName().compareToIgnoreCase(bb.getName());
                    case 1:
                        return aa.getDownloadSize() < bb.getDownloadSize() ? 1 : -1;
                    case 2:
                        return aa.getHost().compareToIgnoreCase(bb.getHost());
                    case 3:
                        if (aa.getLinkStatus().getStatusText() != null && bb.getLinkStatus().getStatusText() != null) {
                            return aa.getLinkStatus().getStatusText().compareToIgnoreCase(bb.getLinkStatus().getStatusText());
                        } else
                            return -1;
                    default:
                        return -1;
                    }

                }

            });
        }
        broadcaster.fireEvent(new LinkGrabberFilePackageEvent(this, LinkGrabberFilePackageEvent.UPDATE_EVENT));
    }

    public void addListener(LinkGrabberFilePackageListener l) {
        broadcaster.addListener(l);
    }

    public void removeListener(LinkGrabberFilePackageListener l) {
        broadcaster.removeListener(l);
    }

    public String getHoster() {
        if (hosts == null) updateHosts();
        return hosts;
    }

    private void updateHosts() {
        Set<String> hosterList = new HashSet<String>();
        synchronized (downloadLinks) {
            for (DownloadLink dl : downloadLinks) {
                hosterList.add(dl.getHost());
            }
        }
        hosts = hosterList.toString();
    }

    public void handle_LinkGrabberFilePackageEvent(LinkGrabberFilePackageEvent event) {
        switch (event.getID()) {
        case LinkGrabberFilePackageEvent.ADD_LINK:
        case LinkGrabberFilePackageEvent.REMOVE_LINK:
            updateHosts();
            break;
        default:
            break;
        }

    }

}
