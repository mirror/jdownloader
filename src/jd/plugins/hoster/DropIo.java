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

import java.io.File;
import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.SizeFormatter;

//drop.io by pspzockerscene
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "drop.io" }, urls = { "http://[\\w\\.]*?drop\\.io/[0-9-zA-Z\\._#]+" }, flags = { 0 })
public class DropIo extends PluginForHost {

    public DropIo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://drop.io/terms";
    }

    public void correctDownloadLink(DownloadLink link) {
        // Remove subdomains
        link.setUrlDownload("http://drop.io/" + new Regex(link.getDownloadURL(), "drop\\.io/(.+)").getMatch(0));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("Guest or Admin Password")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("\"title\":\"(.*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("\"filename\":\"(.*?)\"").getMatch(0);
            if (filename == null) filename = br.getRegex("media_view_order=\".*?title=\"(.*?)\"").getMatch(0);
        }
        String filesize = br.getRegex("\"filesize\":(\\d+),\"").getMatch(0);
        // the filesize of videolinks is null so we only check if the filename
        // is null
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename);
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        String id = br.getRegex("id\":(\\d+)").getMatch(0);
        if (id == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String playerUrl = downloadLink.getDownloadURL() + "/asset/" + id + "/player";
        br.postPage(playerUrl, "");
        String IamAHuman = br.getRegex("class=\"downloadButton\" style=\"width: 150px; margin: 0pt auto; height: 40px; position: relative; top: 23px; font-size: 11px;\"><a href=\"(http://.*?)\"").getMatch(0);
        if (IamAHuman == null) IamAHuman = br.getRegex("\"(http://drop\\.io/download/[a-z0-9]+/[a-z0-9]+/Asset/\\d+/v3/original_content)\"").getMatch(0);
        if (IamAHuman == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // check if the download starts instantly or if there is a need to type
        // in captchas
        URLConnectionAdapter con = br.openGetConnection(IamAHuman);
        if (con.getContentType().contains("html")) {
            // Captcha handling
            br.followConnection();
            String k = br.getRegex("Recaptcha\\.create\\(\"(.*?)\",").getMatch(0);
            if (k != null) {
                /* recaptcha */
                // Usually we use the Re Captcha class for Re Captcha sites but
                // in this case, the site is different so this is how to
                // manually handle re captcha
                Browser rcBr = br.cloneBrowser();
                rcBr.getPage("http://api.recaptcha.net/challenge?k=" + k);
                String challenge = rcBr.getRegex("challenge : '(.*?)',").getMatch(0);
                String server = rcBr.getRegex("server : '(.*?)',").getMatch(0);
                String captchaAddress = server + "image?c=" + challenge;
                File captchaFile = this.getLocalCaptchaFile();
                Browser.download(captchaFile, rcBr.openGetConnection(captchaAddress));
                String code = getCaptchaCode(captchaFile, downloadLink);
                Form DLForm = new Form();
                DLForm.put("recaptcha_challenge_field", challenge);
                DLForm.put("recaptcha_response_field", code);
                jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLForm, true, 0);
                if (!(dl.getConnection().isContentDisposition())) {
                    br.followConnection();
                    // Captcha errorhandling
                    if (br.containsHTML("Captcha Does Not Match")) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dl.startDownload();
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        jd.plugins.BrowserAdapter.openDownload(br, downloadLink, IamAHuman, true, 0);
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