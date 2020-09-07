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

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class AbcvideoCc extends XFileSharingProBasic {
    public AbcvideoCc(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: 2020-06-19: reCaptchaV2<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "abcvideo.cc" });
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
        return XFileSharingProBasic.buildAnnotationUrls(getPluginDomains());
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
    protected boolean isVideohoster_enforce_video_filename() {
        /* 2020-03-17: Special */
        return true;
    }

    @Override
    protected boolean isVideohosterEmbed() {
        return true;
    }

    @Override
    protected boolean useRUA() {
        /* 2020-09-07: Cat & mouse games */
        return true;
    }

    @Override
    public Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(this.browserPrepped.containsKey(prepBr) && this.browserPrepped.get(prepBr) == Boolean.TRUE)) {
            /* 2020-09-07: Cat & mouse games */
            prepBr.setCookie(getMainPage(), "game", "checker");
        }
        return prepBr;
    }

    @Override
    protected String requestFileInformationVideoEmbed(final DownloadLink link, final Account account, final boolean findFilesize) throws Exception {
        String dllink = getDllink(link, account, br, correctedBR);
        final Browser brc = this.br.cloneBrowser();
        if (StringUtils.isEmpty(dllink)) {
            if (findFilesize) {
                /* Do not ask for captchas in during availablecheck! */
                return null;
            }
            /* 2020-06-19: Special */
            final String getvideo = br.getRegex("(/dl\\?op=video_src\\&file_code=" + this.fuid + "&g-recaptcha-response=)").getMatch(0);
            if (getvideo == null) {
                return null;
            }
            final CaptchaHelperHostPluginRecaptchaV2 rc2 = getCaptchaHelperHostPluginRecaptchaV2(this, br);
            final String token = rc2.getToken();
            this.getPage(brc, getvideo + Encoding.urlEncode(token));
            // if (brc.getURL() != null && !brc.getURL().contains("/embed")) {
            // final String embed_access = getMainPage() + "/embed-" + fuid + ".html";
            // getPage(brc, embed_access);
            // /**
            // * 2019-07-03: Example response when embedding is not possible (deactivated or it is not a video-file): "Can't create video
            // * code" OR "Video embed restricted for this user"
            // */
            // }
            // /*
            // * Important: Do NOT use 404 as offline-indicator here as the website-owner could have simply disabled embedding while it was
            // * enabled before --> This would return 404 for all '/embed' URLs! Only rely on precise errormessages!
            // */
            // if (brc.toString().equalsIgnoreCase("File was deleted")) {
            // /* Should be valid for all XFS hosts e.g. speedvideo.net, uqload.com */
            // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            // }
            dllink = getDllink(link, account, brc, brc.toString());
            // final String url_thumbnail = getVideoThumbnailURL(br.toString());
        }
        if (findFilesize && !StringUtils.isEmpty(dllink) && !dllink.contains(".m3u8")) {
            /* Get- and set filesize from directurl */
            final boolean dllink_is_valid = checkDirectLinkAndSetFilesize(link, dllink, true) != null;
            /* Store directurl if it is valid */
            if (dllink_is_valid) {
                storeDirecturl(link, account, dllink);
            }
        }
        return dllink;
    }
}