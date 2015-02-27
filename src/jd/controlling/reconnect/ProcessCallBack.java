package jd.controlling.reconnect;


public interface ProcessCallBack {
    public void setProgress(Object caller, int percent);

    public void setStatusString(Object caller, String string);

    /**
     * @param liveHeaderDetectionWizard
     * @param ret
     */
    public void setStatus(Object caller, Object statusObject);

    /**
     * @param b
     */
    public void setMethodConfirmEnabled(boolean b);

    /**
     * @return
     */
    public boolean isMethodConfirmEnabled();

}
