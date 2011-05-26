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

package jd.plugins;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.swing.ImageIcon;

import jd.config.Configuration;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.LinkGrabberController;
import jd.nutils.io.JDIO;
import jd.utils.JDUtilities;

import org.appwork.utils.event.Eventsender;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.translate._JDT;

class LinkGrabberFilePackageBroadcaster extends Eventsender<LinkGrabberFilePackageListener, LinkGrabberFilePackageEvent> {

    @Override
    protected void fireEvent(LinkGrabberFilePackageListener listener, LinkGrabberFilePackageEvent event) {
        listener.handle_LinkGrabberFilePackageEvent(event);
    }

}

public class LinkGrabberFilePackage extends Property implements LinkGrabberFilePackageListener {

    private static final long                           serialVersionUID = 5865820033205069205L;
    private String                                      downloadDirectory;
    private ArrayList<DownloadLink>                     downloadLinks    = new ArrayList<DownloadLink>();
    private String                                      name             = "";
    private boolean                                     postProcessing   = true;
    private boolean                                     useSubDir        = true;
    private String                                      comment          = "";
    private String                                      password         = "";
    private long                                        size             = -1;

    private long                                        lastSizeCalc     = 0;
    private int                                         lastfail         = 0;
    private long                                        lastFailCount    = 0;
    private String                                      hosts;
    private boolean                                     ignorePackage    = false;
    private transient LinkGrabberFilePackageBroadcaster broadcaster      = new LinkGrabberFilePackageBroadcaster();
    private long                                        lastEnabledCount = 0;
    private int                                         lastenabled      = 0;
    /**
     * can be set via {@link #setCustomIcon(ImageIcon, String)} to set a custom
     * icon to be shown in the LinkGrabberTable
     */
    private ImageIcon                                   customIcon       = null;
    /**
     * can be set via {@link #setCustomIcon(ImageIcon, String)} to set a custom
     * tooltip to be shown in the LinkGrabberTable
     */
    private String                                      customIconText   = null;
    private int                                         lastunchecked    = 0;
    private long                                        created          = -1;

    /**
     * @return the created
     */
    public long getCreated() {
        return created;
    }

    /**
     * @param created
     *            the created to set
     */
    public void setCreated(long created) {
        this.created = created;
    }

    public boolean isIgnored() {
        return ignorePackage;
    }

    public void setIgnore(boolean b) {
        ignorePackage = b;
    }

    public LinkGrabberFilePackage() {
        created = System.currentTimeMillis();
        downloadDirectory = org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder();
        name = JDUtilities.removeEndingPoints(_JDT._.controller_packages_defaultname());
        useSubDir = JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_PACKETNAME_AS_SUBFOLDER, false);
        // TODO
        // OptionalPluginWrapper addon = JDUtilities.getOptionalPlugin("unrar");
        // if (addon != null && addon.isEnabled()) {
        // postProcessing =
        // addon.getPluginConfig().getBooleanProperty("ACTIVATED", true);
        // }
        broadcaster = new LinkGrabberFilePackageBroadcaster();
        broadcaster.addListener(this);

