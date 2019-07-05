//jDownloader - Downloadmanager
//Copyright (C) 2016  JD-Team support@jdownloader.org
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

import java.util.regex.Pattern;

import org.jdownloader.plugins.components.YetiShareCore;

import jd.PluginWrapper;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class FreefileMe extends YetiShareCore {
    public FreefileMe(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(getPurchasePremiumURL());
    }

    /**
     * DEV NOTES YetiShare<br />
     ****************************
     * mods: See overridden functions<br />
     * limit-info: 2019-02-28: Untested because of CoinHive captcha <br />
     * captchatype-info: 2019-02-28: CoinHive<br />
     * other: <br />
     */
    /* 1st domain = current domain! */
    public static String[] domains = new String[] { "freefile.me" };

    public static String[] getAnnotationNames() {
        return new String[] { domains[0] };
    }

    /**
     * returns the annotation pattern array: 'https?://(?:www\\.)?(?:domain1|domain2)/[A-Za-z0-9]+(?:/[^/]+)?'
     *
     */
    public static String[] getAnnotationUrls() {
        // construct pattern
        final String host = getHostsPattern();
        return new String[] { host + "/[A-Za-z0-9]+(?:/[^/<>]+)?" };
    }

    /** Returns '(?:domain1|domain2)' */
    private static String getHostsPatternPart() {
        final StringBuilder pattern = new StringBuilder();
        for (final String name : domains) {
            pattern.append((pattern.length() > 0 ? "|" : "") + Pattern.quote(name));
        }
        return pattern.toString();
    }

    /** returns 'https?://(?:www\\.)?(?:domain1|domain2)' */
    private static String getHostsPattern() {
        final String hosts = "https?://(?:www\\.)?" + "(?:" + getHostsPatternPart() + ")";
        return hosts;
    }

    @Override
    public String[] siteSupportedNames() {
        return domains;
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
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public int getMaxSimultaneousFreeAccountDownloads() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 1;
    }

    @Override
    public void checkErrors(final DownloadLink link, final Account account) throws PluginException {
        super.checkErrors(link, account);
        /* 2019-03-01: They use crypto-loot.com for FREE and FREE ACCOUNT download-modes which we do not (yet) support */
        if (account == null || account.getType() != AccountType.PREMIUM) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "crypto-loot.com 'captcha' is not supported");
        }
    }

    }