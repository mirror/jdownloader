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

import java.util.concurrent.atomic.AtomicReference;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imgsrc.ru" }, urls = { "https?://decryptedimgsrc\\.ru/[^/]+/\\d+\\.html(\\?pwd=[a-z0-9]{32})?" }, flags = { 0 })
public class ImgSrcRu extends PluginForHost {

    // DEV NOTES
    // drop requests on too much traffic, I suspect at the firewall on connection.

    private String                  ddlink    = null;
    private String                  password  = null;
    private String                  js        = null;
    private AtomicReference<String> userAgent = new AtomicReference<String>(null);

    public ImgSrcRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void init() {
        Browser.setRequestIntervalLimitGlobal(this.getHost(), 500);
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("decryptedimgsrc", "imgsrc"));
    }

    @Override
    public String getAGBLink() {
        return "http://imgsrc.ru/main/dudes.php";
    }

    public boolean hasAutoCaptcha() {
        return false;
    }

    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return false;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return false;
        }
        return false;
    }

    public Browser prepBrowser(Browser prepBr, Boolean neu) {
        if (neu) {
            String refer = prepBr.getHeaders().get("Referer");
            prepBr = new Browser();
            prepBr.getHeaders().put("Referer", refer);
        }
        prepBr.setFollowRedirects(true);
        prepBr.setReadTimeout(180000);
        prepBr.setConnectTimeout(180000);
        if (userAgent.get() == null || neu) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            userAgent.set(jd.plugins.hoster.MediafireCom.stringUserAgent());
        }
        prepBr.getHeaders().put("User-Agent", userAgent.get());
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        prepBr.setCookie(this.getHost(), "iamlegal", "yeah");
        prepBr.setCookie(this.getHost(), "lang", "en");
        prepBr.setCookie(this.getHost(), "per_page", "48");
        return prepBr;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        prepBrowser(br, false);
        final String r = downloadLink.getStringProperty("Referer", null);
        if (r != null) {
            br.getHeaders().put("Referer", r);
        }
        getPage(downloadLink.getDownloadURL(), downloadLink);
        js = br.getRegex("<script type=\"text/javascript\">([\r\n\t ]+var [a-z]='[a-zA-Z0-9]+';[\r\n\t ]+var [a-z]=[^<]+)</script>").getMatch(0);
        if (js != null) {
            getDllink();
            if (ddlink != null) {
                final URLConnectionAdapter con = br.openGetConnection(ddlink);
                if (con.getContentType().contains("html")) {
                    downloadLink.setAvailable(false);
                    return AvailableStatus.FALSE;
                }
                String filename = getFileNameFromHeader(con);
                String oldname = new Regex(downloadLink.getDownloadURL(), "(\\d+)\\.html").getMatch(0);
                downloadLink.setFinalFileName(oldname + filename.substring(filename.lastIndexOf(".")));
                downloadLink.setDownloadSize(con.getLongContentLength());
                downloadLink.setAvailable(true);
            }
        }
        // dllink = br.getRegex("").getMatch(0);
        return AvailableStatus.TRUE;
    }

    private void processJS() throws Exception {
        final String best = new Regex(js, "'(ori_?pic|big_?pic)'").getMatch(0);
        if (best == null) {
            logger.warning("determining best!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");
        // wrapper fucks up, returns +u.charAt(0)+ as ascii int value. http://www.asciitable.com/
        // ScriptEngineManager mgr = jd.plugins.hoster.DummyScriptEnginePlugin.getScriptEngineManager(this);
        // ScriptEngine engine = mgr.getEngineByName("javascript");
        Object result = null;
        try {
            engine.eval("var document = { getElementById: function (a) { if (!this[a]) { this[a] = new Object(); function src() { return a.src; } this[a].src = src(); } return this[a]; }};");
            engine.eval(js + "\r\nvar result=document.getElementById('" + best + "').src;");
            result = engine.get("result");
        } catch (Throwable e) {

        }
        if (result != null) {
            ddlink = result.toString();
        }
    }

    private void getDllink() throws Exception {
        processJS();
        if (ddlink == null) {
            ddlink = br.getRegex("name=bb onclick='select\\(\\);' type=text style='\\{width:\\d+;\\}' value='\\[URL=[^<>\"]+\\]\\[IMG\\](http://[^<>\"]*?)\\[/IMG\\]").getMatch(0);
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (ddlink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, ddlink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void getPage(String url, DownloadLink downloadLink) throws Exception {
        if (url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(url);
        if (br.containsHTML(">This album has not been checked by the moderators yet\\.|<u>Proceed at your own risk</u>")) {
            // /main/passcheck.php?ad=\d+ links can not br.getURL + "?warned=yeah"
            // lets look for the link
            final String yeah = br.getRegex("/[^/]+/a\\d+\\.html\\?warned=yeah").getMatch(-1);
            if (yeah != null) {
                br.getPage(yeah);
            } else {
                // fail over
                br.getPage(br.getURL() + "?warned=yeah");
            }
        }
        // needs to be before password
        if (br.containsHTML(">Album foreword:.+Continue to album >></a>")) {
            final String newLink = br.getRegex(">shortcut\\.add\\(\"Right\",function\\(\\) \\{window\\.location=\\'(http://imgsrc\\.ru/[^<>\"\\'/]+/[a-z0-9]+\\.html(\\?pwd=([a-z0-9]{32})?)?)\\'").getMatch(0);
            if (newLink == null) {
                logger.warning("Couldn't process Album forward");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(newLink);
        }
        if (br.containsHTML(">Album owner has protected his work from unauthorized access")) {
            Form pwForm = br.getFormbyProperty("name", "passchk");
            if (pwForm == null) {
                logger.warning("Password form finder failed!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (password == null) {
                password = downloadLink.getStringProperty("pass");
                if (password == null) {
                    password = getUserInput("Enter password for link:", downloadLink);
                    if (password == null || password.equals("")) {
                        logger.info("User abored/entered blank password");
                        throw new PluginException(LinkStatus.ERROR_FATAL);
                    }
                }
            }
            pwForm.put("pwd", Encoding.urlEncode(password));
            br.submitForm(pwForm);
            pwForm = br.getFormbyProperty("name", "passchk");
            if (pwForm != null) {
                downloadLink.setProperty("pass", Property.NULL);
                password = null;
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            downloadLink.setProperty("pass", password);
        } else if (br.getURL().equals("http://imgsrc.ru/")) {
            // link has been removed!
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 3;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}