//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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
import java.util.List;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Base64;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mov-world.net", "xxx-4-free.net", "chili-warez.net" }, urls = { "http://(www\\.)?mov\\-world\\.net/(\\?id=\\d+|.*?/.*?\\d+\\.html)", "http://(www\\.)?xxx\\-4\\-free\\.net/.*?/.*?\\.html", "http://(www\\.)?chili\\-warez\\.net/[^<>\"]+\\d+\\.html" }, flags = { 0, 0, 0 })
public class MvWrldNt extends PluginForDecrypt {

    public MvWrldNt(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String UNSUPPORTEDLINKS = "http://(www\\.)?(xxx\\-4\\-free\\.net|mov\\-world\\.net|chili\\-warez\\.net)//?(news/|topliste/|premium_zugang|suche/|faq|pics/index|clips/index|movies/index|streams/index|stories/index|partner/anmelden|kontakt).*?\\.html";

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        if (parameter.matches(UNSUPPORTEDLINKS)) {
            logger.info("Invalid link: " + parameter);
            return decryptedLinks;
        }
        br.setFollowRedirects(true);
        try {
            br.getPage(parameter);
        } catch (final BrowserException e) {
            logger.info("Link offline (server error): " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML("<h1>Dieses Release ist nur noch bei <a")) {
            logger.info("Link offline (offline): " + parameter);
            return decryptedLinks;
        }
        if (br.getURL().contains("mov-world.net/error.html?error=404") || br.containsHTML(">404 Not Found<")) {
            logger.info("Link offline (error 404): " + parameter);
            return decryptedLinks;
        }

        final String MAINPAGE = "http://" + br.getHost();
        final String password = br.getRegex("class=\"password\">Password: (.*?)</p>").getMatch(0);
        ArrayList<String> pwList = null;
        String captchaUrl = br.getRegex("\"(/captcha/\\w+\\.gif)\"").getMatch(0);
        final Form captchaForm = br.getForm(0);
        if (captchaUrl == null && !captchaForm.containsHTML("Captcha")) { return null; }
        captchaUrl = captchaForm.getRegex("img src=\"(.*?)\"").getMatch(0);
        Browser brc = br.cloneBrowser();
        captchaUrl = MAINPAGE + captchaUrl;
        final File captchaFile = getLocalCaptchaFile();
        brc.getDownload(captchaFile, captchaUrl);
        String code = null;
        for (int i = 0; i <= 5; i++) {
            if (i > 0) {
                // Recognition failed, ask the user!
                code = getCaptchaCode(null, captchaFile, param);
            } else {
                code = getCaptchaCode("mov-world.net", captchaFile, param);
            }
            captchaForm.put("code", code);
            br.submitForm(captchaForm);
            if (br.containsHTML("\"Der Sicherheits Code")) {
                continue;
            }
            break;
        }
        if (br.containsHTML("\"Der Sicherheits Code")) { throw new DecrypterException(DecrypterException.CAPTCHA); }
        /* Base64 Decode */
        final byte[] cDecode = Base64.decodeFast(br.getRegex("html\":\"(.*?)\"").getMatch(0).replace("\\", "").toCharArray());
        if (cDecode == null || cDecode.length == 0) { return null; }
        final StringBuilder sb = new StringBuilder();
        for (int e : cDecode) {
            // Abweichung vom Standard Base64 Decoder
            if (e < 0) {
                e = e + 256;
            }
            sb.append(String.valueOf((char) e));
        }
        /* lzw Decompress */
        final String result = lzwDecompress(sb.toString());
        final String[] links = new Regex(result, "<td class=\"link\"><p><a href=\"([^<>]+)\" target=\"_blank\" title=\"[^<>]+\" class=\"(online|unknown)\"[^<>]+>\\d+</a>").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("mov-world.net: The content of this link is completly offline");
            logger.warning("mov-world.net: Please confirm via browser, and report any bugs to developement team. :" + parameter);
            return decryptedLinks;
        }
        boolean toManyLinks = false;
        if (links.length > 100) {
            toManyLinks = true;
        }
        progress.setRange(links.length);
        for (String dl : links) {
            if (!dl.startsWith("http")) {
                dl = MAINPAGE + dl;
            }
            brc = br.cloneBrowser();
            brc.setFollowRedirects(false);
            if (dl.startsWith(MAINPAGE)) {
                brc.getPage(dl);
                if (brc.getRequest().getHttpConnection().getResponseCode() == 302) {
                    dl = brc.getRedirectLocation();
                } else {
                    dl = brc.getRegex("iframe src=\"(.*?)\"").getMatch(0);
                }
            }
            progress.increase(1);
            if (dl == null) {
                continue;
            }
            final DownloadLink downLink = createDownloadlink(dl);
            if (toManyLinks) {
                downLink.setAvailable(true);
            }
            pwList = new ArrayList<String>();
            final String host = Browser.getHost(parameter);
            if (password != null && !password.equals("")) {
                pwList.add(password.trim());
            }
            if (host != null) {
                pwList.add(host.trim());
            }
            downLink.setSourcePluginPasswordList(pwList);
            try {
                distribute(downLink);
            } catch (final Throwable e) {
                /* does not exist in 09581 */
            }
            decryptedLinks.add(downLink);
        }
        return decryptedLinks;
    }

    public String lzwDecompress(final String a) throws Exception {
        final List<Integer> b = new ArrayList<Integer>();
        for (int i = 0, dict_count = 256, bits = 8, rest = 0, rest_length = 0; i < a.length(); i++) {
            rest = (rest << 8) + a.codePointAt(i);
            rest_length += 8;
            if (rest_length >= bits) {
                rest_length -= bits;
                b.add(rest >> rest_length);
                rest &= (1 << rest_length) - 1;
                dict_count++;
                if (dict_count >> bits > 0) {
                    bits++;
                }
            }
        }
        final List<String> c = new ArrayList<String>();
        for (int i = 0; i <= 255; i++) {
            c.add(String.valueOf((char) i));
        }
        final List<String> d = new ArrayList<String>();
        String element = "";
        String word = "";
        for (int i = 0; i <= b.size() - 1; i++) {
            if (b.get(i) >= c.size()) {
                element = word + word.charAt(0);
            } else {
                element = c.get(b.get(i));
            }
            d.add(element);
            if (i > 0) {
                c.add(word + element.charAt(0));
            }
            word = element;
        }
        final StringBuilder sb = new StringBuilder();
        for (final String e : d) {
            sb.append(e);
        }
        return sb.toString();
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}