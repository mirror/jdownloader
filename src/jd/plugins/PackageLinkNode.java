package jd.plugins;

public interface PackageLinkNode {

    /**
     * a ListOrderID that is linked to original DownloadLinkList Order, set by
     * DownloadController
     * 
     * @return
     */
    public int getListOrderID();

    public boolean isEnabled();

    public long getFinishedDate();

    public long getCreated();

    public long getRemainingKB();
}
