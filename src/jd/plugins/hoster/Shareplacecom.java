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
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.UserAgents;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "shareplace.com" }, urls = { "http://[\\w\\.]*?shareplace\\.(com|org)/\\?(?:d=)?[\\w]+(/.*?)?" }, flags = { 0 })
public class Shareplacecom extends PluginForHost {

    public Shareplacecom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("\\.org", ".com"));
        link.setUrlDownload(link.getDownloadURL().replaceFirst("Download", ""));
    }

    @Override
    public String getAGBLink() {
        return "http://shareplace.com/rules.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 10;
    }

    private Browser prepBR(final Browser br) {
        br.getHeaders().put("User-Agent", UserAgents.stringUserAgent());
        br.setCustomCharset("UTF-8");
        br.setFollowRedirects(true);
        return br;
    }

    private String correctedBR = null;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        String url = downloadLink.getDownloadURL();
        setBrowserExclusive();
        prepBR(this.br);
        getPage(url);
        final String fid = new Regex(downloadLink.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
        if (br.getRedirectLocation() == null) {
            final String iframe = br.getRegex("<frame name=\"main\" src=\"(.*?)\">").getMatch(0);
            if (iframe != null) {
                br.getPage(iframe);
            }
            if (br.containsHTML("Your requested file is not found") || !br.containsHTML("Filename:<")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String filename = new Regex(correctedBR, "Filename:</font></b>(.*?)<b><br>").getMatch(0);
            String filesize = br.getRegex("Filesize.*?b>(.*?)<b>").getMatch(0);
            if (inValidate(filename)) {
                filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
            }
            if (filesize == null) {
                filesize = br.getRegex("File.*?size.*?:.*?</b>(.*?)<b><br>").getMatch(0);
            }
            if (filesize == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (!inValidate(filename)) {
                /* Let's check if we can trust the results ... */
                filename = Encoding.htmlDecode(filename.trim());
                downloadLink.setFinalFileName(filename);
            } else {
                downloadLink.setName(fid);
            }
            downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.trim()));
            return AvailableStatus.TRUE;
        } else {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    private boolean inValidate(final String s) {
        if (s == null || s.matches("\\s+") || s.equals("") || s.toLowerCase().contains("jdownloader")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    private void doFree(final DownloadLink downloadLink) throws Exception {
        String dllink = null;
        for (final String[] s : br.getRegex("<script language=\"Javascript\">(.*?)</script>").getMatches()) {
            if (!new Regex(s[0], "(vvvvvvvvv|teletubbies|zzipitime)").matches()) {
                continue;
            }
            dllink = rhino(s[0]);
            if (dllink != null) {
                break;
            }
        }
        if (dllink == null) {
            if (br.containsHTML("<span>You have got max allowed download sessions from the same IP\\!</span>")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Limit reached or IP already loading", 60 * 60 * 1001l);
            }
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
        final String cleanup = new Regex(s, "(var.*?)var zzipitime").getMatch(0);
        final String[] vars = new Regex(s, "<a href=\"[a-z0-9 \\+]*'\\s*\\+\\s*(.*?)\\s*\\+\\s*'\"").getColumn(0);
        if (vars != null) {
            final ArrayList<String> vrrs = new ArrayList<String>(Arrays.asList(vars));
            // Collections.reverse(vrrs);
            for (final String var : vrrs) {
                String result = null;
                final ScriptEngineManager manager = jd.plugins.hoster.DummyScriptEnginePlugin.getScriptEngineManager(this);
                final ScriptEngine engine = manager.getEngineByName("javascript");
                try {
                    engine.eval(cleanup);
                    result = (String) engine.get(var);
                } catch (final Throwable e) {
                    continue;
                }
                // crap here
                final String crap = new Regex(s, "<a href=\"([a-z]+)'\\s*\\+\\s*" + var).getMatch(0);
                if (crap != null) {
                    result = crap + result;
                }
                try {
                    new URL(result);
                } catch (Exception e) {
                    continue;
                }
                if (result == null || (result.contains("jdownloader") && !result.startsWith("http"))) {
                    continue;
                }
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = null;
                try {
                    con = br2.openHeadConnection(result);
                    if (con.getContentType().contains("html")) {
                        continue;
                    }
                } catch (final Exception e) {
                    // e.printStackTrace();
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable t) {
                    }
                }
                return result;
            }
        }
        return null;
    }

    private void getPage(final String url) throws IOException, NumberFormatException, PluginException {
        br.getPage(url);
        correctBR();
    }

    /* Removes HTML code which could break the plugin */
    private void correctBR() throws NumberFormatException, PluginException {
        correctedBR = br.toString();
        ArrayList<String> regexStuff = new ArrayList<String>();

        // remove custom rules first!!! As html can change because of generic cleanup rules.

        /* generic cleanup */
        regexStuff.add("<\\!(\\-\\-.*?\\-\\-)>");
        regexStuff.add("(display: ?none;\">.*?</div>)");
        regexStuff.add("(visibility:hidden>.*?<)");

        for (String aRegex : regexStuff) {
            String results[] = new Regex(correctedBR, aRegex).getColumn(0);
            if (results != null) {
                for (String result : results) {
                    correctedBR = correctedBR.replace(result, "");
                }
            }
        }
    }

}