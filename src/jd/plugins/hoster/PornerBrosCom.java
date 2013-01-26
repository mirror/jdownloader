//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pornerbros.com" }, urls = { "http://(www\\.)?pornerbros\\.com/\\d+/[\\w-]+\\.html" }, flags = { 0 })
public class PornerBrosCom extends PluginForHost {

    private String DLLINK = null;

    public PornerBrosCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String decryptUrl(String encrypted) {
        char[] c = new char[encrypted.length() / 2];
        for (int i = 0, j = 0; i < encrypted.length(); i += 2, j++) {
            c[j] = (char) ((encrypted.codePointAt(i) - 65) * 16 + (encrypted.codePointAt(i + 1) - 65));
        }
        return String.valueOf(c);
    }

    @Override
    public String getAGBLink() {
        return "http://www.pornerbros.com/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(<title>404 \\- Not Found</title>|This Video Was Not Found On Our Servers</div>|This Video Has Been Removed<)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.getURL().equals("http://www.pornerbros.com/")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?)(\\.)?</title>").getMatch(0);
        if (filename == null) filename = br.getRegex("<h1>(.*?)(\\.)?</h1>").getMatch(0);
        filename = filename.trim().replaceAll("\\.$", "");
        // both downloadmethods are still in use
        String paramJs = br.getRegex("<script type=\"text/javascript\" src=\"(/content/\\d+\\.js([^\"]+)?)\"></script>").getMatch(0);
        if (paramJs != null) {
            br.getPage("http://www.pornerbros.com" + paramJs);
            DLLINK = br.getRegex("url:escape\\(\\'(.*?)\\'\\)").getMatch(0);
            if (DLLINK == null) logger.warning("Null download link, reverting to secondary method. Continuing....");
            String fileExtension = new Regex(DLLINK, "https?://[\\w\\/\\-\\.]+(\\.[a-zA-Z0-9]{0,4})\\?.*").getMatch(0);
            if (fileExtension == ".")
                downloadLink.setFinalFileName(Encoding.htmlDecode(filename + ".flv"));
            else if (fileExtension != "." && fileExtension != null) downloadLink.setFinalFileName(Encoding.htmlDecode(filename + fileExtension));
        } else {
            String paramXml = br.getRegex("name=\"FlashVars\" value=\"xmlfile=(.*?)?(http://.*?)\"").getMatch(1);
            if (paramXml == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.getPage(paramXml);
            String urlCipher = br.getRegex("file=\"(.*?)\"").getMatch(0);
            if (urlCipher == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

            DLLINK = decryptUrl(urlCipher);
            if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String ext = new Regex(DLLINK, "\\&format=([A-Za-z0-9]{1,5})").getMatch(0);
            if (ext == null) {
                ext = DLLINK.substring(DLLINK.lastIndexOf("."));
                if (ext == null || ext.length() > 5) ext = ".mp4";
            }
            if (!ext.startsWith(".")) ext = "." + ext;
            downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ext);
        }

        Browser br2 = br.cloneBrowser();
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html"))
                downloadLink.setDownloadSize(con.getLongContentLength());
            else
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

}