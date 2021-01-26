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
import jd.http.Browser;
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
            return 1;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return -10;
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
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public Form findFormDownload2Free() {
        /* 2019-05-15: Special */
        final Form formf1Free = super.findFormDownload2Free();
        /* Should only be required for premium but we're doing it for free mode anyways! */
        fixFormF1(formf1Free);
        return formf1Free;
    }

    @Override
    public Form findFormDownload2Premium() throws Exception {
        /* 2019-05-15: Special */
        Form formf1Premium = super.findFormDownload2Premium();
        if (formf1Premium == null) {
            /* 2021-01-26 */
            formf1Premium = br.getFormByInputFieldKeyValue("op", "download_orig");
        }
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
    public String regExTrafficLeft() {
        String trafficleft = super.regExTrafficLeft();
        if (StringUtils.isEmpty(trafficleft)) {
            trafficleft = new Regex(correctedBR, "Traffic available today</TD></TR>\\s*?</thead>\\s*?<TR><TD><b>\\s*([^<>\"]+)\\s*</b><").getMatch(0);
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

    @Override
    public boolean isPremiumOnly() {
        /*
         * 2020-01-30: Special because template code matches also on ">\\s*Available Only for Premium Members" which is always present in
         * their html
         */
        final boolean premiumonly_filehost = new Regex(correctedBR, "( can download files up to |>\\s*Upgrade your account to download (?:larger|bigger) files|>\\s*The file you requested reached max downloads limit for Free Users|Please Buy Premium To download this file\\s*<|This file reached max downloads limit|>\\s*This file is available for Premium Users only|>File is available only for Premium users|>\\s*This file can be downloaded by)").matches();
        /* 2019-05-30: Example: xvideosharing.com */
        final boolean premiumonly_videohost = new Regex(correctedBR, ">\\s*This video is available for Premium users only").matches();
        return premiumonly_filehost || premiumonly_videohost;
    }

    @Override
    protected int getDllinkViaOfficialVideoDownloadExtraWaittimeSeconds() {
        /** 2021-01-20: Tested in premium mode by user */
        return 2;
    }

    public String[] scanInfo(final String[] fileInfo) {
        /* 2021-01-22: Prefer this as template will pickup filename without extension */
        fileInfo[0] = new Regex(correctedBR, "<i class=\"glyphicon glyphicon-download\"></i>([^<>\"]+)<").getMatch(0);
        return super.scanInfo(fileInfo);
    }

    private boolean premiumWorkaroundActive = false;
    // @Override
    // protected String requestFileInformationVideoEmbed(final DownloadLink link, final Account account, final boolean findFilesize) throws
    // Exception {
    // if (premiumWorkaroundActive) {
    // return null;
    // } else {
    // return super.requestFileInformationVideoEmbed(link, account, findFilesize);
    // }
    // }

    @Override
    protected String getDllinkVideohost(final String src) {
        if (premiumWorkaroundActive) {
            return null;
        } else {
            return super.getDllinkVideohost(src);
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        /**
         * 2021-01-26: They're providing AES encrypted videostreaming to premium users. By default, this is the preferred download method
         * but it will fail -> This works around it
         */
        if (account.getType() == AccountType.PREMIUM) {
            premiumWorkaroundActive = true;
            try {
                super.handlePremium(link, account);
            } finally {
                premiumWorkaroundActive = false;
            }
        } else {
            premiumWorkaroundActive = false;
        }
    }

    @Override
    protected String getDllink(final DownloadLink link, final Account account, final Browser br, String src) {
        /** 2021-01-26: Special */
        String dllink = new Regex(correctedBR, "href=\"(https?://[^\"]+)\"[^>]*>Direct Download Link<").getMatch(0);
        if (dllink != null) {
            return dllink;
        } else {
            /* Fallback to template handling */
            return super.getDllink(link, account, br, src);
        }
    }
}