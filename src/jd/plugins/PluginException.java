package jd.plugins;


public class PluginException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = -413339039711789194L;
    private int linkStatus=-1;
    private String errorMessage=null;
    private long value=-1;

    public PluginException(int linkStatus) {
       this.linkStatus=linkStatus;
    }

    public PluginException(int linkStatus, String errorMessage, long value) {
       this(linkStatus);
       this.errorMessage=errorMessage;
       this.value=value;
    }

    public PluginException(int linkStatus, String errorMessage) {
        this(linkStatus);
        this.errorMessage=errorMessage;
       
    }

    public int getLinkStatus() {
        return linkStatus;
    }

    public void setLinkStatus(int linkStatus) {
        this.linkStatus = linkStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public void fillLinkStatus(LinkStatus linkStatus) {
        if(this.linkStatus>=0)linkStatus.addStatus(this.linkStatus);
        if(value>=0)linkStatus.setValue(value);
        if(errorMessage!=null)linkStatus.setErrorMessage(errorMessage);
        
    }

}
