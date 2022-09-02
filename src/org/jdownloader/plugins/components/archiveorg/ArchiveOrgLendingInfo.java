package org.jdownloader.plugins.components.archiveorg;

import java.util.ArrayList;

import org.appwork.utils.Time;

import jd.http.Cookies;

public class ArchiveOrgLendingInfo {
    private Cookies           cookies                             = null;
    private Long              timestamp                           = null;
    private ArrayList<String> pageURLs                            = new ArrayList<String>();
    private int               numberofSuccessfullyDownloadedPages = 0;

    public ArchiveOrgLendingInfo(final Cookies cookies) {
        this.cookies = cookies;
        this.timestamp = Time.systemIndependentCurrentJVMTimeMillis();
    }

    public Cookies getCookies() {
        return this.cookies;
    }

    /** Returns whether or not this session is considered valid. */
    public boolean isValid() {
        if (this.timestamp == null) {
            return false;
        } else if (Time.systemIndependentCurrentJVMTimeMillis() - this.timestamp.longValue() < 60 * 60 * 1000) {
            return true;
        } else {
            return false;
        }
    }

    /** If this returns null, loan is allowed immediately. */
    public Long getTimeUntilNextLoanAllowed() {
        if (this.timestamp == null) {
            return null;
        } else {
            final long timePassedSinceLastLoan = Time.systemIndependentCurrentJVMTimeMillis() - this.timestamp.longValue();
            final long maxLoanTimeframeMillis = 5 * 60 * 1000;
            if (timePassedSinceLastLoan < maxLoanTimeframeMillis) {
                /* Book has been loaned within the last 5 minutes -> Wait at least 5 minutes between loaning. */
                return maxLoanTimeframeMillis - timePassedSinceLastLoan;
            } else {
                return null;
            }
        }
    }

    /** Returns true if this session has been newly added within the last X minutes. */
    public boolean hasJustBeenLoaned() {
        if (getTimeUntilNextLoanAllowed() == null) {
            return false;
        } else {
            return true;
        }
    }

    public void setPageURLs(final ArrayList<String> urls) {
        this.pageURLs.clear();
        this.pageURLs.addAll(urls);
    }

    /** Returns URL to desired pageIndexNumber. */
    public String getPageURL(final int pageIndexNumber) {
        if (pageIndexNumber > -1 && pageIndexNumber < this.pageURLs.size()) {
            return this.pageURLs.get(pageIndexNumber);
        } else {
            return null;
        }
    }

    /** Increases downloaded page counter. */
    public void increaseDownloadedPageCounter() {
        this.numberofSuccessfullyDownloadedPages += 1;
    }

    public void setNumberofSuccessfullyDownloadedPages(final int num) {
        this.numberofSuccessfullyDownloadedPages = num;
    }

    public int getNumberofSuccessfullyDownloadedPages() {
        return this.numberofSuccessfullyDownloadedPages;
    }

    public boolean looksLikeBookDownloadIsComplete() {
        if (this.numberofSuccessfullyDownloadedPages >= this.pageURLs.size()) {
            return true;
        } else {
            return false;
        }
    }
}