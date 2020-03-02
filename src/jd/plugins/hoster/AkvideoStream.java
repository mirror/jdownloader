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

import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class AkvideoStream extends XFileSharingProBasic {
    public AkvideoStream(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: 2020-01-21: null<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "akvideo.stream" });
        return ret;
    }

    public static String[] buildAnnotationUrls(List<String[]> pluginDomains) {
        /* 2020-01-21: Special */
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:[A-Za-z0-9]+\\.)?" + buildHostsPatternPart(domains) + "/(?:embed\\-|video/)?[a-z0-9]{12}(?:/[^/]+\\.html)?");
        }
        return ret.toArray(new String[0]);
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
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
    protected boolean isVideohoster_enforce_video_filename() {
        /* 2020-01-21: Special */
        return true;
    }

    @Override
    protected String getDllinkViaOfficialVideoDownload(final Browser brc, final DownloadLink link, final Account account, final boolean returnFilesize) throws Exception {
        final String ret = super.getDllinkViaOfficialVideoDownload(brc, link, account, returnFilesize);
        if (ret != null) {
            return ret;
        } else {
            /*
             * 2020-02-04: Rare error. If this happens and we return null, template will fallback to stream download and not download the
             * highest quality! <br><b class="err">Security error</b>
             */
            if (brc.containsHTML(">\\s*Security error")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'Security error'", 1 * 60 * 1000l);
            } else {
                return null;
            }
        }
    }

    @Override
    protected String regexWaittime() {
        /*
         * 2020-02-20: Special: Workaround - if we do not wait some seconds in getDllinkViaOfficialVideoDownload we will get errormessage
         * "Security error".
         */
        return "2";
    }

    @Override
    protected String getDllink(final DownloadLink link, final Account account, final Browser br, String src) {
        /*
         * 2020-02-20: Special: Seems like they sometimes provide fake final downloadurls or failures were caused by forgotten html code of
         * them.
         */
        final String dllink = br.getRegex("class=\"dwbutt\" href=\"(https?://[^<>\"]+)\"\\s*onclick=\"window\\.open").getMatch(0);
        if (dllink != null) {
            logger.info("Found dllink via special RegEx");
            return dllink;
        }
        logger.info("Failed to find dllink via special RegEx");
        return super.getDllink(link, account, br, src);
    }

    @Override
    protected Form findFormDownload2Free() {
        /* 2020-03-02: Special */
        Form dlForm = super.findFormDownload2Free();
        if (dlForm == null) {
            dlForm = br.getFormbyProperty("id", "F1");
        }
        return dlForm;
    }

    @Override
    protected void checkErrors(final DownloadLink link, final Account account, final boolean checkAll) throws NumberFormatException, PluginException {
        /* 2020-03-02: Special --> Support offline after Form F1 */
        if (this.isOffline(link)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        super.checkErrors(link, account, checkAll);
    }
}