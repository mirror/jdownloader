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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicReference;

import jd.PluginWrapper;
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
import jd.plugins.components.UserAgents;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hostuje.net" }, urls = { "http://[\\w\\.]*?hostuje\\.net/file\\.php\\?id=[a-zA-Z0-9]+" }) 
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

    private static AtomicReference<String> userAgent = new AtomicReference<String>();

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws PluginException, IOException {
        br = new Browser();
        this.setBrowserExclusive();
        if (userAgent.get() == null) {
            userAgent.set(UserAgents.stringUserAgent());
        }
        br.getHeaders().put("User-Agent", userAgent.get());
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML(">Podany plik nie został odnaleziony|>Podany plik został skasowany z powodu naruszania praw autorskich")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = br.getRegex("name=\"name\" value=\"([^<>\"]*?)\"").getMatch(0);
        final String filesize = br.getRegex("<b>\\s*Rozmiar\\s*:\\s*</b>\\s*([^<>\"]*?)\\s*<br>").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setName(Encoding.htmlDecode(filename).trim());
        if (filesize != null) {
            downloadLink.setDownloadSize(SizeFormatter.getSize(Encoding.htmlDecode(filesize)));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        final String cryptedScripts[] = br.getRegex("p\\}\\((.*?)\\.split\\('\\|'\\)").getColumn(0);
        String decoded = "";
        if (cryptedScripts != null && cryptedScripts.length != 0) {
            for (String crypted : cryptedScripts) {
                try {
                    Regex params = new Regex(crypted, "'(.*?[^\\\\])',(\\d+),(\\d+),'(.*?)'");

                    String p = params.getMatch(0).replaceAll("\\\\", "");
                    int a = Integer.parseInt(params.getMatch(1));
                    int c = Integer.parseInt(params.getMatch(2));
                    String[] k = params.getMatch(3).split("\\|");

                    while (c != 0) {
                        c--;
                        if (k[c].length() != 0) {
                            p = p.replaceAll("\\b" + Integer.toString(c, a) + "\\b", k[c]);
                        }
                    }

                    decoded += "\r\n" + p;
                } catch (Exception e) {
                }
            }
        }
        Browser o = br.cloneBrowser();
        o.getHeaders().put("Accept", "*/*");
        // this is obstructed in packaged
        // o.cloneBrowser().getPage("/swfobject_34a.js?srmmaaserr");
        // fix decoded and get swfojbect
        if (decoded != null && !"".equals(decoded)) {
            decoded = decoded.replaceAll("\\s*('|\")\\s*\\+\\s*\\1", "");
            final String swfobject = new Regex(decoded, "swfobject[^\"]+").getMatch(-1);
            if (swfobject != null) {
                Browser a = o.cloneBrowser();
                a.getPage(swfobject);
                // now we want that capture image
                String[] captchas = a.getRegex("preload\\(('|\")(\\w+\\.php)\\1\\);").getColumn(1);
                if (captchas != null) {
                    for (final String captcha : captchas) {
                        /**
                         * THIS IS REQUIRED <br>
                         * captcha image is used to detect automation - raztoki 20150106
                         **/
                        this.simulateBrowser(br, "/" + captcha);
                    }
                }
            }
        } else {
            // hes been a dick
            final String[] s1 = br.getRegex("<script[^>]+>").getColumn(-1);
            final ArrayList<String> s0 = new ArrayList<String>();
            if (s1 != null) {
                for (String s : s1) {
                    if (s != null /* && new Regex(s, "type=('|\")text/javascript\\1").matches() */) {
                        final String s2 = new Regex(s, "src=(\"|')(.*?)\\1").getMatch(1);
                        if (s2 != null) {
                            s0.add(s2);
                        }
                    }
                }
            }
            final LinkedHashSet<String> dupe = new LinkedHashSet<String>();
            for (final String s : s0) {
                if (!dupe.add(s) || ((s.startsWith("://") || s.startsWith("http")) && !Browser.getHost(s).contains(this.getHost()))) {
                    continue;
                }
                Browser a = o.cloneBrowser();
                a.getPage(s);
                // now we want that capture image
                String[] captchas = a.getRegex("(?:preload\\()?('|\")(\\w+\\.php[^\"']*)\\1\\);").getColumn(1);
                if (captchas != null) {
                    for (final String captcha : captchas) {
                        /**
                         * THIS IS REQUIRED <br>
                         * captcha image is used to detect automation - raztoki 20150106
                         **/
                        this.simulateBrowser(br, "/" + captcha);
                    }
                }
            }
        }
        // do not use static posts!
        Form f = br.getForm(1);
        if (f == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.containsHTML("'sitekey'\\s*:\\s*'[\\w+]+'") && br.containsHTML("document\\.getElementById\\('pobierz'\\)\\.disabled=false")) {
            // recaptcha v2
            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
            f.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
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