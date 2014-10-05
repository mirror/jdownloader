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

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;
import org.mozilla.javascript.ConsString;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "zippyshare.com" }, urls = { "http://www\\d{0,}\\.zippyshare\\.com/(d/\\d+/\\d+/.|v/\\d+/[^<>\"/]*?\\.html?|.*?key=\\d+|downloadMusic\\?key=\\d+|swf/player_local\\.swf\\?file=\\d+)" }, flags = { 0 })
public class Zippysharecom extends PluginForHost {

    private String filename = null;
    private String ddlink   = null;
    private String math     = null;

    public Zippysharecom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(final DownloadLink link) {

        final String addedLink = link.getDownloadURL();
        if (addedLink.matches("http://www\\d{0,}\\.zippyshare\\.com/downloadMusic\\?key=\\d+")) {
            link.setUrlDownload(addedLink.replace("downloadMusic?key=", "view.jsp?key="));
        } else if (addedLink.matches("http://www\\d{0,}\\.zippyshare\\.com/swf/player_local\\.swf\\?file=\\d+")) {
            final Regex linkInfo = new Regex(addedLink, "http://(www\\d{0,})\\.zippyshare\\.com/swf/player_local\\.swf\\?file=(\\d+)");
            final String server = linkInfo.getMatch(0);
            final String fid = linkInfo.getMatch(1);
            link.setUrlDownload("http://" + server + ".zippyshare.com/v/" + fid + "/file.html");
        } else if (addedLink.matches("http://www\\d{0,}\\.zippyshare\\.com/d/\\d+/\\d+/.*?")) {
            final Regex linkInfo = new Regex(addedLink, "http://(www\\d{0,})\\.zippyshare\\.com/d/(\\d+)");
            final String server = linkInfo.getMatch(0);
            final String fid = linkInfo.getMatch(1);
            link.setUrlDownload("http://" + server + ".zippyshare.com/v/" + fid + "/file.html");
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        prepareBrowser(downloadLink);
        if (br.containsHTML("(File has expired and does not exist anymore on this server|<title>Zippyshare.com \\- File does not exist</title>|File does not exist on this server)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        String filename = br.getRegex("<title>Zippyshare\\.com \\- (.*?)</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex(Pattern.compile("Name:(\\s+)?</font>(\\s+)?<font style=.*?>(.*?)</font>", Pattern.CASE_INSENSITIVE)).getMatch(2);
        }
        if (filename != null && filename.contains("fileName?key=")) {
            filename = null;
        }
        if (filename == null) {
            filename = br.getRegex("<input value=\"\\[url=[^\\]]+\\](.*?)\\[/url\\]").getMatch(0);
        }
        if (filename == null) {
            final String var = br.getRegex("var fulllink.*?'\\+(.*?)\\+'").getMatch(0);
            filename = br.getRegex("'\\+" + var + "\\+'/(.*?)';").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("document\\.getElementById\\(\\'dlbutton\\'\\)\\.href(.*?\";)").getMatch(0);
            if (filename != null) {
                filename = new Regex(filename, "/([^<>\"]*?)\";").getMatch(0);
            }
        }

        if (filename == null || filename.contains("/fileName?key=")) {
            String url = br.getRegex("document\\.location = '(/d/[^<>\"]+';)").getMatch(0);
            if (url != null) {
                filename = new Regex(url, "d/\\d+/\\d+/([^<>\"]+)';").getMatch(0);
            } else {
                url = br.getRegex("dlbutton'(?:.*?)\"/d/\\d+/.*?/(.*?)\"").getMatch(0);
                if (url != null) {
                    filename = url;
                }
            }
        }

        if (filename == null) {
            filename = br.getRegex("\\+\"/(.*?)\";").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        downloadLink.setName(Encoding.htmlDecode(filename.trim()));
        if (!br.containsHTML(">Share movie:")) {
            final String filesize = br.getRegex(Pattern.compile("Size:(\\s+)?</font>(\\s+)?<font style=.*?>(.*?)</font>", Pattern.CASE_INSENSITIVE)).getMatch(2);
            if (filesize != null) {
                downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.replaceAll(",", "\\.")));
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public String getAGBLink() {
        return "http://www.zippyshare.com/terms.html";
    }

    private int getHashfromFlash(final String flashurl, final int time) throws Exception {
        final String[] args = { "-abc", flashurl };
        // disassemble abc
        final String asasm = flash.swf.tools.SwfxPrinter.main(args);
        final String doABC = new Regex(asasm, "<doABC2>(.*)</doABC2>").getMatch(0);
        final String[] methods = new Regex(doABC, "(function.+?Traits Entries)").getColumn(0);
        String function = null;
        for (final String method : methods) {
            if (method.contains(":::break")) {
                for (final String lines : Regex.getLines(method)) {
                    if (lines.contains("pushbyte")) {
                        function = new Regex(lines, "\t(\\d+)").getMatch(0);
                    } else if (lines.contains("multiply")) {
                        function += "*" + time + "#";
                    } else if (lines.contains("pushint")) {
                        function += new Regex(lines, "\t(\\d+)\t").getMatch(0);
                    } else if (lines.contains("modulo") && function != null) {
                        function = function.replace("#", "%");
                    }
                }
                if (function == null) {
                    return 0;
                }
            }
        }
        return Integer.parseInt(execJS(function, true));
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(true);
        setBrowserExclusive();
        requestFileInformation(downloadLink);
        filename = downloadLink.getName();
        if (!br.containsHTML("</body>\\s*</html>")) {
            // page didn't fully load! http://svn.jdownloader.org/issues/50445 jd://0121413173041
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        final String mainpage = downloadLink.getDownloadURL().substring(0, downloadLink.getDownloadURL().indexOf(".com/") + 5);
        // DLLINK via packed JS or Flash App
        if (br.containsHTML("DownloadButton_v1\\.14s\\.swf")) {
            final String flashContent = br.getRegex("swfobject.embedSWF\\((.*?)\\)").getMatch(0);
            final String flashurl = mainpage + "swf/DownloadButton_v1.14s.swf";
            if (flashContent != null) {
                ddlink = new Regex(flashContent, "url: '(.*?)'").getMatch(0);
                final String seed = new Regex(flashContent, "seed: (\\d+)").getMatch(0);
                if (ddlink == null || seed == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                int time = Integer.parseInt(seed);
                try {
                    time = getHashfromFlash(flashurl, time);
                } catch (final Throwable e) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "JD2 BETA needed!");
                }
                if (time == 0) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                ddlink = ddlink + "&time=" + time;
                // corrupted files?
                if (ddlink.startsWith("nulldownload")) {
                    ddlink = ddlink.replaceAll("null", mainpage);
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                }
            }
        } else if (br.containsHTML("Recaptcha\\.create\\(")) {
            final String rcID = br.getRegex("Recaptcha\\.create\\(\"([^<>\"/]*?)\"").getMatch(0);
            final String shortenCode = br.getRegex("shortencode: \\'(\\d+)\\'").getMatch(0);
            if (rcID == null || shortenCode == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setId(rcID);
            rc.load();
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            final String server = new Regex(br.getURL(), "(http://www\\d{0,}\\.zippyshare\\.com/)").getMatch(0);
            for (int i = 1; i <= 3; i++) {
                final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode(cf, downloadLink);
                br.postPage(server + "rest/captcha/test", "challenge=" + Encoding.urlEncode(rc.getChallenge()) + "&response=" + Encoding.urlEncode(c) + "&shortencode=" + shortenCode);
                if (br.toString().trim().equals("false")) {
                    rc.reload();
                    continue;
                }
                break;
            }
            if (br.toString().trim().equals("false")) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            if (!br.toString().trim().equals("true")) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            ddlink = server + "d/" + getDlCode(downloadLink.getDownloadURL()) + "/" + shortenCode + "/" + Encoding.urlEncode(downloadLink.getName());
        } else {
            ddlink = br.getRegex("var fulllink = \\'(.*?)\\';").getMatch(0);
            if (ddlink != null) {
                final String var = new Regex(ddlink, "'\\+(.*?)\\+'").getMatch(0);
                String data = br.getRegex("var " + var + " = (.*?)\r?\n").getMatch(0);
                data = execJS(data, false);
                if (ddlink.contains(var)) {
                    ddlink = ddlink.replace("'+" + var + "+'", data);
                }
            } else {
                handleWeb();
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, ddlink, false, 1);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
        try {
            dl.startDownload();
        } catch (final PluginException e) {
            if (e.getLinkStatus() != LinkStatus.ERROR_ALREADYEXISTS && downloadLink.getVerifiedFileSize() >= 0 && downloadLink.getDownloadCurrent() == 0) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error (server sends empty file)");
            }
            throw e;
        }
    }

    private void handleWeb() throws Exception {
        ddlink = br.getRegex("(document\\.getElementById\\('dlbutton'\\)\\.href\\s*=\\s*\"/((?!\\s*)|.*?)\";)").getMatch(0);
        if (ddlink == null) {
            ddlink = br.getRegex("(document\\.getElementById\\([^\\)]*\\)\\.href\\s*=\\s*\"(/d/(?!\\s*)|(?!/i/).*?)\";)").getMatch(0);
            if (ddlink == null) {
                // ddlink = br.getRegex("document\\.getElementById\\('dlbutton'\\)\\.href\\s*=\\s*(\"|')?.*?;").getMatch(-1);
                if (ddlink == null) {
                    String[] scripts = br.getRegex("<script type=\"text/javascript\">.*?</script>").getColumn(-1);
                    if (scripts != null) {
                        for (String script : scripts) {
                            if (script.contains("document.getElementById('fimage')")) {
                                ddlink = new Regex(script, regexLastChance3()).getMatch(-1);
                                if (ddlink != null) {
                                    ddlink = ddlink.trim();
                                    break;
                                }
                            }
                        }
                    }
                    if (ddlink != null) {
                        // some correction required
                        setCorrection3 = true;
                    } else {
                        ddlink = br.getRegex(regexLastChance2()).getMatch(0);
                        if (ddlink != null) {
                            // some correction required
                            setCorrection2 = true;
                        } else {

                            ddlink = br.getRegex(regexLastChance1()).getMatch(0);
                            if (ddlink != null) {
                                // some correction required
                                setCorrection1 = true;
                            } else {
                                ddlink = br.getRegex(regexLastChance0()).getMatch(0);
                                if (ddlink != null) {
                                    // some correction required
                                    setCorrection0 = true;
                                } else {

                                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                                }
                            }
                        }
                    }
                }
            }
        }
        math = br.getRegex("<script type=\"text/javascript\">([^>]+var\\s+\\w+\\s*=\\s*function\\(\\)\\s*\\{.*?" + Pattern.quote(ddlink) + ".*?\\}[^<]*)</script>").getMatch(0);
        if (math == null) {
            // this covers when they drop function and var
            math = br.getRegex(".*(<script type=\"text/javascript\">.*?" + Pattern.quote(ddlink) + ".*?[^<]*</script>(\\s*<script type=\"text/javascript\">.*?</script>){0,})").getMatch(0);
        }
        if (ddlink != null && math != null) {
            if (setCorrection0) {
                math = someCorrection0(math);
            } else if (setCorrection1) {
                math = someCorrection1(math);
            } else if (setCorrection2) {
                math = someCorrection2(math);
            } else if (setCorrection3) {
                math = someCorrection3(math);
            } else {
                math = math.replaceAll("\\s*" + Pattern.quote(ddlink), "\r\n\tvar result = " + ddlink);
            }
            String data = execJS(math, false);
            if (data == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (!data.startsWith("/")) {
                data = "/" + data;
            }
            ddlink = data;
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    private String regexLastChance0() {
        return "[\r\n]*([^\r\n]*('|\")dlbutton\\2,\\s*('|\")(/d/\\d+/(?!\\s*)|(?!/i/)[^\r\n]*)\\3\\);)";
    }

    private String regexLastChance1() {
        return "[\r\n]*([^\r\n]*('|\")?(?:dlbutton)\\2,\\s*([^\r\n]*('|\")?(?!/i/)[^\r\n]*)\\4\\);)";
    }

    private String regexLastChance2() {
        return "(\\s*[\\w+\\.]+\\(\\s*('|\")?(?:\\w+)\\2\\s*,\\s*([^\r\n]*('|\")?(?!/i/)[^\r\n]*" + Pattern.quote(filename) + ")\\4\\);)";
    }

    private String regexLastChance3() {
        return "\\s*[\\w+\\.]+\\(\\s*('|\")?(?:\\w+)\\1\\s*,\\s*([^\r\n]*)\\);";
    }

    private boolean setCorrection0 = false;
    private boolean setCorrection1 = false;
    private boolean setCorrection2 = false;
    private boolean setCorrection3 = false;

    private String someCorrection0(String math) {
        String test = new Regex(ddlink, regexLastChance0()).getMatch(3);
        if (test != null) {
            String cleanup = "document.getElementById('dlbutton').href = \"" + test + "\"";
            // has to be first
            math = math.replace(ddlink, cleanup);
            ddlink = cleanup;
        }
        return math;
    }

    private String someCorrection1(String math) {
        String test = new Regex(ddlink, regexLastChance1()).getMatch(2);
        if (test != null) {
            String cleanup = "document.getElementById('dlbutton').href = " + test + "\"";
            // has to be first
            math = math.replace(ddlink, cleanup);
            ddlink = cleanup;
        }
        return math;
    }

    private String someCorrection2(String math) {
        String test = new Regex(ddlink, regexLastChance2()).getMatch(2);
        if (test != null) {
            String cleanup = "document.getElementById('dlbutton').href = " + test + "\"";
            // has to be first
            math = math.replace(ddlink, cleanup);
            ddlink = cleanup;
        }
        return math;
    }

    private String someCorrection3(String math) {
        String test = new Regex(ddlink, regexLastChance3()).getMatch(1);
        if (test != null) {
            String cleanup = "document.getElementById('dlbutton').href = " + test;
            // has to be first
            math = math.replace(ddlink, cleanup);
            ddlink = cleanup;
        }
        return math;
    }

    private String execJS(final String funny, final boolean fromFlash) throws Exception {
        String fun = funny;
        Object result = new Object();
        try {
            if (!fromFlash) {
                // validate function name?, we need html/js parser (which knows open and closing statement of js/html!
                String[] functions = new Regex(fun, "var ([a-z0-9]+) = function\\(\\)").getColumn(0);
                if (functions != null) {
                    for (String f : functions) {
                        result = new Object();
                        try {
                            result = processJS(f, fun);
                        } catch (final Throwable e) {
                        }
                        if (result != null && result instanceof ConsString) {
                            return result.toString();
                        }
                        // nullify
                        fun = fun.replace("var " + f + " = function()", "var " + getSoup());
                    }
                }
                result = new Object();
                String v = new Regex(fun, "var ([a-z0-9]+) = function").getMatch(0);
                if (v == null) {
                    // prevent null value or static value been used against us.
                    v = getSoup();
                }
                // document.getElementById('id').href
                try {
                    result = processJS(v, fun);
                } catch (final Throwable e) {
                }
                if (result != null && result instanceof ConsString) {
                    return result.toString();
                }
            } else {
                final ScriptEngineManager manager = jd.plugins.hoster.DummyScriptEnginePlugin.getScriptEngineManager(this);
                final ScriptEngine engine = manager.getEngineByName("javascript");
                result = ((Double) engine.eval(fun)).intValue();
            }
        } catch (final Throwable e) {
            throw new Exception("JS Problem in Rev" + getVersion(), e);
        }
        return result == null ? null : result.toString();
    }

    private String getSoup() {
        final Random r = new Random();
        final String soup = "abcdefghijklmnopqrstuvwxyz";
        String v = "";
        for (int i = 0; i < 12; i++) {
            v = v + soup.charAt(r.nextInt(soup.length()));
        }
        return v;
    }

    private Object processJS(final String v, final String fun) throws Exception {
        final ScriptEngineManager manager = jd.plugins.hoster.DummyScriptEnginePlugin.getScriptEngineManager(this);
        final ScriptEngine engine = manager.getEngineByName("javascript");
        engine.eval("var document = { getElementById: function (a) { if (!this[a]) { this[a] = new Object(); function href() { return a.href; } this[a].href = href(); } return this[a]; }};");
        engine.eval(fun + "if(typeof " + v + "=='function'){" + v + "();}\r\nvar result=document.getElementById('dlbutton').href;");
        return engine.get("result");
    }

    private static AtomicReference<String> agent = new AtomicReference<String>(null);

    private void prepareBrowser(final DownloadLink downloadLink) throws IOException {
        if (agent.get() == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            agent.set(jd.plugins.hoster.MediafireCom.stringUserAgent());
        }
        br.getHeaders().put("User-Agent", agent.get());
        br.setCookie("http://www.zippyshare.com", "ziplocale", "en");
        br.getPage(downloadLink.getDownloadURL().replaceAll("locale=..", "locale=en"));
        if (br.getRedirectLocation() != null) {
            br.getPage(br.getRedirectLocation());
        }
    }

    private String getDlCode(final String dlink) {
        String dlcode = new Regex(dlink, "zippyshare\\.com/v/(\\d+)").getMatch(0);
        if (dlcode == null) {
            dlcode = new Regex(dlink, "(\\d+)$").getMatch(0);
        }
        return dlcode;
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

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }

}