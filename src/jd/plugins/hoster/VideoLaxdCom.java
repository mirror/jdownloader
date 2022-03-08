//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.TimeFormatter;

import jd.PluginWrapper;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class VideoLaxdCom extends VideoFCTwoCore {
    public VideoLaxdCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://home.laxd.com/signup.php?switch_language=en&ref=video");
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "video.laxd.com" });
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
            ret.add("https?://" + buildHostsPatternPart(domains) + "/(?:en/)?a/content/\\w+");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://help.fc2.com/common/tos/en/";
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    protected String getAccountNameSpaceLogin() {
        return "https://login.laxd.com/?ref=video";
    }

    @Override
    protected String getAccountNameSpacePremium() {
        return "https://video.laxd.com/a/payment/laxd_premium/";
    }

    @Override
    protected String getAccountNameSpaceForLoginCheck() {
        return "https://" + this.getHost() + "/en/";
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        synchronized (account) {
            final AccountInfo ai = new AccountInfo();
            this.login(account, true, this.getAccountNameSpaceForLoginCheck());
            final String namespacePremium = getAccountNameSpacePremium();
            final String relativeURL_Payment = new URL(namespacePremium).getPath();
            if (!br.getURL().contains(relativeURL_Payment)) {
                br.getPage(relativeURL_Payment);
            }
            /* Check for multiple traits - we want to make sure that we correctly recognize premium accounts! */
            boolean isPremium = br.containsHTML("/a/payment/laxd_premium/cancel/");
            final Regex expireRegex = br.getRegex("(?i)<dt>有効期限</dt><dd>(\\d+).(\\d{2}).(\\d{2})");
            if (!isPremium && expireRegex.matches()) {
                isPremium = true;
            }
            if (isPremium) {
                /* Only set expire date if we find one */
                if (expireRegex.matches()) {
                    final String expire = expireRegex.getMatch(0) + "/" + expireRegex.getMatch(1) + "/" + expireRegex.getMatch(2);
                    ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy/MM/dd", Locale.ENGLISH), this.br);
                }
                account.setType(AccountType.PREMIUM);
            } else {
                account.setType(AccountType.FREE);
            }
            ai.setUnlimitedTraffic();
            return ai;
        }
    }
}