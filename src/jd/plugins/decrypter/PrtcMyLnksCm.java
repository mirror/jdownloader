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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "protect-my-links.com" }, urls = { "http://(www\\.)?(protect\\-my\\-links\\.com|i23\\.in)/([a-z0-9]+(/[a-z0-9]+)?\\.html|\\?id=[a-z0-9]+)" }, flags = { 0 })
public class PrtcMyLnksCm extends PluginForDecrypt {

    public PrtcMyLnksCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("i23.in/", "protect-my-links.com/");
        if (parameter.matches("http://(www\\.)?protect\\-my\\-links\\.com/\\?id=[a-z0-9]+")) parameter = "http://protect-my-links.com/" + new Regex(parameter, "([a-z0-9]+)$").getMatch(0) + ".html";
        br.setFollowRedirects(false);
        br.getPage(parameter);
        final String capPage = br.getRegex("\"(/handezSrc\\.php\\?id=\\d+)\"").getMatch(0);
        if (capPage == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final String fpName = br.getRegex("<title>Download ([^<>\"]*?)</title>").getMatch(0);
        Browser ajaxBr = br.cloneBrowser();
        ajaxBr.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        ajaxBr.getPage("http://protect-my-links.com" + capPage);
        final String gonum = ajaxBr.getRegex("\\'\\&gonum=(\\d+)\\',").getMatch(0);
        final String rcID = ajaxBr.getRegex("Recaptcha\\.create\\(\"([^<>\"]*?)\"").getMatch(0);
        if (rcID == null || gonum == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        /* recaptcha */
        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((jd.plugins.hoster.DirectHTTP) recplug).getReCaptcha(br);
        rc.setId(rcID);
        rc.load();
        for (int i = 0; i <= 5; i++) {
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode("recaptcha", cf, param);
            ajaxBr.postPage("http://protect-my-links.com/ajaxln.php", "recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c + "&id=" + new Regex(parameter, "protect\\-my\\-links\\.com/([a-z0-9]+).*?").getMatch(0) + "&gonum=" + gonum);
            if (ajaxBr.containsHTML(">Captcha not valid<")) {
                rc.reload();
                continue;
            }
            break;
        }
        if (ajaxBr.containsHTML(">Captcha not valid<")) throw new DecrypterException(DecrypterException.CAPTCHA);
        /* next */
        final String gotoPage = ajaxBr.getRegex("\"goto\":\"([^<>\"]*?)\"").getMatch(0);
        if (gotoPage == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        br.getHeaders().put("Referer", br.getHost());
        br.getPage("http://protect-my-links.com" + gotoPage.replace("\\", ""));

        if (!br.containsHTML("(Page protected by|JavaScript Encoder)")) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        /* decode js */
        br.getRequest().setHtmlCode(jsDecoder());
        /* parsing links */
        String[] links = br.getRegex("class=\"server\">.*?<a href=\"(.*?)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String psp : links) {
            if (psp.startsWith("http")) {
                decryptedLinks.add(createDownloadlink(psp));
            }
        }
        if (decryptedLinks.size() == 0) {
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private String jsDecoder() {
        String c = br.getRegex("javascript>c=\"(.*?)\";").getMatch(0);
        String x = br.getRegex("x\\(\"(.*?)\"\\)").getMatch(0);
        if (c == null || x == null) return null;
        String step1Str = step1(c);
        if (step1Str == null) return null;
        ArrayList<Integer> step2Lst = step2(step1Str);
        if (step2Lst == null || step2Lst.size() == 0) return null;
        return step3(step2Lst, x);
    }

    private static String step1(String varC) {
        String result = "";
        String d = "";

        for (int i = 0; i < varC.length(); i++) {
            if (i % 3 == 0) {
                d += "%";
            } else {
                d += varC.charAt(i);
            }
        }

        try {
            result = URLDecoder.decode(d, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return result;
    }

    private static ArrayList<Integer> step2(String step1Str) {
        // String to match : t=Array(63,2,21,25,58,38,59,47, ...);

        Pattern pattern = Pattern.compile("Array((.*?));");
        Matcher matcher = pattern.matcher(step1Str);

        ArrayList<Integer> intList = new ArrayList<Integer>();
        if (matcher.find()) {
            String tab[] = matcher.group().split("\\D");

            for (String str : tab) {
                if (str.length() != 0) {
                    intList.add(new Integer(str));
                }
            }
        } else {
            System.out.println("Not found.");
        }

        return intList;
    }

    private static String step3(ArrayList<Integer> step2Lst, String varParamX) {
        int l = varParamX.length();
        int b = 1024;
        int i;
        double j;
        StringBuilder globalStr = new StringBuilder();
        int p = 0;
        int s = 0;
        int w = 0;

        // It's important to cast to double !!
        for (j = Math.ceil((double) l / b); j > 0; j--) {
            StringBuilder r = new StringBuilder();

            for (i = Math.min(l, b); i > 0; i--, l--) {
                w |= (step2Lst.get((int) varParamX.charAt(p++) - 48)) << s;

                if (s != 0) {
                    r.append((char) (165 ^ w & 255));
                    w >>= 8;
                    s -= 2;
                } else {
                    s = 6;
                }
            }
            globalStr.append(r.toString());
        }

        return globalStr.toString();
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}