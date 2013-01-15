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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "share-links.biz" }, urls = { "http://[\\w\\.]*?(share-links\\.biz/_[0-9a-z]+|s2l\\.biz/[a-z0-9]+)" }, flags = { 0 })
public class ShrLnksBz extends PluginForDecrypt {

    private String MAINPAGE = "http://share-links.biz/";
    private String ua       = RandomUserAgent.generate();

    public ShrLnksBz(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        if (parameter.contains("s2l.biz")) {
            br.setFollowRedirects(false);
            br.getPage(parameter);
            parameter = br.getRedirectLocation();
        }
        setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getHeaders().clear();
        br.getHeaders().put("Cache-Control", null);
        br.getHeaders().put("Pragma", null);
        br.getHeaders().put("User-Agent", ua);
        br.getHeaders().put("Accept", "*/*");
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        br.getHeaders().put("Accept-Encoding", "gzip,deflate");
        br.getHeaders().put("Accept-Charset", "utf-8,*");
        br.getHeaders().put("Keep-Alive", "115");
        br.getHeaders().put("Connection", "keep-alive");

        br.getPage(parameter);
        if (br.containsHTML("(>No usable content was found<|not able to find the desired content under the given URL.<)")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        /* Very important! */
        final String gif[] = br.getRegex("/template/images/([^\"]+)\\.gif").getColumn(-1);
        if (gif != null) {
            Set<String> hashSet = new HashSet<String>(Arrays.asList(gif));
            List<String> gifList = new ArrayList<String>(hashSet);
            URLConnectionAdapter con = null;
            final Browser br2 = br.cloneBrowser();
            for (final String template : gifList) {
                try {
                    con = br2.openGetConnection(template);
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }

            }
        }
        /* Check if a redirect was there before */
        if (br.getRedirectLocation() != null) {
            br.getPage(br.getRedirectLocation());
        }
        if (br.containsHTML("(>No usable content was found<|not able to find the desired content under the given URL.<)")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        /* Folderpassword */
        if (br.containsHTML("id=\"folderpass\"")) {
            for (int i = 0; i <= 3; i++) {
                String latestPassword = getPluginConfig().getStringProperty("PASSWORD", null);
                final Form pwform = br.getForm(0);
                if (pwform == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                pwform.setAction(parameter);
                // First try the stored password, if that doesn't work, ask the user to enter it
                if (latestPassword == null || latestPassword.equals("")) {
                    latestPassword = Plugin.getUserInput("Enter password for: " + parameter, param);
                }
                pwform.put("password", latestPassword);
                br.submitForm(pwform);
                if (br.containsHTML("This folder requires a password\\.")) {
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
            if (br.containsHTML("This folder requires a password\\.")) {
                getPluginConfig().setProperty("PASSWORD", null);
                getPluginConfig().save();
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
        }
        /* Captcha handling */
        if (br.containsHTML("(/captcha/|captcha_container|\"Captcha\"|id=\"captcha\")")) {
            // Captcha Recognition broken - auto = false
            boolean auto = false;
            final int max = 5;
            for (int i = 0; i <= max; i++) {
                String Captchamap = br.getRegex("\"(/captcha\\.gif\\?d=\\d+.*?PHPSESSID=.*?)\"").getMatch(0);
                if (Captchamap == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                Captchamap = Captchamap.replaceAll("(\\&amp;|legend=1)", "");
                final File file = this.getLocalCaptchaFile();
                final Browser temp = br.cloneBrowser();
                temp.getDownload(file, "http://share-links.biz" + Captchamap + "&legend=1");
                temp.getDownload(file, "http://share-links.biz" + Captchamap);
                String nexturl = null;
                if (Integer.parseInt(JDUtilities.getRevision().replace(".", "")) < 10000 || !auto) {
                    final Point p = UserIO.getInstance().requestClickPositionDialog(file, "Share-links.biz", JDL.L("plugins.decrypt.shrlnksbz.desc", "Read the combination in the background and click the corresponding combination in the overview!"));
                    if (p == null) { throw new DecrypterException(DecrypterException.CAPTCHA); }
                    nexturl = getNextUrl(p.x, p.y);
                } else {
                    try {
                        final String[] code = this.getCaptchaCode(file, param).split(":");
                        nexturl = getNextUrl(Integer.parseInt(code[0]), Integer.parseInt(code[1]));
                    } catch (final Exception e) {
                        final Point p = UserIO.getInstance().requestClickPositionDialog(file, "Share-links.biz", JDL.L("plugins.decrypt.shrlnksbz.desc", "Read the combination in the background and click the corresponding combination in the overview!"));
                        if (p == null) { throw new DecrypterException(DecrypterException.CAPTCHA); }
                        nexturl = getNextUrl(p.x, p.y);
                    }
                }
                if (nexturl == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
                br.setFollowRedirects(true);
                br.getPage(MAINPAGE + nexturl);
                if (br.containsHTML("> Your choice was wrong\\.<")) {
                    br.getPage(parameter);
                    if (i == max && auto) {
                        i = 0;
                        auto = false;
                    }
                    continue;
                }
                break;
            }
            if (br.containsHTML("> Your choice was wrong\\.<")) { throw new DecrypterException(DecrypterException.CAPTCHA); }
        }
        /* use cnl2 button if available */
        if (br.containsHTML("/cnl2/")) {
            String flashVars = br.getRegex("swfobject.embedSWF\\(\"(.*?)\"").getMatch(0);
            if (flashVars != null) {
                final Browser cnlbr = new Browser();
                String test = cnlbr.getPage(MAINPAGE + "get/cnl2/_" + flashVars);
                String[] encVars = null;
                if (test != null) encVars = test.split("\\;\\;");
                if (encVars == null || encVars.length < 3) {
                    logger.warning("CNL code broken!");
                } else {
                    final String jk = new StringBuffer(Encoding.Base64Decode(encVars[1])).reverse().toString();
                    final String crypted = new StringBuffer(Encoding.Base64Decode(encVars[2])).reverse().toString();
                    String pkgName = br.getRegex("<title>Share.*?\\.biz - (.*?)</title>").getMatch(0);
                    if (pkgName != null && pkgName.length() > 0) {
                        pkgName = "package=" + Encoding.formEncoding(pkgName) + "&";
                    }
                    flashVars = pkgName + "passwords=autopostpw&crypted=" + Encoding.formEncoding(crypted) + "&jk=" + Encoding.formEncoding(jk) + "&source=" + Encoding.formEncoding(parameter);
                    cnlbr.setConnectTimeout(5000);
                    cnlbr.getHeaders().put("jd.randomNumber", System.getProperty("jd.randomNumber"));
                    try {
                        cnlbr.postPage("http://127.0.0.1:9666/flash/addcrypted2", flashVars);
                        if (cnlbr.containsHTML("success")) {
                            return decryptedLinks;
                        } else {
                            logger.warning("Click-N-Load failed!");
                        }
                    } catch (final Throwable e) {
                    }
                }
            }
        }
        /* Load Contents. Container handling (DLC) */
        final String dlclink = br.getRegex("get as dlc container\".*?\"javascript:_get\\('(.*?)', 0, 'dlc'\\);\"").getMatch(0);
        if (dlclink != null) {
            decryptedLinks = loadcontainer(br, MAINPAGE + "get/dlc/" + dlclink);
            if (decryptedLinks != null && decryptedLinks.size() > 0) { return decryptedLinks; }
        }
        /* File package handling */
        int pages = 1;
        final String pattern = parameter.substring(parameter.lastIndexOf("/") + 1, parameter.length());
        if (br.containsHTML("folderNav")) {
            pages = pages + br.getRegex(pattern + "\\?n=[0-9]++\"").getMatches().length;
        }
        final LinkedList<String> links = new LinkedList<String>();
        for (int i = 1; i <= pages; i++) {
            br.getPage(MAINPAGE + pattern);
            final String[] linki = br.getRegex("decrypt\\.gif\" onclick=\"javascript:_get\\('(.*?)'").getColumn(0);
            if (linki.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            links.addAll(Arrays.asList(linki));
        }
        if (links.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String tmplink : links) {
            br.getPage(MAINPAGE + "get/lnk/" + tmplink);
            final String clink0 = br.getRegex("unescape\\(\"(.*?)\"").getMatch(0);
            if (clink0 != null) {
                try {
                    br.getPage(new Regex(Encoding.htmlDecode(clink0), "\"(http://share-links\\.biz/get/frm/.*?)\"").getMatch(0));
                } catch (final Throwable e) {
                    continue;
                }
                final String fun = br.getRegex("eval(\\(.*\\))[\r\n]+").getMatch(0);
                String result = fun != null ? unpackJS(fun, 1) : null;
                if (result != null) {
                    if (result.contains("share-links.biz")) {
                        br.setFollowRedirects(false);
                        br.getPage(result);
                        result = br.getRedirectLocation() != null ? br.getRedirectLocation() : null;
                        if (result == null) {
                            continue;
                        }
                    }
                    final DownloadLink dl = createDownloadlink(result);
                    try {
                        distribute(dl);
                    } catch (final Throwable e) {
                        /* does not exist in 09581 */
                    }
                    decryptedLinks.add(dl);
                } else {
                    continue;
                }
            }
        }
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    /** finds the correct shape area for the given point */
    private String getNextUrl(final int x, final int y) {
        final String[][] results = br.getRegex("<area shape=\"rect\" coords=\"(\\d+),(\\d+),(\\d+),(\\d+)\" href=\"/(.*?)\"").getMatches();
        String hit = null;
        for (final String[] ret : results) {
            final int xmin = Integer.parseInt(ret[0]);
            final int ymin = Integer.parseInt(ret[1]);
            final int xmax = Integer.parseInt(ret[2]);
            final int ymax = Integer.parseInt(ret[3]);
            if (x >= xmin && x <= xmax && y >= ymin && y <= ymax) {
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
        URLConnectionAdapter con = null;
        try {
            con = brc.openGetConnection(dlclinks);
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
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }

            if (file != null && file.exists() && file.length() > 100) {
                final ArrayList<DownloadLink> decryptedLinks = JDUtilities.getController().getContainerLinks(file);
                if (decryptedLinks.size() > 0) { return decryptedLinks; }
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            return new ArrayList<DownloadLink>();
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    private String unpackJS(final String fun, final int value) throws Exception {
        Object result = new Object();
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");
        final Invocable inv = (Invocable) engine;
        try {
            logger.info(fun);
            if (value == 1) {
                result = engine.eval(fun);
                result = "parent = 1;" + result.toString().replace(".frames.Main.location.href", "").replace("window", "\"window\"");
                logger.info(result.toString());
                result = engine.eval(result.toString());
            } else {
                engine.eval(fun);
                result = inv.invokeFunction("f");
            }
        } catch (final Exception e) {
            logger.severe(e.getMessage());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (result == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        return result.toString();
    }

    /* NOTE: no override to keep compatible to old stable */
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }
}