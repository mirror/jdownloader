package org.jdownloader.api.captcha;

import org.appwork.storage.Storable;

public class CaptchaJob implements Storable {

    public static enum TYPE {
        NORMAL;
    }

    private String captchaID;
    private String hosterID;
    private String linkID;
    private TYPE   type = TYPE.NORMAL;

    /**
     * @return the type
     */
    public TYPE getType() {
        return type;
    }

    /**
     * @param type
     *            the type to set
     */
    public void setType(TYPE type) {
        this.type = type;
    }

    public CaptchaJob() {
    }

    public String getCaptchaID() {
        return captchaID;
    }

    public String getHosterID() {
        return hosterID;
    }

    public String getLinkID() {
        return linkID;
    }

    /**
     * @param captchaID
     *            the captchaID to set
     */
    public void setCaptchaID(String captchaID) {
        this.captchaID = captchaID;
    }

    /**
     * @param hosterID
     *            the hosterID to set
     */
    public void setHosterID(String hosterID) {
        this.hosterID = hosterID;
    }

    /**
     * @param linkID
     *            the linkID to set
     */
    public void setLinkID(String linkID) {
        this.linkID = linkID;
    }

}
