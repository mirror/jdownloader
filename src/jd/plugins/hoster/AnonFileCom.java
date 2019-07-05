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

import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

import org.jdownloader.plugins.components.UnknownHostingScriptCore;

@HostPlugin(revision = "$Revision $", interfaceVersion = 2, names = {}, urls = {})
public class AnonFileCom extends UnknownHostingScriptCore {
    private final String   MaxSimultaneousDownloads_LIMIT = "MaxSimultaneousDownloads_LIMIT";
    private final String[] MaxSimultaneousDownloads       = new String[] { "Unlimited", "1", "2", "3", "4", "5" };

    public AnonFileCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
        setConfigElements();
    }

    /**
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: null<br />
     * other: 2019-05-15: NOT RELATED TO anonfiles.com!!!<br />
     */
    /* 1st domain = current domain! */
    public static String[] domains = new String[] { "anonfile.com" };

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
            return 0;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return 0;
        } else {
            /* Free(anonymous) and unknown account type */
            return 0;
        }
    }

    public int getMaxDownloadSelect() {
        final int chosenDownloadLimit = getPluginConfig().getIntegerProperty(MaxSimultaneousDownloads_LIMIT, 0);
        try {
            if (chosenDownloadLimit > 0) {
                return Integer.parseInt(MaxSimultaneousDownloads[chosenDownloadLimit]);
            } else {
                return -1;
            }
        } catch (final Throwable e) {
            logger.log(e);
            return -1;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return getMaxDownloadSelect();
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return getMaxDownloadSelect();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public boolean supports_availablecheck_via_api() {
        return super.supports_availablecheck_via_api();
    }

    public static String[] getAnnotationNames() {
        return new String[] { domains[0] };
    }

    /**
     * returns the annotation pattern array: 'https?://(?:www\\.)?(?:domain1|domain2)/[A-Za-z0-9]+'
     *
     */
    /**
     * returns the annotation pattern array: 'https?://(?:www\\.)?(?:domain1|domain2)/[A-Za-z0-9]+(?:/[^/]+)?'
     *
     */
    public static String[] getAnnotationUrls() {
        // construct pattern
        final String host = getHostsPattern();
        return new String[] { host + "/[A-Za-z0-9]+(?:/[^/<>]+)?" };
    }

    /** Returns '(?:domain1|domain2)' */
    private static String getHostsPatternPart() {
        final StringBuilder pattern = new StringBuilder();
        for (final String name : domains) {
            pattern.append((pattern.length() > 0 ? "|" : "") + Pattern.quote(name));
        }
        return pattern.toString();
    }

    /** returns 'https?://(?:www\\.)?(?:domain1|domain2)' */
    private static String getHostsPattern() {
        final String hosts = "https?://(?:www\\.)?" + "(?:" + getHostsPatternPart() + ")";
        return hosts;
    }

    @Override
    public String[] siteSupportedNames() {
        return domains;
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), MaxSimultaneousDownloads_LIMIT, MaxSimultaneousDownloads, "Max. simultaneous downloads (Free+Free account)").setDefaultValue(0));
    }
}