//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "researchgate.net" }, urls = { "https?://(?:www\\.)?researchgate\\.net/(publication/\\d+_[A-Za-z0-9\\-_]+|figure/[A-Za-z0-9\\-_]+_\\d+)" })
public class ResearchgateNet extends PluginForHost {
    public ResearchgateNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://www.researchgate.net/application.TermsAndConditions.html";
    }

    private String dllink = null;

    @Override
    public void init() {
        super.init();
        Browser.setRequestIntervalLimitGlobal(getHost(), 500);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        final String urlTitle = new Regex(link.getPluginPatternMatcher(), "(?:publication|figure)/(.+)").getMatch(0);
        if (!link.isNameSet()) {
            /* Set fallback filename */
            link.setName(urlTitle + ".zip");
        }
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(429);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 429) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Server error 429", 10 * 60 * 1000l);
        } else if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (this.br.getURL().length() <= 60) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String json_source = this.br.getRegex("createInitialWidget\\((.*?\\})\\s+").getMatch(0);
        dllink = PluginJSonUtils.getJsonValue(json_source, "downloadLink");
        String filename = PluginJSonUtils.getJsonValue(json_source, "fileName");
        String filesize = PluginJSonUtils.getJsonValue(json_source, "fileSize");
        if (filesize == null) {
            /* 2021-12-01 */
            filesize = br.getRegex("ile-list-item__meta-data-item\"><span class=\"\">(\\d+[^<]+)</span>").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("full-text\" href=\"([^\"]+)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\"citation_pdf_url\"\\s*content\\s*=\\s*\"(https?://[^\"]*?\\.pdf)\"").getMatch(0);
            }
        }
        if (dllink == null) {
            /* 2021-12-01 */
            dllink = br.getRegex("js-lite-click\" href=\"(https?://[^\"]+)\"").getMatch(0);
        }
        if (dllink == null && StringUtils.containsIgnoreCase(link.getPluginPatternMatcher(), "/figure/")) {
            dllink = br.getRegex("property\\s*=\\s*\"og:image\"\\s*content\\s*=\\s*\"(https?://.*?\\.(png|jpe?g))\"").getMatch(0);
        }
        if (filename == null || filename.equals("")) {
            filename = br.getRegex("<title>\\s*([^<>\"].*?)\\s*((\\(PDF Download Available\\))|(\\|\\s*Download Scientific Diagram))?\\s*</title>").getMatch(0);
        }
        if (!StringUtils.isEmpty(filename)) {
            String correctFileExtension = null;
            if (dllink != null) {
                correctFileExtension = Plugin.getFileNameExtensionFromURL(dllink);
            }
            if (StringUtils.containsIgnoreCase(link.getPluginPatternMatcher(), "/figure/")) {
                final String ext = getFileNameExtensionFromURL(dllink, ".jpg");
                filename = Encoding.htmlDecode(filename.trim()) + ext;
            } else if (correctFileExtension != null) {
                filename = Encoding.htmlDecode(filename.trim());
                filename = this.correctOrApplyFileNameExtension(filename, correctFileExtension);
            } else {
                filename = Encoding.htmlDecode(filename.trim()) + ".zip";
            }
            link.setName(filename);
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        if (StringUtils.isEmpty(dllink) && br.containsHTML("=su_requestFulltext")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Request full-text PDF");
        } else {
            return AvailableStatus.TRUE;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        String directurl = checkDirectLink(link, "directlink");
        if (directurl == null) {
            directurl = this.dllink;
            if (directurl == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, directurl, false, 1);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 429) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Server error 429", 10 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        link.setProperty("directlink", dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openGetConnection(dllink);
                if (looksLikeDownloadableContent(con)) {
                    return dllink;
                } else {
                    br2.followConnection(true);
                    throw new IOException();
                }
            } catch (final Exception e) {
                logger.log(e);
                link.setProperty(property, Property.NULL);
                return null;
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
    public int getMaxSimultanFreeDownloadNum() {
        // avoid 429 Too Many Requests
        return 5;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}