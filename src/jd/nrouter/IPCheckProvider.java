package jd.nrouter;

public interface IPCheckProvider {

    /**
     * this method should return current IP as String Object or
     * IPCheck.CHECK.FAILED if there has been an error or
     * IPCheck.CHECK.SEQFAILED if this method should be paused
     * 
     * @return
     */
    public Object getIP();

    /**
     * returns a String with Info about this Provider (eg Website url)
     * 
     * @return
     */
    public String getInfo();
}
