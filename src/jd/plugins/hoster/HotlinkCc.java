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
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class HotlinkCc extends XFileSharingProBasic {
    public HotlinkCc(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: 2019-05-11: null<br />
     * other: 2019-05-09: Login via username&pw not possible anymore, only via EMAIL&PASSWORD! <br />
     */
    private static String[] domains = new String[] { "hotlink.cc" };

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
            return 1;
        }
    }

    @Override
    public int getMaxSimultaneousFreeAnonymousDownloads() {
        return 1;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public Form findFormF1() {
        /* 2019-05-15: Special */
        final Form formf1Free = super.findFormF1();
        /* Should only be required for premium but we're doing it for free mode anyways! */
        fixFormF1(formf1Free);
        return formf1Free;
    }

    @Override
    public Form findFormF1Premium() throws Exception {
        /* 2019-05-15: Special */
        final Form formf1Premium = super.findFormF1Premium();
        fixFormF1(formf1Premium);
        return formf1Premium;
    }

    private Form fixFormF1(final Form formf1) {
        if (formf1 != null && formf1.hasInputFieldByName("dl_bittorrent")) {
            formf1.remove("dl_bittorrent");
            formf1.put("dl_bittorrent", "0");
        }
        return formf1;
    }

    @Override
    public ArrayList<String> getCleanupHTMLRegexes() {
        /*
         * 2019-05-15: Special: Return empty Array as default values will kill free mode of this plugin (important html code will get
         * removed!)
         */
        return new ArrayList<String>();
    }

    @Override
    public String regexWaittime() {
        /* 2019-05-15: Special */
        String waitStr = super.regexWaittime();
        if (StringUtils.isEmpty(waitStr)) {
            waitStr = new Regex(correctedBR, "class=\"seconds yellow\"><b>(\\d+)</b>").getMatch(0);
            if (StringUtils.isEmpty(waitStr)) {
                waitStr = new Regex(correctedBR, "class=\"seconds[^\"]+\"><b>(\\d+)</b>").getMatch(0);
            }
        }
        return waitStr;
    }

    @Override
    public void doFree(DownloadLink link, Account account) throws Exception, PluginException {
        if (checkShowFreeDialog(getHost())) {
            showFreeDialog(getHost());
        }
        super.doFree(link, account);
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
    public boolean supports_precise_expire_date() {
        return super.supports_precise_expire_date();
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
    public boolean supports_availablecheck_filename_abuse() {
        return super.supports_availablecheck_filename_abuse();
    }

    @Override
    public boolean supports_availablecheck_filesize_html() {
        return super.supports_availablecheck_filesize_html();
    }

    @Override
    public String regExTrafficLeft() {
        String trafficleft = super.regExTrafficLeft();
        if (StringUtils.isEmpty(trafficleft)) {
            trafficleft = new Regex(correctedBR, "Traffic available today</TD></TR>\\s*?</thead>\\s*?<TR><TD><b>([^<>\"]+)</b><").getMatch(0);
        }
        return trafficleft;
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