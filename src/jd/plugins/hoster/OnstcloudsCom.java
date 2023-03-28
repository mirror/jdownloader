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
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class OnstcloudsCom extends PluginForHost {
    public OnstcloudsCom(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("http://onstclouds.com/vip.php");
    }

    @Override
    public String getAGBLink() {
        return "http://www.onstclouds.com/";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "kufile.net", "onstclouds.com" });
        return ret;
    }

    @Override
    public String rewriteHost(String host) {
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/file/([A-Za-z0-9]+)\\.html");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = 1;
    private static final int     FREE_MAXDOWNLOADS = 1;

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

    /** Similar to xingyaodisk.com | xingyaopan.com */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (!link.isNameSet()) {
            /* Fallback */
            link.setName(this.getFID(link));
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(?i)404 File does not exist")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* 2021-04-12: Trust filename inside URL. */
        String filename = br.getRegex("class=\"file_name\"[^>]*>([^<]+)<").getMatch(0);
        if (filename != null) {
            final String fileExtension = br.getRegex("src='images/filetype_\\d+/([a-z0-9]+)\\.gif'").getMatch(0);
            filename = Encoding.htmlDecode(filename).trim();
            filename = this.correctOrApplyFileNameExtension(filename, "." + fileExtension);
            link.setName(filename);
        }
        String filesize = br.getRegex("(?i)大小：(\\d+\\.\\d{1,2} (?:G|M))<").getMatch(0);
        if (filesize != null) {
            if (!filesize.toLowerCase(Locale.ENGLISH).contains("b")) {
                filesize += "b";
            }
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        if (filename == null || filesize == null) {
            if (br.containsHTML("<title>\\s*文件已经被删除")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void handleDownload(final DownloadLink link, final Account account, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (!attemptStoredDownloadurlDownload(link, directlinkproperty, resumable, maxchunks)) {
            this.requestFileInformation(link);
            // if (account != null) {
            // this.login(account, false);
            // /* Extra check! */
            // br.getPage(link.getPluginPatternMatcher());
            // if (!this.isLoggedin(br)) {
            // throw new AccountUnavailableException("Session expired?", 30 * 1000l);
            // }
            // }
            final String internalFileID = br.getRegex("add_ref\\((\\d+)\\);").getMatch(0);
            final String action0 = br.getRegex("action=(pc_\\d+)").getMatch(0);
            if (internalFileID == null || action0 == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final Browser ajax = this.br.cloneBrowser();
            ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            final UrlQuery query = new UrlQuery();
            query.add("action", action0);
            query.add("file_id", internalFileID);
            query.add("ms", new Random().nextInt(100) + "*" + new Random().nextInt(100));
            /* Screen resolution */
            query.add("sc", new Random().nextInt(1000) + "*" + new Random().nextInt(1000));
            ajax.postPage("/ajax.php", query);
            br.getPage("/ajax.php?action=load_time&ctime=" + System.currentTimeMillis());
            /* 2022-09-07: Waittime is skippable */
            final boolean skipWaittime = true;
            if (!skipWaittime) {
                final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
                final int waitSeconds = ((Number) entries.get("waittime")).intValue();
                if (waitSeconds > 180) {
                    /* Prefer reconnect to reset this time back to default (30). */
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waitSeconds * 1001l);
                }
                this.sleep(waitSeconds * 1001l, link);
            }
            boolean solvedCaptcha = false;
            for (int counter = 0; counter <= 3; counter++) {
                final String code = getCaptchaCode("/imagecode.php?t=" + System.currentTimeMillis(), link);
                br.postPage("/ajax.php", "action=check_code&code=" + Encoding.urlEncode(code));
                if (br.toString().equals("true")) {
                    solvedCaptcha = true;
                    break;
                } else {
                    logger.info("Wrong captcha");
                }
            }
            if (!solvedCaptcha) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            // ajax.postPage("/ajax.php", "action=load_down_addr1&action2=gethtml&file_id=" + internalFileID);
            ajax.postPage("/ajax.php", "action=load_down_addr2&file_id=" + internalFileID);
            String dllink = ajax.getRegex("a href=\"([^\"]*cd\\.php[^\"]+)").getMatch(0);
            if (dllink == null) {
                dllink = ajax.getRegex("true\\|<a href=\"([^<>\"]+)").getMatch(0);
            }
            if (dllink == null) {
                dllink = ajax.getRegex("true\\|(https?[^<>\"]+)").getMatch(0);
            }
            if (dllink == null) {
                dllink = ajax.getRegex("(vip\\.php[^<>\"\\']+)").getMatch(0);
            }
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if (dllink.startsWith("vip.php")) {
                throw new AccountRequiredException();
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                /* Limit waittime / time needed until start of more downloads is allowed. */
                final String waitSecsStr = br.getRegex("var seconds?_left\\s*=\\s*(\\d+)\\s*;").getMatch(0);
                if (waitSecsStr != null) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(waitSecsStr) * 1001l);
                } else if (br.getURL().contains("/promo.php")) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1001l);
                } else if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 5 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 503) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 503 too many connections", 5 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
                }
            }
            link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        }
        dl.startDownload();
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        /* Captcha can be skipped */
        return false;
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final String directlinkproperty, final boolean resumable, final int maxchunks) throws Exception {
        final String url = link.getStringProperty(directlinkproperty);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, resumable, maxchunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            link.removeProperty(directlinkproperty);
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            this.dl = null;
            return false;
        }
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.PhpDisk;
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