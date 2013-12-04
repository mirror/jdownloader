package org.jdownloader.plugins;

import javax.swing.ImageIcon;

import jd.controlling.packagecontroller.AbstractNode;
import jd.nutils.Formatter;
import jd.plugins.Account;
import jd.plugins.DownloadLink;

import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

public class WaitForAccountSkipReason implements ConditionalSkipReason, IgnorableConditionalSkipReason, TimeOutCondition {

    private final Account   account;
    private final ImageIcon icon;

    public WaitForAccountSkipReason(Account account) {
        this.account = account;
        icon = NewTheme.I().getIcon("wait", 16);
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
        return Math.max(0, account.getTmpDisabledTimeout() - System.currentTimeMillis());
    }

    @Override
    public boolean canIgnore() {
        return true;
    }

    @Override
    public boolean isConditionReached() {
        return account.isEnabled() == false || account.isValid() == false || account.getAccountController() == null || account.isTempDisabled() == false;
    }

    @Override
    public String getMessage(Object requestor, AbstractNode node) {
        long left = getTimeOutLeft();
        if (left > 0) {
            return _JDT._.gui_download_waittime_status2(Formatter.formatSeconds(left / 1000));
        } else {
            return _JDT._.gui_download_waittime_status2("");
        }
    }

    @Override
    public ImageIcon getIcon(Object requestor, AbstractNode node) {
        return icon;
    }

    @Override
    public void finalize(DownloadLink link) {
    }

}
