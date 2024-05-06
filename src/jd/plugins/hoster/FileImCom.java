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
import java.util.List;
import java.util.Map;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fileim.com" }, urls = { "https?://(?:www\\.)?fileim\\.com/file/([a-z0-9]+)\\.html" })
public class FileImCom extends PluginForHost {
    public FileImCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.fileim.com/terms.html";
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
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie(this.getHost(), "SiteLang", "en-us");
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!this.canHandle(br.getURL())) {
            /* E.g. redirect to /notfound.html */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(Sorry, the file or folder does not exist|>Not Found<|FileIM \\- Not Found)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("id=\"FileName\" title=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>\\s*FileIM Download File\\s*: ([^<>\"]*?)</title>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("id=\"download_name\" title=\"([^<>\"]*?)\"").getMatch(0);
        }
        String filesizeBytes = null;
        try {
            filesizeBytes = br.getFormbyKey("download_name").getInputField("download_filesize").getValue();
        } catch (final Throwable ignore) {
        }
        final String filesizeVague = br.getRegex("id=\"FileSize\">([^<>\"]*?)<").getMatch(0);
        if (filename != null) {
            link.setFinalFileName(Encoding.htmlDecode(filename).trim());
        } else {
            logger.warning("Failed to find filename");
        }
        if (filesizeBytes != null) {
            link.setVerifiedFileSize(Long.parseLong(filesizeBytes));
        } else if (filesizeVague != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesizeVague.replaceAll("(\\(|\\))", "")));
        } else {
            logger.warning("Failed to find filesize");
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        if (br.containsHTML("\">\\s*Another Download Is Progressing")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait before starting new downloads", 5 * 60 * 1000l);
        }
        final Form form = br.getFormbyKey("download_name");
        final String download_mip = form.getInputField("download_mip").getValue();
        final String download_args = form.getInputField("download_args").getValue();
        final String download_fuseronlycode = form.getInputField("download_fonlycode").getValue();
        final String fid = br.getRegex("download\\.fid=(\\d+);").getMatch(0);
        if (download_args == null || download_mip == null || fid == null || download_fuseronlycode == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Browser br2 = br.cloneBrowser();
        br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        /* 2022-08-25: Waittime is skippable */
        final boolean skipWaittime = true;
        if (!skipWaittime) {
            br2.getPage("/ajax/download/gettimer.ashx");
            final String waitSecondsStr = br2.getRegex("^(\\d+)(_\\d+)?").getMatch(0);
            final int waitSeconds = Integer.parseInt(waitSecondsStr);
            /* 2022-08-25: Seems to be min 30 max 180 seconds */
            final int waitSecondsDefault = 30;
            if (waitSeconds > waitSecondsDefault) {
                logger.info("Pre-download waittime is higher than default value --> A reconnect could bring it back down to " + waitSecondsDefault + " seconds");
            }
            /* Longer than X seconds? Let's reconnect to lower this time to default. */
            if (waitSeconds >= 300) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
            }
            sleep(waitSeconds * 1001l, link);
        }
        br2.getPage("/ajax/download/getdownservers.ashx?type=0");
        final List<Map<String, Object>> downservers = (List<Map<String, Object>>) JavaScriptEngineFactory.jsonToJavaObject(Encoding.htmlDecode(br2.getHttpConnection().getRequest().getHtmlCode()));
        final Map<String, Object> downserver0 = downservers.get(0);
        final String domain = downserver0.get("domain").toString();
        br2.getPage("/ajax/download/setperdown.ashx?mip=" + Encoding.urlEncode(download_mip) + "&args=" + Encoding.urlEncode(download_args));
        final String dlpath = br2.getHttpConnection().getRequest().getHtmlCode();
        final String filenameSlug = link.getName().replaceAll("[^a-zA-Z0-9]", "_");
        // br2.getPage("http://" + domain + "/hi.ashx?jsoncallback=jQuery" + System.currentTimeMillis() + "_" + System.currentTimeMillis() +
        // "&fileuseronlycode=" + download_fuseronlycode + "&fileonlycode=" + this.getFID(link) + "&filesize=" + link.getDownloadSize() +
        // "&_=" + System.currentTimeMillis());
        // final String isfile = PluginJSonUtils.getJsonValue(br2, "isfile");
        // if (!StringUtils.equalsIgnoreCase(isfile, "true")) {
        // /* Broken file? */
        // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 5 * 60 * 1000l);
        // }
        /* 2022-08-25: Without this small delay we won't be able to download. */
        this.sleep(2000, link);
        /* 2022-08-25: This direct-URL is not re-usable! */
        final String dllink = "http://" + domain + "/download/" + dlpath + "/" + download_args + "/" + filenameSlug;
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        if (dl.getConnection().getLongContentLength() == 0) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: Empty file", 5 * 60 * 1000l);
        }
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            final String errorcodeStr = br.getRegex("\\[(\\d+)\\]：").getMatch(0);
            if (errorcodeStr != null) {
                final String errorText;
                if (br.getRequest().getHtmlCode().length() <= 100) {
                    /* Complete html response = Human readable error message */
                    errorText = br.getRequest().getHtmlCode();
                } else {
                    /* html response may contain other garbage -> Create custom error text */
                    errorText = "Error " + Encoding.htmlDecode(errorcodeStr).trim();
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errorText);
            } else if (br.containsHTML("(?i)<div>\\s*Another download is started")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Hoster believes your IP address is already downloading", 10 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}