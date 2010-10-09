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
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploadspace.pl" }, urls = { "http://[\\w\\.]*?uploadspace\\.pl/plik[a-zA-Z0-9]+(/.+)?\\.htm" }, flags = { 0 })
public class UploadSpacePl extends PluginForHost {

    public UploadSpacePl(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://uploadspace.pl/tos.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(File not found|This file is either removed due to copyright claim or is deleted by the uploader)") || (!br.containsHTML("render.php"))) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // TODO: Filename and size are shown in a picture.
        // To display it correctly a (captcha) recognition would be needed
        String filename = new Regex(link.getDownloadURL(), "uploadspace\\.pl/plik[a-zA-Z0-9]+/(.*?)\\.htm").getMatch(0);
        if (filename != null) link.setName(filename);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        handleErrors();
        br.setFollowRedirects(false);
        Form dlform = br.getFormbyProperty("name", "plik");
        if (dlform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String time2download = br.getRegex("timerend=d\\.getTime\\(\\)\\+(\\d+);").getMatch(0);
        int dlTime = 60;
        if (time2download != null) dlTime = Integer.parseInt(time2download);
        sleep(dlTime + 1000, downloadLink);
        br.submitForm(dlform);
        Form captchaForm = new Form();
        captchaForm.setMethod(MethodType.POST);
        captchaForm.setAction(br.getURL());
        for (int i = 0; i <= 5; i++) {
            String hash = br.getRegex("name=\"hash\" value=\"(.*?)\"").getMatch(0);
            String id = br.getRegex("\\?k=(.*?)\"").getMatch(0);
            if (hash == null || id == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            captchaForm.put("hash", hash);
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setForm(captchaForm);
            rc.setId(id);
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            rc.setCode(c);
            if (br.containsHTML("api.recaptcha.net")) continue;
            break;
        }
        if (br.containsHTML("api.recaptcha.net")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            handleErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            handleErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public void handleErrors() throws PluginException {
        String time2wait = br.getRegex("function starthtimer\\(\\)\\{.*?timerend=d\\.getTime\\(\\)\\+(\\d+);").getMatch(0);
        if (time2wait != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(time2wait));
        if (br.containsHTML(">You are currently downloading")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads", 10 * 60 * 1000l);
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