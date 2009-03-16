package jd.gui.skins.simple.components.Linkgrabber;

import java.util.LinkedList;
import java.util.Vector;

import jd.config.Property;
import jd.gui.skins.simple.components.ComboBrowseFile;
import jd.gui.skins.simple.components.JDFileChooser;
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
    private String name = null;
    private boolean extractAfterDownload = true;
    private String comment = null;
    private String password = null;

    public LinkGrabberV2FilePackage() {
        downloadDirectory = JDUtilities.getConfiguration().getDefaultDownloadDirectory();
        brwSaveTo = new ComboBrowseFile("DownloadSaveTo");
        brwSaveTo.setEditable(true);
        brwSaveTo.setFileSelectionMode(JDFileChooser.DIRECTORIES_ONLY);
        brwSaveTo.setText(downloadDirectory);
    }

    public LinkGrabberV2FilePackage(String name) {
        this();
        this.setName(name);
    }

    public String getName() {
        if (name == null) return "various";
        return name;
    }

    public ComboBrowseFile getComboBrowseFile() {
        return brwSaveTo;
    }

    public void add(DownloadLink link) {
        if (!downloadLinks.contains(link)) {
            downloadLinks.add(link);
        }
    }

    public void add(int index, DownloadLink link) {
        if (downloadLinks.contains(link)) {
            downloadLinks.remove(link);
        }
        downloadLinks.add(index, link);
    }

    public void addAll(Vector<DownloadLink> links) {
        for (int i = 0; i < links.size(); i++) {
            add(links.get(i));
        }
    }

    public boolean isExtractAfterDownload() {
        return extractAfterDownload;
    }

    public void setExtractAfterDownload(boolean extractAfterDownload) {
        this.extractAfterDownload = extractAfterDownload;
    }

    public void addAllAt(Vector<DownloadLink> links, int index) {
        for (int i = 0; i < links.size(); i++) {
            add(index + i, links.get(i));
        }
    }

    public boolean contains(DownloadLink link) {
        return downloadLinks.contains(link);
    }

    public DownloadLink get(int index) {
        return downloadLinks.get(index);
    }

    public Vector<DownloadLink> getDownloadLinks() {
        return downloadLinks;
    }

    public String getPassword() {
        return password == null ? "" : password;
    }

    public String getComment() {
        return comment == null ? "" : comment;
    }

    /**
     * @return true/false, je nachdem ob ein Passwort festgelegt wurde
     *         (archivpasswort)
     */
    public boolean hasPassword() {
        return password != null && password.length() > 0;
    }

    public int indexOf(DownloadLink link) {
        return downloadLinks.indexOf(link);
    }

    public DownloadLink lastElement() {
        return downloadLinks.lastElement();
    }

    public boolean remove(DownloadLink link) {
        boolean ret = downloadLinks.remove(link);
        return ret;
    }

    public DownloadLink remove(int index) {
        DownloadLink link = downloadLinks.remove(index);
        return link;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setDownloadLinks(Vector<DownloadLink> downloadLinks) {
        this.downloadLinks = new Vector<DownloadLink>(downloadLinks);
    }

    public void setName(String name) {
        if (name == null || name.length() == 0) {
            this.name = JDUtilities.removeEndingPoints(JDLocale.L("controller.packages.defaultname", "various"));
        } else
            this.name = JDUtilities.removeEndingPoints(JDIO.validateFileandPathName(name));
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int size() {
        return downloadLinks.size();
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
