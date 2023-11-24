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

import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;

/**
 * hostedtube template by pimproll. These guys have various scripts. This is the most common.
 *
 * @see http://www.hostedtube.com/
 * @see http://www.pimproll.com/support.html
 * @linked to wankz.com which still shows "Protranstech BV. DBA NetPass, Postbus 218. ljmudien, 1970AE, Netherlands" in footer message.
 *
 * @author raztoki
 *
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
/* 2020-08-27: Working example: dirtymovie.com */
public class PimpRollHostedTube extends PluginForHost {
    public static String[] t = { "bestporno.net", "descargarpelisporno.com", "hentaiporntube.org", "porrfilmer.biz", "privatepornlinks.com", "pussyfights.com", "r89.com", "realitystation.com", "redgayporntube.com", "redtube-com.info", "reet.com", "rubguide.com", "sex-cake.com", "sexernet.com", "sexfilmer.nu", "sexlifetube.com", "sexmovieporn.com", "sexparty.tv", "sexthings.com", "sextube-6.com", "smuthouse.com", "strapon.se", "swingersx.com", "tastyblackpussy.com", "teenpornocity.com", "teensteam.com", "thehotbabes.net", "themeatmen.com", "themostgay.com", "thestagparty.com", "throatpokers.com", "topfemales.com", "totalfetish.com", "videolivesex.com", "vidz.info", "vip-babes-world.com", "watchpornvideos.com", "xmovielove.com", "xpoko.com", "xtube.mobi", "xxxhelp.com", "xxxlinkshunter.com", "xxxpromos.com", "xxxthailand.net", "youjizz.net", "youjizz66.com", "wankz.com" };

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    /**
     * Returns the annotations names array
     *
     * @return
     */
    public static String[] getAnnotationNames() {
        return t;
    }

    /**
     * returns the annotation pattern array
     *
     * @return
     */
    public static String[] getAnnotationUrls() {
        String[] s = new String[t.length];
        for (int ssize = 0; ssize != t.length; ssize++) {
            s[ssize] = constructUrl(t[ssize]);
        }
        /* Special RegExes go here */
        s[s.length - 1] = "https?://(?:www\\.)?wankz\\.com/(?:[\\w\\-]+-\\d+|embed/)\\d+";
        return s;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.PimpRoll_HostedTube;
    }

    public static String constructUrl(final String host) {
        /**
         * 2018-04-11: e.g. not accept http://www.adult-lock.com/aactors/12345 </br>
         * 2022-02-25: Now accept such URLs too as old RegEx blocked some legit video URLs too. </br>
         * This will now handle such URLs and display them as offline if they do not lead to video content.
         */
        return "http://(?:(?:www|m)\\.)?" + Pattern.quote(host) + "/[\\w\\-]+/\\d+";
    }

    @SuppressWarnings("deprecation")
    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        if (link.getDownloadURL().contains("wankz.com/")) {
            link.setUrlDownload(link.getDownloadURL().replace("/embed/", "/"));
        }
    }

    public PimpRollHostedTube(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "https://www.pimproll.com/";
    }

    /**
     * defines custom browser requirements.
     */
    private Browser prepBrowser(final Browser prepBr) {
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        prepBr.setFollowRedirects(true);
        return prepBr;
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
        return new Regex(link.getPluginPatternMatcher(), "(\\d+)$").getMatch(0);
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

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        prepBrowser(br);
        final String extDefault = ".mp4";
        if (!link.isNameSet()) {
            link.setName(this.getFID(link) + extDefault);
        }
        br.getPage(link.getPluginPatternMatcher());
        if (br.containsHTML("(?i)was not found on this server, please try a")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 410) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // 404 on mobile page
        if (br.containsHTML("<a href=\"#sorting\">")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<title>([^<>]+)</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<h1[^>]*>([^<>]*?)</h1>").getMatch(0);
        }
        // DLLINK on desktop page
        final String[] qualities = { "1080p", "720p", "480p", "360p", "240p" };
        for (final String quality : qualities) {
            dllink = br.getRegex("\"" + quality + "\":\"(http[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                /* In most cases e.g.: "name":"480p" */
                dllink = br.getRegex("\"[a-z]+\":\"" + quality + "\",\"url\":\"(http[^<>\"]*?)\"").getMatch(0);
            }
            if (dllink != null) {
                break;
            }
        }
        // DLLINK on mobile page
        if (dllink == null) {
            dllink = br.getRegex("\"(http://[a-z0-9\\-\\.]+(movies|media)\\.hostedtube\\.com/[^<>\"]*?)\"").getMatch(0);
        }
        if (dllink == null) {
            String embed = br.getRegex("(http[^<>]+?/embed/[^<>]+?)&quot;").getMatch(0);
            if (embed != null) {
                // Browser bre = br.cloneBrowser();
                // bre.getPage(embed);
                // To do: find the final link
            }
        }
        if (dllink == null) {
            /* E.g. adult-lock.com */
            final String contentID = br.getRegex("data-item=\"(\\d+)\"").getMatch(0);
            if (contentID == null) {
                /* No video content available */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (filename != null) {
            filename = Encoding.htmlDecode(filename).trim();
            filename = this.correctOrApplyFileNameExtension(filename, extDefault);
            link.setFinalFileName(filename);
        }
        if (dllink != null) {
            dllink = HTMLEntities.unhtmlentities(dllink);
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            // br2.getHeaders().put("Referer", "");
            URLConnectionAdapter con = null;
            try {
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                } else {
                    if (con.getResponseCode() == 401) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Error 401 unauthorized");
                    } else {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (dllink == null && br.containsHTML("(?i)<h3>Access Denied</h3>")) {
            /* E.g. wankz.com */
            throw new AccountRequiredException();
        } else if (dllink == null && br.containsHTML("(?i)>\\s*Free Video Limit Reached")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Free Video Limit Reached", 3 * 60 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server issue");
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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
