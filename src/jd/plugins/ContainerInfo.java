package jd.plugins;

import java.util.HashMap;
import java.util.Vector;


public class ContainerInfo {
    
    static HashMap<String, ContainerInfo> mapFileToContainerInfo = new HashMap<String, ContainerInfo>();

    private String containerFile = null;
    private Vector<String> downloadLinksURL;
    private Vector<DownloadLink> containedLinks = new Vector<DownloadLink>();
    public String getContainerFile() {
        return containerFile;
    }

    public void setContainerFile(String containerFile) {
        this.containerFile = containerFile;
    }

    public Vector<DownloadLink> getContainedLinks() {
        return containedLinks;
    }
    public void setContainedLinks(Vector<DownloadLink> containedLinks) {
        this.containedLinks = containedLinks;
    }
    public static HashMap<String, ContainerInfo> getMapFileToContainerInfo() {
        return mapFileToContainerInfo;
    }
    public Vector<String> getDownloadLinksURL() {
        return downloadLinksURL;
    }

    public void setDownloadLinksURL(Vector<String> downloadLinksURL) {
        this.downloadLinksURL = downloadLinksURL;
    }
}
