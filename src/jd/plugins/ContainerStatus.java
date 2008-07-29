package jd.plugins;

import java.io.File;


public class ContainerStatus {

    private int status = TODO;
    private String statusText=null;
    private int lastestStatus=  TODO;
    private String errorMessage;
    private File container;
   public static final int TODO =1<<0;
public static final int STATUS_FINISHED = 1<<1;
public static final int STATUS_FAILED = 1<<2;

    public ContainerStatus() {
       
    }


    public ContainerStatus(File lc) {
      this.container=lc;
    }


    /**
     * Setzt den Linkstatus. Es dürfen nur LInkStatus.*STATUS ids verwendet
     * werden
     * 
     * @param status
     */
    public void setStatus(int status) {
        this.status = status;
        this.lastestStatus=status;
    }

    /**
     * Fügt einen LinkStatus.* Status hinzu.Der alte status wird dabei nicht
     * gelöscht.
     * 
     * @param status
     */
    public void addStatus(int status) {
        this.status |= status;
        this.lastestStatus=status;

    }

    /** Entfernt eine Statusid */
    public void removeStatus(int status) {
        this.status ^= status;
    }

    /**
     * Gibt zurück ob der zugehörige Link einen bestimmten status hat.
     * 
     * @param status
     * @return
     */
    public boolean hasStatus(int status) {

        return (this.status | status) > 0;
    }

    public void setStatusText(String l) {
        this.statusText = l;

    }

 

    public int getLatestStatus() {
       
        return 0;
    }

    public boolean isStatus(int status) {
      return this.status==status;
    }

    public void setErrorMessage(String string) {
       this.errorMessage=string;
        
    }


    public File getContainer() {
        return container;
    }

}
