//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.List;

import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class EasyBytezCom extends XFileSharingProBasic {
    public EasyBytezCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: 2019-08-06: Premium untested, set FREE account limits <br />
     * captchatype-info: 2019-08-06: null<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "easybytez.com", "easybytez.co", "easybytez.to", "zingload.com", "easyload.to", "ezbytez.com", "ebytez.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return XFileSharingProBasic.buildAnnotationUrls(getPluginDomains());
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return true;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return true;
        }
    }

    @Override
    public int getMaxChunks(final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return 1;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return 1;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
        }
    }

    @Override
    protected AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        /* 2019-08-06: Special */
        final AccountInfo ai = super.fetchAccountInfo(account);
        if (account.getType() == AccountType.FREE) {
            /*
             * 2019-08-06: Special: Allow downloads even if account does not have enough traffic. By performing a reconnect we can reset
             * that limit and the account will have full traffic again (2 GB/day[?])
             */
            ai.setSpecialTraffic(true);
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        /* 2019-08-06: Special */
        if (link.getView().getBytesTotal() > account.getAccountInfo().getTrafficLeft()) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Reconnect required to reset free account traffic", 5 * 60 * 1000l);
        }
        super.handlePremium(link, account);
    }

    @Override
    protected void checkErrors(final DownloadLink link, final Account account, final boolean checkAll) throws NumberFormatException, PluginException {
        /* 2019-08-06: Special */
        if (account != null && account.getType() == AccountType.FREE) {
            /* Run without Account object so that reconnects are performed whenever the user runs into downloadlimits. */
            super.checkErrors(link, null, checkAll);
        } else {
            /* Default way */
            super.checkErrors(link, account, checkAll);
        }
    }

    @Override
    public int getMaxSimultaneousFreeAnonymousDownloads() {
        return 1;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 1;
    }

    @Override
    protected boolean supports_https() {
        /* 2019-08-06: Special */
        return false;
    }
}