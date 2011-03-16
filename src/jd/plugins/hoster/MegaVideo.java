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
import java.util.Locale;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.http.Request;
import jd.nutils.DynByteBuffer;
import jd.nutils.encoding.Encoding;
import jd.nutils.io.JDIO;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "megavideo.com" }, urls = { "http://[\\w\\.]*?megavideo\\.com/(.*?(v|d)=|v/)[a-zA-Z0-9]+" }, flags = { 2 })
public class MegaVideo extends PluginForHost {

    private static String agent = RandomUserAgent.generate();
    private final static Object LOGINLOCK = new Object();

    public MegaVideo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.megavideo.com/?c=premium");
    }

    @Override
    public String getAGBLink() {
        return "http://www.megavideo.com/?c=terms";
    }

    private void antiJDBlock(Browser br) {
        try {
            if (br == null) return;
            br.getHeaders().put("User-Agent", agent);
            br.setAcceptLanguage("en-us,de;q=0.7,en;q=0.3");
            br.setCookie("http://www.megavideo.com", "l", "en");
        } catch (Throwable e) {
            /* setCookie throws exception in 09580 */
        }
    }

    public String getDownloadID(DownloadLink link) throws MalformedURLException {
        String url = link.getDownloadURL().replace("/v/", "/?v=");
        HashMap<String, String> p = Request.parseQuery(url);
        String ret = p.get("v");
        if (ret == null) {
            try {
                Browser br = new Browser();
                antiJDBlock(br);
                br.getPage(link.getDownloadURL());
                ret = br.getRegex("previewplayer/\\?v=(.*?)&width").getMatch(0);
            } catch (Exception e) {
            }
        }
        if (ret == null) ret = "";
        if (ret.length() > 8) {
            ret = ret.substring(0, 8);
        }
        return ret.toUpperCase(Locale.ENGLISH);
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        link.setUrlDownload("http://www.megavideo.com/v/" + getDownloadID(link));
    }

    public void login(final Account account, final boolean cookielogin) throws IOException, PluginException {
        String user = account.getStringProperty("user", null);
        synchronized (LOGINLOCK) {
            this.antiJDBlock(this.br);
            if (cookielogin && user != null) {
                this.br.setCookie("http://www.megavideo.com", "l", "en");
                this.br.setCookie("http://www.megavideo.com", "user", user);
                return;
            } else {
                if (account.getUser().trim().equalsIgnoreCase("cookie")) {
                    this.setBrowserExclusive();
                    this.br.setCookie("http://www.megavideo.com", "l", "en");
                    this.br.setCookie("http://www.megavideo.com", "user", account.getPass());
                    this.br.getPage("http://www.megavideo.com/");
                } else {
                    this.setBrowserExclusive();
                    this.br.setCookie("http://www.megavideo.com", "l", "en");
                    this.br.getPage("http://www.megavideo.com/?s=account");
                    this.br.postPage("http://www.megavideo.com/?s=account", "login=1&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                }
                user = this.br.getCookie("http://www.megavideo.com", "user");
                this.br.setCookie("http://www.megavideo.com", "user", user);
                account.setProperty("user", user);
                if (user == null) {
                    account.setProperty("ispremium", false);
                    account.setProperty("typeknown", false);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
        }
    }

    public boolean isPremium(final Account account, final Browser br, final boolean refresh, boolean cloneBrowser) throws IOException {
        synchronized (LOGINLOCK) {
            if (account == null) { return false; }
            if (account.getBooleanProperty("typeknown", false) == false || refresh) {
                final Browser brc;
                if (cloneBrowser) {
                    brc = br.cloneBrowser();
                } else {
                    brc = br;
                }
                this.antiJDBlock(brc);
                brc.getPage("http://www.megavideo.com/?c=account");
                final String type = brc.getRegex(Pattern.compile("Account type:.*?<b>([^</ ]+)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
                if (type == null || type.equalsIgnoreCase("regular")) {
                    account.setProperty("ispremium", false);
                    if (type != null) {
                        account.setProperty("typeknown", true);
                    } else {
                        account.setProperty("typeknown", false);
                    }
                    return false;
                } else {
                    account.setProperty("ispremium", true);
                    account.setProperty("typeknown", true);
                    return true;
                }
            } else {
                return account.getBooleanProperty("ispremium", false);
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        this.setBrowserExclusive();
        synchronized (LOGINLOCK) {
            try {
                this.login(account, false);
            } catch (final PluginException e) {
                account.setValid(false);
                return ai;
            }
            if (!this.isPremium(account, this.br, true, false)) {
                account.setValid(false);
                ai.setStatus("Free Membership");
                return ai;
            }
        }
        br.getPage("http://www.megavideo.com/?c=account");
        final String type = this.br.getRegex(Pattern.compile("Account type:.*?<b>([^</ ]+)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (type != null && !type.contains("Lifetime")) {
            ai.setStatus("Premium Membership");
            final String days = this.br.getRegex("<b>Premium</b>.*?\\((\\d+) days remaining - <a").getMatch(0);
            if (days != null && !days.equalsIgnoreCase("Unlimited")) {
                ai.setValidUntil(System.currentTimeMillis() + Long.parseLong(days) * 24 * 60 * 60 * 1000);
            } else if (days == null || days.equals("0")) {
                final String hours = this.br.getRegex("<b>Premium</b>.*?\\((\\d+) hours remaining - <a").getMatch(0);
                if (hours != null) {
                    ai.setValidUntil(System.currentTimeMillis() + Long.parseLong(hours) * 60 * 60 * 1000);
                } else {
                    ai.setExpired(true);
                    account.setValid(false);
                    return ai;
                }
            }
        } else if (type != null && type.contains("Lifetime")) {
            ai.setStatus("Lifetime Membership");
        }
        final String points = this.br.getRegex(Pattern.compile("Reward points available:.*?<strong>(\\d+)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (points != null) {
            ai.setPremiumPoints(Long.parseLong(points));
        }
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        synchronized (LOGINLOCK) {
            this.login(account, true);
            if (!this.isPremium(account, this.br.cloneBrowser(), false, true)) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
        }
        br.getPage(downloadLink.getDownloadURL());
        br.forceDebug(true);
        Browser brc = br.cloneBrowser();
        /* get original downloadlink */
        brc.getPage("http://www.megavideo.com/xml/player_login.php?u=" + br.getCookie("http://www.megavideo.com", "user") + "&v=" + getDownloadID(downloadLink));
        String url = Encoding.urlDecode(brc.getRegex("downloadurl=\"(.*?)\"").getMatch(0), true);
        if (url == null) {
            logger.info("Could not download original file, try to download normal one!");
            this.handleFree(downloadLink);
            return;
        } else {
            String name = downloadLink.getName();
            String cpName = url.substring(url.lastIndexOf("/") + 1);
            downloadLink.setFinalFileName(name.endsWith("." + JDIO.getFileExtension(cpName)) ? name : name + "." + JDIO.getFileExtension(cpName));
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, 0);
            dl.startDownload();
        }
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        String url = null;
        String ID = getDownloadID(link);
        Browser brc = br.cloneBrowser();
        brc.getPage("http://www.megavideo.com/xml/videolink.php?v=" + ID);
        if (brc.containsHTML("hd=\"1\"")) {
            /* hd link */
            url = Encoding.urlDecode(brc.getRegex("hd_url=\"(.*?)\"").getMatch(0), true);
        } else {
            /* normal link */
            String s = brc.getRegex("s=\"(\\d+)\"").getMatch(0);
            String un = brc.getRegex("un=\"(.*?)\"").getMatch(0);
            String k1 = brc.getRegex("k1=\"(\\d+)\"").getMatch(0);
            String k2 = brc.getRegex("k2=\"(\\d+)\"").getMatch(0);
            if (s == null || un == null || k1 == null || k2 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            url = "http://www" + s + ".megavideo.com/files/" + decrypt(un, Integer.parseInt(k1), Integer.parseInt(k2)) + "/";
        }
        if (!link.getName().endsWith("." + JDIO.getFileExtension(link.getName()))) {
            link.setName(link.getName() + ".flv");
        }
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, url, false, 1);
        if (dl.getConnection().getLongContentLength() == -1) {
            /* no content length= waiting page */
            DynByteBuffer buffer = new DynByteBuffer(1024);
            int read = -1;
            byte bytebuffer[] = new byte[1];
            while ((read = dl.getConnection().getInputStream().read(bytebuffer)) != -1) {
                /*
                 * filter invalid chars
                 */
                if (read == 1) {
                    if (bytebuffer[0] > 33) buffer.put(bytebuffer, read);
                    if (buffer.position() > 8192) {
                        logger.severe("more than 8kb loaded, but no content-length!");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
            }
            String ret = buffer.toString();
            String waittime = new Regex(ret, "wait(\\d+)").getMatch(0);
            if (waittime != null) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(waittime) * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
            }
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        antiJDBlock(br);
        br.setFollowRedirects(false);
        br.getPage(parameter.getDownloadURL());
        if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("unavailable")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Browser brc = br.cloneBrowser();
        brc.getPage("http://www.megavideo.com/xml/videolink.php?v=" + getDownloadID(parameter));
        String name = Encoding.htmlDecode(brc.getRegex("title=\"(.*?)\"").getMatch(0));
        if (name != null) {
            if (name.length() < 2) name = null;
        }
        if (name == null) {
            name = Encoding.htmlDecode(br.getRegex("description=\"(.*?)\"").getMatch(0));
        }
        if (name != null) {
            if (name.length() < 2) name = null;
        }
        if (name == null) {
            name = "MegaVideoClip_" + System.currentTimeMillis();
        }
        if (brc.containsHTML("hd=\"1\"")) {
            name = name + " (HD)";
        }
        if (brc.containsHTML("error=\"1\"")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String size = brc.getRegex("size=\"(\\d+)\"").getMatch(0);
        if (size != null) parameter.setDownloadSize(Long.parseLong(size));
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
     * decrypts the crypted link from megavideo un = input
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
