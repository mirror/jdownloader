package jd.plugins;

import java.io.File;
import java.io.Serializable;

public class BackupLink implements Serializable {
  

    public static final int LINKTYPE_NORMAL = 0;
    public static final int LINKTYPE_CONTAINER = 1;
  

    /**
     * Von hier soll de Download stattfinden
     */
    private String            urlDownload;
 
    /**
     * Containername
     */
    private String            container;
    

    /**
     * Dateiname des Containers
     */
    private String            containerFile;
    /**
     * Index dieses DownloadLinks innerhalb der Containerdatei
     */
    private int               containerIndex=-1;
    private int linkType;
    private String containerType;
  
  
    public BackupLink(String urlDownload) {       
        linkType=LINKTYPE_NORMAL;
            this.urlDownload = urlDownload;
    
    }
    public BackupLink(File containerfile, int id,String containerType) {       
        containerFile=containerfile.getAbsolutePath();
        containerIndex=id;
       this.containerType=containerType;
        linkType=LINKTYPE_CONTAINER;

}
    public String getUrlDownload() {
        return urlDownload;
    }
    public String getContainer() {
        return container;
    }
    public String getContainerFile() {
        return containerFile;
    }
    public int getContainerIndex() {
        return containerIndex;
    }
    public int getLinkType() {
        return linkType;
    }
    public String getContainerType() {
        return containerType;
    }
public String toString(){
    return " containerType :"+containerType+" linkType :"+linkType+" containerIndex :"+containerIndex+" container :"+container+" urlDownload :"+urlDownload;
}
   
}
