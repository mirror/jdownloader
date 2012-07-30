//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.LnkCrptWs;
import jd.utils.JDHexUtils;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filestore.to" }, urls = { "http://[\\w\\.]*?filestore\\.to/\\?d=[A-Z0-9]+" }, flags = { 0 })
public class FilestoreTo extends PluginForHost {

    private String aBrowser = "";

    public FilestoreTo(final PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(2000l);

    }

    @Override
    public String getAGBLink() {
        return "http://www.filestore.to/?p=terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 2000;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        final String[] pwnage = br.getRegex(zStatistic("RkM4Q0ZBRkFGQjAzQ0RFMzFFQzBCMjlGREU1RDQyRjEyOTkwNzFCQjA4NUVEODlCMUUxODVCM0IxNTM5ODY4NTU5REZGQzNGOEQ1REU2M0UyMjQxNTNFOUVBMjYxQzczRTA3NTJFQ0FDMUM5NjU2NUE4NzNENkU0NkIwQzM3M0E1QUY2Q0QyOTVEMkFGQkM4MjFBRTJFQkI1MjYwQ0ZCMzQyRkE4QkRFNkJBRDk4QUI5QkQ5MkJDMzJGNjc3ODE1QzM3QTNBNTA=")).getColumn(0);
        if (pwnage == null || pwnage.length == 0) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        final String waittime = br.getRegex("Bitte warte (\\d+) Sekunden und starte dann").getMatch(0);
        final String dlink = "http://filestore.to/ajax/download.php?DDL=";
        int wait = 10;
        if (waittime != null) {
            if (Integer.parseInt(waittime) < 61) {
                wait = Integer.parseInt(waittime);
            }
        }
        sleep(wait * 1001l, downloadLink);
        // If plugin breaks most times this link is changed
        for (final String gam3r : pwnage) {
            br.getPage(dlink + gam3r);
            if (br.containsHTML("(Da hat etwas nicht geklappt|Wartezeit nicht eingehalten|Versuche es erneut)")) {
                continue;
            }
            break;
        }
        if (br.containsHTML("(Da hat etwas nicht geklappt|Wartezeit nicht eingehalten|Versuche es erneut)")) {
            logger.warning("FATAL waittime error!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(true);
        final String dllink = br.toString().trim();
        if (!dllink.startsWith("http://")) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public void haveFun() throws Exception {
        final ArrayList<String> someStuff = new ArrayList<String>();
        final ArrayList<String> regexStuff = new ArrayList<String>();
        regexStuff.add(zStatistic("Rjk4MEZFQTBGQjA3Q0RFNzFCOTZCNkNFREU1MDQ2QUQyREM1NzRFOTA4NThEODlEMUExQjVFMzgxNDZGODNEMDVEODlGQzMyODgwMEUyMzgyMTE2NTRCRUVCMjAxRDc3RTEyMTI4Q0FDMTk4NjczNUFDMjBEMkU0NkY1RjM2Mzk1RkEyQzk3Mw=="));
        regexStuff.add(zStatistic("Rjk4MEZFQTBGRTU2QzlCNzFFQzJCM0M4REE1Qw=="));
        regexStuff.add(zStatistic("Rjk4MEZEQTdGRTBB"));
        regexStuff.add(zStatistic("Rjk4MEZEQTJGQzUyQzlFRg=="));
        for (final String aRegex : regexStuff) {
            aBrowser = br.toString();
            final String replaces[] = br.getRegex(aRegex).getColumn(0);
            if (replaces != null && replaces.length != 0) {
                for (final String dingdang : replaces) {
                    someStuff.add(dingdang);
                }
            }
        }
        for (final String gaMing : someStuff) {
            aBrowser = aBrowser.replace(gaMing, "");
        }
    }

    @Override
    public void init() {
        Browser.setRequestIntervalLimitGlobal(getHost(), 500);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        // Many other browsers seem to be blocked
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:9.0.1) Gecko/20100101 Firefox/9.0.1");
        br.setCustomCharset("utf-8");
        final String url = downloadLink.getDownloadURL();
        String downloadName = null;
        String downloadSize = null;
        for (int i = 1; i < 5; i++) {
            try {
                br.getPage(url);
            } catch (final Exception e) {
                continue;
            }
            if (br.containsHTML(">Download\\-Datei wurde nicht gefunden<")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            if (br.containsHTML(">Download\\-Datei wurde gesperrt<")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            if (br.containsHTML("Entweder wurde die Datei von unseren Servern entfernt oder der Download-Link war")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            haveFun();
            downloadName = new Regex(aBrowser, "(Datei|Dateiname|FileName|Filename): (.*?)(Dateigröße|Filesize):").getMatch(1);
            downloadSize = new Regex(aBrowser, "(Dateigröße|Filesize): (.*?)Uploaded:").getMatch(1);
            if (downloadName != null) {
                downloadLink.setName(Encoding.htmlDecode(downloadName.trim()));
                if (downloadSize != null) {
                    downloadLink.setDownloadSize(SizeFormatter.getSize(downloadSize.replaceAll(",", "\\.").trim()));
                }
                return AvailableStatus.TRUE;
            }

        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {

    }

    @Override
    public void resetPluginGlobals() {
    }

    private String zStatistic(String s) {
        s = Encoding.Base64Decode(s);
        return JDHexUtils.toString(LnkCrptWs.IMAGEREGEX(s));
    }

}