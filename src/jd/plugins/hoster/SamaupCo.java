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

import java.util.regex.Pattern;

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
public class SamaupCo extends XFileSharingProBasic {
    public SamaupCo(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: 2019-05-08: Account untested (registration was broken), set FREE limits <br />
     * captchatype-info: 2019-05-08: null<br />
     * other:<br />
     */
    private static String[] domains = new String[] { "samaup.co", "samaup.com", "samaup.cc" };

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
    public int getMaxSimultaneousFreeAnonymousDownloads() {
        return 4;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return 4;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 4;
    }

    @Override
    public String[] scanInfo(final String[] fileInfo) {
        /* 2019-05-08: Special: Especially required for: Filenames of Picture-URLs, filesizes */
        super.scanInfo(fileInfo);
        if (StringUtils.isEmpty(fileInfo[0])) {
            fileInfo[0] = new Regex(correctedBR, "<img id=\"img\\-preview\"[^>]+alt=\"([^<>\"]+)\" onclick=\"showFullScreen\\(\\)\">").getMatch(0);
        }
        if (StringUtils.isEmpty(fileInfo[0])) {
            fileInfo[1] = new Regex(correctedBR, "<li>size : (\\d+(?:(?:\\.|,)\\d+)? [A-Za-z]+)").getMatch(0);
        }
        return fileInfo;
    }

    @Override
    public String regexFilenameAbuse(final Browser br) {
        String filename = super.regexFilenameAbuse(br);
        if (StringUtils.isEmpty(filename)) {
            filename = br.getRegex("name=\"file_name\" value=\"([^<>\"]+)\"").getMatch(0);
        }
        return filename;
    }

    @Overrideprotected boolean supports_availablecheck_filesize_html() {
        /* 2019-05-08: Special */
        return false;
    }

    public static String[] getAnnotationNames() {
        return new String[] { domains[0] };
    }

    @Override
    public String[] siteSupportedNames() {
        return domains;
    }

    /**
     * returns the annotation pattern array: 'https?://(?:www\\.)?(?:domain1|domain2)/(?:embed\\-)?[a-z0-9]{12}'
     *
     */
    public static String[] getAnnotationUrls() {
        // construct pattern
        final String host = getHostsPattern();
        return new String[] { host + "/(?:embed\\-)?[a-z0-9]{12}(?:/[^/]+\\.html)?" };
    }

    /** returns 'https?://(?:www\\.)?(?:domain1|domain2)' */
    private static String getHostsPattern() {
        final String hosts = "https?://(?:www\\.)?" + "(?:" + getHostsPatternPart() + ")";
        return hosts;
    }

    /** Returns '(?:domain1|domain2)' */
    public static String getHostsPatternPart() {
        final StringBuilder pattern = new StringBuilder();
        for (final String name : domains) {
            pattern.append((pattern.length() > 0 ? "|" : "") + Pattern.quote(name));
        }
        return pattern.toString();
    }
}