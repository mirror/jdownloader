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
import java.util.Random;

import jd.PluginWrapper;
import jd.config.Property;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mandamais.com.br" }, urls = { "http://[\\w\\.]*?mandamais\\.com\\.br/download/[0-9a-z]+" }, flags = { 0 })
public class MandamaisComBr extends PluginForHost {

    public MandamaisComBr(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.mandamais.com.br/denuncia.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("<title>MandaMais\\.com\\.br :: Disco Virtual Grátis") || br.getURL().contains("mandamais.com.br/home.html")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?) \\| MandaMais\\.com\\.br ::").getMatch(0);
        if (filename == null) br.getRegex("Nome do arquivo:</b> <font color=\"#49436B\"><b>(.*?)</b>").getMatch(0);
        String filesize = br.getRegex("<b>Tamanho do arquivo:</b> (.*?)<br />").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", ".")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = downloadLink.getStringProperty("dllink");
        if (dllink == null) {
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.parse();
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            rc.getForm().put("imageField.x", Integer.toString(new Random().nextInt(100)));
            rc.getForm().put("imageField.y", Integer.toString(new Random().nextInt(100)));
            rc.setCode(c);
            if (br.containsHTML("api\\.recaptcha\\.net/")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            dllink = br.getRegex("</span><br /><br /><center><a href=\"(http://.*?)\"").getMatch(0);
            if (dllink == null) dllink = br.getRegex("\"(http://download\\.mandamais\\.com\\.br/\\d+/\\d+/.*?)\"").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            if (downloadLink.getStringProperty("dllink") != null) {
                logger.info("resetting dllink and trying again");
                downloadLink.setProperty("dllink", Property.NULL);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // Save downloadlink to try to skip captcha when resuming
        downloadLink.setProperty("dllink", dllink);
        dl.startDownload();
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