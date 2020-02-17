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
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class SharingWtf extends YetiShareCore {
    public SharingWtf(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(getPurchasePremiumURL());
    }

    /**
     * DEV NOTES YetiShare<br />
     ****************************
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: null solvemedia reCaptchaV2<br />
     * other: <br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "sharing.wtf", "dirrtyshar.es", "filesharing.io" });
        return ret;
    }

    @Override
    public String rewriteHost(String host) {
        /* 2020-01-17: Old domain was: dirrtyshar.es */
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
        final List<String[]> pluginDomains = getPluginDomains();
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + YetiShareCore.getDefaultAnnotationPatternPart());
        }
        return ret.toArray(new String[0]);
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
    public String[] scanInfo(final DownloadLink link, final String[] fileInfo) {
        /* 2020-01-17: Special */
        super.scanInfo(link, fileInfo);
        if (StringUtils.isEmpty(fileInfo[1])) {
            fileInfo[1] = br.getRegex("class=\"fa fa-file-o\"></i>([^<>\"]+)<span").getMatch(0);
            if (!StringUtils.isEmpty(fileInfo[1])) {
                fileInfo[1] = fileInfo[1].trim();
                if (!fileInfo[1].contains("b")) {
                    fileInfo[1] += "b";
                }
            }
        }
        return fileInfo;
    }

    @Override
    protected String getContinueLink() {
        /* 2020-01-18: Special */
        /* Their html contains a commented-out line of code which is what our template code would normally pick up --> Endless loop */
        String continue_link = br.getRegex("\\$\\(\\'\\.download-timer\\'\\)\\.html.+\\$\\(\\'\\.download-timer\\'\\)\\.html\\(\"[^\\)]+\\'(https://[^\\']+)").getMatch(0);
        if (continue_link == null) {
            /* 2020-02-17: For premium mode */
            continue_link = br.getRegex("(https?://transfer[a-z0-9]*?\\.[^/]+/jdb\\?url=[^\"]+)").getMatch(0);
            if (continue_link != null) {
                continue_link = Encoding.htmlDecode(continue_link);
                final String[] urls = HTMLParser.getHttpLinks(continue_link, "");
                if (urls.length > 1) {
                    continue_link = urls[urls.length - 1];
                }
            }
        }
        if (continue_link == null) {
            continue_link = super.getContinueLink();
        }
        return continue_link;
    }

    @Override
    public void checkErrors(final DownloadLink link, final Account account) throws PluginException {
        /* 2020-02-17: Special */
        if (br.containsHTML("you need to be a registered user to download any files")) {
            throw new AccountRequiredException();
        }
        super.checkErrors(link, account);
    }
}