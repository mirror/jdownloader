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

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.YetiShareCore;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class VisharePl extends YetiShareCoreSpecialOxycloud {
    public VisharePl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://" + this.getHost() + "/upgrade/2");
    }

    /**
     * DEV NOTES YetiShare<br />
     ****************************
     * mods: See overridden functions<br />
     * limit-info: 2021-01-04: No limits at all <br />
     * captchatype-info: 2021-01-04: reCaptchaV2<br />
     * other: <br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "vishare.pl" });
        return ret;
    }

    @Override
    public String rewriteHost(final String host) {
        return this.rewriteHost(getPluginDomains(), host, new String[0]);
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return YetiShareCore.buildAnnotationUrls(getPluginDomains());
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

    public int getMaxChunks(final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return 0;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return 0;
        } else {
            /* Free(anonymous) and unknown account type */
            return 0;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public int getMaxSimultaneousFreeAccountDownloads() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public boolean requires_WWW() {
        /* 2021-01-04 */
        return false;
    }
    /* Removed override again as "/upgrade" will auto redirect to "/upgrade/2" */
    // @Override
    // protected String getAccountNameSpaceUpgrade() {
    // /* 2021-01-07 */
    // return "/upgrade/2";
    // }

    @Override
    public void checkErrors(final DownloadLink link, final Account account) throws PluginException {
        String errorMsgURL = null;
        try {
            final UrlQuery query = UrlQuery.parse(br.getURL());
            errorMsgURL = query.get("e");
            if (errorMsgURL != null) {
                errorMsgURL = URLDecoder.decode(errorMsgURL, "UTF-8");
            }
        } catch (final Throwable e) {
            logger.log(e);
        }
        /*
         * 2021-01-04: E.g.
         * "Musisz odczekać 1 Hour przed następnym pobraniem. Spróbuj ponownie później lub kup konto premium, aby pobrać natychmiast."
         */
        final String reconnectWaitStr = new Regex(errorMsgURL, ".*Musisz odczekać (\\d+) Hour przed następnym pobraniem.*").getMatch(0);
        if (reconnectWaitStr != null) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(reconnectWaitStr) * 60 * 60 * 1001l);
        }
        super.checkErrors(link, account);
    }

    @Override
    public boolean supports_https() {
        /* 2021-01-04 */
        return false;
    }
}