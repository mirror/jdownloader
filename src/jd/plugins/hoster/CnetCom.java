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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "cnet.com" }, urls = { "https?://(?:www\\.)?download\\.cnet\\.com/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+-(\\d+)\\.html" })
public class CnetCom extends PluginForHost {
    public CnetCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://download.cnet.com/";
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
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        this.br.setAllowedResponseCodes(500);
        br.getPage(link.getPluginPatternMatcher());
        if (br.containsHTML("(>Whoops\\! You broke the Internet\\!<|>No, really,  it looks like you clicked on a borked link)") || this.br.getHttpConnection().getResponseCode() == 404 || this.br.getHttpConnection().getResponseCode() == 500 || br.getURL().contains("/most-popular/")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // // External mirrors are of course not supported
        // if (br.containsHTML(">Visit Site<")) {
        // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // }
        /*
         * 2021-10-06: Alternative API to get file information:
         * https://cmg-prod.apigee.net/v1/xapi/composer/download/pages/post-download/<fid>/web?contentOnly=true&apiKey=<apikey>
         */
        String filename = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>([^<>\"]*?) \\- CNET Download\\.com</title>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("\\&fileName=([^<>\"]*?)(\\'|\")").getMatch(0);
        }
        if (filename == null) {
            /* 2021-10-06 */
            filename = br.getRegex("class=\"c-productSummary_title g-text-xxlarge\"[^>]*>([^<>\"]+)</h1>").getMatch(0);
        }
        String filesize = br.getRegex(">File size:</span>([^<>\"]*?)</li>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex(">File Size:</span> <span>([^<>\"]*?)</span>").getMatch(0);
        }
        if (filesize == null) {
            filesize = br.getRegex(">File Size:</div>[\t\n\r ]+<div class=\"product-landing-quick-specs-row-content\">([^<>\"]*?)</div>").getMatch(0);
        }
        if (filename != null) {
            link.setName(Encoding.htmlDecode(filename.trim()));
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        if (br.containsHTML("(?i)>\\s*Visit Site<")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Not downloadable (external download, see browser)");
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        /* 2021-10-06 */
        final boolean useAPI = true;
        String dllink = null;
        if (useAPI) {
            /* 2021-10-06: See https://download.cnet.com/a/neutron/7dbdf09.modern.js */
            final String apikey = "6zDmakBWMyKV8oS6mCrigTAO08QxiVsK";
            final Browser brc = br.cloneBrowser();
            brc.getPage("https://cmg-prod.apigee.net/v1/xapi/products/signedurl/download/" + this.getFID(link) + "/web?apiKey=" + apikey);
            if (brc.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Not downloadable (external download, see browser)");
            }
            final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(brc.toString());
            dllink = JavaScriptEngineFactory.walkJson(entries, "data/item/url").toString();
        } else {
            /* Try to get installer without adware */
            String continueLink = br.getRegex("<a href=\\'(https?://[^<>\"]*?)\\' class=\"dln\\-a\">[\t\n\r ]+<span class=\"dln\\-cta\">Direct Download Link</span>").getMatch(0);
            /* If not, we can only download the installer with ads */
            if (continueLink == null) {
                continueLink = br.getRegex("<a href=\\'(https?://[^<>\"]*?)\\' class=\"dln\\-a\">[\t\n\r ]+<span class=\"dln\\-cta\">Download Now</span>").getMatch(0);
            }
            if (continueLink == null) {
                /* 2021-10-06 */
                continueLink = br.getRegex("uppercase c-globalButton-medium c-globalButton-standard\"[^>]*><a href=\"(/[^\"]+)\"").getMatch(0);
            }
            if (continueLink != null) { // 20170614 continueLink is no longer available?
                br.getPage(continueLink);
            }
            dllink = br.getRegex("data-download-now-url=\"(https?://[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("http\\-equiv=\"refresh\" content=\\'0;url=(https?://[^<>\"]*?)\\'").getMatch(0);
            }
            if (dllink == null) {
                dllink = br.getRegex("data-dl-url=\\'(https?://[^<>\"]*?)\\'").getMatch(0);
            }
        }
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (br.containsHTML("(?i)File not found")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String serverFilename = getFileNameFromHeader(dl.getConnection());
        if (!StringUtils.isEmpty(serverFilename)) {
            link.setFinalFileName(serverFilename);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}