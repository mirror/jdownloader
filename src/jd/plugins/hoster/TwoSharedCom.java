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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import jd.PluginWrapper;
import jd.http.Browser;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "2shared.com" }, urls = { "http://[\\w\\.]*?2shared\\.com/(audio|file|video)/.*?/[a-zA-Z0-9._]+" }, flags = { 0 })
public class TwoSharedCom extends PluginForHost {

    private static final String MAINPAGE = "http://www.2shared.com";

    public TwoSharedCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("/audio/", "/file/"));
        link.setUrlDownload(link.getDownloadURL().replace("/video/", "/file/"));
    }

    public String decrypt(final int a, final List<String> param) throws Exception {
        Object result = new Object();
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");
        final Invocable inv = (Invocable) engine;
        try {
            if (a == 0) {
                engine.eval("function charalgo(M){" + param.get(0) + "return (M);}");
                result = inv.invokeFunction("charalgo", param.get(1));
            } else {
                engine.eval(param.get(0) + param.get(1));
                result = inv.invokeFunction("decode", param.get(2));
            }
        } catch (final ScriptException e) {
            e.printStackTrace();
        }
        if (result == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        return result.toString();
    }

    @Override
    public String getAGBLink() {
        return "http://www.2shared.com/terms.jsp";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        this.requestFileInformation(downloadLink);
        final Form pwform = this.br.getForm(0);
        if (pwform != null) {
            String passCode;
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput(null, downloadLink);
            } else {
                passCode = downloadLink.getStringProperty("pass", null);
            }
            pwform.put("userPass2", passCode);
            this.br.submitForm(pwform);
            if (this.br.containsHTML("passError\\(\\);")) {
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_CAPTCHA, JDL.L("plugins.hoster.2sharedcom.errors.wrongcaptcha", "Wrong captcha"));
            } else {
                downloadLink.setProperty("pass", passCode);
            }
        }
        String link = this.br.getRegex(Pattern.compile("\\$\\.get\\('(.*?)'", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        String result;
        if (!this.br.containsHTML("\\+key")) {
            final List<String> param = new ArrayList<String>();
            final Browser fn = this.br.cloneBrowser();
            if (!link.contains("\\d+")) {
                // Function Decode
                param.add(new Regex(fn.toString().replaceAll("\n|\t", ""), "function decode(.*?)\\}.*?\\}").getMatch(-1));
                // var em
                param.add(this.br.getRegex("var em='(.*?)';").getMatch(-1));
                // var key
                param.add(this.br.getRegex("var key='(.*?)';").getMatch(0));
                result = this.decrypt(1, param);
                link += result;
            }
            final String jsquery = fn.getPage(TwoSharedCom.MAINPAGE + this.br.getRegex("src=\"(.*?)\"").getMatch(0, 1));
            final String charalgo = new Regex(jsquery, "var viw(.*)l2surl;\\}").getMatch(-1).replaceAll("M\\.url", "M");
            if ((link == null) || (jsquery == null) || (charalgo == null)) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            param.clear();
            param.add(charalgo);
            param.add(link);
            result = this.decrypt(0, param);
        } else {
            result = link + this.br.getRegex("var key='(.*?)';").getMatch(0);
        }
        if (result == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        link = this.br.getPage(TwoSharedCom.MAINPAGE + result).trim();
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, link, true, 1);
        if (this.dl.getConnection().getContentType().contains("html") & (this.dl.getConnection().getURL().getQuery() == null)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            if (this.dl.getConnection().getURL().getQuery().contains("MAX_IP")) {
                this.dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDL.L("plugins.hoster.2sharedcom.errors.sessionlimit", "Session limit reached"), 10 * 60 * 1000l);
            }
        }
        this.dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws PluginException, IOException {
        this.br.setCookiesExclusive(true);
        this.br.getPage(downloadLink.getDownloadURL());
        final Form pwform = this.br.getForm(0);
        if (pwform != null) {
            final String filename = this.br.getRegex("<td class=\"header\" align=\"center\">Download (.*?)</td>").getMatch(0);
            if (filename == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            downloadLink.setName(filename.trim());
            return AvailableStatus.TRUE;
        }
        final String filesize = this.br.getRegex(Pattern.compile("<span class=.*?>File size:</span>(.*?)&nbsp; &nbsp;", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        final String filename = this.br.getRegex("<title>2shared - download(.*?)</title>").getMatch(0);
        if ((filesize == null) || (filename == null)) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.trim().replaceAll(",|\\.", "")));
        return AvailableStatus.TRUE;
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
}
