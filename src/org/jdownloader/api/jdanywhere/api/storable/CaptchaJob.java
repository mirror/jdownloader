package org.jdownloader.api.jdanywhere.api.storable;

import org.appwork.storage.Storable;

public class CaptchaJob implements Storable {

    private long   captchaID;
    private String hosterID;
    private long   linkID;
    private String type;
    private String captchaCategory;
    private long   count = 0;

    /**
     * @return the captchaCount
     */
    public long getCount() {
        return count;
    }

    /**
     * @param captchaCount
     *            the captchaCount to set
     */
    public void setCount(long count) {
        this.count = count;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type
     *            the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    public CaptchaJob() {
    }

    public long getID() {
        return captchaID;
    }

    public String getHoster() {
        return hosterID;
    }

    public long getLink() {
        return linkID;
    }

    /**
     * @param captchaID
     *            the captchaID to set
     */
    public void setID(long captchaID) {
        this.captchaID = captchaID;
    }

    /**
     * @param hosterID
     *            the hosterID to set
     */
    public void setHoster(String hosterID) {
        this.hosterID = hosterID;
    }

    /**
     * @param linkID
     *            the linkID to set
     */
    public void setLink(long linkID) {
        this.linkID = linkID;
    }

    public String getCaptchaCategory() {
        return captchaCategory;
    }

    public void setCaptchaCategory(String captchaCategory) {
        this.captchaCategory = captchaCategory;
    }

}
