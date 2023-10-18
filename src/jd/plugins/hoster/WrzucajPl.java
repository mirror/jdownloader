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

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.YetiShareCore;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class WrzucajPl extends YetiShareCore {
    public WrzucajPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(getPurchasePremiumURL());
    }

    /**
     * DEV NOTES YetiShare<br />
     ****************************
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: 2023-03-21: null <br />
     * other: <br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        // sister site of: wrzucajpliki.pl
        ret.add(new String[] { "wrzucaj.pl" });
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
        return YetiShareCore.buildAnnotationUrls(getPluginDomains());
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
    protected AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        final AccountInfo ai = super.fetchAccountInfoWebsite(account);
        /* 2023-10-18 */
        this.getPage("/account/edit");
        final String availableTrafficHTML = br.getRegex("<div([^>]+)>-</div>\\s*<h3>\\s*Available transfer\\s*</h3>").getMatch(0);
        if (availableTrafficHTML != null) {
            final Regex trafficmaxRegex = new Regex(availableTrafficHTML, "data-postfix=\"\\&nbsp;/ (\\d+(\\.\\d+)?) ([A-Za-z]+)\"");
            final String trafficAvailableStr = new Regex(availableTrafficHTML, "data-end=\"(\\d+(\\.\\d+)?)\"").getMatch(0);
            if (trafficmaxRegex.patternFind() && trafficAvailableStr != null) {
                final String trafficMaxStr = trafficmaxRegex.getMatch(0);
                final String trafficUnit = trafficmaxRegex.getMatch(1);
                ai.setTrafficLeft(SizeFormatter.getSize(trafficAvailableStr + trafficUnit));
                ai.setTrafficMax(SizeFormatter.getSize(trafficMaxStr + trafficUnit));
            } else {
                logger.warning("Failed to find traffic information inside availableTrafficHTML");
            }
        } else {
            logger.warning("Failed to find availableTrafficHTML");
        }
        return ai;
    }
}