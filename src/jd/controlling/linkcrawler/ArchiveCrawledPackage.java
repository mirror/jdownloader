package jd.controlling.linkcrawler;

public class ArchiveCrawledPackage extends CrawledPackage {
    private String id;

    public ArchiveCrawledPackage(String packageID, String name) {
        super();
        setCustomName(name);
        this.id = packageID;
        view = new ArchiveCrawledPackageView();
    }

    public String getName() {
        return super.getName();
    }

    public String getAutoPackageName() {
        return id;
    }

}
