//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "1000eb.com" }, urls = { "http://(www\\.)?1000eb\\.com/[a-z0-9]+" }, flags = { 0 })
public class OneThousandEbCom extends PluginForHost {

    public OneThousandEbCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://1000eb.com/agreements.htm";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String dllink = requestDownloadLink();
        if (dllink == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String jsExecute(final String fun, final boolean b) {
        Object result = new Object();
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");
        final Invocable inv = (Invocable) engine;
        try {
            engine.eval(fun);
            if (b) {
                result = engine.get("servURL");
            } else {
                result = inv.invokeFunction("getDownLink", engine.get("srv"));
            }
        } catch (final Throwable e) {
            return null;
        }
        return result.toString();
    }

    private String requestDownloadLink() throws IOException {
        // js Funktion vorbereiten. Leider recht statisch.
        String servUrl = br.getRegex("(var servURL.*?;)").getMatch(0);
        String dlUrl = br.getRegex("(function getDownLink[^<>]+\\})").getMatch(0);
        if (servUrl == null || dlUrl == null) { return null; }
        servUrl = servUrl.replaceAll("(\\$U\\.environment\\.file\\.|\\$ds\\.thunder\\.)", "");
        dlUrl = dlUrl.replaceAll("(\\$U\\.environment\\.file\\.|downloadSettings\\.)", "");
        final StringBuilder sb = new StringBuilder();
        sb.append("var fid=" + br.getRegex("fid\\s?:\\s?(\'[^<>,]+\')").getMatch(0) + ";");
        String fid = sb.toString();
        sb.append("var fname=" + br.getRegex("fname\\s?:\\s?(\'[^<>,]+\')").getMatch(0) + ";");
        sb.append("var dxservers=" + br.getRegex("dxservers\\s?:\\s?(\'[^<>\n]+\')").getMatch(0) + ";");
        sb.append("var ltservers=" + br.getRegex("ltservers\\s?:\\s?(\'[^<>\n]+\')").getMatch(0) + ";");
        sb.append("var downDelay=" + br.getRegex("downDelay\\s?:\\s?(\\d+)").getMatch(0) + ";");
        sb.append(br.getRegex("(var screctTime = .*?;)").getMatch(0));
        sb.append(br.getRegex("(var acdos = .*?;)").getMatch(0));
        sb.append(servUrl);
        sb.append(dlUrl + ";");

        // und ausführen ...
        final String result = jsExecute(sb.toString(), true);
        if (result == null) { return null; }
        br.getPage(result);

        // ohne diesen Keks kein dl
        fid = fid.split("\'")[1];
        br.setCookie(br.getHost(), "co_AllowedDown", fid);

        // weitere Zutaten ...
        final String query = br.getRegex("queryString:\\s?(\'.*?\'),").getMatch(0);
        final String domain = br.getRegex("\'domain\':\\s?'(\\w+)',").getMatch(0);
        if (query == null || domain == null) { return null; }
        sb.append("var queryString=" + query + ";");
        sb.append("var srv ='" + domain + ".big';");

        // finalen Link bauen
        return jsExecute(sb.toString(), false);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getURL().contains("1000eb.com/exception_0.htm") || br.containsHTML("你要下载的文件已经不存在</span>")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = br.getRegex("\" title=\"点击查看 ([^<>\"\\']+) 的访问统计\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("\">文件名</div>[\t\n\r ]+<div class=\"infotext singlerow\" title=\"([^<>\"\\']+)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("name=\"dl\" href=\"/[a-z0-9]+\"> ([^<>\"\\']+) 的下载地址：</a>").getMatch(0);
            }
        }
        String filesize = br.getRegex("class=\"infotitle\">文件大小</div>[\t\n\r ]+<div class=\"infotext\">([^<>\"\\']+)</div>").getMatch(0);
        if (filename == null || filesize == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        link.setName(Encoding.htmlDecode(filename.trim()));
        filesize = filesize.replace("M", "MB");
        filesize = filesize.replace("K", "KB");
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}