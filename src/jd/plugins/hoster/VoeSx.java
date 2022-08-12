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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class VoeSx extends XFileSharingProBasic {
    public VoeSx(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: 2020-11-27: Premium untested, set FREE limits <br />
     * captchatype-info: 2020-08-19: null<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "voe.sx", "voe-unblock.com", "voe-unblock.net", "voeunblock.com", "voeunblk.com", "voeunblck.com", "voe-un-block.com", "un-block-voe.net", "voeunbl0ck.com", "voeunblock1.com", "voeunblock2.com", "voeunblock3.com", "voeunblock4.com", "voeunblock5.com", "voeunblock6.com", "voeun-block.net", "v-o-e-unblock.com", "audaciousdefaulthouse.com", "launchreliantcleaverriver.com", "reputationsheriffkennethsand.com", "fittingcentermondaysunday.com" });
        return ret;
    }

    public static final String getDefaultAnnotationPatternPartVoeSx() {
        return "/(?:embed-|e/)?[a-z0-9]{12}(?:/[^/]+(?:\\.html)?)?";
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + VoeSx.getDefaultAnnotationPatternPartVoeSx());
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
        return VoeSx.buildAnnotationUrls(getPluginDomains());
    }

    @Override
    public String getFUIDFromURL(final DownloadLink dl) {
        try {
            final String result = new Regex(new URL(dl.getPluginPatternMatcher()).getPath(), "/(?:embed-|e/)?([a-z0-9]{12})").getMatch(0);
            return result;
        } catch (MalformedURLException e) {
            logger.log(e);
        }
        return null;
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return true;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return true;
        }
    }

    @Override
    public int getMaxChunks(final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return -5;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return -5;
        } else {
            /* Free(anonymous) and unknown account type */
            return -5;
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
        /* 2020-08-19 */
        return true;
    }

    @Override
    protected boolean supportsAPIMassLinkcheck() {
        return isAPIKey(this.getAPIKey());
    }

    @Override
    protected boolean supportsAPISingleLinkcheck() {
        return isAPIKey(this.getAPIKey());
    }

    @Override
    protected boolean isVideohosterEmbed() {
        /* 2021-03-09 */
        return true;
    }

    @Override
    protected String getDllinkVideohost(final String src) {
        /** 2021-03-01: Prefer HLS over HTTP as they've hidden their http URLs via js */
        final String hlsMaster = new Regex(src, "\"hls\"\\s*:\\s*\"(https?://[^\"]+)").getMatch(0);
        if (hlsMaster != null) {
            return hlsMaster;
        } else {
            return super.getDllinkVideohost(src);
        }
    }

    @Override
    protected void checkErrors(final Browser br, final String html, final DownloadLink link, final Account account, final boolean checkAll) throws NumberFormatException, PluginException {
        super.checkErrors(br, html, link, account, checkAll);
        // if (br.containsHTML(">\\s*This video can be watched as embed only")) {
        // throw new PluginException(LinkStatus.ERROR_FATAL, "This video can be watched as embed only");
        // }
    }

    @Override
    protected String requestFileInformationVideoEmbed(final Browser br, final DownloadLink link, final Account account, final boolean findFilesize) throws Exception {
        /* 2021-03-09: Special: New browser required else they won't let us stream some videos at all! */
        final boolean embedOnly = br.containsHTML(">\\s*This video can be watched as embed only");
        br.setFollowRedirects(true);
        br.getPage("https://" + this.getHost() + "/e/" + this.getFUIDFromURL(link));
        final String dllink = getDllinkVideohost(br.toString());
        if (StringUtils.isEmpty(dllink) && embedOnly) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "This video can be watched as embed only");
        }
        return dllink;
    }
}