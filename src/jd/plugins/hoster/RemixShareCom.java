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
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "remixshare.com" }, urls = { "http://[\\w\\.]*?remixshare\\.com/(.*?\\?file=|download/|dl/)[a-z0-9]+" }, flags = { 0 })
public class RemixShareCom extends PluginForHost {

    public static final String BLOCKED = "(class=\"blocked\"|>Von Deiner IP Adresse kamen zu viele Anfragen innerhalb kurzer Zeit\\.)";

    public RemixShareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(3000l);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws PluginException {
        // clean links so prevent dupes and has less side effects with multihosters...
        final String fuid = getFileID(link);
        final String pnd = new Regex(link.getDownloadURL(), "https?://[\\w\\.]*?remixshare\\.com/").getMatch(-1);
        if (fuid == null || pnd == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setUrlDownload(pnd + "download/" + fuid);
        final String linkID = getHost() + "://" + fuid;
        try {
            link.setLinkID(linkID);
        } catch (Throwable e) {
            link.setProperty("LINKDUPEID", linkID);
        }
        // invalid linkformat for open in browser you need to nullify it.
        if (link.getContentUrl() != null && link.getContentUrl().matches("http://[\\w\\.]*?remixshare\\.com/dl/[a-z0-9]+")) {
            link.setContentUrl(link.getDownloadURL());
        }
    }

    private String getFileID(DownloadLink link) {
        return new Regex(link.getDownloadURL(), "(\\?file=|download/|dl/)([a-z0-9]+)").getMatch(1);
    }

    private static final AtomicReference<String> userAgent = new AtomicReference<String>(jd.plugins.hoster.MediafireCom.stringUserAgent());

    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        correctDownloadLink(downloadLink);
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", userAgent.get());
        br.setCookie("http://remixshare.com", "lang_en", "english");
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML(BLOCKED)) {
            return AvailableStatus.UNCHECKABLE;
        }
        br.setFollowRedirects(false);
        // 300 = The uploader has deleted the file
        // 400 = File deleted, maybe abused
        // 500 = Wrong link or maybe deleted some time ago
        if (br.containsHTML("<b>Error Code: [345]00\\.") || br.containsHTML("Please check the downloadlink")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = Encoding.htmlDecode(br.getRegex(Pattern.compile("<span title=\\'([0-9]{10}_)?(.*?)\\'>", Pattern.CASE_INSENSITIVE)).getMatch(1));
        if (filename == null) {
            filename = Encoding.htmlDecode(br.getRegex(Pattern.compile("<title>(.*?)Download at remiXshare Filehosting", Pattern.CASE_INSENSITIVE)).getMatch(0));
        }
        String filesize = br.getRegex("(>|\\.\\.\\.)\\&nbsp;\\((.*?)\\)<").getMatch(1);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setName(filename.trim());
        if (filesize != null) {
            filesize = Encoding.htmlDecode(filesize);
            downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", ".")));
        }
        String md5Hash = br.getRegex("/>MD5:(.*?)<").getMatch(0);
        if (md5Hash != null && md5Hash.trim().length() == 32) {
            downloadLink.setMD5Hash(md5Hash.trim());
        } else {
            /* fix broken md5 sums */
            downloadLink.setMD5Hash(null);
        }
        return AvailableStatus.TRUE;
    }

    private String execJS() throws Exception {
        String fun = null;
        if (fun == null) {
            fun = br.getRegex("document\\.getElementById\\('[\\w\\-]+'\\)\\.href\\s*(=\\s*\"http[^\r\n]+)").getMatch(0);
        }
        if (fun == null) {
            fun = br.getRegex("<a[^>]*href=\"(https?://(?:\\w+\\.)?remixshare\\.com/(?:[^/]+/){4,}\\d+)\"[^>]+title=\"DOWNLOAD\"").getMatch(0);
            if (fun != null) {
                return fun;
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Retry in 15 minutes", 15 * 60 * 1000l);
        }
        fun = "var url" + fun;
        // fun = "function url() { return url; }";
        Object result = null;
        // prevent infinate loop
        int t = -1;
        while (t++ < 15 && true) {
            final ScriptEngineManager manager = jd.plugins.hoster.DummyScriptEnginePlugin.getScriptEngineManager(this);
            final ScriptEngine engine = manager.getEngineByName("javascript");
            final Invocable inv = (Invocable) engine;
            try {
                engine.eval(fun);
                engine.eval("function result(){return url}");
                result = inv.invokeFunction("result", fun);
                break;
            } catch (final Throwable e) {
                if (e.getMessage() != null) {
                    // do not use language components of the error message. Only static identifies, otherwise other languages will
                    // fail!
                    // -raztoki
                    final String ee = new Regex(e.getMessage(), "\"([\\$\\w]+)\" .+").getMatch(0);
                    // should only be needed on the first entry, then on after 'cache' should get result the first time!
                    if (ee != null) {
                        // lets look for missing reference
                        final String ref = new Regex(br, "var\\s+" + Pattern.quote(ee) + "\\s*=\\s*.*?;").getMatch(-1);
                        if (ref != null) {
                            fun = ref + "\r\n" + fun;
                            continue;
                        } else {
                            logger.warning("Could not find missing var/function");
                        }
                    } else {
                        logger.warning("Could not find reference Error");
                    }
                    // getLogger().log(e);
                    // prevent infinate loop
                    break;
                }
            }
        }
        if (result == null) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Retry in 15 minutes", 15 * 60 * 1000l);
        }
        return result.toString();
    }

    public String getAGBLink() {
        return "http://remixshare.com/information/";
    }

    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        if (br.containsHTML(BLOCKED)) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, 10 * 1000l);
        }
        if (br.containsHTML("Download password")) {
            Form pw = br.getFormbyProperty("name", "pass");
            String pass = downloadLink.getStringProperty("pass", null);
            if (pass == null) {
                pass = Plugin.getUserInput("Password?", downloadLink);
            }
            pw.put("passwd", pass);
            br.submitForm(pw);
            br.getPage(br.getRedirectLocation());
            if (br.containsHTML("Incorrect password entered")) {
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.errors.wrongpassword", "Password wrong"));
            } else {
                downloadLink.setProperty("pass", pass);
            }
        }
        String lnk = execJS();
        if (lnk == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!lnk.startsWith("http://") && !lnk.startsWith("https://")) {
            lnk = new Regex(lnk, "<a href=\"(http://.*?)\"").getMatch(0);
        }
        br.getPage(lnk);
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getLongContentLength() == 0) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 2 * 60 * 60 * 1000l);
        }
        if (!(dl.getConnection().isContentDisposition())) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();

    }

    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }

    public void resetPluginGlobals() {
    }

}