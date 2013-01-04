//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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

import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "file-upload.net" }, urls = { "((http://(www\\.|en\\.)?file\\-upload\\.net/(member/){0,1}download\\-\\d+/(.*?).html)|(http://(www\\.)?file\\-upload\\.net/(view\\-\\d+/(.*?)\\.html|member/view_\\d+_(.*?)\\.html))|(http://(www\\.)*?file\\-upload\\.net/member/data3\\.php\\?user=(.*?)\\&name=(.*)))" }, flags = { 0 })
public class FileUploadDotnet extends PluginForHost {

    private final Pattern PAT_Download = Pattern.compile("http://[\\w\\.]*?file-upload\\.net/(member/){0,1}download-\\d+/(.*?).html", Pattern.CASE_INSENSITIVE);
    private final Pattern PAT_VIEW     = Pattern.compile("http://[\\w\\.]*?file-upload\\.net/(view-\\d+/(.*?).html|member/view_\\d+_(.*?).html)", Pattern.CASE_INSENSITIVE);
    private final Pattern PAT_Member   = Pattern.compile("http://[\\w\\.]*?file-upload\\.net/member/data3\\.php\\?user=(.*?)&name=(.*)", Pattern.CASE_INSENSITIVE);
    private static String UA           = RandomUserAgent.generate();

    public FileUploadDotnet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("http://en.file-upload", "http://www.file-upload"));
    }

    public String getAGBLink() {
        return "http://www.file-upload.net/to-agb.html";
    }

    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException {
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        br.getHeaders().put("User-Agent", UA);
        br.setFollowRedirects(true);
        try {
            if (new Regex(downloadLink.getDownloadURL(), Pattern.compile(PAT_Download.pattern() + "|" + PAT_Member.pattern(), Pattern.CASE_INSENSITIVE)).matches()) {
                /* LinkCheck für DownloadFiles */
                String downloadurl = downloadLink.getDownloadURL();

                br.getPage(downloadurl);
                if (!br.containsHTML(">Datei existiert nicht")) {
                    // Get complete name
                    String filename = br.getRegex("<title>File\\-Upload\\.net \\- ([^<>\"]*?)</title>").getMatch(0);
                    // This name might be cut
                    if (filename == null) filename = br.getRegex("<h1 class=\\'dateiname\\'>([^<>\"]*?)</h1>").getMatch(0);
                    String filesize = br.getRegex("label>Dateigröße:</label><span>([^<>\"]*?)").getMatch(0);
                    if (filesize != null) {
                        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
                    }
                    downloadLink.setName(Encoding.htmlDecode(filename));
                    return AvailableStatus.TRUE;
                }
            } else if (new Regex(downloadLink.getDownloadURL(), PAT_VIEW).matches()) {
                /* LinkCheck für DownloadFiles */
                String downloadurl = downloadLink.getDownloadURL();
                br.getPage(downloadurl);
                if (!br.containsHTML("Datei existiert nicht auf unserem Server")) {
                    String filename = br.getRegex("<h1>Bildeigenschaften von \"(.*?)\"</h1>").getMatch(0);
                    String filesize;
                    if ((filesize = br.getRegex("e:</b>(.*?)Kbyte").getMatch(0)) != null) {
                        downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(filesize.trim())) * 1024);
                    }
                    downloadLink.setName(filename);
                    return AvailableStatus.TRUE;
                }
            }
        } catch (Exception e) {
            logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.getHeaders().put("User-Agent", UA);
        if (new Regex(downloadLink.getDownloadURL(), Pattern.compile(PAT_Download.pattern() + "|" + PAT_Member.pattern(), Pattern.CASE_INSENSITIVE)).matches()) {
            String dllink = br.getRegex("(http://(www\\.)file\\-upload\\.net/download\\.php\\?valid=[\\d\\.]+&id=\\d+&name=[^\"\\']+)").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dllink = Encoding.htmlDecode(dllink);
            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink);
        } else if (new Regex(downloadLink.getDownloadURL(), PAT_VIEW).matches()) {
            /* DownloadFiles */
            String downloadurl = br.getRegex("<center>\n<a href=\"(.*?)\" rel=\"lightbox\"").getMatch(0);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadurl);
        } else {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        dl.startDownload();
    }

    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }

    public void resetPluginGlobals() {

    }
}