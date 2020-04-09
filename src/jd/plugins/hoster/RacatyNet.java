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
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class RacatyNet extends XFileSharingProBasic {
    public RacatyNet(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: 2019-05-21: Premium untested, set FREE account limits<br />
     * captchatype-info: 2019-05-21: null<br />
     * other:<br />
     */
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

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "racaty.net", "racaty.com" });
        return ret;
    }

    @Override
    public String rewriteHost(String host) {
        return this.rewriteHost(getPluginDomains(), host, new String[0]);
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
            return -3;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return -3;
        } else {
            /* Free(anonymous) and unknown account type */
            return -3;
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
    public String[] scanInfo(final String[] fileInfo) {
        fileInfo[0] = new Regex(correctedBR, "<title>([^<>\"]+) free download at Racaty\\.[A-Za-z0-9]+</title>").getMatch(0);
        if (StringUtils.isEmpty(fileInfo[0])) {
            fileInfo[0] = new Regex(correctedBR, "<h5>Identity</h5><p>([^<>\"]+)</p>").getMatch(0);
        }
        fileInfo[1] = new Regex(correctedBR, "b>Size:</b> (\\d+(?:\\.\\d+)? .*?)\\s+").getMatch(0);
        if (fileInfo[1] == null) {
            /* 2020-03-31 */
            fileInfo[1] = new Regex(correctedBR, ">Size\\s*:([^<>\"]+) / Uploaded:").getMatch(0);
        }
        super.scanInfo(fileInfo);
        return fileInfo;
    }

    @Override
    public String regexFilenameAbuse(final Browser br) {
        /* 2020-03-31: Special */
        String filename = br.getRegex("<title>([^<>\"]+) free download at Racaty\\.com</title>").getMatch(0);
        if (filename == null) {
            filename = super.regexFilenameAbuse(br);
        }
        return filename;
    }

    @Override
    public String getLoginURL() {
        return this.getMainPage() + "/?op=login_ajax";
    }

    @Override
    protected boolean supports_availablecheck_filesize_html() {
        return false;
    }
}
