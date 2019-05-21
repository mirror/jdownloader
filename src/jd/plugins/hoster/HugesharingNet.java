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

import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class HugesharingNet extends XFileSharingProBasic {
    public HugesharingNet(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: 2019-02-19: Premium- and FREE account untested, set FREE limits<br />
     * captchatype-info: 2019-02-19: null<br />
     * other: Special: This host migrated from YetiShare to XFS so this plugin accepts their old YetiShare URLs which redirect to new XFS
     * URLs!<br />
     */
    private static String[] domains = new String[] { "hugesharing.net" };

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
        return 8;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return 8;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 8;
    }

    @Override
    public boolean supports_https() {
        return super.supports_https();
    }

    @Override
    public boolean supports_precise_expire_date() {
        return super.supports_precise_expire_date();
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
    public boolean isImagehoster() {
        return super.isImagehoster();
    }

    @Override
    public boolean supports_availablecheck_alt() {
        return super.supports_availablecheck_alt();
    }

    @Override
    public boolean supports_availablecheck_filesize_alt_fast() {
        return super.supports_availablecheck_filesize_alt_fast();
    }

    @Override
    public boolean prefer_availablecheck_filesize_alt_type_old() {
        return super.prefer_availablecheck_filesize_alt_type_old();
    }

    @Override
    public boolean supports_availablecheck_filename_abuse() {
        return super.supports_availablecheck_filename_abuse();
    }

    @Override
    public boolean supports_availablecheck_filesize_html() {
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
     * returns the annotation pattern array, usually: 'https?://(?:www\\.)?(?:domain1|domain2)/(?:embed\\-)?[a-z0-9]{12}(?:/[^/]+\\.html)?'
     *
     */
    public static String[] getAnnotationUrls() {
        // construct pattern
        final String host = getHostsPattern();
        return new String[] { host + "/((?!users)[A-Za-z0-9]+/[^/]+|(?:embed\\-)?[a-z0-9]{12}(?:/[^/]+\\.html)?)" };
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