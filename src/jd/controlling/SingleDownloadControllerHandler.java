package jd.controlling;

import jd.plugins.Account;
import jd.plugins.DownloadLink;

public abstract interface SingleDownloadControllerHandler {

    /**
     * returns false if SingleDownloadController should proceed with this Link
     * 
     * @param link
     * @return
     */
    public abstract boolean handleDownloadLink(DownloadLink link,Account acc);

}
