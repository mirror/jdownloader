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

import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.YetiShareCore;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class FilexCx extends YetiShareCore {
    public FilexCx(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(getPurchasePremiumURL());
    }

    /**
     * DEV NOTES YetiShare<br />
     ****************************
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: 2022-10-07: untested <br />
     * other: <br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "filex.cx" });
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
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:(www|n3)\\.)?" + YetiShareCore.buildHostsPatternPart(domains) + "/(?!folder|shared)(video/embed/[A-Za-z0-9]+(/\\d+x\\d+/[^/<>]+)?|[A-Za-z0-9]+(?:/[^/<>]+)?)");
        }
        return ret.toArray(new String[0]);
    }

    final String PATTERN_EMBED = "(?i)https?://[^/]+/video/embed/([A-Za-z0-9]+)(/\\d+x\\d+/([^/<>]+))?";

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        /* link cleanup, but respect users protocol choosing or forced protocol */
        if (link == null || link.getPluginPatternMatcher() == null) {
            return;
        }
        final String url = this.getContentURL(link);
        /* Change embed URLs -> Normal file-URLs. */
        final Regex embed = new Regex(url, PATTERN_EMBED);
        if (embed.matches()) {
            final String host = Browser.getHost(url, true);
            final String filenameInsideURL = embed.getMatch(2);
            String newurl = "https://" + appendWWWIfRequired(host) + "/" + embed.getMatch(0);
            if (filenameInsideURL != null) {
                newurl += "/" + filenameInsideURL;
            }
            link.setPluginPatternMatcher(newurl);
        }
    }

    protected String appendWWWIfRequired(final String host) {
        if (!requires_WWW() || StringUtils.startsWithCaseInsensitive(host, "www.")) {
            // do not modify host
            return host;
        } else {
            final String hostTld = Browser.getHost(host, false);
            if (!StringUtils.equalsIgnoreCase(host, hostTld)) {
                // keep subdomain
                return host;
            } else {
                // add www.
                return "www." + host;
            }
        }
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
        return false;
    }
}