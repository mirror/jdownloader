package org.jdownloader.plugins;

import javax.swing.Icon;

import jd.controlling.packagecontroller.AbstractNode;
import jd.nutils.Formatter;
import jd.plugins.Account;
import jd.plugins.DownloadLink;

import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.translate._JDT;

public class WaitForAccountSkipReason implements ConditionalSkipReason, IgnorableConditionalSkipReason, TimeOutCondition {
    private final Account account;
    private final Icon    icon;

    public WaitForAccountSkipReason(Account account) {
        this.account = account;
        icon = new AbstractIcon(IconKey.ICON_WAIT, 16);
    }

    public Account getAccount() {
        return account;
    }

    @Override
    public long getTimeOutTimeStamp() {
        return account.getTmpDisabledTimeout();
    }

    @Override
    public long getTimeOutLeft() {
        return Math.max(0, getTimeOutTimeStamp() - System.currentTimeMillis());
    }

    @Override
    public boolean canIgnore() {
        return true;
    }

    @Override
    public String toString() {
        return "WaitForAccountSkipReason|Account:" + getAccount() + "|" + getMessage(this, null);
    }

    @Override
    public boolean isConditionReached() {
        return account.isEnabled() == false || account.isValid() == false || account.getAccountController() == null || account.isTempDisabled() == false || getTimeOutLeft() == 0;
    }

    @Override
    public String getMessage(Object requestor, AbstractNode node) {
        final long left = getTimeOutLeft();
        if (left > 0) {
            return _JDT.T.gui_download_waittime_status2(Formatter.formatSeconds(left / 1000));
        } else {
            return _JDT.T.gui_download_waittime_status2("");
        }
    }

    @Override
    public Icon getIcon(Object requestor, AbstractNode node) {
        return icon;
    }

    @Override
    public void finalize(DownloadLink link) {
    }
}
