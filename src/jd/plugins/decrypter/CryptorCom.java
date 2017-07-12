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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "cryptor.to" }, urls = { "https?://(?:www\\.)?cryptor\\.to/folder/[A-Za-z0-9\\-_]+" })
public class CryptorCom extends PluginForDecrypt {
    public CryptorCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String                   html_passwordrequired = "folder_access";
    private static AtomicReference<String> LASTSESSIONPASSWORD   = new AtomicReference<String>();

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        this.br.setFollowRedirects(true);
        this.br.setAllowedResponseCodes(500);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 500 || this.br.containsHTML("Ordner nicht verfügbar")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final List<String> passwords = getPreSetPasswords();
        final String lastSessionPassword = CryptorCom.LASTSESSIONPASSWORD.get();
        if (lastSessionPassword != null && !passwords.contains(lastSessionPassword)) {
            passwords.add(lastSessionPassword);
        }
        if (this.br.containsHTML(html_passwordrequired)) {
            boolean failed = true;
            for (int i = 0; i <= 3; i++) {
                String postData = "";
                if (this.br.containsHTML("\"g\\-recaptcha\"")) {
                    final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                    postData += "g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response);
                }
                final String passCode;
                if (this.br.containsHTML("\"folder_access_password_check\"")) {
                    if (passwords.size() > 0) {
                        passCode = passwords.remove(0);
                        i = 0;
                    } else {
                        passCode = getUserInput("Password?", param);
                    }
                    if (postData.length() == 0) {
                        postData += "folder_access%5Bpassword_check%5D=" + Encoding.urlEncode(passCode);
                    } else {
                        postData += "&folder_access%5Bpassword_check%5D=" + Encoding.urlEncode(passCode);
                    }
                } else {
                    passCode = null;
                    if (postData.length() == 0) {
                        postData += "folder_access";
                    } else {
                        postData += "&folder_access";
                    }
                }
                if (postData.length() == 0) {
                    postData += "folder_access%5Bsubmit%5D=";
                } else {
                    postData += "&folder_access%5Bsubmit%5D=";
                }
                this.br.postPage(this.br.getURL(), postData);
                if (this.br.containsHTML(html_passwordrequired)) {
                    if (passCode != null) {
                        CryptorCom.LASTSESSIONPASSWORD.set(passCode);
                    }
                    continue;
                }
                failed = false;
                break;
            }
            if (failed) {
                /* Captcha cannot really fail so most likely a fail here means that the user entered a wrong password too often! */
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
        }
        this.br.setFollowRedirects(false);
        final String fpName = br.getRegex("class=\"text\\-center\">[\t\n\r ]*?<h1>([^<>]+)</h1>").getMatch(0);
        final String mirrors[][] = br.getRegex("option value=\"(\\d+)\"\\s*(selected)?\\s*>\\s*(.*?)\\s*<").getMatches();
        if (mirrors == null || mirrors.length == 0) {
            decryptedLinks.addAll(parsePage(br, param));
        } else {
            for (final String mirror[] : mirrors) {
                if (!"selected".equals(mirror[1])) {
                    br.getPage(parameter + "/" + mirror[0]);
                }
                decryptedLinks.addAll(parsePage(br, param));
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private List<DownloadLink> parsePage(Browser br, CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String cnlSource = br.getRegex("'source'\\s*:\\s*'(.*?)'").getMatch(0);
        final String cnlJk = br.getRegex("'jk'\\s*:\\s*'(.*?)(?<!\\\\)'").getMatch(0);
        final String cnlCrypted = br.getRegex("'crypted'\\s*:\\s*'(.*?)'").getMatch(0);
        if (cnlJk != null && cnlCrypted != null) {
            if (cnlSource == null) {
                cnlSource = param.getCryptedUrl();
            }
            decryptedLinks.add(DummyCNL.createDummyCNL(cnlCrypted, cnlJk.replaceAll("\\\\", ""), null, cnlSource));
        }
        final String[] links = br.getRegex("\"(/(link|dl)/[A-Za-z0-9]+\\-)\"").getRow(0);
        if (links != null) {
            for (final String singleLink : links) {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user");
                    return decryptedLinks;
                }
                final Browser brc = br.cloneBrowser();
                brc.getPage(singleLink);
                final String finallink = brc.getRegex("iframe allowfullscreen=\"true\" noresize=\"\" src=\"(http[^\"]+)").getMatch(0);
                if (finallink != null) {
                    decryptedLinks.add(createDownloadlink(finallink));
                }
            }
        }
        return decryptedLinks;
    }
}
