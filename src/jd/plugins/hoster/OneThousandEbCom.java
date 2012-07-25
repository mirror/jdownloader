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
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDHexUtils;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "1000eb.com" }, urls = { "http://(www\\.)?1000eb\\.com/[a-z0-9]+" }, flags = { 0 })
public class OneThousandEbCom extends PluginForHost {

    public OneThousandEbCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private String fortyTwo(String s) {
        s = Encoding.Base64Decode(s);
        return JDHexUtils.toString(jd.plugins.decrypter.LnkCrptWs.IMAGEREGEX(s));
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
        JDUtilities.getPluginForDecrypt("linkcrypt.ws");
        requestFileInformation(downloadLink);
        final String js = br.getRegex(fortyTwo("RkM4QkZBRjFGQTAwQzhCMjFGOTZCMjk1REU1RDQzRkMyODkwNzBCRjBEMEFEQzk4MUU0QzVFMzgxNDY4ODY4NTU4OEZGQzMzODgwQUUxNkYyNTQ2NTVCREVBNzUxQzI2RTE3NzJEOUNDNUNDNjU2MkE4NzREMkU1NkY1QTM2Njg1RUExQ0Q3OTVDNzlGQjk5MjRGODJBRUY1MjNFQ0ZCMzQyRkE4QkRFNkJBRDlGRkQ5Q0Q4MkI5MjJDMzU3QzRFQzcyMDNFNTc2RkQ5ODUzNzNBMTMwNEZDMTg2NTEyN0UwMEU2OEE3MjMxRkE3M0M2ODA5NzYwN0E0QTZGNThDNDhCMzI=")).getMatch(0);
        final String dllink = requestDownloadLink(js);
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
                result = engine.get(fortyTwo("RkM4QkZCRjZGQjAxQ0NFMDE4OTFCNTlGREMwNg=="));
            } else {
                result = inv.invokeFunction(fortyTwo("RkQ4RkZCRjZGQjA3Q0ZFMjFCQzJCNzlBREUwMDQwQUIyOTlENzFFQTA4MDk="), engine.get("srv"));
            }
        } catch (final Throwable e) {
            return null;
        }
        return result.toString();
    }

    private String requestDownloadLink(final String js) throws IOException {
        if (js == null) { return null; }
        final Browser jsBr = br.cloneBrowser();
        // js Funktion vorbereiten. Leider recht statisch.
        final StringBuilder sb = new StringBuilder();
        sb.append(br.getRegex(fortyTwo("Rjk4MEZBRjVGQTAyQ0NFNDFGOTRCNUNCREY1NjQyRkIyODk2NzFCQTA4NThEOUNBMTkxRTVGMzIxNTM4ODY4MTVEREVGODZCOEQ1RkU3NkUyNTFB")).getMatch(0));
        sb.append(br.getRegex(fortyTwo("Rjk4MEZBRjVGQTAyQ0NFNDFGOTRCNjk5REUwMzQzRkYyOUMxNzFFQzA4MEREOENGMUExRTVDMzgxNTY5ODc4MDU4OEZGQzMzODg1Q0UyM0IyMDEwNTVFOEVCMjAxRDc3RTEyMTI4Q0JDMUM3")).getMatch(0));
        jsBr.getPage(js);
        String dlUrl = jsBr.getRegex(fortyTwo("Rjk4MEZCRjVGQjA2Q0RCMzFCOTdCNzk5REU1QzQyQUUyOUMxNzVCRjA4NUNEOENCMUIxRTVEM0YxNTNBODc4MzU5REVGRTY5ODgwMEUyNjkyMTQxNTNFRUVDMjAxQzc1RTEyMjJFQ0RDMTlDNjU2MkFENzVENkVG")).getMatch(0);
        String servUrl = jsBr.getRegex(fortyTwo("Rjk4MEZBRjVGQTAyQ0NFNDFGOTRCNzlFREU1MDQzRkEyODkyNzJCQTBCNTlEQTlEMUU0RjVCNkExMDNBODNENjVEODI=")).getMatch(0);
        if (servUrl == null || dlUrl == null) { return null; }
        dlUrl = dlUrl.replaceAll(fortyTwo("RkVEQkZGRjdGOTA2Q0VCNTFGQzFCNjk4REUwMDQzRkUyOTlENzBCRDA4MEREODlCMUE0RTVGM0UxNTM5ODc4MDVBRDhGODZGODgwRkUyMzUyMTQwNTBCOUVDMjYxRDcz"), "");
        servUrl = servUrl.replaceAll(fortyTwo("RkVEQkZGRjdGOTA2Q0VCNTFGQzFCNjk4REUwMDQzRkUyOTlENzBCRDA4MEREODlCMUE0RTVGM0UxNTM5ODc4MDVBRDhGODZGODgwRkUyMzUyMTQwNTBCOUVDMjYxRDcz"), "");
        servUrl = servUrl.replaceAll(fortyTwo("RkVEQkZGRjdGQTA3Q0NFNQ=="), fortyTwo("RkQ4Q0ZCQTVGQjA0Q0RCMzFCQzdCNkNCREU1NDQyRkMyQTk3NzFCQTA5NUZEOUNBMUExMzVGNkUxNTZCODc4Nw=="));
        sb.append(dlUrl + ";");
        sb.append(jsBr.getRegex(fortyTwo("Rjk4MEZBRjVGQTAyQ0NFNDFGOTRCNzlFREU1NjQzRkEyOTkxNzFCQzA5NUZEQkNBMUExMzVGNkYxNTY5ODJEMTVEREFGOTZDOEQ1QkU2MzU=")).getMatch(0));
        sb.append(jsBr.getRegex(fortyTwo("Rjk4MEZBRjVGQTAyQ0NFNDFGOTRCNjlDREU1NjQyRkMyOUMyNzBCQzBDMEVEQzlGMUY0QzVBNjkxMTY1")).getMatch(0));
        sb.append(fortyTwo("RkM4RUZCRjJGQjAxQzlFNjFCOTJCNjk0REU1MTQ3QUMyOTkyNzFCNjA4MDhEOENCMTgxMzVGNkUxNTZBODZEMjVCOERGQzMzODg1QUUyMzkyMjEwNTBCNEVGMjMxODI0RTU3MzI5Q0FDNUM4NjY2NEFDMjBEMkIyNkYwOTMzNjg1QUYxQ0Q3MzVDN0NGQkM4MjZGMzJBRUM1NjZFQ0NCNzQzQTk4RTg0NkVBQjlDQUM5QURGMkZDNTJDNjc3QzQyQzMyMDNGNTE2QzgzODAzMzNBNDcwMEFCMUEzNjE2NzkwMkIyOEE3NTM0QUI3MjlDODBDNjY1MkM0QTZFNUNDMDhFNkU0OEI3QzkzMERBQ0UzMjFBMDhENDcyNTlDMUI0RDgzODAzNEI1N0M3QjJBOTFERDgzOUY1NkQ2QjcyOUYxQzcw"));
        sb.append(servUrl);
        final String bleistift = br.getRegex(fortyTwo("RkVEQkZGRjVGQjAwQ0RFMjFFQzBCMjk1REEwMDQ2QTkyQ0MyNzVCNjBDNUM=")).getMatch(0);

        // und ausführen ...
        final String result = jsExecute(sb.toString(), true);
        if (result == null) { return null; }
        br.getPage(result);

        // ohne diesen Keks kein dl
        br.setCookie(br.getHost(), fortyTwo("RkQ4QkZCQTVGOTU1Q0ZFNzFCQzdCNkNFREUwMzQzRkYyOTkxNzFCQjBBNUZEODk4MUIxRDVGNkU="), bleistift);

        // weitere Zutaten ...
        final String query = br.getRegex(fortyTwo("RkM4OUZBRjZGQTA2Q0NFNDFBOURCNTlFREY1MTQzRkEyOTlENzFFQTA4NUNERDlGMTk0OTVFMzgxMDNBODI4QzVEOENGODZGOEM1OEU3NkEyNTE0NTRCNUVCMjY=")).getMatch(0);
        String kugelschreiber = br.getRegex(fortyTwo("Rjk4RkZCRjdGQTU1Q0RCMjFCOTVCNjk0REUwMDQ2RkYyQ0M1NzJFQzA5NThERDk4MUUxRDVCMzMxNjNGODc4MzVERDlGODMzOEMwRUU2NkY=")).getMatch(0);
        final String[] ks = br.getRegex(fortyTwo("Rjk4RkZCRjdGQTU1Q0RCMjFCOTVCNjk0REUwMDQ2RkYyQUM3NzBCQzBEMERERDlGMTk0OTVFMzgxMDNBODI4MzVEODNGRjY5ODkwRUU2NkUyNTFBNTRCQkVCMjY=")).getColumn(0);
        kugelschreiber = ks[ks.length - 1];
        if (kugelschreiber == null) { return null; }
        sb.append("var queryString=" + query + ";");
        sb.append(fortyTwo("RkM4RUZCRjJGQjAxQzlFNjFBOTdCNzlGREY1MzQ2RjgyQ0MwNzVCOA==") + kugelschreiber + "\';");
        sb.append(fortyTwo("RkQ4MUZCRjVGRTBCQ0NFNTFCOURCN0NDREU1MDQ3QUQyOTkwNzFFOTA5NUNEODlCMUE0OTVGNkQxNTZEODY4MDVBODhGQzNGODkwREUzMzgyMTFBNTBFOUVGNzIxODI1RTAyMjJDOURDNUM2NjczNEFDNzREMkUyNkYwOTM3M0U1RUEyQ0Q3QzVDMjZGQjlFMjRGRjJCQkE1NjYxQ0RCMDQxQUE4QURDNkJGOTk5QTk5RDg4MkFDMDI4M0E3RDE1QzY3MDNGNTA2RDhDODQzNjNCNDEwNEZEMUEzMDEyMkEwN0IyOEYyMzM0QUM3MDlDODA5NDYwMkE0QjM0NUQ5MjhCMzU="));

        // finalen Link bauen
        return jsExecute(sb.toString(), false);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getURL().contains("1000eb.com/exception_") || br.containsHTML("你要下载的文件已经不存在</span>")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
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