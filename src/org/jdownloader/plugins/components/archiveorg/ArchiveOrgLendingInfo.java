package org.jdownloader.plugins.components.archiveorg;

import org.appwork.utils.Time;

import jd.http.Cookies;

public class ArchiveOrgLendingInfo {
    private Cookies cookies   = null;
    private Long    timestamp = null;

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
            if (timePassedSinceLastLoan < 5 * 60 * 1000) {
                /* Book has been loaned within the last 5 minutes -> Wait at least 5 minutes between loaning. */
                return timePassedSinceLastLoan;
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
}