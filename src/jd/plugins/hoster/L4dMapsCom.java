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

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "l4dmaps.com" }, urls = { "http://(www\\.)?l4dmaps\\.com/(details|mirrors|file\\-download)\\.php\\?file=[0-9]+" }, flags = { 0 })
public class L4dMapsCom extends PluginForHost {
    private static final String l4dservers = "l4dservers";

    /** The list of server values displayed to the user */
    private final String[]      servers    = new String[] { "E-Frag #1", "GameServers.com" };

    public L4dMapsCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("(details|mirrors|file-download)", "mirrors"));
    }

    @Override
    public String getAGBLink() {
        return "http://www.l4dmaps.com/terms-of-use.php";
    }

    /**
     * Get the server configured by the user
     * 
     * @return the number of the configured server
     */
    private int getConfiguredServer() {
        switch (getPluginConfig().getIntegerProperty(l4dservers, -1)) {
        case 0:
            logger.fine("The server E-Frag #1 is configured");
            return 0;
        case 1:
            logger.fine("The server GameServers.com is configured");
            return 1;
        default:
            logger.fine("No server is configured, returning default server (E-Frag #1) ...");
            return 1;
        }
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return false;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(parameter.getDownloadURL().replaceAll("(mirrors|file-download|details)", "details"));
        String offlinecheck = br.getRedirectLocation();
        if (offlinecheck != null || br.containsHTML("(404 Not Found|This file could not be found on our system)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>Download(.*?)for Left 4 Dead").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("You are about to download <a href=\".*?\">(.*?)</a>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("rel=\"nofollow\" title=\"Download(.*?)\"").getMatch(0);
            }
        }
        String filesize = br.getRegex(">Size: <em>(.*?)</em>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        if (filesize != null) parameter.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        br.getPage(link.getDownloadURL());
        int configuredServer = getConfiguredServer();
        boolean realusedserver = false;
        String usedServer = "";
        if (configuredServer == 0) {
            usedServer = br.getRegex(">E-Frag #1</a>.*?\"(/file-download\\.php\\?file=.*?)\"").getMatch(0);
            if (usedServer != null) {
                realusedserver = true;
            } else {
                usedServer = br.getRegex(">GameServers.com</a>.*?\"(/file-download\\.php\\?file=.*?)\"").getMatch(0);
            }
        } else if (configuredServer == 1) {
            usedServer = br.getRegex(">GameServers.com</a>.*?\"(/file-download\\.php\\?file=.*?)\"").getMatch(0);
            if (usedServer != null) {
                realusedserver = true;
            } else {
                br.getRegex(">E-Frag #1</a>.*?\"(/file-download\\.php\\?file=.*?)\"").getMatch(0);
            }
        }
        if (usedServer == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        usedServer = "http://www.l4dmaps.com" + Encoding.htmlDecode(usedServer);
        if (realusedserver == true) {
            logger.info("Link to configured server has been successfully taken, link = " + usedServer);
        } else {
            logger.warning("Link to configured server hasn't been successfully taken, link = " + usedServer);
        }
        br.getPage(usedServer);
        String dllink = null;
        if (br.containsHTML(">Enter the captcha to start your download")) {
            String ajaxData = br.getRegex("data: \\{(\".*?\")\\}").getMatch(0);
            if (!br.containsHTML("captcha_show\\.php") || ajaxData == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            ajaxData = ajaxData.replace("\"", "");
            String[][] data = new Regex(ajaxData, "(.*?):([a-z0-9:]+),?").getMatches();
            if (data == null || data.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            int wait = 60;
            String waittime = br.getRegex("\"t\":(\\d+)").getMatch(0);
            if (waittime == null) wait = Integer.parseInt(waittime);
            final StringBuilder sb = new StringBuilder();
            for (String[] singleData : data) {
                sb.append(Encoding.urlEncode(singleData[0]) + "=" + Encoding.urlEncode(singleData[1]) + "&");
            }
            String code = getCaptchaCode("http://www.l4dmaps.com/captcha/captcha_show.php", link);
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getHeaders().put("Accept", "application/json, text/javascript, */*");
            br.postPage("http://www.l4dmaps.com/ajax_assorted.php", "intent=validate_captcha&guess=" + Encoding.urlEncode(code));
            if (br.containsHTML("\"Invalid code entered, please try again")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            if (!br.containsHTML("\"success\"")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            sleep(wait * 1001l, link);
            br.postPage("http://www.l4dmaps.com/ajax_assorted.php", sb.toString());
            dllink = br.getRegex("\"(fbounce\\.php\\?file=[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dllink = Encoding.htmlDecode(dllink);
            dllink = "http://www.l4dmaps.com/" + dllink;
        } else {
            dllink = br.getRegex("http\\-equiv=\"refresh\" content=\"\\d+;url=(.*?)\"").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // Workaround to make the dllink valid
            String entry = new Regex(usedServer, "entry=(\\d+)").getMatch(0);
            if (entry != null) dllink = dllink.replace("entry=0", "entry=" + entry);
            dllink = Encoding.htmlDecode(dllink);
            dllink = "http://www.l4dmaps.com/" + dllink;
        }
        br.getPage(dllink);
        dllink = br.getRedirectLocation();
        if (dllink == null || dllink.contains("index.php")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        logger.info("Final downloadlink = " + dllink);
        // You can download up to 3 files simultaneously from one server so if
        // someone knows how to make JD know that it is downloading 3 files from
        // one server you could make jd switch to the other servers so you can
        // download 6 files at the same time
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, -3);
        link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), l4dservers, servers, JDL.L("plugins.host.L4dMapsCom.servers", "Use this server:")).setDefaultValue(0));
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }
}