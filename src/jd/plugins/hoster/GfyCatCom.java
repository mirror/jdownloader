//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.config.GfycatConfig;
import org.jdownloader.plugins.components.config.GfycatConfig.PreferredFormat;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gfycat.com" }, urls = { "https?://(?:www\\.)?(?:gfycat\\.com(?:/ifr)?|gifdeliverynetwork\\.com(?:/ifr)?|redgifs\\.com/(?:watch|ifr))/([A-Za-z0-9]+)" })
public class GfyCatCom extends PluginForHost {
    public GfyCatCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://gfycat.com/terms";
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replace("http://", "https://"));
        final String fid = this.getFID(link);
        if (Browser.getHost(link.getPluginPatternMatcher()).equalsIgnoreCase("gifdeliverynetwork.com") && fid != null) {
            /*
             * 2020-06-18: Special: gfycat.com would redirect to gifdeliverynetwork.con in this case but redgifs.com will work fine and
             * return the expected json!
             */
            link.setPluginPatternMatcher("https://www.redgifs.com/watch/" + fid);
        }
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public String getMirrorID(DownloadLink link) {
        String fid = null;
        if (link != null && StringUtils.equals(getHost(), link.getHost()) && (fid = getFID(link)) != null) {
            return getHost() + "://" + fid;
        } else {
            return super.getMirrorID(link);
        }
    }

    private String dllink = null;

    /*
     * Using API: http://gfycat.com/api 2020-06-18: Not using the API - wtf does this comment mean?? Maybe website uses the same json as API
     * ... but API needs authorization!
     */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    private static String redgifsAccessKey             = "";
    private static String redgifsAccessToken           = "";
    private static long   redgifsAccessTokenValidUntil = -1;

    private String[] getDownloadURL(final DownloadLink link, final Map<String, Object> video, final Map<String, Object> photo, final PreferredFormat format) throws Exception {
        // TODO: use JSON in complicatedJSON, it contains all available formats/qualities
        switch (format) {
        case WEBM:
            if (video == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                String ret = (String) video.get("contentUrl");
                if (!StringUtils.isEmpty(ret)) {
                    final String verifyWebM = ret.replace(".mp4", ".webm");
                    if (verifyDownloadURL(link, verifyWebM)) {
                        ret = verifyWebM;
                    } else {
                        return getDownloadURL(link, video, photo, PreferredFormat.MP4);
                    }
                }
                return new String[] { format.name(), ret, ".webm" };
            }
        case GIF:
            if (photo == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                final String ret = (String) photo.get("contentUrl");
                return new String[] { format.name(), ret, ".gif" };
            }
        case MP4:
        default:
            if (video == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                /* MP4 */
                String ret = (String) video.get("contentUrl");
                if (!StringUtils.isEmpty(ret)) {
                    if (!verifyDownloadURL(link, ret) && !StringUtils.endsWithCaseInsensitive(ret, "mobile.mp4")) {
                        final String mobile = ret.replaceFirst("\\.mp4$", "-mobile.mp4");
                        if (verifyDownloadURL(link, mobile)) {
                            ret = mobile;
                        }
                    }
                }
                return new String[] { format.name(), ret, ".mp4" };
            }
        }
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        if (!link.isNameSet()) {
            link.setName(this.getFID(link));
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        this.br.getHeaders().put("User-Agent", "JDownloader");
        br.setAllowedResponseCodes(new int[] { 500 });
        if (Browser.getHost(link.getPluginPatternMatcher()).equals("redgifs.com")) {
            /* 2021-05-04: New API handling required for URLs of this domain */
            synchronized (redgifsAccessKey) {
                if (StringUtils.isEmpty(redgifsAccessKey)) {
                    br.getPage(link.getPluginPatternMatcher());
                    /* 2021-05-04: /assets/app.59be79d0c1811e38f695.js */
                    final String jsurl = br.getRegex("<script src=\"(/assets/app\\.[a-f0-9]+\\.js)\">").getMatch(0);
                    if (jsurl == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    br.getPage(jsurl);
                    redgifsAccessKey = br.getRegex("webloginAccessKey=\"([^\"]+)\"").getMatch(0);
                    if (redgifsAccessKey == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
            }
            final Browser brapi = br.cloneBrowser();
            synchronized (redgifsAccessToken) {
                if (StringUtils.isEmpty(redgifsAccessToken) || redgifsAccessTokenValidUntil < System.currentTimeMillis()) {
                    if (redgifsAccessToken == null) {
                        logger.info("Creating token for the first time");
                    } else {
                        logger.info("Creating new token because the old one has expired");
                    }
                    brapi.postPageRaw("https://weblogin.redgifs.com/oauth/webtoken", "{\"access_key\":\"" + redgifsAccessKey + "\"}");
                    final Map<String, Object> entries = JSonStorage.restoreFromString(brapi.toString(), TypeRef.HASHMAP);
                    redgifsAccessToken = (String) entries.get("access_token");
                    if (StringUtils.isEmpty(redgifsAccessToken)) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    redgifsAccessTokenValidUntil = System.currentTimeMillis() + ((Number) entries.get("expires_in")).longValue() * 1000l;
                }
            }
            brapi.getHeaders().put("Referer", "https://redgifs.com/");
            brapi.getHeaders().put("Authorization", "Bearer " + redgifsAccessToken);
            brapi.getPage("https://api.redgifs.com/v1/gfycats/" + this.getFID(link));
            if (brapi.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            Map<String, Object> entries = JSonStorage.restoreFromString(brapi.toString(), TypeRef.HASHMAP);
            entries = (Map<String, Object>) entries.get("gfyItem");
            final Map<String, Object> sources = (Map<String, Object>) entries.get("content_urls");
            final Map<String, Object> selectedSource;
            switch (getPreferredFormat(link)) {
            case WEBM:
                selectedSource = (Map<String, Object>) sources.get("webm");
                break;
            case GIF:
                selectedSource = (Map<String, Object>) sources.get("largeGif");
                break;
            case MP4: // MP4 == default
            default:
                selectedSource = (Map<String, Object>) sources.get("mp4");
            }
            final String username = (String) entries.get("userName");
            link.setFinalFileName(username + " - " + this.getFID(link) + ".webm");
            link.setVerifiedFileSize(((Number) selectedSource.get("size")).longValue());
            this.dllink = (String) selectedSource.get("url");
        } else {
            br.getPage(link.getPluginPatternMatcher());
            if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 500) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (br.getHost().equalsIgnoreCase("gifdeliverynetwork.com")) {
                /* 2020-06-18: New and should not be needed! */
                dllink = br.getRegex("\"(https?://[^<>\"]+\\.webm)\"").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("\"(https?://[^<>\"]+\\.mp4)\"").getMatch(0);
                }
                if (dllink == null || dllink.contains(".webm")) {
                    link.setName(this.getFID(link) + ".webm");
                } else {
                    link.setName(this.getFID(link) + ".mp4");
                }
            } else {
                final String simpleJSON = br.getRegex("<script data-react-helmet\\s*=\\s*\"true\"\\s*type\\s*=\\s*\"application/ld\\+json\">\\s*(.*?)\\s*</script>").getMatch(0);
                final String complicatedJSON = br.getRegex("___INITIAL_STATE__\\s*=\\s*(\\{.*?)\\s*</script").getMatch(0);
                if (simpleJSON != null) {
                    final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(simpleJSON);
                    final String datePublished = (String) entries.get("datePublished");
                    final String description = (String) entries.get("description");
                    final Map<String, Object> photo = (Map<String, Object>) entries.get("image");
                    final Map<String, Object> video = (Map<String, Object>) entries.get("video");
                    if (!StringUtils.isEmpty(description) && link.getComment() == null) {
                        link.setComment(description);
                    }
                    final String username = (String) entries.get("author");
                    String title = null;
                    if (DebugMode.TRUE_IN_IDE_ELSE_FALSE && !true) {
                        /* Alternative ways to find title */
                        if (complicatedJSON != null) {
                            try {
                                final Object rootO = JavaScriptEngineFactory.jsonToJavaObject(complicatedJSON);
                                // final List<Object> ressourcelist = JSonStorage.restoreFromString(complicatedJSON, TypeRef.LIST);
                                final Map<String, Object> allMedia = (Map<String, Object>) JavaScriptEngineFactory.walkJson(rootO, "{0}/cache/gifs");
                                final Map<String, Object> thisMediaInfo = (Map<String, Object>) allMedia.get(this.getFID(link));
                                title = (String) thisMediaInfo.get("title");
                            } catch (final Throwable e) {
                            }
                        }
                        if (StringUtils.isEmpty(title)) {
                            title = br.getRegex("<h1 class\\s*=\\s*\"title\">\\s*([^<>\"]+)\\s*</h1>").getMatch(0);
                        }
                    }
                    title = (String) entries.get("headline");
                    if (!StringUtils.isEmpty(title)) {
                        /* 2020-11-26: Remove stuff we don't want! */
                        title = title.replaceFirst("(\\s*Porn\\s*GIF\\s*(by.+)?)", "");
                    }
                    /* 2021-03-09: Fallback - title can be "" (empty) [after title-correction]! */
                    title = this.getFID(link);
                    if (!StringUtils.isAllNotEmpty(datePublished, username, title)) {
                        /* Most likely content is not downloadable e.g. gyfcat.com/upload */
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    String dateFormatted = new Regex(datePublished, "(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
                    if (dateFormatted == null) {
                        /* Fallback */
                        dateFormatted = datePublished;
                    }
                    final String downloadURL[] = getDownloadURL(link, video, photo, getPreferredFormat(link));
                    dllink = downloadURL[1];
                    final String ext = downloadURL[2];
                    if (link.getFinalFileName() == null) {
                        /*
                         * 2020-11-26: Include fid AND title inside filenames because different URLs can have the same title and can be
                         * published on the same date (very rare case).
                         */
                        String filename = dateFormatted + "_" + username;
                        /* fid is used as fallback-title so in this case we don't want to have it twice in our filename! */
                        if (!title.equals(this.getFID(link))) {
                            filename += " - " + this.getFID(link);
                        }
                        filename += " - " + title + ext;
                        link.setFinalFileName(filename);
                    }
                } else {
                    /* Old handling */
                    // final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>)
                    // JavaScriptEngineFactory.jsonToJavaMap(json);
                    if (StringUtils.isEmpty(complicatedJSON) || br.getHttpConnection().getResponseCode() == 404) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    final String username = PluginJSonUtils.getJsonValue(complicatedJSON, "author");
                    final String filename = this.getFID(link);
                    final String filesize = PluginJSonUtils.getJsonValue(complicatedJSON, "webmSize");
                    if (StringUtils.isEmpty(username) || StringUtils.isEmpty(filename)) {
                        /* Most likely content is not downloadable e.g. gyfcat.com/upload */
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    link.setFinalFileName(username + " - " + filename + ".webm");
                    if (!StringUtils.isEmpty(filesize)) {
                        link.setDownloadSize(SizeFormatter.getSize(filesize));
                    }
                    dllink = PluginJSonUtils.getJsonValue(complicatedJSON, "webmUrl");
                }
            }
            if (!StringUtils.isEmpty(this.dllink) && !isDownload) {
                if (!verifyDownloadURL(link, dllink)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    private boolean verifyDownloadURL(final DownloadLink link, final String downloadURL) throws IOException {
        if (StringUtils.isEmpty(downloadURL)) {
            return false;
        }
        URLConnectionAdapter con = null;
        try {
            final Browser brc = br.cloneBrowser();
            brc.setFollowRedirects(true);
            con = brc.openHeadConnection(downloadURL);
            if (con.getResponseCode() == 404) {
                return false;
            } else if (looksLikeDownloadableContent(con)) {
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
                return true;
            } else {
                return false;
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link, true);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (IOException e) {
                logger.log(e);
            }
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 3 * 60 * 1000l);
            }
        }
        dl.startDownload();
    }

    private final String gfycatFormat = "gfycatFormat";

    private PreferredFormat getPreferredFormat(final DownloadLink link) {
        final String linkFormat = link.getStringProperty(gfycatFormat, null);
        if (linkFormat != null) {
            try {
                return PreferredFormat.valueOf(linkFormat);
            } catch (IllegalArgumentException e) {
                logger.exception("Invalid Format:" + linkFormat, e);
                link.removeProperty(gfycatFormat);
            }
        }
        final GfycatConfig cfg = PluginJsonConfig.get(GfycatConfig.class);
        return cfg.getPreferredFormat();
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return GfycatConfig.class;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        link.removeProperty(gfycatFormat);
    }
}