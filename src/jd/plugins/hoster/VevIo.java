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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "vev.io" }, urls = { "https?://(?:www\\.)?(?:thevideo\\.me|thevideo\\.cc|vev\\.io|vev\\.red)/((?:vid)?embed\\-|embed/)?[a-z0-9]{12}" })
public class VevIo extends PluginForHost {
    public VevIo(PluginWrapper wrapper) {
        super(wrapper);
    }
    /* DEV NOTES */
    // protocol: no https
    // other:

    /* Extension which will be used if no correct extension is found */
    private static final String  default_extension = ".mp4";
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;
    private boolean              server_issues     = false;

    @Override
    public String getAGBLink() {
        return "https://vev.io/home";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        return this.getHost() + "://" + getFID(link);
    }

    public String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "([a-z0-9]{12})$").getMatch(0);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        final String fuid = getFID(link);
        link.setPluginPatternMatcher(String.format("https://vev.io/%s", fuid));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException, InterruptedException {
        return requestFileInformation(link, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws IOException, PluginException, InterruptedException {
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        /* 1st offlinecheck */
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* 2019-05-08: Unsure about that */
        boolean requiresCaptcha = true;
        String filename = null;
        try {
            final String json = br.getRegex("window\\.__INITIAL_STATE__=(\\{.*?\\});\\(function\\(\\)").getMatch(0);
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json);
            entries = (LinkedHashMap<String, Object>) entries.get("videoplayer");
            requiresCaptcha = ((Boolean) entries.get("captcha")).booleanValue();
            entries = (LinkedHashMap<String, Object>) entries.get("video");
            filename = (String) entries.get("title");
        } catch (final Throwable e) {
        }
        if (StringUtils.isEmpty(filename)) {
            filename = br.getRegex("<title>Watch ([^<>\"]+) \\- Vidup</title>").getMatch(0);
        }
        if (StringUtils.isEmpty(filename)) {
            /* Last chance fallback */
            filename = this.getFID(link);
        }
        String ext;
        if (!StringUtils.isEmpty(dllink)) {
            ext = getFileNameExtensionFromString(dllink, default_extension);
            if (ext != null && !ext.matches("\\.(?:flv|mp4)")) {
                ext = default_extension;
            }
        } else {
            ext = default_extension;
        }
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        if (check_filesize_via_directurl() || isDownload) {
            dllink = this.getDllink(link, isDownload);
            link.setFinalFileName(filename);
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    link.setDownloadSize(con.getLongContentLength());
                } else {
                    server_issues = true;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        } else {
            /* We cannot be sure whether we have the correct extension or not! */
            link.setName(filename);
        }
        return AvailableStatus.TRUE;
    }

    private String getDllink(final DownloadLink link, final boolean isDownload) throws IOException, PluginException, InterruptedException {
        br.getHeaders().put("origin", "https://" + this.getHost());
        br.getHeaders().put("referer", "https://" + this.getHost() + "/" + this.getFID(link));
        br.getHeaders().put("accept", "application/json");
        /* According to website, this way we'll get a higher downloadspeed */
        br.getHeaders().put("x-adblock", "0");
        br.setAllowedResponseCodes(new int[] { 400 });
        int loop = 0;
        boolean captchaFailed = false;
        String recaptchaV2Response = null;
        do {
            String postData = "";
            if (loop > 0) {
                postData = "{\"g-recaptcha-verify\":\"" + recaptchaV2Response + "\"}";
            }
            br.postPageRaw("https://" + this.getHost() + "/api/serve/video/" + this.getFID(link), postData);
            /* 2nd offlinecheck */
            // final String errormessage = PluginJSonUtils.getJson(br, "message");
            if (br.getHttpConnection().getResponseCode() == 400) {
                /* Captcha required */
                if (!isDownload) {
                    /* Do not request captchas during linkcheck */
                    return null;
                }
                captchaFailed = true;
                /* 2019-08-05: Hardcoded reCaptchaV2 key */
                recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, "6Ld6RqIUAAAAAKjcjfIgh2TmF_HmAc5hvrQx_D9a").getToken();
            } else if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                captchaFailed = false;
                break;
            }
            loop++;
        } while (loop <= 1);
        if (captchaFailed) {
            /* This should never happen! */
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        String dllink = null;
        try {
            HashMap<String, Object> entries = (HashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            entries = (HashMap<String, Object>) entries.get("qualities");
            Object quality_temp_o = null;
            long quality_temp = 0;
            String quality_temp_str = null;
            long quality_best = 0;
            String dllink_temp = null;
            final Iterator<Entry<String, Object>> it = entries.entrySet().iterator();
            while (it.hasNext()) {
                final Entry<String, Object> entry = it.next();
                quality_temp_o = entry.getKey();
                dllink_temp = (String) entry.getValue();
                if (quality_temp_o != null && quality_temp_o instanceof Long) {
                    quality_temp = JavaScriptEngineFactory.toLong(quality_temp_o, 0);
                } else if (quality_temp_o != null && quality_temp_o instanceof String) {
                    quality_temp_str = (String) quality_temp_o;
                    if (quality_temp_str.matches("\\d+p")) {
                        /* E.g. '360p' */
                        quality_temp = Long.parseLong(new Regex(quality_temp_str, "(\\d+)p").getMatch(0));
                    } else {
                        /* Bad / Unsupported format */
                        continue;
                    }
                }
                if (StringUtils.isEmpty(dllink_temp) || quality_temp == 0) {
                    continue;
                } else if (dllink_temp.contains(".m3u8")) {
                    /* Skip hls */
                    continue;
                }
                if (quality_temp > quality_best) {
                    quality_best = quality_temp;
                    dllink = dllink_temp;
                }
            }
            if (!StringUtils.isEmpty(dllink)) {
                logger.info("BEST handling for multiple video source succeeded");
            }
        } catch (final Throwable e) {
            logger.info("BEST handling for multiple video source failed");
        }
        return dllink;
    }

    private boolean check_filesize_via_directurl() {
        return false;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        /* 2019-08-05: getDllink will also find out about offline status so we do not necessarily have to do this here (twice). */
        // requestFileInformation(link, true);
        if (!this.check_filesize_via_directurl()) {
            dllink = this.getDllink(link, true);
        }
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (StringUtils.isEmpty(dllink)) {
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
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

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
