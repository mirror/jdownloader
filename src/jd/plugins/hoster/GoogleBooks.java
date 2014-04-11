//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import jd.PluginWrapper;
import jd.captcha.easy.load.LoadImage;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "books.google.com" }, urls = { "http://googlebooksdecrypter(\\.[a-z]+){1,2}/books\\?id=.*&pg=.*" }, flags = { 0 })
public class GoogleBooks extends PluginForHost {

    // Dev Notes
    // not entirely sure if sigs are bound to cookie session.

    private String                        agent    = null;
    // [bookuid+pageuid], sig
    private LinkedHashMap<String, String> bookList = new LinkedHashMap<String, String>();
    // last ajax request
    private static AtomicLong             sysTime  = new AtomicLong(0);

    /**
     * @author raztoki
     * */
    public GoogleBooks(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("googlebooksdecrypter", "books.google"));
    }

    @Override
    public String getAGBLink() {
        return "http://books.google.com/intl/en/googlebooks/tos.html";
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void handleFree(DownloadLink link) throws Exception {
        // jd on first run needs to load previous LinkedHashMap
        LinkedHashMap<String, String> saved = new LinkedHashMap<String, String>();
        saved = (LinkedHashMap<String, String>) getPluginConfig().getProperty("savedSigs");
        if (saved != null) bookList = saved;
        this.setBrowserExclusive();
        prepBrowser(br);
        br.setFollowRedirects(true);

        String buid = link.getStringProperty("buid", null);
        if (buid == null) buid = new Regex(link.getDownloadURL(), "(&|\\?)id=([a-zA-Z_\\-]{12})").getMatch(1);
        String page = link.getStringProperty("page", null);
        if (page == null) page = new Regex(link.getDownloadURL(), "(&|\\?)pg=([A-Z]{2}\\d+)").getMatch(1);

        if (br.getCookies(link.getDownloadURL()) == null) br.getPage(link.getDownloadURL());

        String dllink = bookList.get(buid + page);
        if (dllink == null) {
            // when was the last download?? lets prevent more requests if last download was to recent.
            long ran = (new Random().nextInt(10) * 1317) + 9531;
            if (System.currentTimeMillis() <= sysTime.get() + ran) sleep(ran, link);
            dllink = getImg(buid, page, link);
            if (dllink == null) {
                // we have hit some session limit
                getPluginConfig().setProperty("cookies", Property.NULL);
                getPluginConfig().setProperty("agent", Property.NULL);
                getPluginConfig().save();
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Limit Reached", ran * 69);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        URLConnectionAdapter con = dl.getConnection();
        if (con.getContentType().contains("html")) {
            br.followConnection();
            bookList.remove(buid + page);
            getPluginConfig().setProperty("cookies", Property.NULL);
            getPluginConfig().setProperty("agent", Property.NULL);
            getPluginConfig().save();
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        // Get and set the correct ending of the file!
        String correctEnding = LoadImage.getFileType(link.getDownloadURL(), dl.getConnection().getContentType());
        String wrongEnding = null;
        if (link.getName().lastIndexOf('.') > 0) wrongEnding = link.getName().substring(link.getName().lastIndexOf('.'));
        if (correctEnding != null && wrongEnding != null) link.setFinalFileName(link.getName().replace(wrongEnding, correctEnding));
        if (correctEnding != null && wrongEnding == null) link.setFinalFileName(link.getName() + correctEnding);
        dl.startDownload();
        // post download events
        bookList.remove(buid + page);
        getPluginConfig().setProperty("savedSigs", bookList);
        // set last download
        sysTime.set(System.currentTimeMillis());
        // saving session info can result in you not having to enter a captcha for each new link viewed!
        final HashMap<String, String> cookies = new HashMap<String, String>();
        final Cookies add = br.getCookies(this.getHost());
        for (final Cookie c : add.getCookies()) {
            cookies.put(c.getKey(), c.getValue());
        }
        getPluginConfig().setProperty("cookies", cookies);
        getPluginConfig().setProperty("agent", agent);
        getPluginConfig().save();
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }

    private Browser prepBrowser(Browser prepBr) {
        // load previous agent, could be referenced with cookie session. (not tested)

        // define custom browser headers and language settings.
        if (agent == null) agent = getPluginConfig().getStringProperty("agent", null);
        if (agent == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            agent = jd.plugins.hoster.MediafireCom.stringUserAgent();
        }
        prepBr.getHeaders().put("User-Agent", agent);

        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        prepBr.getHeaders().put("Accept-Charset", null);
        prepBr.getHeaders().put("Pragma", null);
        // loading previous cookie session results in less captchas
        final Object ret = getPluginConfig().getProperty("cookies", null);
        if (ret != null) {
            final HashMap<String, String> cookies = (HashMap<String, String>) ret;
            for (Map.Entry<String, String> entry : cookies.entrySet()) {
                prepBr.setCookie(this.getHost(), entry.getKey(), entry.getValue());
            }
        }
        return prepBr;
    }

    public String getImg(String buid, String page, DownloadLink link) throws Exception {
        String dl = null;
        String host = new Regex(link.getDownloadURL(), "(https?://[^/]+)").getMatch(0);
        // http: //
        // books.google.com/books?id=ODfjmOeNLMUC&printsec=frontcover&dq=reflection&hl=de&sa=X&ei=ndlHU8uMF4Xt4gS1joCYCg&redir_esc=y
        br.clearCookies(host);
        br.getPage(link.getDownloadURL());
        // br.getHeaders().put("Referer", host + "/books?id=" + buid + "&printsec=frontcover&dq=reflection");
        br.getHeaders().put("Referer", host + "/books?id=" + buid + "&printsec=frontcover&dq=reflection&hl=de&sa=X&ei=ndlHU8uMF4Xt4gS1joCYCg&redir_esc=y");

        // br.getPage("http://books.google.de/books?id=ODfjmOeNLMUC&lpg=PP1&dq=reflection&hl=de&pg=PR9&jscmd=click3&vq=reflection&source=gbs_snippet&redir_esc=y");
        br.getPage(host + "/books?id=" + buid + "&lpg=PP1&pg=" + page + "&jscmd=click3");

        String[][] results = br.getRegex("\"pid\":\"(P[AP][0-9]+)\",\"src\":\"(http[^\"]+)").getMatches();

        if (results != null && results.length != 0) {
            for (String[] result : results) {
                bookList.put(buid + result[0], unescape(result[1]));
                if (result[0].equalsIgnoreCase(page)) {
                    dl = unescape(result[1]);
                }
            }
        }
        // lets save after each run
        getPluginConfig().setProperty("savedSigs", bookList);
        return dl;
    }

    private static synchronized String unescape(final String s) {
        /* we have to make sure the youtube plugin is loaded */
        final PluginForHost plugin = JDUtilities.getPluginForHost("youtube.com");
        if (plugin == null) throw new IllegalStateException("youtube plugin not found!");

        return jd.plugins.hoster.Youtube.unescape(s);
    }

}