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

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.utils.locale.JDL;

import org.jdownloader.plugins.components.XFileSharingProBasic;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class DdlTo extends XFileSharingProBasic {
    private final String   MaxSimultaneousDownloads_LIMIT = "MaxSimultaneousDownloads_LIMIT";
    private final String[] MaxSimultaneousDownloads       = new String[] { "2", "1" };

    public DdlTo(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
        setConfigElements();
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: 2019-05-22: premium untested, set FREE account limits <br />
     * captchatype-info: 2019-05-22: null<br />
     * other:<br />
     */
    private static String[] domains = new String[] { "ddl.to", "wi.to" };

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

    public int getMaxDownloadSelect() {
        final int chosenDownloadLimit = getPluginConfig().getIntegerProperty(MaxSimultaneousDownloads_LIMIT, 0);
        try {
            return Integer.parseInt(MaxSimultaneousDownloads[chosenDownloadLimit]);
        } catch (final Throwable e) {
            logger.log(e);
            return Integer.parseInt(MaxSimultaneousDownloads[0]);
        }
    }

    @Override
    public int getMaxSimultaneousFreeAnonymousDownloads() {
        return getMaxDownloadSelect();
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return getMaxDownloadSelect();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 2;
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
    public boolean supports_availablecheck_alt() {
        return super.supports_availablecheck_alt();
    }

    @Override
    public boolean supports_availablecheck_filename_abuse() {
        return super.supports_availablecheck_filename_abuse();
    }

    @Override
    public boolean supports_availablecheck_filesize_html() {
        return super.supports_availablecheck_filesize_html();
    }

    @Override
    public boolean supports_availablecheck_filesize_via_embedded_video() {
        return super.supports_availablecheck_filesize_via_embedded_video();
    }

    public static String[] getAnnotationNames() {
        /*
         * only return the first/valid domain, else rewrite won't happen when the other domain is still signaled as existing!
         */
        return new String[] { domains[0] };
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
                ret.add("https?://(?:www\\.)?" + getHostsPatternPart() + "/(?:embed\\-)?[a-z0-9]{12}(?:/[^/]+\\.html)?");
            } else {
                break;
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

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), MaxSimultaneousDownloads_LIMIT, MaxSimultaneousDownloads, JDL.L("", "Max. Downloads (Free) per connection")).setDefaultValue(0));
    }
}