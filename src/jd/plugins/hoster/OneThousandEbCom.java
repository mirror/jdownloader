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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "1000eb.com" }, urls = { "http://(www\\.)?1000eb\\.com/(?!myspace_|upload|bulletin_detail_\\d+|chance|copyrights|agreements|faq|contactus|aboutus|jounus|reportbadinformation)[a-z0-9]+" }, flags = { 0 })
public class OneThousandEbCom extends PluginForHost {

    public OneThousandEbCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String NOCHUNKS = "NOCHUNKS";

    private String fortyTwo(String s) {
        s = Encoding.Base64Decode(s);
        JDUtilities.getPluginForDecrypt("linkcrypt.ws");
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
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        JDUtilities.getPluginForDecrypt("linkcrypt.ws");
        requestFileInformation(downloadLink);
        // final String js =
        // br.getRegex("src=\"(http://static\\.1000eb\\.com/combo/[^<>]+/file\\.js\\&t=\\d+)\">").getMatch(0);
        final String dllink = requestDownloadLink("http://static.1000eb.com/www/js/file/base.js?2013042501");
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        int maxChunks = 0;
        if (downloadLink.getBooleanProperty(NOCHUNKS, false)) maxChunks = 1;

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            downloadLink.setProperty("ServerComaptibleForByteRangeRequest", true);
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) return;
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (downloadLink.getBooleanProperty(OneThousandEbCom.NOCHUNKS, false) == false) {
                    downloadLink.setProperty(OneThousandEbCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && downloadLink.getBooleanProperty(OneThousandEbCom.NOCHUNKS, false) == false) {
                downloadLink.setProperty(OneThousandEbCom.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
    }

    private String jsExecute(final String fun, final boolean b) {
        Object result = new Object();
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");
        final Invocable inv = (Invocable) engine;
        try {
            engine.eval(fun);
            if (b) {
                result = engine.get(fortyTwo(tb(1)));
            } else {
                result = inv.invokeFunction(fortyTwo(tb(2)), engine.get("srv"));
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
        sb.append(br.getRegex(fortyTwo(tb(3))).getMatch(0));
        sb.append(br.getRegex("(fileInfoFileName.*?;)").getMatch(0));
        sb.append(br.getRegex(fortyTwo(tb(4))).getMatch(0));
        jsBr.getPage(js);
        String dlUrl = jsBr.getRegex(fortyTwo(tb(5))).getMatch(0);
        String servUrl = jsBr.getRegex(fortyTwo(tb(6))).getMatch(0);
        if (servUrl == null || dlUrl == null) { return null; }
        dlUrl = dlUrl.replaceAll(fortyTwo(tb(7)), "");
        servUrl = servUrl.replaceAll(fortyTwo(tb(7)), "");
        servUrl = servUrl.replaceAll(fortyTwo(tb(8)), fortyTwo(tb(9)));
        sb.append(dlUrl + ";");
        sb.append(jsBr.getRegex(fortyTwo(tb(10))).getMatch(0));
        sb.append(jsBr.getRegex(fortyTwo(tb(11))).getMatch(0));
        sb.append(fortyTwo(tb(12)));
        sb.append(servUrl);

        // und ausführen ...
        final String result = jsExecute(sb.toString(), true);
        if (result == null) { return null; }
        br.getPage(result);

        // weitere Zutaten ...
        final String query = br.getRegex(fortyTwo(tb(15))).getMatch(0);
        String kugelschreiber = br.getRegex(fortyTwo(tb(16))).getMatch(0);
        final String[] ks = br.getRegex(fortyTwo(tb(17))).getColumn(0);
        kugelschreiber = ks[ks.length - 1];
        if (kugelschreiber == null) { return null; }
        sb.append("var queryString=" + query + ";");
        sb.append(fortyTwo(tb(18)) + kugelschreiber + "\';");
        sb.append(fortyTwo(tb(19)));

        // finalen Link bauen
        return jsExecute(sb.toString(), false);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    private String tb(final int i) {
        final String[] s = new String[20];
        s[0] = "RkM4QkZBRjFGQTAwQzhCMjFGOTZCMjk1REU1RDQzRkMyODkwNzBCRjBEMEFEQzk4MUU0QzVFMzgxNDY4ODY4NTU4OEZGQzMzODgwQUUxNkYyNTQ2NTVCREVBNzUxQzI2RTE3NzJEOUNDNUNDNjU2MkE4NzREMkU1NkY1QTM2Njg1RUExQ0Q3OTVDNzlGQjk5MjRGODJBRUY1MjNFQ0ZCMzQyRkE4QkRFNkJBRDlGRkQ5Q0Q4MkI5MjJDMzU3QzRFQzcyMDNFNTc2RkQ5ODUzNzNBMTMwNEZDMTg2NTEyN0UwMEU2OEE3MjMxRkE3M0M2ODA5NzYwN0E0QTZGNThDNDhCMzI=";
        s[1] = "RkM4QkZCRjZGQjAxQ0NFMDE4OTFCNTlGREMwNg==";
        s[2] = "RkQ4RkZCRjZGQjA3Q0ZFMjFCQzJCNzlBREUwMDQwQUIyOTlENzFFQTA4MDk=";
        s[3] = "Rjk4MEZBRjVGQTAyQ0NFNDFGOTRCNUNCREY1NjQyRkIyODk2NzFCQTA4NThEOUNBMTkxRTVGMzIxNTM4ODY4MTVEREVGODZCOEQ1RkU3NkUyNTFB";
        s[4] = "Rjk4MEZBRjVGQTAyQ0NFNDFGOTRCNjk5REUwMzQzRkYyOUMxNzFFQzA4MEREOENGMUExRTVDMzgxNTY5ODc4MDU4OEZGQzMzODg1Q0UyM0IyMDEwNTVFOEVCMjAxRDc3RTEyMTI4Q0JDMUM3";
        s[5] = "Rjk4MEZCRjVGQjA2Q0RCMzFCOTdCNzk5REU1QzQyQUUyOUMxNzVCRjA4NUNEOENCMUIxRTVEM0YxNTNBODc4MzU5REVGRTY5ODgwMEUyNjkyMTQxNTNFRUVDMjAxQzc1RTEyMjJFQ0RDMTlDNjU2MkFENzVENkVG";
        s[6] = "Rjk4MEZBRjVGQTAyQ0NFNDFGOTRCNzlFREU1MDQzRkEyODkyNzJCQTBCNTlEQTlEMUU0RjVCNkExMDNBODNENjVEODI=";
        s[7] = "RkVEQkZGRjdGOTA2Q0VCNTFGQzFCNjk4REUwMDQzRkUyOTlENzBCRDA4MEREODlCMUE0RTVGM0UxNTM5ODc4MDVBRDhGODZGODgwRkUyMzUyMTQwNTBCOUVDMjYxRDcz";
        s[8] = "RkVEQkZGRjdGQTA3Q0NFNQ==";
        s[9] = "RkQ4Q0ZCQTVGQjA0Q0RCMzFCQzdCNkNCREU1NDQyRkMyQTk3NzFCQTA5NUZEOUNBMUExMzVGNkUxNTZCODc4Nw==";
        s[10] = "Rjk4MEZBRjVGQTAyQ0NFNDFGOTRCNzlFREU1NjQzRkEyOTkxNzFCQzA5NUZEQkNBMUExMzVGNkYxNTY5ODJEMTVEREFGOTZDOEQ1QkU2MzU=";
        s[11] = "Rjk4MEZBRjVGQTAyQ0NFNDFGOTRCNjlDREU1NjQyRkMyOUMyNzBCQzBDMEVEQzlGMUY0QzVBNjkxMTY1";
        s[12] = "RkM4RUZCRjJGQjAxQzlFNjFCOTJCNjk0REU1MTQ3QUMyOTkyNzFCNjA4MDhEOENCMTgxMzVGNkUxNTZBODZEMjVCOERGQzMzODg1QUUyMzkyMjEwNTBCNEVGMjMxODI0RTU3MzI5Q0FDNUM4NjY2NEFDMjBEMkIyNkYwOTMzNjg1QUYxQ0Q3MzVDN0NGQkM4MjZGMzJBRUM1NjZFQ0NCNzQzQTk4RTg0NkVBQjlDQUM5QURGMkZDNTJDNjc3QzQyQzMyMDNGNTE2QzgzODAzMzNBNDcwMEFCMUEzNjE2NzkwMkIyOEE3NTM0QUI3MjlDODBDNjY1MkM0QTZFNUNDMDhFNkU0OEI3QzkzMERBQ0UzMjFBMDhENDcyNTlDMUI0RDgzODAzNEI1N0M3QjJBOTFERDgzOUY1NkQ2QjcyOUYxQzcw";
        s[13] = "RkVEQkZGRjVGQjAwQ0RFMjFFQzBCMjk1REEwMDQ2QTkyQ0MyNzVCNjBDNUM=";
        s[14] = "RkQ4QkZCQTVGOTU1Q0ZFNzFCQzdCNkNFREUwMzQzRkYyOTkxNzFCQjBBNUZEODk4MUIxRDVGNkU=";
        s[15] = "RkM4OUZBRjZGQTA2Q0NFNDFBOURCNTlFREY1MTQzRkEyOTlENzFFQTA4NUNERDlGMTk0OTVFMzgxMDNBODI4QzVEOENGODZGOEM1OEU3NkEyNTE0NTRCNUVCMjY=";
        s[16] = "Rjk4RkZCRjdGQTU1Q0RCMjFCOTVCNjk0REUwMDQ2RkYyQ0M1NzJFQzA5NThERDk4MUUxRDVCMzMxNjNGODc4MzVERDlGODMzOEMwRUU2NkY=";
        s[17] = "Rjk4RkZCRjdGQTU1Q0RCMjFCOTVCNjk0REUwMDQ2RkYyQUM3NzBCQzBEMERERDlGMTk0OTVFMzgxMDNBODI4MzVEODNGRjY5ODkwRUU2NkUyNTFBNTRCQkVCMjY=";
        s[18] = "RkM4RUZCRjJGQjAxQzlFNjFBOTdCNzlGREY1MzQ2RjgyQ0MwNzVCOA==";
        s[19] = "ZmQ4MWZiZjVmZTBiY2NlNTFiOWRiN2NjZGU1MDQ3YWQyOTkwNzFlOTA5NWNkODliMWE0OTVmNmQxNTZkODY4MDVhODhmYzNmODkwZGUzMzgyMTFhNTBlOWVmNzIxODI1ZTAyMjJjOWRjNWM2NjczNGFjNzRkMmUyNmYwOTM3M2U1ZWEyY2Q3YzVjMjZmYjllMjRmZjI5YmE1NjYxY2RiMDQxYWE4YWRjNmJmOTk5YTk5ZDg4MmFjMDI4M2E3ZDE1YzY3MDNmNTA2ZDhjODQzNjNiNDEwNGZkMWEzMDEyMmEwN2IyOGYyMzM0YWM3MDljODA5NDYwMmE0YjM0NWQ5MjhiMzU=";
        return s[i];
    }

}