        int state = SubConfiguration.getConfig(LinkGrabberController.CONFIG).getIntegerProperty(LinkGrabberController.PARAM_NEWPACKAGES, 2);
        if (state == 0) {
            setProperty(LinkGrabberController.PROPERTY_EXPANDED, true);
            setProperty(LinkGrabberController.PROPERTY_USEREXPAND, true);
        } else if (state == 2) {
            setProperty(LinkGrabberController.PROPERTY_EXPANDED, false);
            setProperty(LinkGrabberController.PROPERTY_USEREXPAND, true);
        }
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
        int unchecked = 0;
        synchronized (downloadLinks) {
            for (DownloadLink dl : downloadLinks) {
                if (!dl.isAvailabilityStatusChecked()) {
                    unchecked++;
                } else if (!dl.isAvailable()) {
                    newfail++;
                }
            }
        }
        lastFailCount = System.currentTimeMillis();
        lastfail = newfail;
        lastunchecked = unchecked;
        return lastfail;
    }

    public int countUncheckedLinks() {
        return lastunchecked;
    }

    public synchronized int countEnabledLinks(boolean forceUpdate) {
        if (!forceUpdate && System.currentTimeMillis() - lastEnabledCount < 5000) return lastenabled;
        int newenabled = 0;
        synchronized (downloadLinks) {
            for (DownloadLink dl : downloadLinks) {
                if (dl.isEnabled()) newenabled++;
            }
        }
        lastEnabledCount = System.currentTimeMillis();
        lastenabled = newenabled;
        return lastenabled;
    }

    public void keepHostersOnly(Set<String> hoster) {
        ArrayList<DownloadLink> remove = new ArrayList<DownloadLink>();
        synchronized (downloadLinks) {
            for (DownloadLink dl : downloadLinks) {
                if (!hoster.contains(dl.getHost())) remove.add(dl);
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
    }

    public boolean useSubDir() {
        return useSubDir;
    }

    public void setDownloadDirectory(String dir) {
        downloadDirectory = JDUtilities.removeEndingPoints(dir);
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

    public boolean isPostProcessing() {
        return postProcessing;
    }

    public void setPostProcessing(boolean postProcessing) {
        this.postProcessing = postProcessing;
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
        return password != null ? password : "";
    }

    /**
     * returns a list of archivepasswords set by downloadlinks
     */
    public ArrayList<String> getPasswordAuto() {
        ArrayList<String> pwList = new ArrayList<String>();
        synchronized (downloadLinks) {
            for (DownloadLink element : downloadLinks) {
                if (element.getSourcePluginPasswordList() != null) {
                    for (String pw : element.getSourcePluginPasswordList()) {
                        if (!pwList.contains(pw)) pwList.add(pw);
                    }
                }
            }
        }
        return pwList;
    }

    public String getComment() {
        return comment != null ? comment : "";
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
        this.comment = comment;
    }

    public void clear() {
        this.setDownloadLinks(new ArrayList<DownloadLink>());
    }

    public boolean isEmpty() {
        return getDownloadLinks().isEmpty();
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
            this.name = JDUtilities.removeEndingPoints(_JDT._.controller_packages_defaultname());
        } else
            this.name = JDUtilities.removeEndingPoints(JDIO.validateFileandPathName(name));
        this.name = this.name.trim();
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int size() {
        synchronized (downloadLinks) {
            return downloadLinks.size();
        }
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
        switch (event.getEventID()) {
        case LinkGrabberFilePackageEvent.ADD_LINK:
        case LinkGrabberFilePackageEvent.REMOVE_LINK:
            updateHosts();
            break;
        default:
            break;
        }
    }

    public ArrayList<DownloadLink> getLinksListbyStatus(int status) {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        synchronized (downloadLinks) {
            for (DownloadLink dl : downloadLinks) {
                if (dl.getLinkStatus().hasStatus(status)) {
                    ret.add(dl);
                }
            }
        }
        return ret;
    }

    /**
     * @return the customIcon
     * @see #customIcon
     * @see #setCustomIcon(ImageIcon, String)
     */
    public ImageIcon getCustomIcon() {
        return customIcon;
    }

    /**
     * @return the customIconText
     * @see #customIconText
     * @see #setCustomIcon(ImageIcon, String)
     */
    public String getCustomIconText() {
        return customIconText;
    }

    /**
     * @param customIcon
     *            the customIcon to set
     * @param customIconText
     *            the customIconText to set
     * @see #customIcon
     * @see #getCustomIcon()
     */
    public void setCustomIcon(ImageIcon customIcon, String customIconText) {
        this.customIcon = customIcon;
        this.customIconText = customIconText;
    }

    /**
     * @return is a custom icon set?
     * @see #setCustomIcon(ImageIcon, String)
     */
    public boolean hasCustomIcon() {
        return this.customIcon != null && this.customIconText != null;
    }

    /**
     * Cheks if this package contains splitted archives. if yes, this methods
     * tries to check whether the archive iscomplete.
     * 
     * @return
     */
    public boolean isComplete() {
        return true;
    }

}