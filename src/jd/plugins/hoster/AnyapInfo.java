//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "anyap.info" }, urls = { "http://(www\\.)?nicosound\\.anyap\\.info/sound/sm\\d+" }, flags = { 0 })
public class AnyapInfo extends PluginForHost {

    /**
     * @author psp
     * @author raztoki
     * */
    public AnyapInfo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://nicosound.anyap.info/about.aspx";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:21.0) Gecko/20100101 Firefox/21.0");
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(" ※ CD音源がそのまま使われている等、")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = br.getRegex("title:\"([^<>\"]*?)\"").getMatch(0);
        final Regex filesize = br.getRegex("style=\"padding\\-bottom: 26px;\"><b>([^<>\"]*?)</b>([^<>\"]*?)</td>");
        if (filename == null || filesize.getMatches().length != 1) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".mp3");
        link.setDownloadSize(SizeFormatter.getSize(filesize.getMatch(0) + filesize.getMatch(1)));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String uid = new Regex(downloadLink.getDownloadURL(), "/(sm\\d+)$").getMatch(0);
        br.setFollowRedirects(false);
        Form dlform = br.getForm(0);
        if (dlform != null) {
            dlform.remove("ctl00%24ContentPlaceHolder1%24SoundInfo1%24AudioPlayer1%24btnStream");
            dlform.remove("ctl00%24ContentPlaceHolder1%24SoundInfo1%24AudioPlayer1%24btnLoadPlayer");
            dlform.remove("ctl00%24Header1%24VideoConvertingBox1%24btnConvertMp3");
            dlform.remove("ctl00%24Header1%24VideoConvertingBox1%24txtUrl");
            dlform.remove("ctl00%24Header1%24VideoConvertingBox1%24btnConvertMp3_2");
            dlform.put("__EVENTTARGET", Encoding.urlEncode("ctl00$ContentPlaceHolder1$SoundInfo1$btnExtract2"));
        }
        Browser br2 = br.cloneBrowser();
        br2.getPage("http://res.anyap.info/nicosound/js/soundhash.aspx?v=" + uid);
        final String cryptedScripts[] = br2.getRegex("p\\}\\((.*?)\\.split\\('\\|'\\)").getColumn(0);
        if (cryptedScripts != null && cryptedScripts.length != 0) {
            // we want the first one...
            dlform.put("ctl00%24ContentPlaceHolder1%24SoundInfo1%24hdnHash", decodeDownloadLink(cryptedScripts[0]));
        }

        br.submitForm(dlform);

        String dllink = br.getRedirectLocation();
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 400) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
            br.followConnection();
            if (br.containsHTML("あなたのIPアドレスからツール等による非正規のリクエストが送信された為")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String decodeDownloadLink(final String s) {
        String decoded = null;

        try {
            Regex params = new Regex(s, "\\'(.*?[^\\\\])\\',(\\d+),(\\d+),\\'(.*?)\\'");

            String p = params.getMatch(0).replaceAll("\\\\", "");
            int a = Integer.parseInt(params.getMatch(1));
            int c = Integer.parseInt(params.getMatch(2));
            String[] k = params.getMatch(3).split("\\|");

            while (c != 0) {
                c--;
                if (k[c].length() != 0) p = p.replaceAll("\\b" + Integer.toString(c, a) + "\\b", k[c]);
            }

            decoded = p;
        } catch (Exception e) {
        }

        if (decoded != null) {
            // static code based on previous decoded... implement dynamic javascript if needed
            String b = new Regex(decoded, "var b='(.*?)';").getMatch(0);
            String c = "";
            // int i = 0; i <= repeat; i++
            for (int i = 0; i < b.length(); i++) {
                c += Character.toString((char) (b.charAt(i) - 49));
            }
            long a = (Long.parseLong(c) ^ 15);
            return Long.toString(a, 10);
        }
        return null;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}