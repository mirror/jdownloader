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
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class DoodstreamCom extends XFileSharingProBasic {
    public DoodstreamCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: 2020-08-31: null<br />
     * other:<br />
     */
    private static final String TYPE_STREAM   = "https?://[^/]+/e/.+";
    private static final String TYPE_DOWNLOAD = "https?://[^/]+/d/.+";

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "dood.so", "doodstream.com", "dood.to", "doodapi.com", "dood.watch" });
        return ret;
    }

    @Override
    public String rewriteHost(final String host) {
        /* 2021-01-15: Main domain has changed from doodstream.com to dood.so */
        return this.rewriteHost(getPluginDomains(), host);
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return DoodstreamCom.buildAnnotationUrls(getPluginDomains());
    }

    public static final String getDefaultAnnotationPatternPartDoodstream() {
        return "/(?:e|d)/[a-z0-9]+";
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + DoodstreamCom.getDefaultAnnotationPatternPartDoodstream());
        }
        return ret.toArray(new String[0]);
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
            return -2;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return -2;
        } else {
            /* Free(anonymous) and unknown account type */
            return -2;
        }
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        final String linkpart = new Regex(link.getPluginPatternMatcher(), "https?://[^/]+/(.+)").getMatch(0);
        if (linkpart != null) {
            link.setPluginPatternMatcher(getMainPage() + "/" + linkpart);
        }
    }

    @Override
    public int getMaxSimultaneousFreeAnonymousDownloads() {
        return 10;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return 10;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 10;
    }

    @Override
    protected boolean isVideohoster_enforce_video_filename() {
        /* 2020-08-31: Special */
        return true;
    }

    @Override
    public String getFUIDFromURL(final DownloadLink dl) {
        try {
            final String result = new Regex(new URL(dl.getPluginPatternMatcher()).getPath(), "([a-z0-9]+)$").getMatch(0);
            return result;
        } catch (MalformedURLException e) {
            logger.log(e);
        }
        return null;
    }

    @Override
    public String getFilenameFromURL(final DownloadLink dl) {
        return null;
    }

    @Override
    protected boolean supports_availablecheck_alt() {
        return false;
    }

    @Override
    protected boolean supports_availablecheck_filesize_alt_fast() {
        return false;
    }

    @Override
    protected boolean supports_availablecheck_filename_abuse() {
        return false;
    }

    @Override
    protected boolean isOffline(final DownloadLink link) {
        boolean offline = super.isOffline(link);
        if (!offline) {
            /* 2020-10-05: Special: Empty "embed URL". */
            offline = new Regex(correctedBR, "<iframe src=\"/e/\"").matches();
        }
        return offline;
    }

    @Override
    public AvailableStatus requestFileInformationWebsite(final DownloadLink link, final Account account, final boolean downloadsStarted) throws Exception {
        correctDownloadLink(link);
        /* First, set fallback-filename */
        if (!link.isNameSet()) {
            setWeakFilename(link);
        }
        this.br.setFollowRedirects(true);
        getPage(link.getPluginPatternMatcher());
        /* Allow redirects to other content-IDs but files should be offline if there is e.g. a redirect to an unsupported URL format. */
        if (isOffline(link) || !this.canHandle(this.br.getURL())) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (link.getPluginPatternMatcher().matches(TYPE_STREAM)) {
            /* First try to get filename from Chromecast json */
            String filename = new Regex(correctedBR, "<title>\\s*([^<>\"]+)\\s*-\\s*DoodStream\\.com\\s*</title>").getMatch(0);
            if (filename == null) {
                filename = new Regex(correctedBR, "<meta name\\s*=\\s*\"og:title\"[^>]*content\\s*=\\s*\"([^<>\"]+)\"\\s*>").getMatch(0);
            }
            if (StringUtils.isEmpty(filename)) {
                link.setName(this.getFallbackFilename(link));
            } else {
                if (!filename.endsWith(".mp4")) {
                    filename += ".mp4";
                }
                link.setFinalFileName(filename);
            }
        } else {
            String filename = br.getRegex("<meta name\\s*=\\s*\"og:title\"[^>]*content\\s*=\\s*\"([^<>\"]+)\"\\s*>").getMatch(0);
            if (StringUtils.isEmpty(filename)) {
                link.setName(this.getFallbackFilename(link));
            } else {
                if (!filename.endsWith(".mp4")) {
                    filename += ".mp4";
                }
                link.setFinalFileName(filename);
            }
            final String filesize = br.getRegex("class\\s*=\\s*\"size\">.*?</i>\\s*([^<>\"]+)\\s*<").getMatch(0);
            if (!StringUtils.isEmpty(filesize)) {
                link.setDownloadSize(SizeFormatter.getSize(filesize));
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void doFree(final DownloadLink link, final Account account) throws Exception, PluginException {
        /* First bring up saved final links */
        final String directlinkproperty = getDownloadModeDirectlinkProperty(account);
        /*
         * 2019-05-21: TODO: Maybe try download right away instead of checking this here --> This could speed-up the
         * download-start-procedure!
         */
        String dllink = checkDirectLink(link, directlinkproperty);
        if (StringUtils.isEmpty(dllink)) {
            if (link.getPluginPatternMatcher().matches(TYPE_DOWNLOAD)) {
                /* Basically the same as the other type but hides that via iFrame. */
                final String embedURL = br.getRegex("<iframe[^>]*src=\"(/e/[a-z0-9]+)\"").getMatch(0);
                if (embedURL == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                this.getPage(embedURL);
            }
            br.getHeaders().put("x-requested-with", "XMLHttpRequest");
            final String continue_url = br.getRegex("'(/pass_md5/[^<>\"\\']+)'").getMatch(0);
            if (continue_url == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String token = br.getRegex("\\&token=([a-z0-9]+)").getMatch(0);
            if (token == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            this.getPage(continue_url);
            /* Make sure we got a valid URL befopre continuing! */
            final URL dlurl = new URL(br.toString());
            // final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
            dllink = dlurl.toString();
            dllink += "?token=" + token + "&expiry=" + System.currentTimeMillis();
        }
        handleDownload(link, account, dllink, null);
    }

    /* *************************** PUT API RELATED METHODS HERE *************************** */
    @Override
    protected String getAPIBase() {
        /* 2020-08-31: See here: https://doodstream.com/api-docs */
        // final String custom_apidomain = this.getPluginConfig().getStringProperty(PROPERTY_PLUGIN_api_domain_with_protocol);
        // if (custom_apidomain != null) {
        // return custom_apidomain;
        // } else {
        // return "https://doodapi.com/api";
        // }
        return "https://doodapi.com/api";
    }
}