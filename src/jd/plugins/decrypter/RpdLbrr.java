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

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.parser.html.InputField;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rapidlibrary.com" }, urls = { "http://rapidlibrary\\.(com|biz)/(download_file_i\\.php\\?.+|files/[^<>\"/]+\\.html)" }, flags = { 0 })
public class RpdLbrr extends PluginForDecrypt {

    private static AtomicBoolean decryptRunning = new AtomicBoolean(false);

    public RpdLbrr(PluginWrapper wrapper) {
        super(wrapper);

    }

    private final String OLDLINK = "http://rapidlibrary\\.biz/download_file_i\\.php\\?.+";

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        /**
         * only the first link shows a captcha. so we wait and queue parallel requests
         */
        final String crptlink = parameter.getCryptedUrl().replace("rdpidlibrary.com/", "rapidlibrary.biz/");
        try {
            waitQueue();
            if (crptlink.matches(OLDLINK)) {
                br.setCookiesExclusive(false);
                br.getPage(crptlink);
                String fpName = br.getRegex("<<title>File download:(.*?)from .*?</title>").getMatch(0);
                if (fpName == null) {
                    fpName = br.getRegex("<font class=\"texta\">(.*?)</font>").getMatch(0);
                    if (fpName == null) {
                        fpName = br.getRegex("<span style=\"font-size: 16px; color:#0374f1;\">.*?<b>(.*?)</b>").getMatch(0);
                    }
                }
                String pagepiece = br.getRegex("<span style=\"font-size:12px;color:#000000;\">(.*?)<hr width=100% noshade size=\"0\" color=").getMatch(0);
                if (pagepiece == null) pagepiece = br.getRegex("class=\"parts_div_one\"(.*?)</div><br>").getMatch(0);
                if (pagepiece == null) {
                    for (int i = 0; i <= 7; i++) {
                        Form captchaForm = br.getForms()[1];
                        if (captchaForm == null) {
                            logger.warning("Decrypter broken for link: " + crptlink);
                            return null;
                        }
                        captchaForm.setAction(br.getURL());
                        String captchaCode = getCaptchaCode("http://rapidlibrary.biz/code2.php", parameter);
                        InputField nv = new InputField("c_code", captchaCode);
                        captchaForm.addInputField(nv);
                        br.submitForm(captchaForm);
                        if (br.containsHTML("code2\\.php")) continue;
                        break;
                    }
                    if (br.containsHTML("code2\\.php")) throw new DecrypterException(DecrypterException.CAPTCHA);
                    pagepiece = br.getRegex("<span style=\"font-size:12px;color:#000000;\">(.*?)<hr width=100% noshade size=\"0\" color=").getMatch(0);
                    if (pagepiece == null) pagepiece = br.getRegex("class=\"parts_div_one\"(.*?)</div><br>").getMatch(0);
                    if (pagepiece == null) return null;
                    String[] links = HTMLParser.getHttpLinks(pagepiece, "");
                    if (links == null || links.length == 0) {
                        logger.warning("Decrypter broken for link: " + crptlink);
                        return null;
                    }
                    for (String finallink : links) {
                        decryptedLinks.add(createDownloadlink(finallink));
                    }
                }
                if (fpName != null && decryptedLinks.size() > 1) {
                    FilePackage fp = FilePackage.getInstance();
                    fp.setName(fpName.trim());
                    fp.addLinks(decryptedLinks);
                }
            } else {
                br.getPage(crptlink);
                if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
                if (br.containsHTML("Error file not found")) {
                    logger.info("Link offline: " + crptlink);
                    return decryptedLinks;
                }
                if (br.containsHTML(">In response to a complaint")) {
                    logger.info("Link offline: " + parameter);
                    return decryptedLinks;
                }
                String host = br.getRegex("height=\"16\">([^<>\"]*?)</span> <span class=\"size\"><span>size:</span>").getMatch(0);
                String finallink = getFinallink();
                // Maybe the sourc == the finallink so we might be able to skip the captcha sometimes
                if (finallink == null) finallink = br.getRegex("\"/source\\.php\\?file=[a-z0-9]+\\&sec=[a-z0-9]+\">([^<>\"]*?)</a>").getMatch(0);
                if (finallink == null || (host != null && !finallink.contains(Encoding.htmlDecode(host.trim()))) && br.containsHTML("code2\\.php")) {
                    for (int i = 0; i <= 7; i++) {
                        Form captchaForm = br.getForms()[1];
                        if (captchaForm == null) {
                            logger.warning("Decrypter broken for link: " + crptlink);
                            return null;
                        }
                        captchaForm.setAction(br.getURL());
                        String captchaCode = getCaptchaCode("http://rapidlibrary.biz/code2.php", parameter);
                        InputField nv = new InputField("c_code", captchaCode);
                        captchaForm.addInputField(nv);
                        br.submitForm(captchaForm);
                        if (br.containsHTML("code2\\.php")) continue;
                        break;
                    }
                    if (br.containsHTML("code2\\.php")) throw new DecrypterException(DecrypterException.CAPTCHA);
                    finallink = getFinallink();
                }
                if (finallink == null) {
                    logger.warning("Decrypter broken for link: " + crptlink);
                    return null;
                }
                if (!finallink.startsWith("http")) finallink = "http://" + finallink;
                decryptedLinks.add(createDownloadlink(finallink));
            }
        } finally {
            RpdLbrr.decryptRunning.set(false);
        }
        return decryptedLinks;
    }

    private String getFinallink() {
        return br.getRegex("class=\"line\"><div class=\"wrap-input\"><input onclick=\"this\\.select\\(\\);\" value=\"(http[^<>\"]*?)\"").getMatch(0);
    }

    private void waitQueue() throws InterruptedException {
        while (RpdLbrr.decryptRunning.get() == true)
            Thread.sleep(1000);
        RpdLbrr.decryptRunning.set(true);
    }

    /* NOTE: no override to keep compatible to old stable */
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}