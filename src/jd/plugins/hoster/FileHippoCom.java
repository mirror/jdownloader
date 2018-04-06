//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filehippo.com" }, urls = { "https?://(?:www\\.)?filehippo\\.com(?:/(?:es|en|pl|jp|de))?/download_[^<>/\"]+(?:(?:/tech)?/\\d+/)?" })
public class FileHippoCom extends PluginForHost {
    private static final String FILENOTFOUND = "(<h1>404 Error</h1>|<b>Sorry the page you requested could not be found|Sorry an error occurred processing your request)";
    public static final String  MAINPAGE     = "https://www.filehippo.com";

    public FileHippoCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("/(es|en|pl|jp|de)", ""));
        if (link.getDownloadURL().matches("http?://(www\\.)?filehippo\\.com(/(es|en|pl|jp|de))?/download_[^<>/\"]+/tech/\\d+/")) {
            final String numbers = new Regex(link.getDownloadURL(), "/(\\d+)/$").getMatch(0);
            final String linkpart = new Regex(link.getDownloadURL(), "/download_([^<>/\"]+)").getMatch(0);
            link.setUrlDownload("https://www.filehippo.com/download_" + linkpart + "/tech/" + numbers + "/");
        } else {
            if (!link.getDownloadURL().endsWith("/")) {
                link.setUrlDownload(link.getDownloadURL() + "/tech/");
            } else {
                link.setUrlDownload(link.getDownloadURL() + "tech/");
            }
        }
    }

    @Override
    public String getAGBLink() {
        return "https://www.filehippo.com/info/disclaimer/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        br = new Browser();
        br.setFollowRedirects(true);
        this.setBrowserExclusive();
        br.setCookie("https://filehippo.com/", "FH_PreferredCulture", "en-US");
        final String url_name = new Regex(link.getDownloadURL(), "filehippo\\.com/(.+)").getMatch(0);
        br.getPage(link.getDownloadURL());
        if (this.br.getURL().equals("http://www.filehippo.com/")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML(FILENOTFOUND) || link.getDownloadURL().contains("/history")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String realLink = br.getRegex("id=\"_ctl0_contentMain_lblPath\"> <strong>\\&#187;</strong>.*?<a href=\"(/download_.*?/\\d+/)\">").getMatch(0);
        // If the user adds a wrong link we have to find the right one here and
        // set it
        if (realLink != null) {
            realLink = "https://www.filehippo.com" + realLink + "tech/";
            link.setUrlDownload(realLink);
            br.getPage(link.getDownloadURL());
            if (br.containsHTML(FILENOTFOUND) || link.getDownloadURL().contains("/history")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        String filename = br.getRegex("<b>Filename:</b></td><td>(.*?)</td>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>Download (.*?) \\- Technical Details \\- FileHippo\\.com</title>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("title: \\'Download (.*?) \\- Technical Details \\- FileHippo\\.com\\'").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("<span itemprop=\"name\">([^<>\"]*?)</span>").getMatch(0);
                }
            }
        }
        if (filename == null) {
            filename = url_name;
        }
        link.setName(filename.trim());
        String filesize = br.getRegex("\\(([0-9,]+ bytes)\\)").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("Download This Version\\s+<span class=\"normal\">\\(([^<>]*?)\\)<").getMatch(0);
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", "")));
        }
        final String md5 = br.getRegex("MD5 Checksum:</span> <span class=\"field\\-value\">([^<>\"]*?)</span>").getMatch(0);
        if (md5 != null) {
            link.setMD5Hash(md5.trim());
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        String normalPage = br.getRegex("id=\"dlbox\">[\n\r\t ]+<a href=\"(/.*?)\"").getMatch(0);
        if (normalPage == null) {
            normalPage = br.getRegex("download-link green button-link active long[^\"]*\"\\s+href=\"(?:https?://(?:www\\.)?filehippo\\.com)?(/[^<>\"]*?)\"").getMatch(0);
            if (normalPage == null) {
                normalPage = br.getRegex("direct-download-link-container\"><a href=\"(/download_[^<>\"]*?)\"").getMatch(0);
            }
        }
        final String mirrorPage = br.getRegex("table id=\"dlboxinner\"[^\n\r\t]*?<a href=\"(/.*?)\"").getMatch(0);
        if ((normalPage == null) && (mirrorPage == null)) { // Download link
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String pages[] = new String[] { mirrorPage, normalPage };
        for (String page : pages) {
            if (page != null) {
                br.getPage(page);
            }
            String dllink = br.getRegex("http-equiv=\"Refresh\" content=\"\\d+; url=(/.*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("id=\"_ctl0_contentMain_lnkURL\" class=\"black\" href=\"(/.*?)\"").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("(/download/file/[a-z0-9]+(/)?)\"").getMatch(0);
                }
            }
            if (dllink == null) {
                continue;
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                if (!br.getURL().contains("filehippo.com")) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Download impossible - download-url points to external site");
                }
                continue;
            }
            downloadLink.setFinalFileName(getFileNameFromHeader(dl.getConnection()));
            dl.startDownload();
            return;
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}