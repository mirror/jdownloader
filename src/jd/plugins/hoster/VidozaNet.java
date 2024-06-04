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

import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class VidozaNet extends XFileSharingProBasic {
    public VidozaNet(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: null (official download has reCaptchaV2)<br />
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
        ret.add(new String[] { "vidoza.net", "vidoza.org", "videzz.net" });
        return ret;
    }

    @Override
    public String[] scanInfo(final String[] fileInfo) {
        super.scanInfo(fileInfo);
        final String betterFilename = br.getRegex("curFileName\\s*(?:=|:)\\s*\"([^\"]+)\"").getMatch(0);
        if (StringUtils.isNotEmpty(betterFilename)) {
            fileInfo[0] = betterFilename;
        }
        return fileInfo;
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
        return 5;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return 5;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 5;
    }

    @Override
    protected boolean isOffline(final DownloadLink link, final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404) {
            return true;
        } else if (br.containsHTML(">\\s*Conversion stage\\s*:") && br.containsHTML("<title>\\s*Watch\\s*</title>")) {
            /* 2024-04-23: Special offline - embed-only item which is actually offline e.g.: https://videzz.net/3ka8sbjnm59j */
            return true;
        } else if (br.containsHTML("/embed-\\.html\"|Reason for deletion:")) {
            return true;
        } else {
            return super.isOffline(link, br);
        }
    }

    @Override
    public boolean supports_availablecheck_filename_abuse() {
        /* 2019-07-04: Special */
        return false;
    }

    @Override
    protected boolean isVideohoster_enforce_video_filename() {
        return true;
    }

    @Override
    protected boolean isVideohosterEmbed() {
        return true;
    }

    @Override
    protected boolean trustAvailablecheckVideoEmbed() {
        final boolean testThis = false;
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE && testThis) {
            return true;
        } else {
            return false;
        }
    }
}