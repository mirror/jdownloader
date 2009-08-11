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
import java.io.InterruptedIOException;

import jd.PluginWrapper;
import jd.http.RandomUserAgent;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDUtilities;

//bigupload.com by pspzockerscene
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bigupload.com" }, urls = { "http://[\\w\\.]*?bigupload\\.com/d=[A-Z|0-9]+" }, flags = { 0 })
public class BigUploadCom extends PluginForHost {

    public BigUploadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www3.bigupload.com/terms.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("search.php")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex reg = br.getRegex("You have requested:</b>&nbsp;<br>.*?(.*?) \\((.*?)\\)\\..*?</font></td></tr>");
        String filesize = reg.getMatch(1);
        String filename0 = reg.getMatch(0);
        if (filename0 == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = filename0.replaceAll("(\r|\n|\t)", "");
        link.setName(filename);
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        Form form = br.getForm(2);
        form.setAction("/download_frame.php");
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        this.sleep(5 * 1001l, downloadLink);
        br.submitForm(form);
        this.sleep(10 * 1001l, downloadLink);
        URLConnectionAdapter con = br.cloneBrowser().openGetConnection("http://www3.bigupload.com/images/test.bmp?" + System.currentTimeMillis());
        con.disconnect();
        // Link zum Captcha (kann bei anderen Hostern auch mit ID sein)
        String captchaid = br.getRegex("/faq\\.php\\?sid=(.*?)\">Full FAQ").getMatch(0);
        if (captchaid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        String captchaurl = "http://www3.bigupload.com/load_security_code.php?app=download_frame&parent=&obj=sec_img&sid=" + captchaid;
        String code = getCaptchaCode(captchaurl, downloadLink);
        this.sleep(10 * 1001l, downloadLink);
        Form captchaForm = br.getForm(1);
        if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        // Captcha Usereingabe in die Form einfügen
        captchaForm.put("sec_img", code);
        br.setFollowRedirects(false);
        br.submitForm(captchaForm);
        String dllink = br.getRedirectLocation();
        if (br.containsHTML("Enter Code")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        try {
            ((Ftp) JDUtilities.getNewPluginForHostInstance("ftp")).download(Encoding.urlDecode(dllink, true), downloadLink);
        } catch (InterruptedIOException e) {
            if (downloadLink.isAborted()) return;
            throw e;
        } catch (IOException e) {
            if (e.toString().contains("maximum number of clients")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
            } else
                throw e;
        }
    }

    @Override
    public void reset() {
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 8;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
