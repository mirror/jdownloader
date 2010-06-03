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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "share-links.biz" }, urls = { "http://[\\w\\.]*?(share-links\\.biz/_[0-9a-z]+|s2l\\.biz/[a-z0-9]+)" }, flags = { 0 })
public class ShrLnksBz extends PluginForDecrypt {

    private static String host = "http://share-links.biz";

    public ShrLnksBz(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {

        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        this.setBrowserExclusive();

        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setFollowRedirects(false);
        br.getPage(parameter);

        loadContents(br);
        String aha = br.getRedirectLocation();
        if (aha != null) {

            br.getPage(aha);

        }
        /* Error handling */
        if (br.containsHTML("Der Inhalt konnte nicht gefunden werden")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        // Folderpassword+Captcha handling
        if (br.containsHTML("id=\"folderpass\"")) {
            for (int i = 0; i <= 3; i++) {
                String latestPassword = this.getPluginConfig().getStringProperty("PASSWORD", null);

                Form pwform = br.getForm(0);
                pwform.setAction(parameter);
                if (pwform == null) return null;
                // First try the stored password, if that doesn't work, ask the
                // user to enter it
                if (latestPassword == null) latestPassword = Plugin.getUserInput("Password " + getHost() + "?", param);
                pwform.put("password", latestPassword);
                br.submitForm(pwform);
                //
                // br = br;
                if (br.containsHTML("Das eingegebene Passwort ist falsch")) {
                    getPluginConfig().setProperty("PASSWORD", null);
                    getPluginConfig().save();
                    continue;
                } else {
                    // Save actual password if it is valid
                    getPluginConfig().setProperty("PASSWORD", latestPassword);
                    getPluginConfig().save();
                }
                break;
            }
            if (br.containsHTML("Das eingegebene Passwort ist falsch")) {
                getPluginConfig().setProperty("PASSWORD", null);
                getPluginConfig().save();
                throw new DecrypterException(DecrypterException.PASSWORD);
            }

        }
        loadContents(br);
        if (br.containsHTML("(/captcha/|captcha_container|\"Captcha\"|id=\"captcha\")")) {
            boolean auto = true;
            int max = 5;
            for (int i = 0; i <= max; i++) {
                String Captchamap = br.getRegex("\"(/captcha\\.gif\\?d=\\d+.*?PHPSESSID=.*?)\"").getMatch(0);
                if (Captchamap == null) return null;
                Captchamap = Captchamap.replaceAll("(\\&amp;|legend=1)", "");
                File file = this.getLocalCaptchaFile();
                Browser temp = br.cloneBrowser();
                temp.getDownload(file, "http://share-links.biz" + Captchamap + "&legend=1");
                temp.getDownload(file, "http://share-links.biz" + Captchamap);

                String nexturl = null;
                if (Integer.parseInt(JDUtilities.getRevision().replace(".", "")) < 10000 || !auto) {
                    // no autocaptcha
                    Point p = UserIO.getInstance().requestClickPositionDialog(file, "Share-links.biz", JDL.L("plugins.decrypt.shrlnksbz.desc", "Read the combination in the background and click the corresponding combination in the overview!"));
                    nexturl = getNextUrl(p.x, p.y);

                } else {
                    try {
                        String[] code = getCaptchaCode(file, param).split(":");
                        nexturl = getNextUrl(Integer.parseInt(code[0]), Integer.parseInt(code[1]));
                    } catch (Exception e) {
                        Point p = UserIO.getInstance().requestClickPositionDialog(file, "Share-links.biz", JDL.L("plugins.decrypt.shrlnksbz.desc", "Read the combination in the background and click the corresponding combination in the overview!"));
                        nexturl = getNextUrl(p.x, p.y);
                    }
                }

                if (nexturl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                br.setFollowRedirects(true);
                br.getPage("http://share-links.biz/" + nexturl);

                if (br.containsHTML("Die getroffene Auswahl war falsch")) {
                    br.getPage(parameter);
                    if (i == max && auto) {
                        i = 0;
                        auto = false;
                    }
                    continue;
                }
                break;

            }
            if (br.containsHTML("Die getroffene Auswahl war falsch")) { throw new DecrypterException(DecrypterException.CAPTCHA); }
        }
        loadContents(br);

        // container handling (DLC)
        if (br.getRegex("get as dlc container\"  style=\".*?\" onclick=\"javascript:_get\\('(.*?)', 0, 'dlc'\\);\"").getMatch(0) != null) {
            String dlclink = "http://share-links.biz/get/dlc/" + br.getRegex("get as dlc container\"  style=\".*?\" onclick=\"javascript:_get\\('(.*?)', 0, 'dlc'\\);\"").getMatch(0);
            decryptedLinks = loadcontainer(br, dlclink);
            if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
        }

        /* File package handling */
        int pages = 1;
        String pattern = parameter.substring(parameter.lastIndexOf("/"), parameter.length());
        if (br.containsHTML("folderNav")) {
            pages = pages + br.getRegex(pattern + "\\?n=[0-9]++\"").getMatches().length;
        }
        LinkedList<String> links = new LinkedList<String>();
        for (int i = 1; i <= pages; i++) {
            br.getPage(host + pattern + "?n=" + i);
            String[] linki = br.getRegex("decrypt\\.gif\" onclick=\"javascript:_get\\('(.*?)'").getColumn(0);
            if (linki.length == 0) return null;
            links.addAll(Arrays.asList(linki));
        }
        if (links.size() == 0) return null;
        progress.setRange(links.size());
        int waiter = 1000;
        for (String link : links) {

            while (true) {
                link = "http://share-links.biz/get/lnk/" + link;
                br.getPage(link);
                String clink0 = br.getRegex("unescape\\(\"(.*?)\"").getMatch(0);

                if (clink0 != null) {
                    clink0 = Encoding.htmlDecode(clink0);
                    // Cookie: PHPSESSID=lupku56cshpe08gbalemd2u8v3;
                    // lastVisit=1275564000; SLlng=de

                    br.getRequest().setHtmlCode(clink0);
                    String frm = br.getRegex("\"(http://share-links\\.biz/get/frm/.*?)\"").getMatch(0);
                    br.getPage(frm);

                    final Context cx = ContextFactory.getGlobal().enter();
                    final Scriptable scope = cx.initStandardObjects();
                    final String fun = br.getRegex("eval\\((.*)\\)").getMatch(0);
                    Object result = cx.evaluateString(scope, "function f(){return " + fun + ";} f();", "<cmd>", 1, null);
                    String[] row = new Regex(result + "", "(.*)if\\(parent==window\\).*Main.location.href=(.*?\\))").getRow(0);
                    Object result2 = cx.evaluateString(scope, row[0] + " " + row[1], "<cmd>", 1, null);

                    Context.exit();
                    if ((result2 + "").trim().length() != 0) {

                        br.setFollowRedirects(false);
                        br.openGetConnection(result2 + "");

                        DownloadLink dl = createDownloadlink(br.getRedirectLocation());
                        decryptedLinks.add(dl);
                        this.sleep(waiter, param);
                        break;
                    } else {

                        //
                        // throw new
                        // PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        System.out.println("NOTHING");
                        waiter += 500;
                        if (waiter > 5000) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
                    }
                }
            }
            progress.increase(1);
        }
        return decryptedLinks;
    }

    private void loadContents(Browser br) throws IOException {
        // we would like to be as ressource efficient as possible. But this
        // Crypter forces us to download images.
        // the admin may track css, files, js files and more in future. extend
        // this method if required
        String[] images = br.getRegex("<img.*?src=\"(.*?)\"").getColumn(0);
        ArrayList<String> loaded = new ArrayList<String>();
        Browser clone = br.cloneBrowser();

        clone.getHeaders().put("Accept", "Accept: image/png,image/*;q=0.8,*/*;q=0.5");
        for (String image : images) {
            if (loaded.contains(image.trim())) continue;
            if (image.startsWith("http") && !image.contains("share-links")) continue;
            loaded.add(image.trim());
            URLConnectionAdapter loaded2 = clone.openGetConnection(image);
            loaded2.disconnect();
            System.out.println("loaded " + image);

        }

    }

    /** finds the correct shape area for the given point */
    private String getNextUrl(int x, int y) {
        String[][] results = br.getRegex("<area shape=\"rect\" coords=\"(\\d+),(\\d+),(\\d+),(\\d+)\" href=\"/(.*?)\"").getMatches();
        String hit = null;
        for (String[] ret : results) {
            int xmin = Integer.parseInt(ret[0]);
            int ymin = Integer.parseInt(ret[1]);
            int xmax = Integer.parseInt(ret[2]);
            int ymax = Integer.parseInt(ret[3]);
            if (x >= xmin && x <= xmax && y >= ymin && y <= ymax) {
                hit = ret[4];
                break;
            }
        }
        return hit;
    }

    /** by jiaz */
    private ArrayList<DownloadLink> loadcontainer(Browser br, String dlclinks) throws IOException, PluginException {
        Browser brc = br.cloneBrowser();

        if (dlclinks == null) return new ArrayList<DownloadLink>();
        String test = Encoding.htmlDecode(dlclinks);
        File file = null;
        URLConnectionAdapter con = brc.openGetConnection(dlclinks);
        if (con.getResponseCode() == 200) {
            file = JDUtilities.getResourceFile("tmp/sharelinks/" + test.replaceAll("(http://share-links\\.biz/|/|\\?)", "") + ".dlc");
            if (file == null) return new ArrayList<DownloadLink>();
            file.deleteOnExit();
            brc.downloadConnection(file, con);
        } else {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        if (file != null && file.exists() && file.length() > 100) {
            ArrayList<DownloadLink> decryptedLinks = JDUtilities.getController().getContainerLinks(file);
            if (decryptedLinks.size() > 0) return decryptedLinks;
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return new ArrayList<DownloadLink>();
    }

}
