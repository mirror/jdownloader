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
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class VidtodoCom extends XFileSharingProBasic {
    public VidtodoCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: 2019-07-04: null<br />
     * other:<br />
     */
    private static String[] domains = new String[] { "vidtodo.com", "vidtodo.me", "vidtodo.pro", "vidtodoo.com", "vidtodoo.me", "vidtodoo.pro", "vidotodo.com", "vidotodo.me", "vidotodo.pro", "vidtodu.com", "vidtodu.me", "vidtodu.pro", "vidtoro.com", "vidtoro.me", "vidtoro.pro" };

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
            return -2;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return -2;
        } else {
            /* Free(anonymous) and unknown account type */
            return -2;
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
        /* 2019-07-04: Special: Upper class will return bad filenames! */
        super.scanInfo(fileInfo);
        // final String upper_class_filename_result = fileInfo[0];
        final String new_filename_result = new Regex(correctedBR, "id=\"content\" class=\"left\">\\s*?<h2>([^<>\"]+)</h2> ").getMatch(0);
        if (!StringUtils.isEmpty(new_filename_result)) {
            fileInfo[0] = new_filename_result;
        }
        return fileInfo;
    }

    @Override
    public boolean supports_https() {
        return super.supports_https();
    }

    @Override
    public boolean isVideohosterEmbed() {
        return super.isVideohosterEmbed();
    }

    @Override
    public boolean isVideohoster_enforce_video_filename() {
        return super.isVideohoster_enforce_video_filename();
    }

    @Override
    public boolean supports_availablecheck_alt() {
        return super.supports_availablecheck_alt();
    }

    @Override
    public boolean supports_availablecheck_filename_abuse() {
        return super.supports_availablecheck_filename_abuse();
    }

    @Override
    public boolean supports_availablecheck_filesize_html() {
        return false;
    }

    @Override
    public boolean supports_availablecheck_filesize_via_embedded_video() {
        /* 2019-07-04: Special */
        return true;
    }

    public static String[] getAnnotationNames() {
        return domains;
    }

    @Override
    public String[] siteSupportedNames() {
        return domains;
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (int i = 0; i < domains.length; i++) {
            if (i == 0) {
                /* Match all URLs on first (=current) domain */
                ret.add("https?://(?:www\\.)?" + getHostsPatternPart() + XFileSharingProBasic.getDefaultAnnotationPatternPart());
            } else {
                ret.add("");
            }
        }
        return ret.toArray(new String[0]);
    }

    /** Returns '(?:domain1|domain2)' */
    public static String getHostsPatternPart() {
        final StringBuilder pattern = new StringBuilder();
        pattern.append("(?:");
        for (final String name : domains) {
            pattern.append((pattern.length() > 0 ? "|" : "") + Pattern.quote(name));
        }
        pattern.append(")");
        return pattern.toString();
    }
}