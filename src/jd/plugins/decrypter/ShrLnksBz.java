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

package jd.plugins.decrypter;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.OptionalPluginWrapper;
import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.controlling.LinkGrabberController;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.gui.swing.components.Balloon;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.nutils.nativeintegration.LocalBrowser;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "share-links.biz" }, urls = { "http://[\\w\\.]*?(share-links\\.biz/_[0-9a-z]+|s2l\\.biz/[a-z0-9]+)" }, flags = { 0 })
public class ShrLnksBz extends PluginForDecrypt {

    private static String                   host                   = "http://share-links.biz";
    private static long                     LATEST_OPENED_CNL_TIME = 0;
    private static HashMap<String, Boolean> CNL_URL_MAP            = new HashMap<String, Boolean>();

    private static boolean isExternInterfaceActive() {
        final OptionalPluginWrapper plg = JDUtilities.getOptionalPlugin("externinterface");
        return ((plg != null) && plg.isLoaded() && plg.isEnabled());
    }

    public ShrLnksBz(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {

        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();

        // 1. <form action="http://share-links.biz/api/container" method="post">
        // 2. <input type="submit">
        // 3. <input type="hidden" name="folderCode" value="aplme2l4u2u1">
        // 4. </form>

        String folderID = new Regex(parameter, "/\\_([0-9a-z]+)").getMatch(0);
        if (folderID == null) {
            folderID = new Regex(parameter, "s2l\\.biz/([0-9a-z]+)").getMatch(0);
        }
        if ((folderID != null) && ShrLnksBz.isExternInterfaceActive() && SubConfiguration.getConfig(LinkGrabberController.CONFIG).getBooleanProperty(LinkGrabberController.PARAM_USE_CNL2, true)) {
            final Browser cnlcheck = this.br.cloneBrowser();
            final LinkedHashMap<String, String> p = new LinkedHashMap<String, String>();
            p.put("folderCode", folderID);
            cnlcheck.postPage("http://share-links.biz/api/container", p);
            // CNL Dummy
            if (cnlcheck.toString().trim().contains("cnl") && ((System.currentTimeMillis() - ShrLnksBz.LATEST_OPENED_CNL_TIME) > 60 * 1000) && !ShrLnksBz.CNL_URL_MAP.containsKey(parameter)) {

                ShrLnksBz.LATEST_OPENED_CNL_TIME = System.currentTimeMillis();

                LocalBrowser.openDefaultURL(new URL(parameter + "?jd=1"));
                ShrLnksBz.CNL_URL_MAP.put(parameter, Boolean.TRUE);
                Balloon.show(JDL.L("jd.controlling.CNL2.checkText.title", "Click'n'Load"), null, JDL.L("jd.controlling.CNL2.checkText.message", "Click'n'Load URL opened"));
                return decryptedLinks;
            }
        }
        this.setBrowserExclusive();
        // this.br.setDebug(true);
        // Setup static Header
        this.br.setFollowRedirects(false);
        this.br.getHeaders().clear();
        this.br.getHeaders().put("Cache-Control", null);
        this.br.getHeaders().put("Pragma", null);
        this.br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        this.br.getHeaders().put("Accept", "*/*");
        this.br.getHeaders().put("Accept-Language", "en-us");
        this.br.getHeaders().put("Accept-Encoding", "gzip,deflate");
        this.br.getHeaders().put("Accept-Charset", "utf-8,*");
        this.br.getHeaders().put("Keep-Alive", "115");
        this.br.getHeaders().put("Connection", "keep-alive");

        this.br.getPage(parameter);
        // Very important!
        final String gif[] = this.br.getRegex("/template/images/(.*?)\\.gif").getColumn(-1);
        for (final String template : gif) {
            this.br.cloneBrowser().openGetConnection(template);
        }
        // Error handling
        if (this.br.containsHTML("Der Inhalt konnte nicht gefunden werden")) { throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore.")); }
        // Folderpassword
        if (this.br.containsHTML("id=\"folderpass\"")) {
            for (int i = 0; i <= 3; i++) {
                String latestPassword = this.getPluginConfig().getStringProperty("PASSWORD", null);
                final Form pwform = this.br.getForm(0);
                if (pwform == null) { return null; }
                pwform.setAction(parameter);
                // First try the stored password, if that doesn't work, ask the
                // user to enter it
                if (latestPassword == null) {
                    latestPassword = Plugin.getUserInput(null, param);
                }
                pwform.put("password", latestPassword);
                this.br.submitForm(pwform);
                if (this.br.containsHTML("Das eingegebene Passwort ist falsch")) {
                    this.getPluginConfig().setProperty("PASSWORD", null);
                    this.getPluginConfig().save();
                    continue;
                } else {
                    // Save actual password if it is valid
                    this.getPluginConfig().setProperty("PASSWORD", latestPassword);
                    this.getPluginConfig().save();
                }
                break;
            }
            if (this.br.containsHTML("Das eingegebene Passwort ist falsch")) {
                this.getPluginConfig().setProperty("PASSWORD", null);
                this.getPluginConfig().save();
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
        }
        // Captcha
        if (this.br.containsHTML("(/captcha/|captcha_container|\"Captcha\"|id=\"captcha\")")) {
            // Captcha Recognition broken - auto = false
            boolean auto = false;
            final int max = 5;
            for (int i = 0; i <= max; i++) {
                String Captchamap = this.br.getRegex("\"(/captcha\\.gif\\?d=\\d+.*?PHPSESSID=.*?)\"").getMatch(0);
                if (Captchamap == null) { return null; }
                Captchamap = Captchamap.replaceAll("(\\&amp;|legend=1)", "");
                final File file = this.getLocalCaptchaFile();
                final Browser temp = this.br.cloneBrowser();
                temp.getDownload(file, "http://share-links.biz" + Captchamap + "&legend=1");
                temp.getDownload(file, "http://share-links.biz" + Captchamap);
                String nexturl = null;
                if ((Integer.parseInt(JDUtilities.getRevision().replace(".", "")) < 10000) || !auto) {
                    final Point p = UserIO.getInstance().requestClickPositionDialog(file, "Share-links.biz", JDL.L("plugins.decrypt.shrlnksbz.desc", "Read the combination in the background and click the corresponding combination in the overview!"));
                    if (p == null) { throw new DecrypterException(DecrypterException.CAPTCHA); }
                    nexturl = this.getNextUrl(p.x, p.y);
                } else {
                    try {
                        final String[] code = this.getCaptchaCode(file, param).split(":");
                        nexturl = this.getNextUrl(Integer.parseInt(code[0]), Integer.parseInt(code[1]));
                    } catch (final Exception e) {
                        final Point p = UserIO.getInstance().requestClickPositionDialog(file, "Share-links.biz", JDL.L("plugins.decrypt.shrlnksbz.desc", "Read the combination in the background and click the corresponding combination in the overview!"));
                        if (p == null) { throw new DecrypterException(DecrypterException.CAPTCHA); }
                        nexturl = this.getNextUrl(p.x, p.y);
                    }
                }
                if (nexturl == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
                this.br.setFollowRedirects(true);
                this.br.getPage("http://share-links.biz/" + nexturl);
                if (this.br.containsHTML("Die getroffene Auswahl war falsch")) {
                    this.br.getPage(parameter);
                    if ((i == max) && auto) {
                        i = 0;
                        auto = false;
                    }
                    continue;
                }
                break;
            }
            if (this.br.containsHTML("Die getroffene Auswahl war falsch")) { throw new DecrypterException(DecrypterException.CAPTCHA); }
        }
        /*
         * Load Contents. Container handling (DLC)
         */
        final String dlclink = "http://share-links.biz/get/dlc/" + this.br.getRegex("get as dlc container\".*?\"javascript:_get\\('(.*?)', 0, 'dlc'\\);\"").getMatch(0);
        if (dlclink != null) {
            decryptedLinks = this.loadcontainer(this.br, dlclink);
            if ((decryptedLinks != null) && (decryptedLinks.size() > 0)) { return decryptedLinks; }
        }
        // File package handling
        int pages = 1;
        final String pattern = parameter.substring(parameter.lastIndexOf("/"), parameter.length());
        if (this.br.containsHTML("folderNav")) {
            pages = pages + this.br.getRegex(pattern + "\\?n=[0-9]++\"").getMatches().length;
        }
        final LinkedList<String> links = new LinkedList<String>();
        for (int i = 1; i <= pages; i++) {
            this.br.getPage(ShrLnksBz.host + pattern);
            final String[] linki = this.br.getRegex("decrypt\\.gif\" onclick=\"javascript:_get\\('(.*?)'").getColumn(0);
            if (linki.length == 0) { return null; }
            links.addAll(Arrays.asList(linki));
        }
        if (links.size() == 0) { return null; }
        progress.setRange(links.size());
        for (final String tmplink : links) {
            while (true) {
                final String link = "http://share-links.biz/get/lnk/" + tmplink;
                this.br.getPage(link);
                String clink0 = this.br.getRegex("unescape\\(\"(.*?)\"").getMatch(0);
                if (clink0 != null) {
                    clink0 = Encoding.htmlDecode(clink0);
                    this.br.getRequest().setHtmlCode(clink0);
                    final String frm = this.br.getRegex("\"(http://share-links\\.biz/get/frm/.*?)\"").getMatch(0);
                    this.br.getPage(frm);
                    final String fun = this.br.getRegex("eval(.*)\n").getMatch(0);
                    final String result = this.unpackJS(fun, 1);
                    if ((result + "").trim().length() != 0) {
                        if (result.contains("share-links.biz")) {
                            this.br.setFollowRedirects(false);
                            this.br.getPage(result + "");
                            final DownloadLink dl = this.createDownloadlink(this.br.getRedirectLocation());
                            decryptedLinks.add(dl);
                        } else {
                            final DownloadLink dl = this.createDownloadlink(result);
                            decryptedLinks.add(dl);
                        }
                        break;
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
            }
            progress.increase(1);
        }
        return decryptedLinks;
    }

    /** finds the correct shape area for the given point */
    private String getNextUrl(final int x, final int y) {
        final String[][] results = this.br.getRegex("<area shape=\"rect\" coords=\"(\\d+),(\\d+),(\\d+),(\\d+)\" href=\"/(.*?)\"").getMatches();
        String hit = null;
        for (final String[] ret : results) {
            final int xmin = Integer.parseInt(ret[0]);
            final int ymin = Integer.parseInt(ret[1]);
            final int xmax = Integer.parseInt(ret[2]);
            final int ymax = Integer.parseInt(ret[3]);
            if ((x >= xmin) && (x <= xmax) && (y >= ymin) && (y <= ymax)) {
                hit = ret[4];
                break;
            }
        }
        return hit;
    }

    /** by jiaz */
    private ArrayList<DownloadLink> loadcontainer(final Browser br, final String dlclinks) throws IOException, PluginException {
        final Browser brc = br.cloneBrowser();

        if (dlclinks == null) { return new ArrayList<DownloadLink>(); }
        String test = Encoding.htmlDecode(dlclinks);
        File file = null;
        final URLConnectionAdapter con = brc.openGetConnection(dlclinks);
        if (con.getResponseCode() == 200) {
            if (con.isContentDisposition()) {
                test = Plugin.getFileNameFromDispositionHeader(con.getHeaderField("Content-Disposition"));
            } else {
                test = test.replaceAll("(http://share-links\\.biz/|/|\\?)", "") + ".dlc";
            }
            file = JDUtilities.getResourceFile("tmp/sharelinks/" + test);
            if (file == null) { return new ArrayList<DownloadLink>(); }
            file.deleteOnExit();
            brc.downloadConnection(file, con);
        } else {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        if ((file != null) && file.exists() && (file.length() > 100)) {
            final ArrayList<DownloadLink> decryptedLinks = JDUtilities.getController().getContainerLinks(file);
            if (decryptedLinks.size() > 0) { return decryptedLinks; }
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return new ArrayList<DownloadLink>();
    }

    private String unpackJS(final String fun, final int value) throws Exception {
        Object result = new Object();
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");
        final Invocable inv = (Invocable) engine;
        try {
            if (value == 1) {
                result = engine.eval(fun);
                result = "parent = 1;" + result.toString().replace(".frames.Main.location.href", "").replace("window", "\"window\"");
                result = engine.eval(result.toString());
            } else {
                engine.eval(fun);
                result = inv.invokeFunction("f");
            }
        } catch (final Exception e) {
            JDLogger.exception(e);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (result == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        return result.toString();
    }
}
