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

import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class FiveSixSevenFileCom extends PluginForHost {
    public FiveSixSevenFileCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://" + getHost() + "/";
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "567yun.cn", "567file.com", "567pan.cn" });
        return ret;
    }

    @Override
    public String rewriteHost(final String host) {
        /* 2023-06-22: Main domain has changed from 567file.com to 567yun.cn */
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
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:file|down2?)\\-(\\d+)\\.html");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = 1;
    private static final int     FREE_MAXDOWNLOADS = 1;

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
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)>\\s*文件不存在或已删除")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = br.getRegex("<h1>([^<]+)</h1>").getMatch(0);
        String filesize = br.getRegex(">\\s*文件大小：([^>]*?)\\&nbsp;").getMatch(0);
        if (filename != null) {
            link.setName(Encoding.htmlDecode(filename).trim());
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize + "b"));
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
            final String fid = this.getFID(link);
            if (br.containsHTML("down2-" + fid)) {
                br.getPage("/down2-" + fid + ".html");
                br.getPage("/down-" + fid + ".html");
            }
            final String sign = br.getRegex("sign=([a-f0-9]+)").getMatch(0);
            final Browser ajax = this.br.cloneBrowser();
            ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            final boolean skipCaptcha = true;
            if (!skipCaptcha) {
                /** 2022-05-04: Waittime and captcha (required for anonymous downloads in browser) is skippable! */
                br.getPage("/ajax.php?action=load_time&ctime=" + System.currentTimeMillis());
                final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
                final boolean skipWaittime = false;
                if (!skipWaittime) {
                    final int waitSeconds = ((Number) entries.get("waittime")).intValue();
                    if (waitSeconds >= 180) {
                        throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waitSeconds * 1001);
                    }
                    this.sleep(waitSeconds * 1001l, link);
                }
                final String code = getCaptchaCode("/imagecode.php?t=" + System.currentTimeMillis(), link);
                br.postPage("/ajax.php", "action=check_code&code=" + Encoding.urlEncode(code));
                if (br.toString().equals("false")) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
            }
            final UrlQuery query = new UrlQuery();
            query.add("action", "load_down_addr10");
            query.add("file_id", fid);
            if (sign != null) {
                query.add("sign", sign);
            }
            ajax.postPage("/ajax.php", query);
            dllink = ajax.getRegex("((https?://[^/]+)?/dl\\.php[^\"\\']+)").getMatch(0);
            if (dllink == null) {
                dllink = ajax.getRegex("true\\|(http[^<>\"]+)").getMatch(0);
            }
            if (dllink == null) {
                if (br.containsHTML("vip\\.php")) {
                    // throw new AccountRequiredException();
                    /* 2023-12-22: New */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Free download for this file is not possible at this moment", 5 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } else if (dllink.contains("account.php") || dllink.contains("vip\\.php")) {
                throw new AccountRequiredException();
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            /* Limit waittime / time needed until start of more downloads is allowed. */
            final String waitSecsStr = br.getRegex("var secs = (\\d+);").getMatch(0);
            if (waitSecsStr != null) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(waitSecsStr) * 1001l);
            } else if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    return dllink;
                } else {
                    throw new IOException();
                }
            } catch (final Exception e) {
                link.removeProperty(property);
                logger.log(e);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.PhpDisk;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}