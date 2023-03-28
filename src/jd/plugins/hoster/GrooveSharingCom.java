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

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "groovesharing.com" }, urls = { "https?://(?:www\\.)?(?:groovesharing|groovestreams)\\.com/\\?d=([A-Z0-9]+)" })
public class GrooveSharingCom extends PluginForHost {
    public GrooveSharingCom(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium(COOKIE_HOST + "/register.php?g=3");
    }

    // MhfScriptBasic 1.5
    @Override
    public String getAGBLink() {
        return "https://groovesharing.com/rules.php";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("groovestreams.com/", "groovesharing.com/"));
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
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getURL().contains("&code=DL_FileNotFound") || br.containsHTML("(Your requested file is not found|No file found)") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = br.getRegex("class=\"remove\\-margin\">([^<>\"]*?)</h\\d+>").getMatch(0);
        final String filesize = br.getRegex("\\((\\d+(?:\\.\\d+)? ?(KB|MB|GB))\\)").getMatch(0);
        if (filename == null || filename.matches("")) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setFinalFileName(filename.trim());
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(link);
        String dllink = checkDirectLink(link, "directlink");
        Request downloadRequest = null;
        if (dllink != null) {
            final Browser br2 = br.cloneBrowser();
            downloadRequest = br2.createGetRequest(dllink);
        }
        if (downloadRequest == null) {
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.postPage(link.getDownloadURL(), "downloadverify=1&d=1");
            if (this.br.containsHTML("Vous avez atteint la quantite maximum de bande passante permise|groovesharing\\.com/limitsfree/")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
            }
            downloadRequest = findLink();
            if (downloadRequest == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            int wait = 35;
            final String waittime = br.getRegex("countdown\\((\\d+)\\);").getMatch(0);
            if (waittime != null) {
                wait = Integer.parseInt(waittime);
            }
            sleep(wait * 1001l, link);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, downloadRequest, false, 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (br.containsHTML("(?i)>\\s*AccessKey is expired, please request")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "FATAL server error, waittime skipped?");
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        link.setProperty("directlink", dllink);
        dl.startDownload();
    }

    private Request findLink() throws Exception {
        String finalLink = br.getRegex("(https?://.{5,30}getfile\\.php\\?id=\\d+[^<>\"\\']*?)(\"|\\')").getMatch(0);
        if (finalLink == null) {
            /* 2021-11-10 */
            finalLink = br.getRegex("(https?://[^/]+/file/[^<>\"\\']+)(\"|\\')").getMatch(0);
        }
        if (finalLink == null) {
            String[] sitelinks = HTMLParser.getHttpLinks(br.toString(), null);
            if (sitelinks == null || sitelinks.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (String alink : sitelinks) {
                alink = Encoding.htmlDecode(alink);
                if (alink.contains("access_key=") || alink.contains("getfile.php?")) {
                    finalLink = alink;
                    break;
                }
            }
        }
        final Request ret;
        if (finalLink != null) {
            ret = br.createGetRequest(finalLink);
        } else {
            final Form form = br.getFormByInputFieldKeyValue("downloadverify", "1");
            if (form == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.submitForm(form);
            return findLink();
        }
        return ret;
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openHeadConnection(dllink);
                if (!looksLikeDownloadableContent(con)) {
                    downloadLink.setProperty(property, Property.NULL);
                    throw new IOException();
                } else {
                    return dllink;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.MhfScriptBasic;
    }
}