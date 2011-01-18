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

package jd.plugins.decrypter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.OptionalPluginWrapper;
import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginUtils;
import jd.utils.JDUtilities;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "relink.us", "relink.us" }, urls = { "http://[\\w\\.]*?relink\\.us/(go\\.php\\?id=[\\w]+|f/[\\w]+)", "http://[\\w\\.]*?relink\\.us/view\\.php\\?id=\\w+" }, flags = { PluginWrapper.CNL_2, PluginWrapper.CNL_2 })
public class Rlnks extends PluginForDecrypt {

    private static boolean isExternInterfaceActive() {
        final OptionalPluginWrapper plg = JDUtilities.getOptionalPlugin("externinterface");
        return ((plg != null) && plg.isLoaded() && plg.isEnabled());
    }

    ProgressController progress;

    public Rlnks(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private boolean decryptContainer(final String page, final String cryptedLink, final String containerFormat, final ArrayList<DownloadLink> decryptedLinks) throws IOException {
        final String containerURL = new Regex(page, "(download\\.php\\?id=[a-zA-z0-9]+\\&" + containerFormat + "=\\d+)").getMatch(0);
        if (containerURL != null) {
            final File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + "." + containerFormat);
            final Browser browser = this.br.cloneBrowser();
            browser.getHeaders().put("Referer", cryptedLink);
            browser.getDownload(container, "http://relink.us/" + Encoding.htmlDecode(containerURL));
            decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));
            container.delete();
            return true;
        }
        return false;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        this.progress = progress;
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        this.br.setDebug(true);
        this.setBrowserExclusive();
        this.br.setFollowRedirects(true);
        this.br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        String page = this.br.getPage(parameter);
        boolean okay = true;
        for (int i = 0; i < 4; i++) {
            if (this.br.containsHTML("(das richtige Passwort)|(haben ein falsches Passwort)|(Das Passwort ist Falsch)|(container-protection)")) {
                okay = false;
                Form form = this.br.getForm(0);
                if (form == null) {
                    this.br.getPage(parameter);
                    form = this.br.getForm(0);
                }
                final String pw = PluginUtils.askPassword(this);
                if (pw == null) { throw new DecrypterException(DecrypterException.PASSWORD); }
                form.put("password", pw);
                page = this.br.submitForm(form);
            } else {
                okay = true;
                break;
            }
        }
        if (okay == false) { throw new DecrypterException(DecrypterException.CAPTCHA); }
        progress.setRange(0);
        /* use cnl2 button if available */
        if (this.br.containsHTML("cnl2.swf") && Rlnks.isExternInterfaceActive()) {
            final String flashVars = this.br.getRegex("flashVars\" value=\"(.*?)\"").getMatch(0);
            if (flashVars != null) {
                final Browser cnlbr = new Browser();
                cnlbr.setConnectTimeout(5000);
                cnlbr.getHeaders().put("jd.randomNumber", System.getProperty("jd.randomNumber"));
                try {
                    cnlbr.postPage("http://127.0.0.1:9666/flash/addcrypted2", flashVars);
                    if (cnlbr.containsHTML("success")) { return decryptedLinks; }
                } catch (final Throwable e) {
                }
            }
        }
        if (!this.decryptContainer(page, parameter, "dlc", decryptedLinks)) {
            if (!this.decryptContainer(page, parameter, "ccf", decryptedLinks)) {
                this.decryptContainer(page, parameter, "rsdf", decryptedLinks);
            }
        }
        if (decryptedLinks.isEmpty()) {
            this.decryptLinks(decryptedLinks);
            final String more_links[] = new Regex(page, Pattern.compile("<a href=\"(go\\.php\\?id=[a-zA-Z0-9]+\\&seite=\\d+)\">", Pattern.CASE_INSENSITIVE)).getColumn(0);
            for (final String link : more_links) {
                this.br.getPage("http://relink.us/" + link);
                this.decryptLinks(decryptedLinks);
            }
        }
        if (decryptedLinks.isEmpty() && this.br.containsHTML("swf/cnl2.swf")) { throw new DecrypterException("CNL2 only, open this link in Browser"); }
        return decryptedLinks;
    }

    private void decryptLinks(final ArrayList<DownloadLink> decryptedLinks) throws IOException {
        this.br.setFollowRedirects(false);
        final String[] matches = this.br.getRegex("getFile\\('(cid=\\w*?&lid=\\d*?)'\\)").getColumn(0);
        try {
            this.progress.addToMax(matches.length);
            for (final String match : matches) {
                try {
                    Browser brc = null;
                    brc = this.br.cloneBrowser();
                    // brc.setCookiesExclusive(true);
                    brc.getHeaders().put("User-Agent", RandomUserAgent.generate());
                    try {
                        Thread.sleep(2000);
                    } catch (final Exception e) {
                    }
                    brc.getPage("http://www.relink.us/frame.php?" + match);
                    if ((brc != null) && (brc.getRedirectLocation() != null) && brc.getRedirectLocation().contains("relink.us/getfile")) {
                        try {
                            Thread.sleep(150);
                        } catch (final Exception e) {
                        }
                        brc.getPage(brc.getRedirectLocation());
                    }
                    if (brc.getRedirectLocation() != null) {
                        decryptedLinks.add(this.createDownloadlink(Encoding.htmlDecode(brc.getRedirectLocation())));
                        break;
                    } else {
                        final String url = brc.getRegex("iframe.*?src=\"(.*?)\"").getMatch(0);
                        if (url != null) {
                            decryptedLinks.add(this.createDownloadlink(Encoding.htmlDecode(url)));
                        } else {
                            /* as bot detected */
                            return;
                        }
                    }
                } catch (final Exception e) {
                }
                this.progress.increase(1);
            }
        } finally {
            this.br.setFollowRedirects(true);
        }
    }
}