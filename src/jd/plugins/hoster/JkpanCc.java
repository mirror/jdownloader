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
import java.util.Locale;

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "jkpan.cc" }, urls = { "https?://(?:www\\.)?jkpan\\.cc/download/([A-Za-z0-9]+)" })
public class JkpanCc extends PluginForHost {
    public JkpanCc(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("http://www.jkpan.cc/upgrade.html");
    }

    @Override
    public String getAGBLink() {
        return "http://www.jkpan.cc/";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = 1;
    private static final int     FREE_MAXDOWNLOADS = 1;
    // private static final boolean ACCOUNT_FREE_RESUME = false;
    // private static final int ACCOUNT_FREE_MAXCHUNKS = 1;
    // private static final int ACCOUNT_FREE_MAXDOWNLOADS = 1;
    // private static final boolean ACCOUNT_PREMIUM_RESUME = false;
    // private static final int ACCOUNT_PREMIUM_MAXCHUNKS = 1;
    // private static final int ACCOUNT_PREMIUM_MAXDOWNLOADS = 1;
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

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (!link.isNameSet()) {
            link.setName(this.getFID(link));
        }
        this.setBrowserExclusive();
        br.getPage(link.getPluginPatternMatcher());
        if (br.containsHTML("文件已经被删除") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = br.getRegex("<li>文件名:([^<>\"]+)</li>").getMatch(0);
        String filesize = br.getRegex(">\\s*文件大小: ([^<>\"]+)<").getMatch(0);
        if (filename != null) {
            link.setName(Encoding.htmlDecode(filename.trim()));
        }
        if (filesize != null) {
            filesize = Encoding.htmlDecode(filesize);
            filesize += "b";
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        doFree(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            /* Special: real fileid is not inside URL - we have to find it inside their html! */
            final String fid = this.br.getRegex("file_id=(\\d+)").getMatch(0);
            if (fid == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // final Browser brAjax = this.br.cloneBrowser();
            // brAjax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            /* 2017-10-06: No waittime required */
            // int wait = 30;
            // final String waittime = PluginJSonUtils.getJson(brAjax, "waittime");
            // if (waittime != null) {
            // wait = Integer.parseInt(waittime);
            // }
            // /* 2017-07-19: Reconnect-Waittime was easily skippable by using a new session (e.g. private browsing). */
            // if (wait > 75) {
            // throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait * 1001l);
            // }
            // this.sleep(wait * 1001l, downloadLink);
            br.setFollowRedirects(true);
            br.getPage("/down2-" + fid + ".html");
            final Form downform = br.getFormbyKey("formhash");
            if (downform == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.submitForm(downform);
            /* We expect a redirect to: /downhtml/[0-9]+/[0-9]+/[a-f0-9]{32}\\.html */
            // br.postPage("/ajax.php", "action=load_down_addr1&file_id=" + fid);
            // /* 2017-10-06: No captcha required */
            // final String code = getCaptchaCode("/imagecode.php", downloadLink);
            // br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            // br.postPage("/ajax.php", "action=check_code&code=" + Encoding.urlEncode(code));
            // if (br.toString().equals("false")) {
            // throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            // }
            // br.postPage("/ajax.php", "action=load_down_addr2&file_id=" + fid);
            // br.getPage("/dd.php?file_id=" + fid + "&p=1");
            // dllink = br.getRegex("true\\|<a href=\"(http[^<>\"]+)").getMatch(0);
            // if (dllink == null) {
            // dllink = br.getRegex("\"(https?://down(?:[a-z0-9]+)?\\.[^<>\"]+)\"").getMatch(0);
            // if (dllink == null) {
            // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // }
            // }
            /* 2021-04-08: This also skips all waittimes, also long waittimes! */
            dllink = this.br.getRegex("href=\"https?://[^/]+/down\\.php\\?url=(https?://[^\"]+)").getMatch(0);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            if (br.containsHTML("err1")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download impossible for unknown reasons");
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        /*
         * 2021-04-08: No content-disposition header given and file-typ0e is missing in filename most of all times but Content-Type is
         * given.
         */
        final String ext = DirectHTTP.getExtensionFromMimeType(dl.getConnection().getContentType());
        if (ext != null && !link.getName().toLowerCase(Locale.ENGLISH).endsWith("." + ext)) {
            link.setFinalFileName(link.getName() + "." + ext);
        }
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
                    return dllink;
                }
            } catch (final Exception e) {
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
        return SiteTemplate.Unknown_ChineseFileHosting;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}