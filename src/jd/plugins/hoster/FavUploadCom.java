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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "favupload.com" }, urls = { "http://(www\\.)?favupload\\.com/(video|file|audio)/\\d+/" }, flags = { 0 })
public class FavUploadCom extends PluginForHost {

    public FavUploadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.favupload.com/terms-of-service/";
    }

    private static final String PASSWORDPROTECTED = "(> The uploader of this file has requested to password protect this page|Enter the password in the text box below and submit|type=\"hidden\" name=\"file_auth\")";
    private static final String FILEIDREGEX       = "/(\\d+)/$";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getURL().contains("favupload.com/errors/file-not-found/") || br.containsHTML("(>The file you requested could not be found|> It may have been removed by the uploader or violated our)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<strong>File Name:</strong> (.*?) <br").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>Download File: (.*?) \\| FAVUpload</title>").getMatch(0);
        String filesize = br.getRegex("File Size:</strong> (.*?)</div>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        if (br.containsHTML(PASSWORDPROTECTED)) link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.favuploadcom.passwordprotected", "This link is password protected"));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String passCode = null;
        if (br.containsHTML(PASSWORDPROTECTED)) {
            passCode = downloadLink.getStringProperty("pass", null);
            if (passCode == null) passCode = Plugin.getUserInput("Password?", downloadLink);
            br.postPage("http://www.favupload.com/auth/", "file_auth=&file_auth_id=" + new Regex(downloadLink.getDownloadURL(), FILEIDREGEX).getMatch(0) + "&file_pass=" + Encoding.urlEncode(passCode) + "&button2=Submit");
            if (br.containsHTML(PASSWORDPROTECTED) || br.containsHTML(">Sorry, The password you submitted is incorrect")) {
                downloadLink.setProperty("pass", null);
                logger.info("Password wrong!");
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }
        br.setFollowRedirects(false);
        Form capForm = new Form();
        capForm.setMethod(MethodType.POST);
        capForm.setAction(downloadLink.getDownloadURL());
        capForm.put("download_file", "");
        capForm.put("file_id", new Regex(downloadLink.getDownloadURL(), FILEIDREGEX).getMatch(0));
        capForm.put("file_ref", new Regex(downloadLink.getDownloadURL(), "favupload\\.com(/.+)").getMatch(0));
        capForm.put("submit", "Download+File");
        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        rc.setForm(capForm);
        String id = this.br.getRegex("\\?k=([A-Za-z0-9%_\\+\\- ]+)\"").getMatch(0);
        rc.setId(id);
        rc.load();
        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
        String c = getCaptchaCode(cf, downloadLink);
        rc.prepareForm(c);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, rc.getForm(), true, -5);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("(google\\.com/recaptcha/api/|>The Captcha code you submitted was incorrect|>Please enter the verification code below to start your download<)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            // This should never happen!
            if (br.containsHTML("(<title>403 \\- Forbidden</title>|<h1>403 -\\ Forbidden</h1>)")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many simultan downloads", 10 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) downloadLink.setProperty("pass", passCode);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        // Max 5 connections possible
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}