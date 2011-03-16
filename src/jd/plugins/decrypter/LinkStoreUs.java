//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "linkstore.us" }, urls = { "http://(www\\.)?linkstore\\.us/(load\\?[a-z0-9]+|[a-z0-9]+(-m\\d+)?)" }, flags = { 0 })
public class LinkStoreUs extends PluginForDecrypt {

    public LinkStoreUs(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String CIRCLECAPTCHATEXT = "captcha_cross\\.png";
    private static final String RECAPTCHATEXT = "(/recaptcha/api/challenge\\?k=|api\\.recaptcha\\.net)";
    private static final String PASSWORDTEXT = "<h4>Folder-password:</h4>";
    private static final String MAINPAGE = "http://linkstore.us/";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        // Allow only 1 instance of this decrypter at the same time
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<DownloadLink> dlclinks = new ArrayList<DownloadLink>();
        ArrayList<String> containerformats = new ArrayList<String>();
        containerformats.add("dlc");
        containerformats.add("rsdf");
        containerformats.add("ccf");
        String parameter = param.toString();
        br.setCookie("http://linkstore.us/", "lang", "en");
        br.setFollowRedirects(false);
        br.setCustomCharset("utf-8");
        br.getPage(parameter);
        if (br.containsHTML("<h2>Folder not found")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        if (parameter.matches(".*?linkstore\\.us/[a-z0-9]+(-m\\d+)?")) {
            logger.info("Decrypting a normal folderlink...");
            for (int i = 0; i <= 5; i++) {
                String postData = "";
                if (br.containsHTML(RECAPTCHATEXT)) {
                    logger.info("reCaptcha found...");
                    PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                    jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                    rc.parse();
                    rc.load();
                    File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    String c = getCaptchaCode(cf, param);
                    postData = "recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c;
                } else if (br.containsHTML(CIRCLECAPTCHATEXT)) {
                    File file = this.getLocalCaptchaFile();
                    br.cloneBrowser().getDownload(file, "http://linkstore.us/captcha_cross.png");
                    Point p = UserIO.getInstance().requestClickPositionDialog(file, this.getHost(), JDL.L("plugins.decrypter.linkstoreus.captchadescription", "Please click on the open circle!"));
                    postData = "captcha.x=" + p.x + "&captcha.y=" + p.y;
                }
                if (br.containsHTML(PASSWORDTEXT)) {
                    postData += "&pw=" + getUserInput(null, param);
                }
                if (!postData.equals("")) {
                    br.postPage(parameter, postData);
                    if (!br.containsHTML(CIRCLECAPTCHATEXT) && !br.containsHTML(PASSWORDTEXT) && !br.containsHTML(RECAPTCHATEXT)) break;
                } else {
                    break;
                }
            }
            if (br.containsHTML(PASSWORDTEXT)) throw new DecrypterException(DecrypterException.PASSWORD);
            if (br.containsHTML(CIRCLECAPTCHATEXT) || br.containsHTML(PASSWORDTEXT) || br.containsHTML(RECAPTCHATEXT)) throw new DecrypterException(DecrypterException.CAPTCHA);
            // Mirrorlink: Find all mirrors and give the links back to the
            // decrypter (new modules will be started)
            String mirrors[] = br.getRegex("class=\\'mirrortable\\' onclick=\\'self\\.location\\.href=\"([a-z0-9-]+)(\\&[a-zA-Z0-9]+\"|\")\\'").getColumn(0);
            if (mirrors != null && mirrors.length != 0) {
                logger.info("Found mirrorlinks, decrypting...");
                for (String mirror : mirrors)
                    decryptedLinks.add(createDownloadlink("http://linkstore.us/" + mirror));
                return decryptedLinks;
            }
            String password = br.getRegex("<h4>File-password:</h4>\\&nbsp;<span style=\\'font-family:Courier New,sans-serif;\\'>(.*?)</span></span>").getMatch(0);
            String fpName = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
            String containerLink = new Regex(parameter, "(http://(www\\.)?linkstore\\.us/[a-z0-9]+)").getMatch(0) + "-0";
            for (String containerFormat : containerformats) {
                if (br.containsHTML("\\." + containerFormat)) {
                    dlclinks = loadcontainer(containerLink, "." + containerFormat);
                    if (dlclinks != null && dlclinks.size() != 0) {
                        if (fpName != null) {
                            FilePackage fp = FilePackage.getInstance();
                            if (password != null) fp.setPassword(password);
                            fp.setName(fpName.trim());
                            fp.addLinks(dlclinks);
                        }
                        return dlclinks;
                    }
                }
            }
            logger.info("Failed to get the links via DLC, trying webdecryption...");
            // Hier wird geschaut ob die Links unverschlüsselt auf der Seite
            // stehen (kann vorkommen)
            String uncryptedLinksPagepiece = br.getRegex("class=\\'textarea linklist\\'.+readonly>(.*?)</textarea></div><br><br><br").getMatch(0);
            String[] uncryptedLinks = null;
            if (uncryptedLinksPagepiece != null) uncryptedLinks = HTMLParser.getHttpLinks(uncryptedLinksPagepiece, "");
            if (uncryptedLinks != null && uncryptedLinks.length != 0) {
                logger.info("Found uncrypted links....");
                for (String uncryptedLink : uncryptedLinks) {
                    DownloadLink dl = createDownloadlink(uncryptedLink);
                    if (password != null) dl.addSourcePluginPassword(password);
                    decryptedLinks.add(dl);
                }
            } else {
                logger.info("Didn't find any uncrypted links...doing it the hard way!");
                String[] links = br.getRegex("\\'(load\\?[a-zA-Z0-9]+)(\\&|\\')").getColumn(0);
                if (links == null || links.length == 0) links = br.getRegex("</div></td><td width=\\'100\\' ><a href=\\'(.*?)\\'").getColumn(0);
                if (links == null || links.length == 0) {
                    logger.warning("Couldn't find any crypted links...");
                    return null;
                }
                progress.setRange(links.length);
                for (String singleLink : links) {
                    String finallink = getSingleLoadlink(MAINPAGE + singleLink);
                    if (finallink == null) return null;
                    DownloadLink dl = createDownloadlink(finallink);
                    if (password != null) dl.addSourcePluginPassword(password);
                    decryptedLinks.add(dl);
                    progress.increase(1);
                }
            }
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                if (password != null) fp.setPassword(password);
                fp.setName(fpName.trim());
                fp.addLinks(decryptedLinks);
            }
        } else {
            logger.info("Decrypting a single loadlink...");
            String finallink = getSingleLoadlink(parameter);
            if (finallink == null) {
                logger.warning("Failed to decrypt single link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        }
        return decryptedLinks;
    }

    private String getSingleLoadlink(String loadlink) throws IOException {
        br.getPage(loadlink);
        String nextLink = br.getRegex("<frame scrolling=\\'auto\\' noresize src=\\'(load\\?[a-z0-9]+)\\'").getMatch(0);
        if (nextLink == null) {
            logger.warning("Nextlink is null...");
            return null;
        }
        br.getPage(MAINPAGE + nextLink);
        String unescape = br.getRegex("eval\\(unescape\\(\"(.*?)\"\\)\\)").getMatch(0);
        if (unescape == null) return null;
        unescape = Encoding.htmlDecode(unescape);
        if (unescape.contains("document.write")) {
            unescape = unescape.substring(0, unescape.lastIndexOf("}") + 1);
        }
        String finallink = javascript(unescape);
        if (finallink.contains("URL")) {
            finallink = new Regex(finallink, "URL=(.*?)'").getMatch(0);
        } else {
            logger.warning("Finallink regex broken!");
            return null;
        }
        return finallink;
    }

    private String javascript(String fun) throws IOException {
        Object result = new Object();
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");
        try {
            result = engine.eval(fun);
        } catch (final Exception e) {
            JDLogger.exception(e);
            return null;
        }
        if (result == null) {
            logger.warning("Javascript: result is null...");
            return null;
        }
        return result.toString();
    }

    private ArrayList<DownloadLink> loadcontainer(String theLink, String format) throws IOException, PluginException {
        ArrayList<DownloadLink> decryptedLinks = null;
        Browser brc = br.cloneBrowser();
        theLink = Encoding.htmlDecode(theLink);
        File file = null;
        URLConnectionAdapter con = brc.openGetConnection(theLink + format);
        if (con.getResponseCode() == 200) {
            file = JDUtilities.getResourceFile("tmp/linkstoreus/" + theLink.replaceAll("(:|/|=|\\?)", "") + format);
            if (file == null) return null;
            file.deleteOnExit();
            brc.downloadConnection(file, con);
            if (file != null && file.exists() && file.length() > 100) {
                decryptedLinks = JDUtilities.getController().getContainerLinks(file);
            }
        } else {
            con.disconnect();
            return null;
        }

        if (file != null && file.exists() && file.length() > 100) {
            if (decryptedLinks.size() > 0) return decryptedLinks;
        } else {
            return null;
        }
        return null;
    }
}