package org.jdownloader.plugins;

import javax.swing.Icon;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.SIZEUNIT;
import org.jdownloader.translate._JDT;

public class WaitForAccountTrafficSkipReason implements ConditionalSkipReason, IgnorableConditionalSkipReason {
    private final static SIZEUNIT MAXSIZEUNIT = JsonConfig.create(GraphicalUserInterfaceSettings.class).getMaxSizeUnit();
    private final Account         account;
    private final Icon            icon;

    public Icon getIcon() {
        return icon;
    }

    private final long trafficRequired;

    public long getTrafficRequired() {
        return trafficRequired;
    }

    public WaitForAccountTrafficSkipReason(Account account, long trafficRequired) {
        this.account = account;
        icon = new AbstractIcon(IconKey.ICON_WAIT, 16);
        this.trafficRequired = trafficRequired;
    }

    public Account getAccount() {
        return account;
    }

    @Override
    public boolean canIgnore() {
        return true;
    }

    private final boolean hasEnoughTraffic() {
        final AccountInfo ai = getAccount().getAccountInfo();
        if (ai != null) {
            if (ai.isSpecialTraffic() || ai.isUnlimitedTraffic() || !ai.isTrafficRefill()) {
                return true;
            } else {
                final long trafficLeft = ai.getTrafficLeft();
                return trafficLeft > 0 && trafficLeft >= getTrafficRequired();
            }
        } else {
            return true;
        }
    }

    @Override
    public String toString() {
        return "WaitForAccountTrafficSkipReason(Account:" + getAccount() + "|" + getMessage(this, null) + ")";
    }

    @Override
    public boolean isConditionReached() {
        return getAccount().isEnabled() == false || getAccount().isValid() == false || getAccount().getAccountController() == null || hasEnoughTraffic();
    }

    @Override
    public String getMessage(Object requestor, AbstractNode node) {
        if (requestor instanceof CustomConditionalSkipReasonMessageIcon) {
            return ((CustomConditionalSkipReasonMessageIcon) requestor).getMessage(this, node);
        } else {
            final long trafficRequired = getTrafficRequired();
            if (trafficRequired < 0) {
                return _JDT.T.gui_download_waittime_notenoughtraffic2();
            } else {
                return _JDT.T.gui_download_waittime_notenoughtraffic(SIZEUNIT.formatValue(MAXSIZEUNIT, trafficRequired));
            }
        }
    }

    @Override
    public Icon getIcon(Object requestor, AbstractNode node) {
        if (requestor instanceof CustomConditionalSkipReasonMessageIcon) {
            return ((CustomConditionalSkipReasonMessageIcon) requestor).getIcon(this, node);
        } else {
            return getIcon();
        }
    }

    @Override
    public void finalize(DownloadLink link) {
    }
}
