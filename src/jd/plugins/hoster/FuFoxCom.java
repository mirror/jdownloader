//    jDownloader - Downloadmanager
//    Copyright (C) 2010  JD-Team support@jdownloader.org
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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fufox.net", "fufox.com" }, urls = { "http://(www\\.)?fufox\\.(com|net)/\\?d=[A-Za-z0-9]+", "fhejt7wehrtijrthUNUSED_REGEX" }, flags = { 0, 0 })
public class FuFoxCom extends PluginForHost {

    private static final String CAPTCHATEXT = "code\\.php";

    public FuFoxCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.fufox.com/cgu.html";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("fufox.com/", "fufox.net/"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        // Convert old links to new ones
        correctDownloadLink(link);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("<b>Il y a une erreur dans l'url de cette page\\.</b>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<b>Nom du fichier</b> : (.*?) <br").getMatch(0);
        String filesize = br.getRegex("<b>Taille du fichier</b> : (.*?) <br />").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        filesize = filesize.replace("Mo", "Mb").replace("Go", "Gb");
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (!br.containsHTML(CAPTCHATEXT)) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.postPage(downloadLink.getDownloadURL(), "code=" + getCaptchaCode("http://www.fufox.net/code.php", downloadLink));
        if (br.containsHTML(CAPTCHATEXT)) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        Regex allData = br.getRegex("onclick=\"timingout2?\\(\\'(\\d+)\\',\\'(.*?)\\',\\'(.*?)\\',\\'(.*?)\\',\\'(.*?)\\'\\);");
        String name = allData.getMatch(1);
        String password = allData.getMatch(2);
        String filename = allData.getMatch(3);
        String ip = allData.getMatch(4);
        if (name == null || password == null || filename == null || ip == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Waittime can be skipped here
        String dllink = "ftp://" + name + ":" + password + "@" + ip + "/" + filename;
        try {
            ((Ftp) JDUtilities.getNewPluginForHostInstance("ftp")).download(dllink, downloadLink);
        } catch (InterruptedIOException e) {
            if (downloadLink.isAborted()) return;
            throw e;
        } catch (IOException e) {
            if (e.toString().contains("maximum number of clients")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
            } else {
                throw e;
            }
        }
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