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

import jd.OptionalPluginWrapper;
import jd.PluginWrapper;
import jd.captcha.specials.Linksave;
import jd.controlling.ProgressController;
import jd.gui.swing.components.Balloon;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.http.URLConnectionAdapter;
import jd.http.ext.BasicBrowserEnviroment;
import jd.http.ext.ExtBrowser;
import jd.http.ext.ExtBrowserException;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "Linksave.in" }, urls = { "http://[\\w\\.]*?linksave\\.in/(view.php\\?id=)?[\\w]+" }, flags = { 0 })
public class Lnksvn extends PluginForDecrypt {

    private static boolean isExternInterfaceActive() {
        final OptionalPluginWrapper plg = JDUtilities.getOptionalPlugin("externinterface");
        return plg != null && plg.isLoaded() && plg.isEnabled();
    }

    public Lnksvn(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        br.setRequestIntervalLimit(getHost(), 1000);
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        setBrowserExclusive();
        final String parameter = param.toString();
        br.forceDebug(true);

        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setCookie("http://linksave.in/", "Linksave_Language", "german");
        br.setRequestIntervalLimit("linksave.in", 1000);
        br.getPage(param.getCryptedUrl());
        if (br.containsHTML("Ordner nicht gefunden")) {
            logger.info("Error 404 - Ordner: \"" + parameter + "\" nicht gefunden!");
            return decryptedLinks;
        }
        getCaptcha(param, "");
        // CNL
        /* old CNL handling found in revision 13753 */
        if (br.getRegex("cnl\\.jpg").matches() && isExternInterfaceActive()) {
            final Form cnlform = br.getForm(0);
            /* 0.95xx comp */
            final String jkvalue = cnlform.getRegex("<INPUT TYPE=\"hidden\" NAME=\"jk\" VALUE=\"(.*?)\"").getMatch(0);
            cnlform.put("jk", Encoding.formEncoding(jkvalue));

            if (jkvalue != null) {
                final Browser cnlbr = br.cloneBrowser();
                cnlbr.setConnectTimeout(5000);
                cnlbr.getHeaders().put("jd.randomNumber", System.getProperty("jd.randomNumber"));
                try {
                    cnlbr.submitForm(cnlform);
                    if (cnlbr.containsHTML("success")) { return decryptedLinks; }
                } catch (final Throwable e) {
                }
            }
        }
        // Container handling (DLC)
        String[] container = br.getRegex("\\.href\\=unescape\\(\\'(.*?)\\'\\)\\;").getColumn(0);
        if (container != null && container.length > 0) {
            for (final String c : container) {
                final Browser clone = br.cloneBrowser();
                final String test = Encoding.htmlDecode(c);
                File file = null;
                if (test.endsWith(".cnl")) {
                    final URLConnectionAdapter con = clone.openGetConnection("http://linksave.in/" + test.replace("dlc://linksave.in/", ""));
                    if (con.getResponseCode() == 200) {
                        file = JDUtilities.getResourceFile("tmp/linksave/" + test.replace(".cnl", ".dlc").replace("dlc://", "http://").replace("http://linksave.in", ""));
                        clone.downloadConnection(file, con);
                    } else {
                        con.disconnect();
                    }
                } else if (test.endsWith(".rsdf")) {
                    final URLConnectionAdapter con = clone.openGetConnection(test);
                    if (con.getResponseCode() == 200) {
                        file = JDUtilities.getResourceFile("tmp/linksave/" + test.replace("http://linksave.in", ""));
                        clone.downloadConnection(file, con);
                    } else {
                        con.disconnect();
                    }
                } else if (test.endsWith(".dlc")) {
                    final URLConnectionAdapter con = clone.openGetConnection(test);
                    if (con.getResponseCode() == 200) {
                        file = JDUtilities.getResourceFile("tmp/linksave/" + test.replace("http://linksave.in", ""));
                        file.deleteOnExit();
                        clone.downloadConnection(file, con);
                    } else {
                        con.disconnect();
                    }
                }
                if (file != null && file.exists() && file.length() > 100) {
                    try {
                        decryptedLinks = JDUtilities.getController().getContainerLinks(file);
                    } catch (final Exception e) {
                    }
                }
            }
        }
        if (decryptedLinks != null && decryptedLinks.size() > 0) { return decryptedLinks; }
        // if containersearch did not work
        final ArrayList<String> allLinks = new ArrayList<String>();
        int pages = 1;
        final String tmpPages = br.getRegex("\"center\">Seiten(.*?)</a></td></tr>").getMatch(0);
        if (tmpPages != null) {
            pages = new Regex(tmpPages, "\\[(\\d+)\\]").count();
        }
        // erst alle verschlüsselten Links holen
        for (int i = 1; i <= pages; i++) {
            progress.setRange(pages);
            final String extras = "?s=" + i + "#down";
            if (i > 1) {
                br.getPage(param.getCryptedUrl() + extras);
            }
            // Captchaeingabe pro page erforderlich!
            getCaptcha(param, extras);
            String[] links = br.getRegex("<a href=\"(http://linksave[^\"]*)\" onclick=\"javascript:document.getElementById").getColumn(0);
            // Singlelinks
            if (links == null || links.length == 0) {
                if (br.getRegex("<frame scrolling=\"auto\" noresize src=\"([^\"]*)\">").getMatch(0) != null) {
                    links = new String[1];
                    links[0] = parameter;
                }
            }
            for (final String tmplinks : links) {
                allLinks.add(tmplinks);
            }
            if (container != null) {
                container = null;
            }
            progress.increase(1);
            Thread.sleep(500);
        }
        progress.decrease(progress.getMax());
        // alle verschlüsseleten Links in einem Rutsch entschlüsseln
        final class LsDirektLinkTH extends Thread {
            Browser browser;
            String result;

            public LsDirektLinkTH(final Browser browser) {
                this.browser = browser;
            }

            @Override
            public void run() {
                try {
                    result = getDirektLink(browser);
                } catch (final IOException e) {
                    e.printStackTrace();
                }
                synchronized (this) {
                    notifyAll();
                }
            }
        }
        final LsDirektLinkTH[] dlinks = new LsDirektLinkTH[allLinks.size()];
        progress.setRange(allLinks.size());
        for (int i = 0; i < dlinks.length; i++) {
            final Browser clone = br.cloneBrowser();
            clone.getPage(allLinks.get(i));
            dlinks[i] = new LsDirektLinkTH(clone);
            dlinks[i].start();
            progress.increase(1);
            logger.info("Link " + i + " von " + dlinks.length);
        }
        for (final LsDirektLinkTH lsDirektLinkTH : dlinks) {
            while (lsDirektLinkTH.isAlive()) {
                synchronized (lsDirektLinkTH) {
                    try {
                        lsDirektLinkTH.wait(5000);
                    } catch (final InterruptedException e) {
                    }
                }
                progress.increase(1);
            }
            if (lsDirektLinkTH.result != null) {
                logger.info("Added: " + lsDirektLinkTH.result);
                decryptedLinks.add(createDownloadlink(lsDirektLinkTH.result));
            }
        }
        if (decryptedLinks.size() == 0) {
            if (br.getRegex("cnl\\.jpg").matches() && !isExternInterfaceActive()) {
                Balloon.show(JDL.L("jd.controlling.CNL2.checkText.title", "Click'n'Load"), null, JDL.L("gui.config.linkgrabber.cnl2", "Enable Click'n'Load Support"));
                return decryptedLinks;
            }
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    private void getCaptcha(final CryptedLink param, final String extras) throws Exception {
        Form form = br.getFormbyProperty("name", "form");
        for (int retry = 0; retry < 5; retry++) {
            if (form == null) {
                break;
            }
            if (form.containsHTML("besucherpasswort")) {
                final String pw = Plugin.getUserInput(null, param);
                form.put("besucherpasswort", pw);
            }
            br.submitForm(form);
            form = br.getFormbyProperty("name", "form");
            if (form == null) {
                break;
            }
            final String url = "captcha/cap.php?hsh=" + form.getRegex("\\/captcha\\/cap\\.php\\?hsh=([^\"]+)").getMatch(0);
            final File captchaFile = this.getLocalCaptchaFile();
            Browser.download(captchaFile, br.cloneBrowser().openGetConnection(url));
            Linksave.prepareCaptcha(captchaFile);
            final String captchaCode = this.getCaptchaCode(captchaFile, param);
            form.put("code", captchaCode);
            br.submitForm(form);
            if (br.containsHTML("Falscher Code") || br.containsHTML("Captcha-code ist falsch") || br.containsHTML("Besucherpasswort ist falsch")) {
                br.getPage(param.getCryptedUrl() + extras);
                form = br.getFormbyProperty("name", "form");
            } else {
                break;
            }
        }
    }

    private String getDirektLink(final Browser br) throws IOException {
        final String link = br.getRegex("<frame scrolling=\"auto\" noresize src=\"([^\"]*)\">").getMatch(0);
        final String url = br.getURL().toString();
        if (link != null) {
            br.getPage(link);
        }
        String link2 = Encoding.htmlDecode(br.getRegex("iframe src=\"([^\"]*)\"").getMatch(0));
        if (link2 != null) { return link2.trim(); }
        br.getRequest().setHtmlCode(br.toString().replaceFirst("<script type=\"text/javascript\" src=\"[^\"]*.js\">", ""));
        // Start Evaluation of br
        try {
            // this is a workaround to use ExtBrowser Insteadof old
            // JavaScript class.
            final ExtBrowser eb = new ExtBrowser();
            // settings: blacklist allows nothing. this means that only
            // whitelisted links will be loaded
            eb.setBrowserEnviroment(new BasicBrowserEnviroment(new String[] { ".*" }, new String[] { ".*linksave.in.*" }) {
                @Override
                public boolean isAutoProcessSubFrames() {
                    return false;
                }
            });
            eb.eval(br);
            link2 = eb.getRegex("location.replace\\('([^\']*)").getMatch(0);
            if (link2 == null) {
                link2 = eb.getRegex("src=\"([^\"]*)\"").getMatch(0);
            }
            if (link2 == null) {
                link2 = eb.getRegex("URL=([^\"]*)\"").getMatch(0);
            }
            eb.getCommContext().setFollowRedirects(false);
            eb.getPage(link2);
            eb.getCommContext().setFollowRedirects(true);
            link2 = Encoding.htmlDecode(eb.getRegex("iframe .*?src=\"([^\"]*)\"").getMatch(0));
            if (link2 == null && br.getRedirectLocation() != null) {
                link2 = eb.getCommContext().getRedirectLocation();
            }
            if (link2 == null) {
                link2 = eb.getCommContext().getHttpConnection().getHeaderField("Location");
            }
            if (link2 == null && eb.getCommContext().getHttpConnection().getContentType().contains("html")) {
                if (eb.getCommContext().containsHTML("404 - Not Found")) {
                    logger.info("404 - File: \"" + url + "\" not found!");
                    return null;
                }
            }
            return link2.trim();
            // TODO: old code is below... did not find an example about that
            // if (link2 == null) {
            // js = new JavaScript(br);
            // js.runPage();
            // br.getRequest().setHtmlCode(js.getDocment().getContent());
            // link2 = br.getForm(0).getAction();
            // }
            // if (link2 != null) return link2.trim();
            // } catch (SAXException e) {
            // e.printStackTrace();
            // } catch (IOException e) {
            // e.printStackTrace();
            // }
        } catch (final ExtBrowserException e) {
            e.printStackTrace();
        }
        return null;
    }
}