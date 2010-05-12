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
import jd.parser.html.HTMLParser;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "share.cx" }, urls = { "http://[\\w\\.]*?share\\.cx/files/\\d+" }, flags = { 0 })
public class ShareCx extends PluginForHost {

    public ShareCx(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.share.cx/terms";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        String filename = br.getRegex("alt=\"Download\" /></span>(.*?)</h3>").getMatch(0);
        if (filename == null) filename = br.getRegex("name=\"fname\" value=\"(.*?)\"").getMatch(0);
        String filesize = br.getRegex("\">Dateigröße:(.*?)</div>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (filename.trim().equals("") || filesize.trim().equals("")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename.trim());
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        Form dlform0 = br.getForm(0);
        if (dlform0 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dlform0.put("method_free", "Datei+herunterladen");
        br.submitForm(dlform0);
        String reconTime = br.getRegex("startTimer\\((\\d+)\\)").getMatch(0);
        if (reconTime != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(reconTime) * 1001l);
        Form dlform1 = br.getForm(0);
        if (dlform1 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Ticket Time
        String ttt = new Regex(br.toString(), "countdown\">.*?(\\d+).*?</span>").getMatch(0);
        if (ttt == null) ttt = new Regex(br.toString(), "id=\"countdown_str\".*?<span id=\".*?\">.*?(\\d+).*?</span").getMatch(0);
        if (ttt != null) {
            logger.info("Waittime detected, waiting " + ttt.trim() + " seconds from now on...");
            int tt = Integer.parseInt(ttt);
            sleep(tt * 1001, downloadLink);
        }
        if (br.containsHTML("/captchas/")) {
            logger.info("Detected captcha method \"Standard captcha\" for this host");
            String[] sitelinks = HTMLParser.getHttpLinks(br.toString(), null);
            String captchaurl = null;
            if (sitelinks == null || sitelinks.length == 0) {
                logger.warning("Standard captcha captchahandling broken!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (String link : sitelinks) {
                if (link.contains("/captchas/")) {
                    captchaurl = link;
                    break;
                }
            }
            if (captchaurl == null) {
                logger.warning("Standard captcha captchahandling broken!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String code = getCaptchaCode(captchaurl, downloadLink);
            dlform1.put("code", code);
            logger.info("Put captchacode " + code + " obtained by captcha metod \"Standard captcha\" in the form.");
        }
        br.submitForm(dlform1);
        String dllink = br.getRedirectLocation();
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            // This error shows up if you try to download multiple files at the
            // same time
            if (br.containsHTML("<br>oder kaufen Sie sich jetzt einen <a")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}