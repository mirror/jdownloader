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
import java.util.ArrayList;
import java.util.Arrays;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "anime-loads.org" }, urls = { "http://[\\w\\.]*?anime\\-loads\\.org/(redirect/\\d+/[a-z0-9]+|media/\\d+)" }, flags = { 0 })
public class NmLdsrg extends PluginForDecrypt {

    public NmLdsrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static boolean loaded = false;

    /*
     * Note: FilePackage gets overridden when crypt-it.com (link protection service) used. Old posts + streaming links still get caught.
     */

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> links = new ArrayList<String>();
        ArrayList<String> passwords = new ArrayList<String>();
        passwords.add("www.anime-loads.org");
        String parameter = param.toString();
        String fpName = null;
        br.setCookiesExclusive(true);
        if (!loaded) {
            JDUtilities.getPluginForHost("mediafire.com");
            loaded = true;
        }
        br.getHeaders().put("User-Agent", jd.plugins.hoster.MediafireCom.stringUserAgent());
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        if (parameter.contains("/redirect/")) {
            links.add(parameter);
        } else {
            br.getPage(parameter);
            if (br.containsHTML("Link existiert nicht")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            fpName = br.getRegex(":: (.*?)</span></h2>").getMatch(0);
            if (fpName == null) fpName = "No Title";
            String[] continueLinks = br.getRegex("\"(http://(www\\.)?anime\\-loads\\.org/redirect/\\d+/[a-z0-9]+)\"").getColumn(0);
            if (continueLinks == null || continueLinks.length == 0) return null;
            if (continueLinks != null && continueLinks.length != 0) {
                links.addAll(Arrays.asList(continueLinks));
            }
        }

        FilePackage fp = FilePackage.getInstance();
        fp.setProperty("ALLOW_MERGE", true);
        if (fpName != null) {
            fp.setName(fpName.trim());
        }

        for (final String link : links) {
            Browser br2 = br.cloneBrowser();
            if (!parameter.contains("/redirect/")) {
                br2.getPage(link);
            }
            if (br2.containsHTML("No htmlCode read")) {
                logger.info("Link offline: " + link);
                continue;
            }

            final Browser ajax = br2.cloneBrowser();
            final Browser adbr = br2.cloneBrowser();

            ajax.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            ajax.getHeaders().put("Accept-Charset", null);
            ajax.getHeaders().put("Cache-Control", null);
            ajax.getHeaders().put("Pragma", null);
            ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");

            // they check if you hit up this step or not!!
            // <iframe id="ad" scrolling="auto" src="http://www.anime-loads.org/redirect/animemanga" frameborder="0"
            // allowtransparency="true"></iframe>
            String ad_redirect = br2.getRegex("<iframe[^>]+id=\"ad\"[^>]+src=(\"|')?((https?://(www\\.)?anime\\-loads\\.org)?/redirect/[a-z]+)").getMatch(1);
            if (ad_redirect == null) {
                logger.warning("Could not find 'ad_redirect' : " + br.getURL());
            } else {
                adbr.getPage(ad_redirect);
            }

            // disabled by raztoki 20130603, this requires the same captcha method as web!
            // Addition from psp: Without CNL2 usage in this decrypter, this problem is "Solved": http://svn.jdownloader.org/issues/7177
            // Try adding links via CNL
            // ajax.postPage(br.getURL(), "action=cnl");
            // final String jk = ajax.getRegex("name=\"jk\" value=\"([^<>\"]*?)\"").getMatch(0);
            // final String crypted = ajax.getRegex("name=\"crypted\" value=\"([^<>\"]*?)\"").getMatch(0);
            // final String passwrds = ajax.getRegex("name=\"passwords\" value=\"([^<>\"]*?)\"").getMatch(0);
            // final String source = ajax.getRegex("name=\"source\" value=\"([^<>\"]*?)\"").getMatch(0);
            // if (jk != null && crypted != null) {
            // String cnl2post = "jk=" + Encoding.urlEncode(jk) + "&crypted=" + Encoding.urlEncode(crypted);
            // if (passwrds != null) cnl2post += "&passwords=" + Encoding.urlEncode(source);
            // if (source != null) cnl2post += "&source=" + Encoding.urlEncode(source);
            // try {
            // br.getHeaders().put("jd.randomNumber", System.getProperty("jd.randomNumber"));
            // br.postPage("http://127.0.0.1:9666/flash/addcrypted2", cnl2post);
            // if (br.containsHTML("success")) {
            // logger.info("CNL2 = works!");
            // // Add this to prevent out of date errors because links are added via CNL, decryptedLinks list will be empty!
            // decryptedLinks.add(createDownloadlink("http://dummylink.de/dg5649hz09tjkhomdfrlmgoelrhmk"));
            // continue;
            // }
            // if (br.containsHTML("^failed")) {
            // logger.warning("anime-loads.org: CNL2 Postrequest was failed! Please upload now a logfile, contact our support and add this loglink to your bugreport!");
            // logger.warning("anime-loads.org: CNL2 Message: " + br.toString());
            // }
            // } catch (Throwable e) {
            // logger.info("anime-loads.org: ExternInterface(CNL2) is disabled!");
            // }
            // br.getPage(link);
            // }
            // br.getHeaders().put("jd.randomNumber", "");
            // CNL failed? Add links via webdecryption!
            final String rcID = br2.getRegex("google\\.com/recaptcha/api/noscript\\?k=([^<>\"]*?)\"").getMatch(0);
            if (rcID == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((jd.plugins.hoster.DirectHTTP) recplug).getReCaptcha(br);
            rc.setId(rcID);
            rc.load();
            for (int i = 0; i <= 3; i++) {
                final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode(cf, param);
                ajax.postPage(br2.getURL(), "action=web&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c));
                if (ajax.containsHTML("ok\":false")) {
                    rc.reload();
                    continue;
                }
                break;
            }
            if (ajax.containsHTML("ok\":false")) {
                logger.info("Captcha wrong, stopping...");
                throw new DecrypterException(DecrypterException.CAPTCHA);
            }
            try {
                validateLastChallengeResponse();
            } catch (final Throwable e) {
            }
            String dllink = ajax.getRegex("\"response\":\\[?\"([^<>\"]*?)\"").getMatch(0);
            // Links can fail for multiple reasons so let's skip them
            if (dllink == null) {
                logger.info("Found a dead link: " + link);
                logger.info("Mainlink: " + parameter);
                continue;
            }
            dllink = dllink.replace("\\", "");
            if (!dllink.startsWith("http")) dllink = Encoding.Base64Decode(dllink);
            final DownloadLink dl = createDownloadlink(Encoding.htmlDecode(dllink.replace("\\", "")));
            fp.add(dl);
            dl.setSourcePluginPasswordList(passwords);
            try {
                distribute(dl);
            } catch (final Throwable e) {
                /* does not exist in 09581 */
            }
            decryptedLinks.add(dl);
        }
        if (decryptedLinks.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        if (links.size() > 0 && decryptedLinks.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}