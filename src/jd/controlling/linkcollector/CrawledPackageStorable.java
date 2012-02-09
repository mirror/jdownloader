package jd.controlling.linkcollector;

import java.util.ArrayList;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.storage.Storable;

public class CrawledPackageStorable implements Storable {

    public static enum TYPE {
        NORMAL,
        OFFLINE,
        POFFLINE,
        VARIOUS
    }

    private TYPE   type      = null;
    private String packageID = null;

    /* this one is correct during JSON serialization */
    public TYPE getType() {
        if (pkg instanceof OfflineCrawledPackage) return TYPE.OFFLINE;
        if (pkg instanceof PermanentOfflinePackage) return TYPE.POFFLINE;
        if (pkg instanceof VariousCrawledPackage) return TYPE.VARIOUS;
        return TYPE.NORMAL;
    }

    public void setType(TYPE type) {
        this.type = type;
    }

    /* this one was set by JSON on restore */
    public TYPE _getType() {
        return type;
    }

    private CrawledPackage                 pkg;
    private ArrayList<CrawledLinkStorable> links;

    public String getComment() {
        return pkg.getComment();
    }

    public void setComment(String comment) {
        pkg.setComment(comment);
    }

    @SuppressWarnings("unused")
    private CrawledPackageStorable(/* storable */) {
        pkg = new CrawledPackage();
        links = new ArrayList<CrawledLinkStorable>();
    }

    public CrawledPackageStorable(CrawledPackage pkg) {
        this.pkg = pkg;
        links = new ArrayList<CrawledLinkStorable>(pkg.getChildren().size());
        synchronized (pkg) {
            for (CrawledLink link : pkg.getChildren()) {
                links.add(new CrawledLinkStorable(link));
            }
        }
    }

    public CrawledPackage _getCrawledPackage() {
        return pkg;
    }

    public long getCreated() {
        return pkg.getCreated();
    }

    public String getDownloadFolder() {
        if (!pkg.isDownloadFolderSet()) return null;
        return pkg.getRawDownloadFolder();
    }

    public ArrayList<String> getExtractionPasswords() {
        if (pkg.getExtractionPasswords().size() == 0) return null;
        return new ArrayList<String>(pkg.getExtractionPasswords());
    }

    public ArrayList<CrawledLinkStorable> getLinks() {
        return links;
    }

    public boolean isAutoExtractionEnabled() {
        return pkg.isAutoExtractionEnabled();
    }

    public boolean isExpanded() {
        return pkg.isExpanded();
    }

    public void setAutoExtractionEnabled(boolean autoExtractionEnabled) {
        pkg.setAutoExtractionEnabled(autoExtractionEnabled);
    }

    public void setCreated(long created) {
        pkg.setCreated(created);
    }

    public void setDownloadFolder(String downloadFolder) {
        this.pkg.setDownloadFolder(downloadFolder);
    }

    public void setExpanded(boolean expanded) {
        this.pkg.setExpanded(expanded);
    }

    public void setExtractionPasswords(ArrayList<String> extractionPasswords) {
        if (extractionPasswords == null) return;
        this.pkg.getExtractionPasswords().addAll(extractionPasswords);
    }

    public void setLinks(ArrayList<CrawledLinkStorable> links) {
        if (links != null) {
            this.links = links;
            synchronized (pkg) {
                for (CrawledLinkStorable link : links) {
                    CrawledLink l = link._getCrawledLink();
                    pkg.getChildren().add(l);
                    l.setParentNode(pkg);
                }
            }
        }
    }

    /**
     * @param customName
     *            the customName to set
     */
    public void setName(String name) {
        this.pkg.setName(name);
    }

    /**
     * @return the customName
     */
    public String getName() {
        return pkg.getName();
    }

    /**
     * @param packageID
     *            the packageID to set
     */
    public void setPackageID(String packageID) {
        this.packageID = packageID;
    }

    /**
     * @return the packageID
     */
    public String getPackageID() {
        return packageID;
    }

}
