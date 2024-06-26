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

import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "72dj.com" }, urls = { "https?://(?:www\\.)?72dj\\.com/(?:down|play)/(\\d+)\\.htm" })
public class SevenTwoDjCom extends PluginForHost {
    public SevenTwoDjCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.AUDIO_STREAMING };
    }

    @Override
    public String getAGBLink() {
        return "http://72dj.com/";
    }

    public void correctDownloadLink(final DownloadLink link) {
        final String fid = this.getFID(link);
        link.setLinkID(fid);
        link.setUrlDownload(String.format("http://www.72dj.com/down/%s.htm", fid));
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        final String extDefault = ".mp3";
        final String fid = this.getFID(link);
        if (!link.isNameSet()) {
            link.setName(fid + extDefault);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("gbk");
        br.getPage("http://www." + getHost() + "/play/" + fid + ".htm");
        /* Skip anti bot page */
        final String secret = br.getCookie(br.getHost(), "secret");
        final String token = br.getCookie(br.getHost(), "token");
        if (secret != null && secret.matches("\\d+") && token != null) {
            final long secretNew = Long.parseLong(secret) - 100;
            br.setCookie(br.getHost(), "r", Long.toString(secretNew));
            br.setCookie(br.getHost(), "t", token);
            /* Refresh page */
            br.getPage(br.getURL());
        }
        if (this.br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("<TITLE>\\s*无法找到该页\\s*</TITLE>")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = br.getRegex("var p_n=\"([^\"]+)").getMatch(0);
        if (title == null) {
            title = br.getRegex("<TITLE>下载湛江Dj小帅\\-([^<>\"]*?)</TITLE>").getMatch(0);
        }
        if (title == null) {
            title = br.getRegex("<TITLE>([^<]*?)</TITLE>").getMatch(0);
        }
        if (title != null) {
            /* Fix title */
            title = title.replace(" - 72DJ舞曲网 www\\.72dj\\.com", "");
            link.setFinalFileName(Encoding.htmlDecode(title).trim() + extDefault);
        } else {
            logger.warning("Failed to find file title");
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        final String fid = this.getFID(link);
        String dllink = null;
        final String streamPath = br.getRegex("danceFilePath=\"([^\"]+)\"").getMatch(0);
        String streamurl = null;
        if (streamPath != null) {
            /* 2024-06-26 */
            streamurl = "https://mp372dj.lianzhixiu.com:37373/m4adj/" + streamPath + ".m4a";
        }
        br.getPage("/down/" + fid + ".htm");
        dllink = br.getRegex("\"(http://[^<>\"]*?down_mp3[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("var thunder_url= \"\"\\+durl\\+\"([^<>\"]*?)\"").getMatch(0);
            if (dllink != null) {
                /* Old way */
                br.setCookie("http://72dj.com", "uuauth", "ok");
                Browser brc = br.cloneBrowser();
                brc.getPage("http://data.72dj.com/getuuauthcode/");
                String code = brc.getRegex("UUAuthCode=\"(.*?)\"").getMatch(0);
                if (code != null) {
                    code = "?" + code;
                } else {
                    code = "";
                }
                dllink = "http://data.72dj.com/mp3/" + Encoding.htmlDecode(dllink) + code;
            } else {
                /* Try to get stream URL */
                /* 2021-09-29: Official downloads require a paid account. */
                logger.info("Official download requires a paid account -> Try stream download");
                final boolean officialDownloadRequiresAccount = true;
                if (officialDownloadRequiresAccount) {
                    /* Stream download */
                    if (streamurl != null) {
                        /* Stream download, typically 48kBit/s */
                        dllink = streamurl;
                    } else {
                        /* Stream download not possible -> Account required. */
                        throw new AccountRequiredException();
                    }
                } else {
                    final String code = this.getCaptchaCode("http://www.72dj.com/d/imgcode.asp?" + System.currentTimeMillis(), link);
                    br.getPage(String.format("/d/down_mp3url.asp?CheckCode=%s&id=%s", Encoding.urlEncode(code), this.getFID(link)));
                    dllink = br.getRegex("\"(http[^<>\"\\']+type=down[^<>\"\\']+)\"").getMatch(0);
                    if (dllink == null) {
                        if (this.br.containsHTML("window\\.alert\\(")) {
                            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                        }
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}