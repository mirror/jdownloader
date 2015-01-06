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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hostuje.net" }, urls = { "http://[\\w\\.]*?hostuje\\.net/file\\.php\\?id=[a-zA-Z0-9]+" }, flags = { 0 })
public class HostUjeNet extends PluginForHost {

    public HostUjeNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://hostuje.net/regulamin.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws PluginException, IOException {
        br = new Browser();
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.95 Safari/537.36");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML(">Podany plik nie został odnaleziony|>Podany plik został skasowany z powodu naruszania praw autorskich")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = br.getRegex("name=\"name\" value=\"([^<>\"]*?)\"").getMatch(0);
        final String filesize = br.getRegex("<b>Rozmiar:</b>([^<>\"]*?)<br>").getMatch(0);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setName(Encoding.htmlDecode(filename).trim());
        downloadLink.setDownloadSize(SizeFormatter.getSize(Encoding.htmlDecode(filesize)));
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        // Browser o = br.cloneBrowser();
        // o.getHeaders().put("Accept", "*/*");
        // o.cloneBrowser().getPage("/show_ads.js");
        // o.cloneBrowser().getPage("/swfobject2.js?sr");
        // o.cloneBrowser().getPage("/style.css?10");
        // o.cloneBrowser().getPage("/dll_kody.js.php?new7a&flash=0");
        // o.cloneBrowser().getPage("/swfobject_34a.js?srmmaaserr");
        // o.cloneBrowser().getPage("/obraz.php");
        final String[] imgs = br.getRegex("(\"|')([^\"'\r\n\t]+\\.(?:png|jpe?g|gif))\\1").getColumn(1);
        // HashSet<String> dupe = new HashSet<String>();
        if (imgs != null) {
            // for (final String img : imgs) {
            // if (dupe.add(img)) {
            // this.simulateBrowser(br, img);
            // }
            // }

            /**
             * THIS IS REQUIRED <br>
             * captcha image is used to detect automation - raztoki 20150106
             **/
            this.simulateBrowser(br, "/oc123.php");
        }

        // do not use static posts!
        Form f = br.getForm(1);
        if (f == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.submitForm(f);
        String link = null;
        link = br.getRedirectLocation();
        if (link == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, link, false, 1);
        dl.startDownload();
    }

    private void simulateBrowser(final Browser br, final String url) {
        if (br == null || url == null) {
            return;
        }
        Browser rb = br.cloneBrowser();
        rb.getHeaders().put("Accept", "image/webp,*/*;q=0.8");
        URLConnectionAdapter con = null;
        try {
            con = rb.openGetConnection(url);
        } catch (final Throwable e) {
        } finally {
            try {
                con.disconnect();
            } catch (final Exception e) {
            }
        }
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }
}