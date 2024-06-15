//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class MixdropCo extends antiDDoSForHost {
    public MixdropCo(PluginWrapper wrapper) {
        super(wrapper);
        /* 2019-09-30: They do not have/sell premium accounts */
        // this.enablePremium("");
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public String getAGBLink() {
        return "https://mixdrop.co/terms/";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "mixdrop.ag", "mixdrop.co", "mixdrop.to", "mixdrop.club", "mixdrop.sx", "mixdrop.bz", "mixdroop.bz", "mixdrop.vc", "mixdrop.to", "mdy48tn97.com", "mdbekjwqa.pw", "mdfx9dc8n.net", "mdzsmutpcvykb.net", "mixdrop.ms", "mixdrop.is", "mixdrop.club" });
        return ret;
    }

    protected List<String> getDeadDomains() {
        final ArrayList<String> deadDomains = new ArrayList<String>();
        deadDomains.add("mixdrop.co");
        deadDomains.add("md3b0j6hj.com");
        deadDomains.add("mixdroop.bz");
        deadDomains.add("mixdroop.co");
        deadDomains.add("mixdrop.bz");
        deadDomains.add("mixdrop.ch");
        deadDomains.add("mixdrop.gl");
        deadDomains.add("mixdrop.sx");
        deadDomains.add("mixdrop.to");
        deadDomains.add("mixdrop.vc");
        deadDomains.add("mixdrp.co");
        deadDomains.add("mixdrp.to");
        return deadDomains;
    }

    @Override
    public String rewriteHost(final String host) {
        /* 2023-11-09: They are frequently changing their main domain. */
        /* 2023-11-09: Changed from mixdrop.co to mixdrop.ag */
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
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:f|e)/([a-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME           = true;
    private static final int     FREE_MAXCHUNKS        = 1;
    private static final int     FREE_MAXDOWNLOADS     = 20;
    /** Documentation: https://mixdrop.co/api */
    private static final String  API_BASE              = "https://api.mixdrop.ag";
    private static final boolean USE_API_FOR_LINKCHECK = true;

    // private static final boolean ACCOUNT_FREE_RESUME = true;
    // private static final int ACCOUNT_FREE_MAXCHUNKS = 0;
    // private static final int ACCOUNT_FREE_MAXDOWNLOADS = 20;
    // private static final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private static final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    // private static final int ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    //
    // /* don't touch the following! */
    // private static AtomicInteger maxPrem = new AtomicInteger(1);
    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getNormalFileURL(final DownloadLink link) {
        String url = link.getPluginPatternMatcher().replaceFirst("(?i)/e/", "/f/").replaceAll("(?i)http://", "https://");
        final List<String> deadDomains = getDeadDomains();
        final String domainFromURL = Browser.getHost(url, false);
        if (deadDomains.contains(domainFromURL)) {
            url = url.replaceFirst(Pattern.quote(domainFromURL), this.getHost());
        }
        return url;
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    private String getAPIMail() {
        return "psp@jdownloader.org";
    }

    private String getAPIKey() {
        return "u3aH2kgUYOQ36hd";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        final String fid = this.getFID(link);
        if (!link.isNameSet()) {
            /* Fallback */
            link.setName(fid);
        }
        if (USE_API_FOR_LINKCHECK) {
            /* 2019-09-30: Let's just use it that way and hope it keeps working. */
            /*
             * https://mixdrop.co/api#fileinfo --> Also supports multiple fileIDs but as we are unsure how long this will last and this is
             * only a small filehost, we're only using this to check single fileIDs.
             */
            final Browser brc = br.cloneBrowser();
            getPage(brc, API_BASE + "/fileinfo?email=" + Encoding.urlEncode(getAPIMail()) + "&key=" + getAPIKey() + "&ref[]=" + this.getFID(link));
            final Map<String, Object> json = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
            final Boolean success = (Boolean) json.get("success");
            if (brc.getHttpConnection().getResponseCode() == 404 || success == Boolean.FALSE) {
                /* E.g. {"success":false,"result":{"msg":"file not found"}} */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> result = ((List<Map<String, Object>>) json.get("result")).get(0);
            final String filename = result.get("title").toString();
            if (!StringUtils.isEmpty(filename)) {
                link.setFinalFileName(filename);
            }
            final Object filesizeO = result.get("size");
            if (filesizeO != null) {
                if (filesizeO instanceof Number) {
                    link.setDownloadSize(((Number) filesizeO).longValue());
                } else {
                    link.setDownloadSize(Long.parseLong(filesizeO.toString()));
                }
            }
            if ((Boolean) result.get("deleted") == Boolean.TRUE) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } else {
            getPage(getNormalFileURL(link));
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("/imgs/illustration-notfound\\.png")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Regex fileinfo = br.getRegex("imgs/icon-file\\.png\"[^>]*/> <span title=\"([^\"]+)\">[^<>]*</span>([^<>\"]+)</div>");
            String filename = fileinfo.getMatch(0);
            final String filesize = fileinfo.getMatch(1);
            if (filesize != null) {
                link.setDownloadSize(SizeFormatter.getSize(filesize));
            }
            filename = Encoding.htmlDecode(filename).trim();
            link.setName(filename);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void handleDownload(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            requestFileInformation(link);
            if (USE_API_FOR_LINKCHECK) {
                getPage(getNormalFileURL(link));
            }
            /** 2021-03-03: E.g. extra step needed for .mp4 files but not for .zip files (which they call "folders"). */
            final String continueURL = br.getRegex("((?://[^/]+/f/[a-z0-9]+)?\\?download)").getMatch(0);
            if (continueURL != null) {
                logger.info("Found continueURL: " + continueURL);
                getPage(continueURL);
            } else {
                logger.info("Failed to find continueURL");
            }
            String csrftoken = br.getRegex("name=\"csrf\" content=\"([^<>\"]+)\"").getMatch(0);
            if (csrftoken == null) {
                logger.info("Failed to find csrftoken");
                csrftoken = "";
            } else {
                logger.info("Found csrftoken: " + csrftoken);
            }
            final UrlQuery query = new UrlQuery();
            query.add("a", "genticket");
            query.add("csrf", csrftoken);
            /* 2019-12-13: Invisible reCaptchaV2 */
            final boolean requiresCaptcha = true;
            if (requiresCaptcha) {
                final String recaptchaV2Response = getCaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                query.appendEncoded("token", recaptchaV2Response);
            }
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            postPage(br.getURL(), query.toString());
            final Map<String, Object> json = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            dllink = (String) json.get("url");
            if (StringUtils.isEmpty(dllink)) {
                final String errormsg = (String) json.get("msg");
                if (errormsg != null) {
                    if (errormsg.matches("(?i).*Failed captcha verification.*")) {
                        /*
                         * 2020-04-20: Should never happen but happens:
                         * {"type":"error","msg":"Failed captcha verification. Please try again. #errcode: 2"}
                         */
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    } else if (errormsg.matches("(?i).*File not found.*")) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    } else {
                        /* Unknown error */
                        throw new PluginException(LinkStatus.ERROR_FATAL, errormsg);
                    }
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* 2019-09-30: Skip short pre-download waittime */
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toExternalForm());
        dl.startDownload();
    }

    protected CaptchaHelperHostPluginRecaptchaV2 getCaptchaHelperHostPluginRecaptchaV2(PluginForHost plugin, Browser br) throws PluginException {
        return new CaptchaHelperHostPluginRecaptchaV2(this, br, this.getReCaptchaKey()) {
            @Override
            public org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractRecaptchaV2.TYPE getType() {
                return TYPE.INVISIBLE;
            }
        };
    }

    public String getReCaptchaKey() {
        /* 2019-12-13 */
        return "6LetXaoUAAAAAB6axgg4WLG9oZ_6QLTsFXZj-5sd";
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        final String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (!looksLikeDownloadableContent(con)) {
                    throw new IOException();
                } else {
                    return dllink;
                }
            } catch (final Exception e) {
                logger.log(e);
                link.setProperty(property, Property.NULL);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        } else {
            return null;
        }
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        return true;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}