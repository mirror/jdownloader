//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.util.SortedMap;
import java.util.TreeMap;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "biggerupload.com" }, urls = { "http://[\\w\\.]*?biggerupload\\.com/[a-z0-9]{12}" }, flags = { 0 })
public class BiggeruploadCom extends PluginForHost {

    public BiggeruploadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        Form form = br.getForm(0);
        form.setAction(downloadLink.getDownloadURL());
        form.remove("method_premium");
        br.submitForm(form);
        if (br.containsHTML("You have to wait")) {
            int minutes = 0, seconds = 0, hours = 0;
            String tmphrs = br.getRegex("\\s+(\\d+)\\s+hours?").getMatch(0);
            if (tmphrs != null) hours = Integer.parseInt(tmphrs);
            String tmpmin = br.getRegex("\\s+(\\d+)\\s+minutes?").getMatch(0);
            if (tmpmin != null) minutes = Integer.parseInt(tmpmin);
            String tmpsec = br.getRegex("\\s+(\\d+)\\s+seconds?").getMatch(0);
            if (tmpsec != null) seconds = Integer.parseInt(tmpsec);
            int waittime = ((3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
        } else {
            if (br.containsHTML("(You can download files up to.*?only|Upgrade your account to download bigger files)")) {
                String filesizelimit = br.getRegex("You can download files up to(.*?)only").getMatch(0);
                if (filesizelimit != null) {
                    filesizelimit = filesizelimit.trim();
                    logger.warning("As free user you can download files up to " + filesizelimit + " only");
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Free users can download files up to " + filesizelimit + " only");
                } else {
                    logger.warning("Only downloadable via premium");
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable via premium");
                }
            }
            form = br.getFormbyProperty("name", "F1");
            if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

            if (br.containsHTML("background:#ccc;text-align")) {
                /* "Captcha Method" */
                String[][] letters = new Regex(Encoding.htmlDecode(br.toString()), "<span style='position:absolute;padding-left:(\\d+)px;padding-top:\\d+px;'>(\\d)</span>").getMatches();
                if (letters.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                SortedMap<Integer, String> capMap = new TreeMap<Integer, String>();
                for (String[] letter : letters) {
                    capMap.put(Integer.parseInt(letter[0]), letter[1]);
                }
                StringBuilder code = new StringBuilder();
                for (String value : capMap.values()) {
                    code.append(value);
                }
                form.put("code", code.toString());
            }
            form.setAction(downloadLink.getDownloadURL());
            String passCode = null;
            String loginpw = br.getRegex("value=\"login\">(.*?)value=\" Login\"").getMatch(0);
            if (br.containsHTML("name=\"password\"") && !(loginpw != null && loginpw.contains("password"))) {
                logger.info("The downloadlink seems to be password protected.");
                if (downloadLink.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput("Password?", downloadLink);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
                form.put("password", passCode);
                logger.info("Put password \"" + passCode + "\" entered by user in the form.");
            }
            // Ticket Time
            String ttt = br.getRegex("countdown\">.*?(\\d+).*?</span>").getMatch(0);
            if (ttt != null) {
                logger.info("Waittime detected, waiting " + ttt.trim() + " seconds from now on...");
                int tt = Integer.parseInt(ttt);
                sleep(tt * 1001, downloadLink);
            }
            String dllink = br.getRegex("flashvars','file=(http.*?\\.mp3)").getMatch(0);
            if (dllink == null) dllink = br.getRegex("flashvars=\"file=(http.*?\\.mp3)").getMatch(0);
            if (dllink == null) {
                br.submitForm(form);
                dllink = br.getRedirectLocation();
            }
            URLConnectionAdapter con2 = br.getHttpConnection();
            if (con2.getContentType().contains("html")) {
                if (br.containsHTML("You have to wait")) {
                    int minutes = 0, seconds = 0, hours = 0;
                    String tmphrs = br.getRegex("\\s+(\\d+)\\s+hours?").getMatch(0);
                    if (tmphrs != null) hours = Integer.parseInt(tmphrs);
                    String tmpmin = br.getRegex("\\s+(\\d+)\\s+minutes?").getMatch(0);
                    if (tmpmin != null) minutes = Integer.parseInt(tmpmin);
                    String tmpsec = br.getRegex("\\s+(\\d+)\\s+seconds?").getMatch(0);
                    if (tmpsec != null) seconds = Integer.parseInt(tmpsec);
                    int waittime = ((3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
                }
                if (br.containsHTML("(name=\"password\"|Wrong password)") && !(loginpw != null && loginpw.contains("password"))) {
                    logger.warning("Wrong password, the entered password \"" + passCode + "\" is wrong, retrying...");
                    downloadLink.setProperty("pass", null);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                String error = br.getRegex("class=\"err\">(.*?)</font>").getMatch(0);

                if (error != null) {
                    logger.warning(error);
                    con2.disconnect();
                    if (error.equalsIgnoreCase("Wrong captcha") || error.equalsIgnoreCase("Expired session")) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, error, 10000);
                    }
                }
                if (br.containsHTML("Download Link Generated")) dllink = br.getRegex("padding:7px;\">\\s+<a\\s+href=\"(.*?)\">").getMatch(0);
            }
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (passCode != null) {
                downloadLink.setProperty("pass", passCode);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
            boolean error = false;
            try {
                if (dl.getConnection().getContentType() != null && dl.getConnection().getContentType().contains("html")) {
                    error = true;
                }
            } catch (Exception e) {
                error = true;
            }
            if (error == true) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                if (br.containsHTML("File Not Found")) {
                    logger.warning("Server says link offline, please recheck that!");
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public String getAGBLink() {
        return "http://www.biggerupload.com/tos.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://www.biggerupload.com/", "lang", "english");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(No such file)|(No such user)|(Datei nicht gefunden)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex("You\\shave\\srequested\\s<font\\scolor=\"red\">http://[\\w\\.]*?biggerupload\\.com/[a-z0-9]+/(.*?)</font>").getMatch(0));
        String filesize = br.getRegex("</font>\\s\\((.*?)\\)</font>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename);
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
