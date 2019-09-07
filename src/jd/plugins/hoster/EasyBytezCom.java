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
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

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
    protected String[] supports_precise_expire_date() {
        /* 2019-09-06: Disabled by admin request - not supported */
        return null;
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return XFileSharingProBasic.buildAnnotationUrls(getPluginDomains());
    }

    @Override
    protected void setAccountLimitsByType(Account account, AccountType type) {
        super.setAccountLimitsByType(account, type);
        switch (type) {
        case FREE:
            account.setConcurrentUsePossible(true);
            break;
        default:
            break;
        }
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return false;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return false;
        }
    }

    @Override
    public int getMaxChunks(final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return 1;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return 0;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        /* 2019-08-06: Special */
        if (AccountType.FREE.equals(account.getType()) && link.getView().getBytesTotal() > account.getAccountInfo().getTrafficLeft()) {
            // throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Reconnect required to reset free account traffic", 15 * 60 * 1000l);
            throw new AccountUnavailableException("Reconnect required to reset free account traffic", 15 * 60 * 1000l);
        }
        super.handlePremium(link, account);
    }

    @Override
    protected AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        /* 2019-08-06: Special */
        final AccountInfo ai = super.fetchAccountInfoWebsite(account);
        /*
         * 2019-08-13: This is a workaround for newly created easybytez.com FREE accounts. They will be displayed as premium account with
         * expire date (expiredate = CURRENT date [TODAY]) on their website but they are FREE accounts! Premium accounts have unlimited
         * traffic and free accounts have limited traffic --> Use this to recognize this special case and fix AccountType!
         */
        final boolean premiumValidLessThanOneDay = ai.getValidUntil() - System.currentTimeMillis() < (24 * 60 * 60 * 1000l);
        if (AccountType.PREMIUM.equals(account.getType()) && ai.getTrafficLeft() > 0 && premiumValidLessThanOneDay) {
            logger.info("Correcting AccountType from PREMIUM to FREE and removing expire-date");
            account.setType(AccountType.FREE);
            /* Remove wrong expire-date (given via website) */
            ai.setValidUntil(-1);
        }
        if (AccountType.FREE.equals(account.getType())) {
            /*
             * 2019-08-06: Special: Allow downloads even if account does not have enough traffic. By performing a reconnect we can reset
             * that limit and the account will have full traffic again (2 GB/day[?])
             */
            ai.setSpecialTraffic(true);
        }
        return ai;
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
        return -1;
    }

    @Override
    protected boolean supports_https() {
        /* 2019-08-06: Special */
        return false;
    }
}