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

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class TakefileLink extends XFileSharingProBasic {
    public TakefileLink(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://takefile.link/upgrade");
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: 2019-02-11: null<br />
     * other:<br />
     */
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
            /* 2020-10-02 */
            return -2;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
        }
    }

    @Override
    public int getMaxSimultaneousFreeAnonymousDownloads() {
        /*
         * 2020-02-19: According to forum users, they will allow one more connection every 24 hours. It seems like they often host single
         * files > 10GB which means this scenario is realistic.
         */
        return -1;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        /*
         * 2020-02-19: According to forum users, they will allow one more connection every 24 hours. It seems like they often host single
         * files > 10GB which means this scenario is realistic.
         */
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* 2020-10-02 */
        return -1;
    }

    @Override
    public boolean isOffline(final DownloadLink link) {
        final String fuid = super.getFUIDFromURL(link);
        boolean isOffline = super.isOffline(link);
        if (!br.getURL().contains(fuid) || (br.getRedirectLocation() != null && !br.getRedirectLocation().contains(fuid))) {
            /* 2018-11-15: Special - redirect to: https://takefile.link/upgrade */
            isOffline = true;
        }
        return isOffline;
    }

    @Override
    public String getLoginURL() {
        return getMainPage() + "/user_login";
    }

    @Override
    public boolean isLoggedin() {
        boolean isLoggedinHTML = super.isLoggedin();
        if (!isLoggedinHTML) {
            isLoggedinHTML = br.containsHTML("/user_logout");
        }
        return isLoggedinHTML;
    }

    @Override
    public String regExTrafficLeft() {
        String trafficleft = super.regExTrafficLeft();
        if (StringUtils.isEmpty(trafficleft)) {
            trafficleft = new Regex(correctedBR, "Traffic available today</TD></TR>\\s*?</thead>\\s*?<TR><TD><b>\\s*([^<>\"]+)\\s*</b><").getMatch(0);
        }
        return trafficleft;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "takefile.link" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    /**
     * returns the annotation pattern array: 'https?://(?:www\\.)?(?:domain1|domain2)/(?:embed\\-)?[a-z0-9]{12}'
     *
     */
    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:[a-z0-9]+\\.)?" + buildHostsPatternPart(domains) + XFileSharingProBasic.getDefaultAnnotationPatternPart());
        }
        return ret.toArray(new String[0]);
    }

    @Override
    protected void checkErrors(final DownloadLink link, final Account account, final boolean checkAll) throws NumberFormatException, PluginException {
        super.checkErrors(link, account, checkAll);
        /* 2021-01-11: Some files are "paid files": The user has to pay a fee for that single file to be able to download it. */
        if (new Regex(correctedBR, "class=\"price\"").matches()) {
            throw new AccountRequiredException();
        }
    }
}