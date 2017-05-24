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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.ProgressController;
import jd.http.Browser;
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
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.formatter.HexFormatter;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickedPoint;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ScriptableObject;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "share-links.biz" }, urls = { "http://[\\w\\.]*?(share-links\\.biz/_[0-9a-z]+|s2l\\.biz/[a-z0-9]+)" })
public class ShrLnksBz extends antiDDoSForDecrypt {
    private final String NO_DLC = "1";
    private final String NO_CNL = "1";

    public ShrLnksBz(final PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    protected DownloadLink createDownloadlink(String link) {
        DownloadLink ret = super.createDownloadlink(link);
        ret.setUrlProtection(org.jdownloader.controlling.UrlProtection.PROTECTED_DECRYPTER);
        return ret;
    }

    @Override
    protected boolean useRUA() {
        return true;
    }

    @Override
    protected Browser prepBrowser(Browser prepBr, String host) {
        if (!(browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            // we want new agent each time we have virgin browser
            userAgent.set(null);
            super.prepBrowser(prepBr, host);
            /* define custom browser headers and language settings */
            correctLanguageCookie(prepBr, host);
            prepBr.getHeaders().put("Cache-Control", null);
            prepBr.getHeaders().put("Accept", "*/*");
            prepBr.getHeaders().put("Accept-Encoding", "gzip,deflate");
            prepBr.getHeaders().put("Accept-Charset", "utf-8,*");
        }
        return prepBr;
    }

    private void correctLanguageCookie(final Browser prepBr, final String host) {
        if (host.matches("(?i)share-links\\.biz|s2l\\.biz")) {
            /* Prefer English */
            prepBr.setCookie(host, "SLlng", "en");
        }
    }

    @Override
    protected void getPage(Browser ibr, String page) throws Exception {
        super.getPage(ibr, page);
        // they do stupid shit like switch cookies!
        if (!"en".equals(ibr.getCookie(Browser.getHost(page), "SLlng"))) {
            correctLanguageCookie(ibr, page);
        }
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br = new Browser();
        String parameter = param.toString();
        if (parameter.contains("s2l.biz")) {
            br.setFollowRedirects(false);
            getPage(parameter);
            parameter = br.getRedirectLocation();
        }
        setBrowserExclusive();
        br.setFollowRedirects(false);
        /* Prefer English */
        parameter += "?lng=en";
        getPage(parameter);
        if (br.containsHTML("(>No usable content was found<|not able to find the desired content under the given URL.<)")) {
            logger.info("Link offline: " + parameter);
            decryptedLinks.add(this.createOfflinelink(parameter));
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
                    con = openAntiDDoSRequestConnection(br2, br2.createGetRequest(template));
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
            getPage(br.getRedirectLocation());
        }
        if (br.containsHTML("(>No usable content was found<|not able to find the desired content under the given URL.<)")) {
            logger.info("Link offline: " + parameter);
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        /* Folderpassword */
        if (br.containsHTML("id=\"folderpass\"")) {
            final List<String> passwords = getPreSetPasswords();
            for (int i = 0; i <= 3; i++) {
                String latestPassword = null;
                if (passwords.size() > 0) {
                    latestPassword = passwords.remove(0);
                    i = 0;
                } else {
                    latestPassword = getPluginConfig().getStringProperty("PASSWORD", null);
                }
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
                submitForm(pwform);
                if (br.containsHTML("This folder requires a password\\.")) {
                    if (getPluginConfig().setProperty("PASSWORD", null)) {
                        getPluginConfig().save();
                    }
                    continue;
                } else {
                    // Save actual password if it is valid
                    if (getPluginConfig().setProperty("PASSWORD", latestPassword)) {
                        getPluginConfig().save();
                    }
                }
                break;
            }
            if (br.containsHTML("This folder requires a password\\.")) {
                if (getPluginConfig().setProperty("PASSWORD", null)) {
                    getPluginConfig().save();
                }
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
        }
        /* Captcha handling */
        if (br.containsHTML("(/captcha/|captcha_container|\"Captcha\"|id=\"captcha\")")) {
            // Captcha Recognition broken - auto = false
            boolean auto = false;
            final int max = 5;
            boolean failed = true;
            for (int i = 0; i <= max; i++) {
                String Captchamap = br.getRegex("\"(/captcha\\.gif\\?d=\\d+.*?PHPSESSID=.*?)\"").getMatch(0);
                if (Captchamap == null) {
                    invalidateLastChallengeResponse();
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                Captchamap = Captchamap.replaceAll("(\\&amp;|legend=1)", "");
                final File file = this.getLocalCaptchaFile();
                final Browser temp = br.cloneBrowser();
                temp.getDownload(file, Captchamap + "&legend=1");
                temp.getDownload(file, Captchamap);
                String nexturl = null;
                final ClickedPoint cp = getCaptchaClickedPoint(getHost(), file, param, null, JDL.L("plugins.decrypt.shrlnksbz.desc", "Read the combination in the background and click the corresponding combination in the overview!"));
                nexturl = getNextUrl(cp.getX(), cp.getY());
                if (nexturl == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                // they can switch to german here, within REDIRECT we can't have that!
                // br.setFollowRedirects(true);
                getPage(nexturl);
                while (br.getRedirectLocation() != null) {
                    getPage(br.getRedirectLocation());
                }
                if (br.containsHTML("> Your choice was wrong\\.<")) {
                    getPage(parameter);
                    if (i == max && auto) {
                        i = 0;
                        auto = false;
                    }
                    invalidateLastChallengeResponse();
                    continue;
                } else {
                    validateLastChallengeResponse();
                }
                failed = false;
                break;
            }
            if (failed) {
                throw new DecrypterException(DecrypterException.CAPTCHA);
            }
        }
        /* use cnl2 button if available */
        if (br.containsHTML("/cnl2/") || this.br.containsHTML("/cnl2_add\\.png")) {
            if (getPluginConfig().getBooleanProperty(NO_CNL, false) == false) {
                String flashVars = br.getRegex("swfobject.embedSWF\\(\"(.*?)\"").getMatch(0);
                if (flashVars == null) {
                    flashVars = br.getRegex("file[\n\t\r ]*?=[\n\t\r ]*?\"([^<>\"]+)\"").getMatch(0);
                }
                if (flashVars != null) {
                    final Browser cnlbr = br.cloneBrowser();
                    getPage(cnlbr, "/get/cnl2/" + flashVars);
                    String test = cnlbr.toString();
                    String[] encVars = null;
                    if (test != null) {
                        encVars = test.split("\\;\\;");
                    }
                    if (encVars == null || encVars.length < 3) {
                        logger.warning("CNL code broken!");
                    } else {
                        final String jk = new StringBuffer(Encoding.Base64Decode(encVars[1])).reverse().toString();
                        final String crypted = new StringBuffer(Encoding.Base64Decode(encVars[2])).reverse().toString();
                        HashMap<String, String> infos = new HashMap<String, String>();
                        infos.put("crypted", crypted);
                        infos.put("jk", jk);
                        infos.put("source", parameter.toString());
                        String pkgName = br.getRegex("<title>Share.*?\\.biz \\- (.*?)</title>").getMatch(0);
                        if (pkgName != null && !"unnamed Folder".equals(pkgName) && pkgName.length() > 0) {
                            infos.put("package", pkgName);
                        }
                        String json = JSonStorage.toString(infos);
                        final DownloadLink dl = createDownloadlink("http://dummycnl.jdownloader.org/" + HexFormatter.byteArrayToHex(json.getBytes("UTF-8")));
                        distribute(dl);
                        decryptedLinks.add(dl);
                        if (!decryptedLinks.isEmpty()) {
                            return decryptedLinks;
                        }
                    }
                }
            }
        }
        /* Load Contents. Container handling (DLC) */
        final String dlclink = br.getRegex("get as dlc container\".*?\"javascript:_get\\('(.*?)', 0, 'dlc'\\);\"").getMatch(0);
        if (dlclink != null) {
            if (getPluginConfig().getBooleanProperty(NO_DLC, false) == false) {
                decryptedLinks = loadcontainer(br, "/get/dlc/" + dlclink);
                if (!decryptedLinks.isEmpty()) {
                    return decryptedLinks;
                }
            }
        }
        /* File package handling */
        int pages = 1;
        final String pattern = parameter.substring(parameter.lastIndexOf("/") + 1, parameter.length());
        if (br.containsHTML("folderNav")) {
            pages = pages + br.getRegex(pattern + "\\?n=[0-9]++\"").getMatches().length;
        }
        final LinkedList<String> links = new LinkedList<String>();
        for (int i = 1; i <= pages; i++) {
            getPage(pattern);
            final String[] linki = br.getRegex("decrypt\\.gif\" onclick=\"javascript:_get\\('(.*?)'").getColumn(0);
            if (linki.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            links.addAll(Arrays.asList(linki));
        }
        if (links.size() == 0) {
            invalidateLastChallengeResponse();
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String tmplink : links) {
            getPage("/get/lnk/" + tmplink);
            final String clink0 = br.getRegex("unescape\\(\"(.*?)\"").getMatch(0);
            if (clink0 != null) {
                try {
                    getPage(new Regex(Encoding.htmlDecode(clink0), "\"(https?://share-links\\.biz/get/frm/.*?)\"").getMatch(0));
                } catch (final Throwable e) {
                    continue;
                }
                final String fun = br.getRegex("eval(\\(.*\\))[\r\n]+").getMatch(0);
                String result = fun != null ? unpackJS(fun, 1) : null;
                if (result != null) {
                    if (result.contains("share-links.biz")) {
                        br.setFollowRedirects(false);
                        getPage(result);
                        result = br.getRedirectLocation() != null ? br.getRedirectLocation() : null;
                        if (result == null) {
                            continue;
                        }
                    }
                    final DownloadLink dl = createDownloadlink(result);
                    distribute(dl);
                    decryptedLinks.add(dl);
                } else {
                    continue;
                }
            }
        }
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            invalidateLastChallengeResponse();
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        } else {
            validateLastChallengeResponse();
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
        if (dlclinks == null) {
            return new ArrayList<DownloadLink>();
        }
        String test = Encoding.htmlDecode(dlclinks);
        File file = null;
        URLConnectionAdapter con = null;
        try {
            con = brc.openGetConnection(dlclinks);
            if (con.getResponseCode() == 200) {
                if (con.isContentDisposition()) {
                    test = Plugin.getFileNameFromDispositionHeader(con);
                } else {
                    test = test.replaceAll("(https?://share-links\\.biz/|/|\\?)", "") + ".dlc";
                }
                file = JDUtilities.getResourceFile("tmp/sharelinks/" + test);
                if (file == null) {
                    return new ArrayList<DownloadLink>();
                }
                file.deleteOnExit();
                brc.downloadConnection(file, con);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (file != null && file.exists() && file.length() > 100) {
                final List<DownloadLink> decryptedLinks = loadContainerFile(file);
                if (decryptedLinks.size() > 0) {
                    return new ArrayList<DownloadLink>(decryptedLinks);
                }
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
        try {
            logger.info(fun);
            Context cx = null;
            try {
                cx = ContextFactory.getGlobal().enterContext();
                ScriptableObject scope = cx.initStandardObjects();
                if (value == 1) {
                    /*
                     * creating pseudo functions: document.location.protocol + document.write(value)
                     */
                    result = cx.evaluateString(scope, fun, "<cmd>", 1, null);
                    result = "parent = 1;" + result.toString().replace(".frames.Main.location.href", "").replace("window", "\"window\"");
                    logger.info(result.toString());
                    result = cx.evaluateString(scope, result.toString(), "<cmd>", 1, null);
                } else {
                    cx.evaluateString(scope, fun, "<cmd>", 1, null);
                    result = cx.evaluateString(scope, "f()", "<cmd>", 1, null);
                }
            } finally {
                Context.exit();
            }
        } catch (final Exception e) {
            logger.severe(e.getMessage());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (result == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return result.toString();
    }

    /* NOTE: no override to keep compatible to old stable */
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), NO_CNL, JDL.L("plugins.decrypter.shrlinksbz.nocnl", "No cnl?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), NO_DLC, JDL.L("plugins.decrypter.shrlinksbz.nodlc", "No dlc?")).setDefaultValue(false));
    }
}