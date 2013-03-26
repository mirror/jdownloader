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
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ulozisko.sk" }, urls = { "http://[\\w\\.]*?ulozisko\\.sk/[0-9]+(-.*?\\.html|/.+)" }, flags = { 0 })
public class UloziskoSk extends PluginForHost {

    public UloziskoSk(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.ulozisko.sk/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML("Prepáčte, Vaša krajina nie je podporovaná z dôvodu drahého medzinárodnému prenosu dát. Môžete skúsiť")) throw new PluginException(LinkStatus.ERROR_FATAL, "Your country is blocked!");
        String time = br.getRegex("<span class=\"down1\" id=\"cass\">(.*?)</span>").getMatch(0);
        if (time != null) {
            String wait1 = new Regex(time, "(\\d+):").getMatch(0);
            String wait2 = new Regex(time, ".*?:(\\d+)").getMatch(0);
            int wait1int = 0;
            int wait2int = 0;
            if (wait1 != null) wait1int = Integer.parseInt(wait1);
            if (wait2 != null) wait2int = Integer.parseInt(wait2);
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, (wait1int * 60 + wait2int) * 1001l);
        }
        br.setFollowRedirects(false);
        for (int i = 0; i <= 3; i++) {
            Form dlform = br.getFormbyProperty("name", "formular");
            String captchaUrl = br.getRegex("</div> <br /><img src=\"(/.*?)\"").getMatch(0);
            if (captchaUrl == null) captchaUrl = br.getRegex("\"(/obrazky/obrazky\\.php\\?fid=.*?id=.*?)\"").getMatch(0);
            if (dlform == null || captchaUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            captchaUrl = "http://www.ulozisko.sk" + captchaUrl;
            String code = getCaptchaCode(captchaUrl, downloadLink);
            dlform.put("antispam", code);
            br.submitForm(dlform);
            logger.info("Submitted form");
            if ((br.getRedirectLocation() != null && br.getRedirectLocation().contains("error")) || br.containsHTML("Neopísali ste správny overovací reťazec")) {
                logger.info("Code is incorrect!");
                br.getPage(downloadLink.getDownloadURL());
                continue;
            }
            logger.info("Code is correct!");
            break;
        }
        if ((br.getRedirectLocation() != null && br.getRedirectLocation().contains("error")) || br.containsHTML("Neopísali ste správny overovací reťazec")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        if (br.getRedirectLocation() == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, br.getRedirectLocation(), false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("windows-1250");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("Prepáčte, Vaša krajina nie je podporovaná z dôvodu drahého medzinárodnému prenosu dát. Môžete skúsiť")) return AvailableStatus.UNCHECKABLE;
        if (br.containsHTML("(or was removed|is not existed|The requested file does not exists|>Zadaný súbor neexistuje z jedného z nasledujúcich dôvodov:<|Bol zmazaný používateľom\\.|Zle ste opísali adresu odkazu. Pozorne opíšte alebo skopírujte adresu odkazu)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("class=\"down1\">(.*?)<").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("class=\"down2\">(.*?)</div").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("type = \"hidden\" name = \"name\" value = \"(.*?)\"").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("type = \"hidden\" name = \"delete\" value = \"http://www\\.ulozisko\\.sk/\\d+/(.*?)/\"").getMatch(0);
                    if (filename == null) {
                        filename = br.getRegex("type = \"hidden\" name = \"link\" value = \"http://www\\.ulozisko\\.sk/\\d+/(.*?)\"").getMatch(0);
                    }
                }
            }
        }
        String filesize = br.getRegex("Veľkosť súboru: <strong>(.*?)</strong").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }
}