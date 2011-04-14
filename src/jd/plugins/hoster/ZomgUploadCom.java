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

import java.util.SortedMap;
import java.util.TreeMap;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "zomgupload.com" }, urls = { "http://(www\\.)?zomgupload\\.com/[a-z0-9]{12}" }, flags = { 0 })
public class ZomgUploadCom extends PluginForHost {

    public ZomgUploadCom(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public String getAGBLink() {
        return "http://www.zomgupload.com/tos.html";
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        String passCode = null;
        requestFileInformation(link);
        checkErrors(link, false, passCode);
        if (br.containsHTML("<Title>ZOMG Upload - Free File Hosting</Title>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Form form = br.getFormbyProperty("name", "F1");
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Captcha. no image, just html placed numbers
        String[][] temp = br.getRegex("<span style='position:absolute;padding-left:([0-9]+)px;padding-top:[0-9]px;'>([0-9])</span>").getMatches();
        // COPY FROM BIGGERUPLOADCOM
        /* "Captcha Method" */
        if (temp == null || temp.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        SortedMap<Integer, String> capMap = new TreeMap<Integer, String>();
        for (String[] letter : temp) {
            capMap.put(Integer.parseInt(letter[0]), letter[1]);
        }
        StringBuilder code = new StringBuilder();
        for (String value : capMap.values()) {
            code.append(value);
        }
        form.put("code", code.toString());
        // Ticket Time
        String ttt = br.getRegex("countdown\">.*?(\\d+).*?</span>").getMatch(0);
        if (ttt == null) ttt = br.getRegex("id=\"countdown_str\".*?<span id=\".*?\">.*?(\\d+).*?</span").getMatch(0);
        if (ttt != null) {
            logger.info("Waittime detected, waiting " + ttt.trim() + " seconds from now on...");
            int tt = Integer.parseInt(ttt);
            sleep(tt * 1001, link);
        }
        if (br.containsHTML("<br><b>Passwort:</b>")) {
            if (link.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password?", link);
            } else {
                /* gespeicherten PassCode holen */
                passCode = link.getStringProperty("pass", null);
            }
            form.put("password", passCode);
        }
        br.submitForm(form);
        checkErrors(link, true, passCode);
        if (passCode != null) {
            link.setProperty("pass", passCode);
        }
        String dllink = getDllink();
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, -2);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The finallink doesn't seem to be a file...");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();

    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setCookie("http://www.zomgupload.com", "lang", "english");
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("No such file with this filename")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("No such user exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("File not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Form[] forms = br.getForms();
        if (forms == null || forms.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String formFix = br.getRegex("fname.*?value=\"(.*?)\"").getMatch(0);
        if (formFix != null) {
            /* workaround for old browser bug, in 09581 stable */
            forms[0].put("fname", Encoding.urlEncode(formFix));
        }
        forms[0].remove("method_premium");
        br.submitForm(forms[0]);
        String filename = br.getRegex("<tr><td align=right><b>Filename:</b></td><td nowrap>(.*?)</b></td></tr>").getMatch(0);
        String filesize = br.getRegex("<tr><td align=right><b>Size:</b></td><td>(.*?)<small>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        parameter.setName(filename.trim());
        parameter.setDownloadSize(SizeFormatter.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    public String getDllink() {
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            dllink = br.getRegex("dotted #bbb;padding.*?<a href=\"(.*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("This (direct link|download link) will be available for your IP.*?href=\"(http.*?)\"").getMatch(1);
                if (dllink == null) {
                    dllink = br.getRegex("Download: <a href=\"(.*?)\"").getMatch(0);
                }
            }
        }
        return dllink;
    }

    public void checkErrors(DownloadLink theLink, boolean checkAll, String passCode) throws NumberFormatException, PluginException {
        if (checkAll) {
            if (br.containsHTML("(<br><b>Password:</b> <input|<br><b>Passwort:</b> <input|Wrong password)")) {
                logger.warning("Wrong password, the entered password \"" + passCode + "\" is wrong, retrying...");
                theLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (br.containsHTML("Wrong captcha")) {
                logger.warning("Wrong captcha or wrong password!");
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
        }
        // Some waittimes...
        if (br.containsHTML("You have to wait")) {
            int minutes = 0, seconds = 0, hours = 0;
            String tmphrs = br.getRegex("You have to wait.*?\\s+(\\d+)\\s+hours?").getMatch(0);
            if (tmphrs != null) hours = Integer.parseInt(tmphrs);
            String tmpmin = br.getRegex("You have to wait.*?\\s+(\\d+)\\s+minutes?").getMatch(0);
            if (tmpmin != null) minutes = Integer.parseInt(tmpmin);
            String tmpsec = br.getRegex("You have to wait.*?\\s+(\\d+)\\s+seconds?").getMatch(0);
            if (tmpsec != null) seconds = Integer.parseInt(tmpsec);
            int waittime = ((3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
            logger.info("Detected waittime #1, waiting " + waittime + "milliseconds");
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
        }
        if (br.containsHTML("You have reached the download-limit")) {
            String tmphrs = br.getRegex("\\s+(\\d+)\\s+hours?").getMatch(0);
            String tmpmin = br.getRegex("\\s+(\\d+)\\s+minutes?").getMatch(0);
            String tmpsec = br.getRegex("\\s+(\\d+)\\s+seconds?").getMatch(0);
            String tmpdays = br.getRegex("\\s+(\\d+)\\s+days?").getMatch(0);
            if (tmphrs == null && tmpmin == null && tmpsec == null && tmpdays == null) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 60 * 60 * 1000l);
            } else {
                int minutes = 0, seconds = 0, hours = 0, days = 0;
                if (tmphrs != null) hours = Integer.parseInt(tmphrs);
                if (tmpmin != null) minutes = Integer.parseInt(tmpmin);
                if (tmpsec != null) seconds = Integer.parseInt(tmpsec);
                if (tmpdays != null) days = Integer.parseInt(tmpdays);
                int waittime = ((days * 24 * 3600) + (3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
                logger.info("Detected waittime #2, waiting " + waittime + "milliseconds");
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
            }
        }
        if (br.containsHTML("You're using all download slots for IP")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l);
        if (br.containsHTML("Error happened when generating Download Link")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error!", 10 * 60 * 1000l);
        // Errorhandling for only-premium links
        if (br.containsHTML("(You can download files up to.*?only|Upgrade your account to download bigger files|This file reached max downloads)")) {
            String filesizelimit = br.getRegex("You can download files up to(.*?)only").getMatch(0);
            if (filesizelimit != null) {
                filesizelimit = filesizelimit.trim();
                logger.warning("As free user you can download files up to " + filesizelimit + " only");
                throw new PluginException(LinkStatus.ERROR_FATAL, "Free users can only download files up to " + filesizelimit);
            } else {
                logger.warning("Only downloadable via premium");
                throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable via premium");
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

}
