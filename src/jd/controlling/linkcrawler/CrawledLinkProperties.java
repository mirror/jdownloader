package jd.controlling.linkcrawler;

public interface CrawledLinkProperties {
    public Boolean getSticky();

    public void setSticky(Boolean b);

    public boolean isEnabled();

    public boolean setEnabled(boolean b);

    public boolean isCrawlDeep();

    public void setCrawlDeep(boolean crawlDeep);

    public boolean isAutoConfirmEnabled();

    public void setAutoConfirmEnabled(boolean autoAddEnabled);

    public boolean isAutoStartEnabled();

    public void setAutoStartEnabled(boolean autoStartEnabled);

    public boolean isForcedAutoStartEnabled();

    public void setForcedAutoStartEnabled(boolean forcedAutoStartEnabled);
}
