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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;
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
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final HashSet<String> dupe = new HashSet<String>();
        br = new Browser();
        String parameter = param.toString();
        if (parameter.contains("s2l.biz")) {
            getPage(parameter);
            parameter = br.getRedirectLocation();
        }
        setBrowserExclusive();
        br.setFollowRedirects(true);
        /* Prefer English */
        parameter += "?lng=en";
        getPage(parameter);
        if (isOffline()) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        /* Very important! */
        handleImportant();
        br.setFollowRedirects(false);
        /* Folderpassword */
        handlePassword(param, parameter);
        /* Captcha handling */
        handleCaptcha(param, parameter);
        final int count = getCount();
        /* use cnl2 button if available */
        decryptedLinks.addAll(handleClickNLoad(dupe, parameter));
        if (decryptedLinks.size() == count) {
            return decryptedLinks;
        }
        /* Load Contents. Container handling (DLC) */
        decryptedLinks.addAll(handleDlc(dupe));
        if (decryptedLinks.size() == count) {
            return decryptedLinks;
        }
        /* Individual file handling */
        decryptedLinks.addAll(handleIndividualLinks(dupe, parameter));
        if (decryptedLinks.isEmpty()) {
            if (count == 0) {
                return decryptedLinks;
            } else {
                logger.warning("Decrypter out of date for link: " + parameter);
                return null;
            }
        } else {
            if (decryptedLinks.size() != count) {
                logger.warning("decryptedLinks size doesn't reflect against count");
            }
            return decryptedLinks;
        }
    }

    private int getCount() {
        // when individual links are not provided, count seems to be 0/total. use second figure!
        final String c = br.getRegex("Count of secured links:\\s*<span>\\d+/(\\d+)").getMatch(0);
        if (c != null) {
            return Integer.parseInt(c);
        } else {
            return -1;
        }
    }

    private boolean isOffline() {
        final boolean result = br.containsHTML(">No usable content was found<|not able to find the desired content under the given URL\\.<");
        return result;
    }

    private void handlePassword(CryptedLink param, String parameter) throws Exception {
        if (br.containsHTML("id=\"folderpass\"")) {
            final List<String> passwords = getPreSetPasswords();
            final int tries = 3;
            for (int i = 0; i <= tries; i++) {
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
                    throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
                }
                pwform.setAction(parameter);
                // First try the stored password, if that doesn't work, ask the user to enter it
                if (latestPassword == null || latestPassword.equals("")) {
                    latestPassword = Plugin.getUserInput("Enter password for: " + parameter, param);
                }
                pwform.put("password", Encoding.urlEncode(latestPassword));
                submitForm(pwform);
                if (!br.containsHTML("This folder requires a password\\.")) {
                    // Save actual password if it is valid
                    if (getPluginConfig().setProperty("PASSWORD", latestPassword)) {
                        getPluginConfig().save();
                    }
                    return;
                }
                if (tries > i) {
                    throw new DecrypterException(DecrypterException.PASSWORD);
                }
                if (getPluginConfig().setProperty("PASSWORD", null)) {
                    getPluginConfig().save();
                }
            }
        }
    }

    private ArrayList<DownloadLink> handleDlc(Set<String> dupe) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String dlclink = br.getRegex("get as dlc container\".*?\"javascript:_get\\('(.*?)', 0, 'dlc'\\);\"").getMatch(0);
        if (dlclink != null) {
            if (getPluginConfig().getBooleanProperty(NO_DLC, false) == false) {
                for (final DownloadLink dl : loadContainer(br, "/get/dlc/" + dlclink)) {
                    if (dupe.add(dl.getPluginPatternMatcher())) {
                        decryptedLinks.add(dl);
                        distribute(dl);
                    }
                }
            }
        }
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> handleClickNLoad(Set<String> dupe, final String parameter) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (br.containsHTML("/cnl2/") || br.containsHTML("/cnl2_add\\.png")) {
            if (getPluginConfig().getBooleanProperty(NO_CNL, false) == false) {
                String flashVars = br.getRegex("swfobject.embedSWF\\(\"(.*?)\"").getMatch(0);
                if (flashVars == null) {
                    flashVars = br.getRegex("file[\n\t\r ]*?=[\n\t\r ]*?\"([^<>\"]+)\"").getMatch(0);
                }
                if (flashVars == null) {
                    logger.info("Could not find flashVars");
                    return decryptedLinks;
                }
                final Browser cnlbr = br.cloneBrowser();
                getPage(cnlbr, "/get/cnl2/" + flashVars);
                String test = cnlbr.toString();
                String[] encVars = null;
                if (test != null) {
                    encVars = test.split("\\;\\;");
                }
                if (encVars == null || encVars.length < 3) {
                    logger.warning("CNL code broken!");
                    return decryptedLinks;
                }
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
                final LinkCrawler lc = LinkCrawler.newInstance();
                lc.crawl("http://dummycnl.jdownloader.org/" + HexFormatter.byteArrayToHex(json.getBytes("UTF-8")));
                lc.waitForCrawling();
                final List<CrawledLink> crawledLinks = lc.getCrawledLinks();
                // we need to extract the respective DownloadLinks...
                for (final CrawledLink cl : crawledLinks) {
                    final DownloadLink dl = cl.getDownloadLink();
                    if (dupe.add(dl.getPluginPatternMatcher())) {
                        decryptedLinks.add(dl);
                        distribute(dl);
                    }
                }
            }
        }
        return decryptedLinks;
    }

    private void handleImportant() throws Exception {
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
    }

    private void handleCaptcha(final CryptedLink param, final String parameter) throws Exception {
        if (br.containsHTML("(/captcha/|captcha_container|\"Captcha\"|id=\"captcha\")")) {
            final int max = 5;
            for (int i = 0; i < max; i++) {
                String Captchamap = br.getRegex("\"(/captcha\\.gif\\?d=\\d+.*?PHPSESSID=.*?)\"").getMatch(0);
                if (Captchamap == null) {
                    throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
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
                    throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
                }
                // they can switch to german here, within REDIRECT we can't have that!
                // br.setFollowRedirects(true);
                getPage(nexturl);
                while (br.getRedirectLocation() != null) {
                    getPage(br.getRedirectLocation());
                }
                if (br.containsHTML(">\\s*Your choice was wrong\\.\\s*<")) {
                    invalidateLastChallengeResponse();
                    if (i + 1 >= max) {
                        throw new DecrypterException(DecrypterException.CAPTCHA);
                    }
                    getPage(parameter);
                    continue;
                }
                validateLastChallengeResponse();
                return;
            }
        }
    }

    private ArrayList<DownloadLink> handleIndividualLinks(Set<String> dupe, final String parameter) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String[] linki = br.getRegex("decrypt\\.gif\" onclick=\"javascript:_get\\('(.*?)'").getColumn(0);
        if (linki == null || linki.length == 0) {
            return decryptedLinks;
        }
        for (final String tmplink : linki) {
            final Browser br = this.br.cloneBrowser();
            try {
                getPage(br, "/get/lnk/" + tmplink);
                final String clink0 = br.getRegex("unescape\\(\"(.*?)\"").getMatch(0);
                if (clink0 != null) {
                    getPage(br, new Regex(Encoding.htmlDecode(clink0), "\"(https?://share-links\\.biz/get/frm/.*?)\"").getMatch(0));
                    final String fun = br.getRegex("eval(\\(.*\\))[\r\n]+").getMatch(0);
                    String result = fun != null ? unpackJS(fun, 1) : null;
                    if (result == null) {
                        continue;
                    }
                    if ("".equals(result.trim())) {
                        // no point doing any more, as they have the same outcome.
                        // not a bug goeo blocked, add entry so it doesn't show up as defect
                        decryptedLinks.add(null);
                        logger.warning("you can not decypt individual links from this connection");
                        return decryptedLinks;
                    } else if (result.contains("share-links.biz/")) {
                        getPage(br, result);
                        result = br.getRedirectLocation();
                        if (result == null || result.trim().length() == 0) {
                            continue;
                        }
                    }
                    if (dupe.add(result)) {
                        final DownloadLink dl = createDownloadlink(result);
                        distribute(dl);
                        decryptedLinks.add(dl);
                    }
                }
            } catch (final Exception e) {
                continue;
            }
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

    /**
     * by jiaz
     *
     * @throws Exception
     */
    private List<DownloadLink> loadContainer(final Browser br, final String dlclinks) throws Exception {
        if (dlclinks != null) {
            String test = Encoding.htmlDecode(dlclinks);
            File file = null;
            URLConnectionAdapter con = null;
            try {
                final Browser brc = br.cloneBrowser();
                con = brc.openGetConnection(dlclinks);
                if (con.getResponseCode() == 200) {
                    if (con.isContentDisposition()) {
                        test = Plugin.getFileNameFromDispositionHeader(con);
                    } else {
                        test = test.replaceAll("(https?://share-links\\.biz/|/|\\?)", "") + ".dlc";
                    }
                    file = JDUtilities.getResourceFile("tmp/sharelinks/" + test);
                    if (file != null) {
                        file.getParentFile().mkdirs();
                        brc.downloadConnection(file, con);
                        if (file.exists() && file.length() > 100) {
                            return loadContainerFile(file);
                        }
                    }
                }
            } finally {
                try {
                    if (con != null) {
                        con.disconnect();
                    }
                } catch (final Throwable e) {
                }
                if (file != null && file.exists() && !file.delete()) {
                    file.deleteOnExit();
                }
            }
        }
        return new ArrayList<DownloadLink>();
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