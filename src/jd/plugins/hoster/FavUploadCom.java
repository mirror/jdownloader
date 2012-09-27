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

import java.io.File;
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
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "favupload.com" }, urls = { "http://(www\\.)?favupload\\.com/(video|file|audio)/\\d+/" }, flags = { 0 })
public class FavUploadCom extends PluginForHost {

    private static final String PASSWORDPROTECTED = "The uploader of this file has requested to password protect this page<";
    private static final String FILEIDREGEX       = "/(\\d+)/$";
    private static final String FILELINK          = "http://(www\\.)?favupload\\.com/file/\\d+/";
    private static final String AUDIOLINK         = "http://(www\\.)?favupload\\.com/audio/\\d+/";

    // private static final String VIDEOLINK =
    // "http://(www\\.)?favupload\\.com/video/\\d+/";

    public FavUploadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.favupload.com/terms-of-service/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        // Max 5 connections possible
        return 1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getURL().contains("favupload.com/errors/file-not-found/")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (link.getDownloadURL().matches(FILELINK)) {
            String filename;
            String filesize;
            if (br.containsHTML(PASSWORDPROTECTED)) {
                link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.favuploadcom.passwordprotected", "This link is password protected"));
                final Regex info = br.getRegex("name=\"Description\" content=\"Download [^<>\"/]*?, Filename: ([^<>\"]*?) \\((\\d+\\.\\d+ [A-Za-z]{1,5})\\) \\| FAVUpload\\.com");
                filename = info.getMatch(0);
                filesize = info.getMatch(1);
            } else {
                filename = br.getRegex("<title>Download File: (.*?) \\| FAVUpload</title>").getMatch(0);
                filesize = br.getRegex("<h1>[^<>\"/]*? \\((\\d+\\.\\d+ [A-Za-z]{1,5})\\)</h1>").getMatch(0);
            }
            if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
            if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        } else if (link.getDownloadURL().matches(AUDIOLINK)) {
            final String filename = br.getRegex("<title>Stream Audio: ([^<>\"]*?) \\| FAVUpload</title>").getMatch(0);
            if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".mp3");
        } else {
            final String filename = br.getRegex("<title>Stream Video: ([^<>\"]*?)  \\| FAVUpload</title>").getMatch(0);
            if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".flv");
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String passCode = null;
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (dllink == null) {
            if (downloadLink.getDownloadURL().matches(FILELINK)) {
                final String fid = new Regex(downloadLink.getDownloadURL(), FILEIDREGEX).getMatch(0);

                if (br.containsHTML(PASSWORDPROTECTED)) {
                    passCode = downloadLink.getStringProperty("pass", null);
                    if (passCode == null) passCode = Plugin.getUserInput("Password?", downloadLink);
                    br.postPage("http://www.favupload.com/submit/auth.php", "file_auth=&file_auth_id=" + fid + "&file_pass=" + Encoding.urlEncode(passCode) + "&button2=Access+Page");
                    if (br.containsHTML("alert\\(\"Incorrect Password\"\\);")) {
                        downloadLink.setProperty("pass", null);
                        logger.info("Password wrong!");
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    }
                    br.getPage(downloadLink.getDownloadURL());
                }

                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.setFollowRedirects(false);
                PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                String id = this.br.getRegex("\\?k=([A-Za-z0-9%_\\+\\- ]+)\"").getMatch(0);
                if (id == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                rc.setId(id);
                rc.load();
                for (int i = 0; i <= 5; i++) {
                    File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    String c = getCaptchaCode(cf, downloadLink);
                    br.postPage("http://www.favupload.com/submit/dl.php", "download_file=&file_id=" + fid + "&file_ref=%2Ffile%2F" + fid + "%2F&recaptcha_challenge_field=" + Encoding.urlEncode(rc.getChallenge()) + "&recaptcha_response_field=" + Encoding.urlEncode(c));
                    if (br.containsHTML("\"Incorrect code")) {
                        rc.reload();
                        continue;
                    }
                    break;
                }
                if (br.containsHTML("\"Incorrect code")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                dllink = br.getRegex("status\":1,\"txt\":\"(http://[^<>\"]*?)\"").getMatch(0);
            } else {
                dllink = br.getRegex("flashvars=\\'file=(http://[^<>\"]*?)\\&").getMatch(0);
            }
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) downloadLink.setProperty("pass", passCode);
        // Same chunklimit for all linktypes
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -3);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("(google\\.com/recaptcha/api/|>The Captcha code you submitted was incorrect|>Please enter the verification code below to start your download<)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            // This should never happen!
            if (br.containsHTML("(<title>403 \\- Forbidden</title>|<h1>403 -\\ Forbidden</h1>)")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many simultan downloads", 10 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return true;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}