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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "shareplace.com" }, urls = { "http://[\\w\\.]*?shareplace\\.(com|org)/\\?[\\w]+(/.*?)?" }, flags = { 0 })
public class Shareplacecom extends PluginForHost {

    public Shareplacecom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("\\.org", ".com"));
    }

    @Override
    public String getAGBLink() {
        return "http://shareplace.com/rules.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        String url = downloadLink.getDownloadURL();
        setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setCustomCharset("UTF-8");
        br.setFollowRedirects(true);
        br.getPage(url);
        if (br.getRedirectLocation() == null) {
            final String iframe = url = br.getRegex("<frame name=\"main\" src=\"(.*?)\">").getMatch(0);
            br.getPage(iframe);
            if (br.containsHTML("Your requested file is not found")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            final String filename = br.getRegex("Filename:</font></b>(.*?)<b><br>").getMatch(0);
            String filesize = br.getRegex("Filesize.*?b>(.*?)<b>").getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex("File.*?size.*?:.*?</b>(.*?)<b><br>").getMatch(0);
            }
            if (filename == null || filesize == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            downloadLink.setFinalFileName(filename.trim());
            downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.trim()));
            return AvailableStatus.TRUE;
        } else {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String dllink = null;
        for (final String[] s : br.getRegex("<script language=\"Javascript\">(.*?)</script>").getMatches()) {
            if (!new Regex(s[0], "(vvvvvvvvv|teletubbies|zzipitime)").matches()) {
                continue;
            }
            dllink = rhino(new Regex(s[0], "(var.*?)var zzipitime").getMatch(0));
        }
        if (dllink == null) {
            if (br.containsHTML("<span>You have got max allowed download sessions from the same IP\\!</span>")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Limit reached or IP already loading", 60 * 60 * 1001l); }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("dllink doesn't seem to be a file...");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 20 * 60 * 1000l);
        }
        /* Workaround für fehlerhaften Filename Header */
        final String name = Plugin.getFileNameFromHeader(dl.getConnection());
        if (name != null) {
            downloadLink.setFinalFileName(Encoding.deepHtmlDecode(name));
        }
        dl.startDownload();
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
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

    private String rhino(final String s) {
        Object result = new Object();
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");
        try {
            engine.eval(s);
            result = engine.get("coffee");
        } catch (final Throwable e) {
            return null;
        }
        return result != null ? result.toString() : null;
    }

}