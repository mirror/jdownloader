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
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.LinkedList;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "megavideo.com" }, urls = { "http://[\\w\\.]*?megavideo\\.com/(.*?(v|d)=|v/)[a-zA-Z0-9]+" }, flags = { 2 })
public class MegaVideo extends PluginForHost {

    public MegaVideo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.megavideo.com/?c=premium");
    }

    @Override
    public String getAGBLink() {
        return "http://www.megavideo.com/?c=terms";
    }

    public String getDownloadID(DownloadLink link) throws MalformedURLException {
        HashMap<String, String> p = Request.parseQuery(link.getDownloadURL());
        String ret = p.get("v");
        if (ret == null) {
            try {
                Browser br = new Browser();
                br.setCookie("http://www.megavideo.com", "l", "en");
                br.getPage(link.getDownloadURL());
                ret = br.getRegex("previewplayer/\\?v=(.*?)&width").getMatch(0);
            } catch (Exception e) {
            }
        }
        if (ret == null) ret = "";
        return ret.toUpperCase();
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replace("/v/", "/?v="));
        link.setUrlDownload("http://www.megavideo.com/?v=" + getDownloadID(link));
    }

    public void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.setCookie("http://www.megavideo.com", "l", "en");
        br.getPage("http://www.megavideo.com/?s=signup");
        br.postPage("http://www.megavideo.com/?s=signup", "action=login&nickname=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        if (br.getCookie("http://www.megavideo.com", "user") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        br.getPage("http://www.megavideo.com/xml/player_login.php?u=" + br.getCookie("http://www.megavideo.com", "user"));
        if (!br.containsHTML("type=\"premium\"")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        this.setBrowserExclusive();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.getPage(downloadLink.getDownloadURL());
        Browser brc = br.cloneBrowser();
        /* get original downloadlink */
        brc.getPage("http://www.megavideo.com/xml/player_login.php?u=" + br.getCookie("http://www.megavideo.com", "user") + "&v=" + getDownloadID(downloadLink));
        String url = Encoding.urlDecode(brc.getRegex("downloadurl=\"(.*?)\"").getMatch(0), true);
        if (url == null) {
            logger.info("Could not download original file, try to download normal one!");
            this.handleFree(downloadLink);
            return;
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, 0);
            dl.startDownload();
        }
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        String url = null;
        if (br.containsHTML("flashvars.hd = \"1\";")) {
            /* hd link */
            Browser brc = br.cloneBrowser();
            brc.getPage("http://www.megavideo.com/xml/videolink.php?v=" + getDownloadID(link));
            url = Encoding.urlDecode(brc.getRegex("hd_url=\"(.*?)\"").getMatch(0), true);
        } else {
            /* normal link */
            String s = br.getRegex("flashvars.s = \"(\\d+)\";").getMatch(0);
            String un = br.getRegex("flashvars.un = \"(.*?)\";").getMatch(0);
            String k1 = br.getRegex("flashvars.k1 = \"(\\d+)\";").getMatch(0);
            String k2 = br.getRegex("flashvars.k2 = \"(\\d+)\";").getMatch(0);
            if (s == null || un == null || k1 == null || k2 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            url = "http://www" + s + ".megavideo.com/files/" + decrypt(un, Integer.parseInt(k1), Integer.parseInt(k2)) + "/";
        }
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, url, false, 1);
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setCookie("http://www.megavideo.com", "l", "en");
        br.setFollowRedirects(false);
        br.getPage(parameter.getDownloadURL());
        if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("unavailable")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String name = br.getRegex("flashvars.title = \"(.*?)\";").getMatch(0);
        if (name == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("flashvars.hd = \"1\";")) {
            name = name + " (HD)";
        } else {
            Browser brc = br.cloneBrowser();
            brc.getPage("http://www.megavideo.com/xml/videolink.php?v=" + getDownloadID(parameter));
            String size = brc.getRegex("size=\"(\\d+)\"").getMatch(0);
            if (size != null) parameter.setDownloadSize(Long.parseLong(size));
        }
        parameter.setName(Encoding.htmlDecode(name.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /*
     * decrypts the crypted link from megavideo flashvars.un = input
     * flashvars.k1 = k1 flashvars.k2 =k2
     */
    private String decrypt(String input, int k1, int k2) {
        LinkedList<Integer> req1 = new LinkedList<Integer>();
        int req3 = 0;
        while (req3 < input.length()) {
            char c = input.charAt(req3);
            switch (c) {
            case '0':
                req1.add(0);
                req1.add(0);
                req1.add(0);
                req1.add(0);
                break;
            case '1':
                req1.add(0);
                req1.add(0);
                req1.add(0);
                req1.add(1);
                break;
            case '2':
                req1.add(0);
                req1.add(0);
                req1.add(1);
                req1.add(0);
                break;
            case '3':
                req1.add(0);
                req1.add(0);
                req1.add(1);
                req1.add(1);
                break;
            case '4':
                req1.add(0);
                req1.add(1);
                req1.add(0);
                req1.add(0);
                break;
            case '5':
                req1.add(0);
                req1.add(1);
                req1.add(0);
                req1.add(1);
                break;
            case '6':
                req1.add(0);
                req1.add(1);
                req1.add(1);
                req1.add(0);
                break;
            case '7':
                req1.add(0);
                req1.add(1);
                req1.add(1);
                req1.add(1);
                break;
            case '8':
                req1.add(1);
                req1.add(0);
                req1.add(0);
                req1.add(0);
                break;
            case '9':
                req1.add(1);
                req1.add(0);
                req1.add(0);
                req1.add(1);
                break;
            case 'a':
                req1.add(1);
                req1.add(0);
                req1.add(1);
                req1.add(0);
                break;
            case 'b':
                req1.add(1);
                req1.add(0);
                req1.add(1);
                req1.add(1);
                break;
            case 'c':
                req1.add(1);
                req1.add(1);
                req1.add(0);
                req1.add(0);
                break;
            case 'd':
                req1.add(1);
                req1.add(1);
                req1.add(0);
                req1.add(1);
                break;
            case 'e':
                req1.add(1);
                req1.add(1);
                req1.add(1);
                req1.add(0);
                break;
            case 'f':
                req1.add(1);
                req1.add(1);
                req1.add(1);
                req1.add(1);
                break;
            }
            req3++;
        }

        LinkedList<Integer> req6 = new LinkedList<Integer>();
        req3 = 0;
        while (req3 < 384) {
            k1 = (k1 * 11 + 77213) % 81371;
            k2 = (k2 * 17 + 92717) % 192811;
            req6.add((k1 + k2) % 128);
            req3++;
        }
        req3 = 256;
        while (req3 >= 0) {
            int req5 = req6.get(req3);
            int req4 = req3 % 128;
            int req8 = req1.get(req5);
            req1.set(req5, req1.get(req4));
            req1.set(req4, req8);
            --req3;
        }
        req3 = 0;
        while (req3 < 128) {
            req1.set(req3, req1.get(req3) ^ (req6.get(req3 + 256) & 1));
            ++req3;
        }

        String out = "";
        req3 = 0;
        while (req3 < req1.size()) {
            int tmp = (req1.get(req3) * 8);
            tmp += (req1.get(req3 + 1) * 4);
            tmp += (req1.get(req3 + 2) * 2);
            tmp += (req1.get(req3 + 3));
            switch (tmp) {
            case 0:
                out = out + "0";
                break;
            case 1:
                out = out + "1";
                break;
            case 2:
                out = out + "2";
                break;
            case 3:
                out = out + "3";
                break;
            case 4:
                out = out + "4";
                break;
            case 5:
                out = out + "5";
                break;
            case 6:
                out = out + "6";
                break;
            case 7:
                out = out + "7";
                break;
            case 8:
                out = out + "8";
                break;
            case 9:
                out = out + "9";
                break;
            case 10:
                out = out + "a";
                break;
            case 11:
                out = out + "b";
                break;
            case 12:
                out = out + "c";
                break;
            case 13:
                out = out + "d";
                break;
            case 14:
                out = out + "e";
                break;
            case 15:
                out = out + "f";
                break;
            }
            req3 += 4;
        }
        return out;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

}
