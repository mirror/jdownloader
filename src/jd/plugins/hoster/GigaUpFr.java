//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org & pspzockerscene
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gigaup.fr" }, urls = { "http://[\\w\\.]*?gigaup\\.fr/(\\?g=[A-Z|0-9]+|get_file/[A-Z|0-9]+/.+\\.html)" }, flags = { 0 })
public class GigaUpFr extends PluginForHost {

    public GigaUpFr(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.gigaup.fr/contact/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        br.setFollowRedirects(true);
        if (br.containsHTML("Le fichier que vous tentez.*?harger.*?existe pas")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("Fichier supprimé car non utilisé sur une période trop longue")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("Le fichier a.*?sign.*?ill.*?gal")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex("<td>Nom :</td>.*?<td>(.*?)</td>").getMatch(0));
        String filesize = (br.getRegex("<td>Taille :</td>.*?<td>(.*?).</td>").getMatch(0));
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        filesize = filesize + "b";
        link.setName(filename);
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String captchaid = (br.getRegex("<img src=\".*?uid=(.*?)\" style=\"margi").getMatch(0));
        String captchaurl = "http://www.gigaup.fr/constru/images/bot_sucker/bot_sucker.php?uid=" + captchaid;
        String code = getCaptchaCode(captchaurl, downloadLink);
        Form captchaForm = br.getFormbyKey("bot_sucker");
        if (captchaForm == null || captchaid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        captchaForm.remove("dl_file_SSL");
        captchaForm.put("bot_sucker", code);
        br.submitForm(captchaForm);
        if (br.containsHTML("Le code de.*?v.*?ication.*?est incorrecte")) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
        String dllink = br.getRegex("padding:0px;\"><a href=\"(.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(ftp://gigaup.*?:.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 4;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}