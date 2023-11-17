//jDownloader - Downloadmanager
//Copyright (C) 2017  JD-Team support@jdownloader.org
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class BadoinkvrCom extends PluginForHost {
    public BadoinkvrCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://" + getHost() + "/join/");
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "badoinkvr.com" });
        ret.add(new String[] { "kinkvr.com" });
        ret.add(new String[] { "babevr.com" });
        ret.add(new String[] { "vrcosplayx.com" });
        ret.add(new String[] { "18vr.com" });
        // ret.add(new String[] { "czechvrnetwork.com" });
        ret.add(new String[] { "povr.com" });
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
            /**
             * 2023-11-14: </br>
             * vrpornvideo: badoinkvr.com, babevr.com, 18vr.com </br>
             * cosplaypornvideo: vrcosplayx.com </br>
             * bdsm-vr-video: kinkvr.com
             */
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:members/)?[\\w\\-]+/([a-z0-9\\-_]+)\\-(\\d+)/?");
        }
        return ret.toArray(new String[0]);
    }
    /* DEV NOTES */
    // Tags: Porn plugin
    // protocol: no https
    // other:

    /* Connection stuff */
    private static final boolean free_resume            = true;
    private static final int     free_maxchunks         = 0;
    private static final int     free_maxdownloads      = -1;
    private String               dllink                 = null;
    private final String         PROPERTY_ACCOUNT_TOKEN = "authtoken";

    @Override
    public String getAGBLink() {
        return "https://" + getHost() + "/terms";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://video/" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(1);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account, false);
    }

    private boolean useHereSphereAPI(final Account account) {
        // TODO: Allow API usage without account for some websites such as czechvrnetwork.com
        if (account != null && account.getType() == AccountType.PREMIUM) {
            return true;
        } else {
            return false;
        }
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        dllink = null;
        final String videoid = this.getFID(link);
        final String extDefault = ".mp4";
        final String titleFromURL = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0).replace("_", " ").trim();
        if (!link.isNameSet()) {
            link.setName(videoid + "_" + titleFromURL + extDefault);
        }
        this.setBrowserExclusive();
        long filesize = -1;
        String filename = null;
        String title = null;
        String description = null;
        if (useHereSphereAPI(account)) {
            /* Use heresphere API */
            this.login(account, false);
            br.postPageRaw("https://" + this.getHost() + "/heresphere/video/" + videoid, "");
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            title = entries.get("title").toString();
            description = (String) entries.get("description");
            long filesizeMax = 0;
            int resolutionMax = 0;
            String pickedMediaName = null;
            final List<Map<String, Object>> medias = (List<Map<String, Object>>) entries.get("media");
            for (final Map<String, Object> media : medias) {
                final String mediaName = media.get("name").toString();
                final List<Map<String, Object>> sources = (List<Map<String, Object>>) media.get("sources");
                for (final Map<String, Object> source : sources) {
                    final Object filesizeO = source.get("size");
                    final Object resolutionO = source.get("resolution");
                    final String url = source.get("url").toString();
                    if (filesizeO != null) {
                        /* Filesize is not always given */
                        final long thisFilesize = ((Number) filesizeO).longValue();
                        if (thisFilesize > filesizeMax) {
                            filesizeMax = thisFilesize;
                            this.dllink = url;
                            pickedMediaName = mediaName;
                        }
                    } else if (resolutionO instanceof Number) {
                        final int thisResolutionValue = ((Number) resolutionO).intValue();
                        if (thisResolutionValue > resolutionMax) {
                            resolutionMax = thisResolutionValue;
                            this.dllink = url;
                            pickedMediaName = mediaName;
                        }
                    }
                    /* Fallback: We always want to have a result */
                    if (this.dllink == null) {
                        this.dllink = url;
                        pickedMediaName = mediaName;
                    }
                }
            }
            if (this.dllink != null) {
                filename = Plugin.getFileNameFromURL(this.dllink);
            }
            title += "_" + pickedMediaName;
            filesize = filesizeMax;
        } else {
            /* Use website */
            br.getPage(link.getPluginPatternMatcher());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (br.containsHTML(videoid + "/trailer/")) {
                br.getPage(br.getURL() + "trailer/");
                logger.info("Looks like trailer is available -> Accessing it");
                final String[] trailerurls = br.getRegex("<source[^<]*src=\"(https?://[^\"]+)\"[^<]*type=\"video/mp4\"").getColumn(0);
                if (trailerurls != null && trailerurls.length > 0) {
                    /* Assume that last item is highest quality trailer URL. */
                    this.dllink = trailerurls[trailerurls.length - 1];
                }
            } else {
                /* Legacy handling e.g. povr.com */
                String trailerHlsMaster = null;
                final String[] urls = HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), br.getURL());
                for (final String url : urls) {
                    if (StringUtils.endsWithCaseInsensitive(url, ".mp4")) {
                        this.dllink = url;
                        break;
                    } else if (StringUtils.containsIgnoreCase(url, ".m3u8")) {
                        // TODO: Make use of this HLS URL e.g. as fallback if http stream download fails.
                        trailerHlsMaster = url;
                    }
                }
            }
            title = titleFromURL;
        }
        if (!StringUtils.isEmpty(description) && link.getComment() == null) {
            link.setComment(description);
        }
        if (filename != null) {
            /* Pre defined filename -> Prefer that and use it as final filename. */
            link.setFinalFileName(filename);
        } else if (!StringUtils.isEmpty(title)) {
            title = videoid + "_" + title;
            title = Encoding.htmlDecode(title);
            title = title.trim();
            link.setName(this.correctOrApplyFileNameExtension(title, extDefault));
        }
        if (filesize > 0) {
            /* Successfully found 'MOCH-filesize' --> Display assumed filesize for MOCH download. */
            link.setDownloadSize(filesize);
        } else if (!StringUtils.isEmpty(dllink) && !isDownload) {
            /* Find filesize via header */
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(this.dllink);
                handleConnectionErrors(br, con);
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
                final String extReal = Plugin.getExtensionFromMimeTypeStatic(con.getContentType());
                if (extReal != null) {
                    link.setFinalFileName(this.correctOrApplyFileNameExtension(title, "." + extReal));
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, null, true);
        if (StringUtils.isEmpty(dllink)) {
            /* Assume that trailer download is impossible and this content can only be accessed by premium users. */
            throw new AccountRequiredException();
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    private void handleConnectionErrors(final Browser br, final URLConnectionAdapter con) throws PluginException, IOException {
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            if (con.getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Video broken?");
            }
        }
    }

    private Map<String, Object> login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            br.setCookiesExclusive(true);
            br.getHeaders().put("User-Agent", "HereSphere");
            String token = account.getStringProperty(PROPERTY_ACCOUNT_TOKEN);
            final String urlpathAccountinfo = "/heresphere";
            if (token != null) {
                // TODO: If we know a token validity, add forced token refresh every X time
                prepLoginHeader(br, token);
                if (!force) {
                    /* Don't validate token */
                    return null;
                }
                logger.info("Attempting token login");
                br.postPageRaw("https://" + this.getHost() + urlpathAccountinfo, "");
                final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                if (isLoggedin(entries)) {
                    logger.info("token login successful");
                    return entries;
                } else {
                    logger.info("token login failed: Token expired?");
                    /* Remove token so we won't try again with this one */
                    account.removeProperty(PROPERTY_ACCOUNT_TOKEN);
                }
            }
            logger.info("Performing full login");
            final Map<String, Object> postdata = new HashMap<String, Object>();
            postdata.put("username", account.getUser());
            postdata.put("password", account.getPass());
            br.postPageRaw("https://" + getHost() + "/heresphere/auth", JSonStorage.serializeToJson(postdata));
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            if (!isLoggedin(entries)) {
                throw new AccountInvalidException();
            }
            token = entries.get("auth-token").toString();
            account.setProperty(PROPERTY_ACCOUNT_TOKEN, token);
            prepLoginHeader(br, token);
            br.getPage(urlpathAccountinfo);
            return entries;
        }
    }

    private void prepLoginHeader(final Browser br, final String token) {
        br.getHeaders().put("auth-token", token);
    }

    private boolean isLoggedin(final Map<String, Object> entries) {
        final Number loginstatus = (Number) entries.get("access");
        if (loginstatus != null && (loginstatus.intValue() == 0 || loginstatus.intValue() == 1)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        final Map<String, Object> usermap = login(account, true);
        final Number loginstatus = (Number) usermap.get("access");
        ai.setUnlimitedTraffic();
        if (loginstatus.intValue() == 1) {
            account.setType(AccountType.PREMIUM);
        } else {
            account.setType(AccountType.FREE);
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account, true);
        if (StringUtils.isEmpty(dllink)) {
            /* No download or only trailer download possible. */
            throw new AccountRequiredException();
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }
    // @Override
    // public Class<? extends PluginConfigInterface> getConfigInterface() {
    // return HereSphereConfig.class;
    // }
    //
    // @PluginHost(host = "badoinkvr.com", type = Type.HOSTER)
    // public static interface HereSphereConfig extends PluginConfigInterface {
    // public static final TRANSLATION TRANSLATION = new TRANSLATION();
    //
    // public static class TRANSLATION {
    // public String getPreferredQuality_label() {
    // return "Use https for final downloadurls?";
    // }
    // }
    //
    // public static enum PreferredQuality implements LabelInterface {
    // BEST {
    // @Override
    // public String getLabel() {
    // return "Best";
    // }
    // },
    // Q_144P {
    // @Override
    // public String getLabel() {
    // return "144p";
    // }
    // },
    // Q_270P {
    // @Override
    // public String getLabel() {
    // return "270p";
    // }
    // };
    // }
    //
    // @AboutConfig
    // @DefaultEnumValue("BEST")
    // @DescriptionForConfigEntry("Preferred quality")
    // @Order(100)
    // PreferredQuality getPreferredQuality();
    //
    // void setPreferredQuality(PreferredQuality quality);
    // }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
