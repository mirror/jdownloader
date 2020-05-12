//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.config.Keep2shareConfig;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.PluginForHost;

/**
 *
 * @author raztoki
 *
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "keep2share.cc" }, urls = { "https?://(?:[a-z0-9\\-]+\\.)?(?:keep2share|k2s|k2share|keep2s|keep2)\\.cc/(?:file|preview)/(?:info/)?([a-z0-9]+)(/([^/\\?]+))?(\\?site=([^\\&]+))?" })
public class Keep2ShareCc extends K2SApi {
    public Keep2ShareCc(PluginWrapper wrapper) {
        super(wrapper);
    }

    public final String MAINTLD = "k2s.cc";

    // private final String DOMAINS_HTTP = "(https?://((www|new)\\.)?" + DOMAINS_PLAIN + ")";
    @Override
    public String[] siteSupportedNames() {
        // keep2.cc no dns
        return new String[] { "keep2share.cc", "k2s.cc", "keep2s.cc", "k2share.cc", "keep2share.com", "keep2share" };
    }

    @Override
    public String rewriteHost(String host) {
        if (host == null) {
            return "keep2share.cc";
        }
        for (final String supportedName : siteSupportedNames()) {
            if (supportedName.equals(host)) {
                return "keep2share.cc";
            }
        }
        return super.rewriteHost(host);
    }

    @Override
    public String buildExternalDownloadURL(final DownloadLink link, final PluginForHost buildForThisPlugin) {
        if (StringUtils.equals("real-debrid.com", buildForThisPlugin.getHost())) {
            return "http://keep2share.cc/file/" + getFUID(link);// do not change
        } else {
            return link.getPluginPatternMatcher();
        }
    }

    @Override
    protected String getInternalAPIDomain() {
        return MAINTLD;
    }

    /**
     * easiest way to set variables, without the need for multiple declared references
     *
     * @param account
     */
    @Override
    protected void setConstants(final Account account) {
        if (account != null) {
            if (account.getType() == AccountType.FREE) {
                // free account
                chunks = 1;
                resumes = true;
                isFree = true;
                directlinkproperty = "freelink2";
            } else {
                // premium account
                chunks = -10;
                resumes = true;
                isFree = false;
                directlinkproperty = "premlink";
            }
            logger.finer("setConstants = " + account.getUser() + " @ Account Download :: isFree = " + isFree + ", upperChunks = " + chunks + ", Resumes = " + resumes);
        } else {
            // free non account
            chunks = 1;
            resumes = true;
            isFree = true;
            directlinkproperty = "freelink1";
            logger.finer("setConstants = Guest Download :: isFree = " + isFree + ", upperChunks = " + chunks + ", Resumes = " + resumes);
        }
    }

    @Override
    protected void setAccountLimits(Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            maxPrem.set(1);
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            maxPrem.set(20);
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return maxPrem.get();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return maxFree.get();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        super.resetLink(link);
    }

    /**
     * because stable is lame!
     */
    public void setBrowser(final Browser ibr) {
        this.br = ibr;
    }

    @Override
    protected String getReCaptchaV2WebsiteKey() {
        return "6LezloQUAAAAAJrSZsWi6lDfGZEwgacI3tTIndGU";
    }

    @Override
    public Class<? extends Keep2shareConfig> getConfigInterface() {
        return Keep2shareConfig.class;
    }
}