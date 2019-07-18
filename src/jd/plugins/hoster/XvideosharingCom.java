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

import jd.PluginWrapper;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;

import org.jdownloader.plugins.components.XFileSharingProBasic;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class XvideosharingCom extends XFileSharingProBasic {
    public XvideosharingCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        /* 2019-05-30: Special: redirects to /<fuid>.html !! */
        final boolean followRedirects = this.br.isFollowingRedirects();
        if (!followRedirects) {
            this.br.setFollowRedirects(true);
        }
        final AvailableStatus status = super.requestFileInformation(link);
        this.br.setFollowRedirects(followRedirects);
        return status;
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: 2019-05-30: Tested<br />
     * captchatype-info: 2019-02-08: null<br />
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
        ret.add(new String[] { "xvideosharing.com" });
        return ret;
    }

    @Override
    public boolean supports_availablecheck_alt() {
        /* 2019-07-10: Their mass-linkchecker seems to be broken as it returns OFFLINE no matter which URL is entered! */
        return false;
    }

    @Override
    public boolean supports_https() {
        /* 2019-07-11: Special */
        return false;
    }

    @Override
    protected boolean supports_availablecheck_filesize_html() {
        /* 2019-07-10: Special */
        return false;
    }

    @Override
    public boolean isVideohoster_enforce_video_filename() {
        return true;
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
        return -1;
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
    public ArrayList<String> getCleanupHTMLRegexes() {
        final ArrayList<String> regexStuff = new ArrayList<String>();
        // remove custom rules first!!! As html can change because of generic cleanup rules.
        /* generic cleanup */
        regexStuff.add("<\\!(\\-\\-.*?\\-\\-)>");
        /* 2019-07-10: Do not add this one as we will not be able to find out apikey inside fetchAccountInfoWebsite then! */
        // regexStuff.add("(display: ?none;\">.*?</div>)");
        regexStuff.add("(visibility:hidden>.*?<)");
        return regexStuff;
    }

    @Override
    protected boolean supports_api() {
        /* 2019-07-10: Special: For testing purposes */
        return super.supports_api();
    }

    /** 2019-02-08: Special */
    @Override
    public String getDllink(DownloadLink link, Account account) {
        String dllink = super.getDllink(link, account);
        // if (StringUtils.isEmpty(dllink)) {
        // dllink = new Regex(super.correctedBR, "\"(https?://[^\"]+v\\.mp4)\"").getMatch(0);
        // }
        return dllink;
    }
}