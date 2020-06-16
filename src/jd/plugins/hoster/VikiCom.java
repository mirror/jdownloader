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

import java.util.LinkedHashMap;

import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDHexUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "viki.com" }, urls = { "https?://(?:www\\.)?viki\\.(?:com|mx|jp)/videos/(\\d+v)" })
public class VikiCom extends PluginForHost {
    public VikiCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.viki.com/sign_up");
    }

    /* Extension which will be used if no correct extension is found */
    private static final String  default_Extension       = ".mp4";
    /* Connection stuff */
    private static final boolean free_resume             = true;
    private static final int     free_maxchunks          = 0;
    private static final int     free_maxdownloads       = -1;
    private String               dllink                  = null;
    private String               hls_master              = null;
    private boolean              server_issues           = false;
    private boolean              blocking_geoblocked     = false;
    private boolean              blocking_paywall        = false;
    private boolean              blocking_notyetreleased = false;
    private boolean              blocking_drm            = false;
    private static final String  APP_ID                  = "100005a";
    private static final String  APP_SECRET              = "MM_d*yP@`&1@]@!AVrXf_o-HVEnoTnm$O-ti4[G~$JDI/Dc-&piU&z&5.;:}95=Iad";
    private static final String  API_BASE                = "https://www.viki.com/api";

    @Override
    public String getAGBLink() {
        return "https://www.viki.com/terms_of_use";
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload("https://www.viki.com/videos/" + getVID(link));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null, false);
    }

    /** Thanks for the html5 idea guys: https://github.com/rg3/youtube-dl/blob/master/youtube_dl/extractor/viki.py */
    /**
     * To get more/better qualities and streaming types (protocols) we'd have to use the API: http://dev.viki.com/. Unfortunately the call
     * to get the video streams is not public and only works with their own API secret which we don't have: http://dev.viki.com/v4/streams/
     * ...which is why we're using the html5 version for now. In case we ever use the API - here is the needed hmac hash function:
     * http://stackoverflow.com/questions/6312544/hmac-sha1-how-to-do-it-properly-in-java
     *
     * @throws Exception
     */
    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        blocking_geoblocked = false;
        final String vid = getVID(link);
        link.setName(vid);
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 410 });
        // final UrlQuery query = new UrlQuery();
        // query.append("app", "100005a", true);
        // query.append("t", System.currentTimeMillis() + "", true);
        // query.append("site", "www.viki.com", true);
        /* 2020-05-04: This header is required from now on. */
        br.getHeaders().put("x-viki-app-ver", "4.0.52-175e743");
        br.getPage(API_BASE + "/videos/" + this.getVID(link));
        if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 410) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        /* 2020-06-16: E.g. no DRM = Int9Ig== */
        final String drm_base64 = (String) entries.get("drm");
        if (drm_base64 != null && drm_base64.length() > 20) {
            this.blocking_drm = true;
        }
        hls_master = (String) JavaScriptEngineFactory.walkJson(entries, "streams/hls/url");
        entries = (LinkedHashMap<String, Object>) entries.get("video");
        // final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("");
        String filename = (String) JavaScriptEngineFactory.walkJson(entries, "container/i18n_title");
        blocking_geoblocked = ((Boolean) JavaScriptEngineFactory.walkJson(entries, "blocking/geo")).booleanValue();
        blocking_paywall = ((Boolean) JavaScriptEngineFactory.walkJson(entries, "blocking/paywall")).booleanValue();
        blocking_notyetreleased = ((Boolean) JavaScriptEngineFactory.walkJson(entries, "blocking/upcoming")).booleanValue();
        if (!StringUtils.isEmpty(filename)) {
            /* 2020-03-09: Prevent duplicated filenames */
            filename = this.getVID(link) + "_" + filename;
        } else {
            /* Fallback */
            filename = this.getVID(link);
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        if (blocking_geoblocked) {
            link.setName("GEO_BLOCKED_" + filename + default_Extension);
            return AvailableStatus.TRUE;
        } else if (blocking_paywall) {
            link.setName("PAYWALL_" + filename + default_Extension);
            return AvailableStatus.TRUE;
        } else if (blocking_notyetreleased) {
            link.setName("NOT_YET_RELEASED" + filename + default_Extension);
            return AvailableStatus.TRUE;
        }
        br.getPage("https://www.viki.com/player5_fragment/" + vid + "?action=show&controller=videos");
        server_issues = this.br.containsHTML("Video playback for this video is not supported by your browser");
        /* Should never happen */
        if (server_issues) {
            link.getLinkStatus().setStatusText("Linktype not yet supported");
            link.setName(filename + default_Extension);
            return AvailableStatus.TRUE;
        }
        final boolean allow_idpart_workaround = false;
        String idpart = this.br.getMatch("oster=\"https?://[^/]+/videos/\\d+v/[^_]+_(\\d+)_");
        if (true || idpart == null) {
            final Browser cbr = br.cloneBrowser();
            String apiUrl = (String) JavaScriptEngineFactory.walkJson(entries, "url/api");
            if (apiUrl == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            apiUrl = apiUrl.replace("api-internal.viki.io", "api.viki.io");
            apiUrl = apiUrl.replaceFirst("\\.json", "/streams.json");
            apiUrl += "?app=" + APP_ID + "&t=" + System.currentTimeMillis() / 1000 + "&site=www.viki.com";
            apiUrl += "&sig=" + getSignature(apiUrl.replaceFirst("https?://[^/]+", ""));
            cbr.getPage(apiUrl);
            LinkedHashMap<String, Object> jsonEntries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(cbr.toString());
            String url = null;
            for (String quality : new String[] { "1080p", "720p", "480p", "380p", "240p" }) {
                url = (String) JavaScriptEngineFactory.walkJson(jsonEntries, quality + "/https/url");
                if (url == null) {
                    url = (String) JavaScriptEngineFactory.walkJson(jsonEntries, quality + "/http/url");
                }
                if (url != null) {
                    break;
                }
            }
            if (url != null) {
                logger.info("Found http downloadurl");
                dllink = url;
            }
            // 480p_1709221204.mp4 pattern. 720p is OK.
            // idpart = new Regex(url, "480p_(\\d+)").getMatch(0);
            // if (idpart == null) {
            // // 480p_e63c3e_1709221204.mp4 pattern. 720p is NG.
            // dllink = url480;
            // }
        }
        if (allow_idpart_workaround && idpart != null) {
            /* Thx: https://github.com/dknlght/dkodi/blob/master/plugin.video.viki/plugin.video.viki-1.1.44.zip */
            /* 2017-09-27: Check this: https://forum.kodi.tv/showthread.php?tid=148429 */
            /* 2017-03-11 - also possible for: 360p, 480p */
            dllink = String.format("http://content.viki.com/%s/%s_high_720p_%s.mp4", vid, vid, idpart);
        } else if (dllink == null) {
            dllink = br.getRegex("<source type=\"video/mp4\" src=\"(https?://[^<>\"]*?)\">").getMatch(0);
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        String ext = getFileNameExtensionFromString(dllink, default_Extension);
        if (ext == null) {
            ext = default_Extension;
        }
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        if (!StringUtils.isEmpty(dllink)) {
            dllink = Encoding.htmlDecode(dllink);
            link.setFinalFileName(filename);
            if (!isDownload) {
                final Browser br2 = br.cloneBrowser();
                // In case the link redirects to the finallink
                br2.setFollowRedirects(true);
                URLConnectionAdapter con = null;
                try {
                    con = br2.openHeadConnection(dllink);
                    if (con.isOK() && !con.getContentType().contains("html")) {
                        if (con.getCompleteContentLength() > 0) {
                            link.setDownloadSize(con.getCompleteContentLength());
                        }
                        link.setProperty("directlink", dllink);
                    } else {
                        server_issues = true;
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
        } else {
            /* We cannot be sure whether we have the correct extension or not! */
            link.setName(filename);
        }
        return AvailableStatus.TRUE;
    }

    private String getSignature(String query) {
        HMac hmac = new HMac(new SHA1Digest());
        byte[] buf = new byte[hmac.getMacSize()];
        hmac.init(new KeyParameter(APP_SECRET.getBytes()));
        byte[] qbuf = query.getBytes();
        hmac.update(qbuf, 0, qbuf.length);
        hmac.doFinal(buf, 0);
        return new String(JDHexUtils.getHexString(buf));
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        doFree(link, null);
    }

    public void doFree(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account, true);
        if (blocking_paywall) {
            logger.info("Paid content");
            throw new AccountRequiredException();
        } else if (blocking_geoblocked) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "GEO-blocked content");
        } else if (blocking_notyetreleased) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Content has not yet been released");
        } else if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
        } else if (dllink == null && hls_master == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        boolean preferHLS = false;
        try {
            final UrlQuery query = new UrlQuery().parse(hls_master);
            final String stream = query.get("stream");
            final String stream_decrypted = Encoding.Base64Decode(stream);
            /*
             * Only download HLS streams if: HLS is available AND [Account is available (= higher quality possible) OR http downloadURL is
             * NOT available (= use HLS as fallback)]
             */
            if (stream_decrypted != null && (account != null || this.dllink == null)) {
                preferHLS = true;
                hls_master = stream_decrypted;
            }
        } catch (final Throwable e) {
        }
        /*
         * 2020-06-16: Some content is DRM protected via HLS but not via http. Prefer lower quality http download in this case as DRM
         * protected HLS download is not possible.
         */
        if (preferHLS && !this.blocking_drm) {
            br.getPage(hls_master);
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
            if (hlsbest == null) {
                logger.info("HLS stream broken/restricted");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "HLS stream broken");
            }
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, hlsbest.getDownloadurl());
            dl.startDownload();
        } else {
            /* Download http stream */
            if (dllink == null) {
                if (this.blocking_drm) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "DRM protected content");
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
            if (dl.getConnection().getContentType().contains("html")) {
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                }
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    private String getVID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    private boolean login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    /* Try to avoid login captchas at all cost! */
                    logger.info("Attempting cookie login");
                    this.br.setCookies(this.getHost(), cookies);
                    if (!force && System.currentTimeMillis() - account.getCookiesTimeStamp("") < 5 * 60 * 1000l) {
                        logger.info("Cookies are still fresh --> Trust cookies without login");
                        return false;
                    }
                    br.getPage("https://" + this.getHost() + "/");
                    if (this.isLoggedin()) {
                        logger.info("Cookie login successful");
                        /* Refresh cookie timestamp */
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return true;
                    } else {
                        logger.info("Cookie login failed");
                        br.clearAll();
                    }
                }
                logger.info("Performing full login");
                br.getPage("https://" + this.getHost() + "/sign_in");
                final String reCaptchaKey = br.getRegex("RECAPTCHA_PUBLIC_KEY\\s*:\\s*'([^<>\"\\']+)\\'").getMatch(0);
                final Form loginform = br.getFormbyProperty("id", "loginForm");
                if (loginform == null) {
                    logger.warning("Failed to find loginform");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else if (reCaptchaKey == null) {
                    logger.warning("Failed to find reCaptchaKey");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("login_id", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                final DownloadLink dlinkbefore = this.getDownloadLink();
                try {
                    final DownloadLink dl_dummy;
                    if (dlinkbefore != null) {
                        dl_dummy = dlinkbefore;
                    } else {
                        dl_dummy = new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true);
                        this.setDownloadLink(dl_dummy);
                    }
                    final String recaptchaV2Response = getCaptchaHelperHostPluginRecaptchaV2(this, br, reCaptchaKey).getToken();
                    loginform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                } finally {
                    this.setDownloadLink(dlinkbefore);
                }
                br.submitForm(loginform);
                if (!isLoggedin()) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
                return true;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    protected CaptchaHelperHostPluginRecaptchaV2 getCaptchaHelperHostPluginRecaptchaV2(PluginForHost plugin, Browser br, final String key) throws PluginException {
        return new CaptchaHelperHostPluginRecaptchaV2(this, br, key) {
            @Override
            public org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractRecaptchaV2.TYPE getType() {
                return TYPE.INVISIBLE;
            }
        };
    }

    private boolean isLoggedin() {
        return br.containsHTML("class=\"navbar-dropdown-link\"");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (final PluginException e) {
            throw e;
        }
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        /* free accounts can still have captcha */
        account.setConcurrentUsePossible(false);
        ai.setStatus("Registered (free) user");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(account, true);
        // requestFileInformation(link);
        this.doFree(link, account);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        /* 2020-06-16: No captchas at all */
        return false;
    }

    // private String checkDirectLink(final DownloadLink downloadLink, final String property) {
    // String dllink = downloadLink.getStringProperty(property);
    // if (dllink != null) {
    // URLConnectionAdapter con = null;
    // try {
    // final Browser br2 = br.cloneBrowser();
    // con = br2.openGetConnection(dllink);
    // if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
    // downloadLink.setProperty(property, Property.NULL);
    // dllink = null;
    // }
    // } catch (final Exception e) {
    // downloadLink.setProperty(property, Property.NULL);
    // dllink = null;
    // } finally {
    // try {
    // con.disconnect();
    // } catch (final Throwable e) {
    // }
    // }
    // }
    // return dllink;
    // }
    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

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
