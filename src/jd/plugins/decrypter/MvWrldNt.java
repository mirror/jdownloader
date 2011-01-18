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
import java.util.List;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Base64;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mov-world.net" }, urls = { "http://[\\w\\.]*?mov-world\\.net/.*?/.*?.html" }, flags = { 0 })
public class MvWrldNt extends PluginForDecrypt {

    private static String MAINPAGE = "http://mov-world.net";

    public MvWrldNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("<h1>Dieses Release ist nur noch bei <a")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String password = br.getRegex("class=\"password\">Password: (.*?)</p>").getMatch(0);
        String captchaUrl = br.getRegex("\"(/captcha/-[a-zA-Z0-9]+\\.gif)\"").getMatch(0);
        Form captchaForm = br.getForm(0);
        if (captchaUrl == null && !captchaForm.containsHTML("Captcha")) return null;
        captchaUrl = captchaForm.getRegex("img src=\"(.*?)\"").getMatch(0);
        Browser brc = br.cloneBrowser();
        captchaUrl = MAINPAGE + captchaUrl;
        File captchaFile = getLocalCaptchaFile();
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
            if (br.containsHTML("\"Der Sicherheits Code")) continue;
            break;
        }
        if (br.containsHTML("\"Der Sicherheits Code")) throw new DecrypterException(DecrypterException.CAPTCHA);
        /* Base64 Decode */
        byte[] cDecode = Base64.decodeFast(br.getRegex("html\":\"(.*?)\"").getMatch(0).replace("\\", "").toCharArray());
        if (cDecode == null || cDecode.length == 0) return null;
        StringBuilder sb = new StringBuilder();
        for (int e : cDecode) {
            // Abweichung vom Standard Base64 Decoder
            if (e < 0) e = e + 256;
            sb.append(String.valueOf((char)e));
        }
        /* lzw Decompress */
        String result = lzwDecompress(sb.toString());
        String[] links = new Regex(result, "href=\"(/.*?)\" target.*?>\\d+</a>").getColumn(0);
        if (links == null || links.length == 0) return null;
        progress.setRange(links.length);
        for (String dl : links) {
            brc.getPage(MAINPAGE + dl);
            if (brc.getRequest().getHttpConnection().getResponseCode() == 302) {
                dl = brc.getRequest().getHttpConnection().getHeaderField("Location");
            } else {
                dl = brc.getRegex("iframe src=\"(.*?)\"").getMatch(0);
            }
            progress.increase(1);
            if (dl == null) continue;
            DownloadLink downLink = createDownloadlink(dl);
            downLink.addSourcePluginPassword("mov-world.net");
            if (password != null && !password.equals("")) downLink.addSourcePluginPassword(password.trim());
            decryptedLinks.add(downLink);
        }
        return decryptedLinks;
    }

    public String lzwDecompress(String a) throws Exception {
        List<Integer> b = new ArrayList<Integer>();
        for (int i = 0, dict_count = 256, bits = 8, rest =0, rest_length = 0; i < a.length(); i++) {
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
        List<String> c = new ArrayList<String>();
        for (int i = 0; i <= 255; i++) {
            c.add(String.valueOf((char)i));
        }
        List<String> d = new ArrayList<String>();
        String element = new String();
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
        StringBuilder sb = new StringBuilder();
        for (String e : d) {
            sb.append(e);
        }
        return sb.toString();
    }
}
