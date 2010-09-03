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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "protect-my-links.com" }, urls = { "http://[\\w\\.]*?protect-my-links\\.com/\\?id=[a-z0-9]+" }, flags = { 0 })
public class PrtcMyLnksCm extends PluginForDecrypt {

    public PrtcMyLnksCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        FilePackage fp = FilePackage.getInstance();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        boolean decrypterBroken = false;
        if (decrypterBroken) return null;

        /* Error handling */
        if (br.containsHTML("This data has been removed by the owner")) {
            logger.warning("Wrong link");
            logger.warning(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            return new ArrayList<DownloadLink>();
        }

        /* File package handling */
        for (int i = 0; i <= 5; i++) {
            Form captchaForm = br.getForm(1);
            if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String passCode = null;
            String captchalink0 = br.getRegex("src=\"(mUSystem.*?)\"").getMatch(0);
            String captchalink = "http://protect-my-links.com/" + captchalink0;
            if (captchalink0.contains("null")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String code = getCaptchaCode(captchalink, param);
            captchaForm.put("captcha", code);

            if (br.containsHTML("Password :")) {
                passCode = getUserInput(null, param);
                captchaForm.put("passwd", passCode);
            }
            br.submitForm(captchaForm);
            if (br.containsHTML("Captcha is not valid") || br.containsHTML("Password is not valid")) continue;
            break;
        }
        if (br.containsHTML("Captcha is not valid")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String fpName = br.getRegex("h1 class=\"pmclass\">(.*?)</h1></td>").getMatch(0).trim();
        fp.setName(fpName);
        String[] links = br.getRegex("><a href='(/\\?p=.*?)'").getColumn(0);
        if (links == null || links.length == 0) return null;
        progress.setRange(links.length);
        for (String psp : links) {
            // Fixed, thx to goodgood.51@gmail.com
            br.getPage("http://protect-my-links.com" + psp);
            String c = br.getRegex("javascript>c=\"(.*?)\";").getMatch(0);
            String x = br.getRegex("x\\(\"(.*?)\"\\)").getMatch(0);
            if (c == null || x == null) return null;
            String step1Str = step1(c);
            if (step1Str == null) return null;
            ArrayList<Integer> step2Lst = step2(step1Str);
            if (step2Lst == null || step2Lst.size() == 0) return null;
            String step3Str = step3(step2Lst, x);
            if (step3Str == null) return null;
            String finallink = step4(step3Str);
            if (finallink == null) return null;
            decryptedLinks.add(createDownloadlink(finallink));
            progress.increase(1);
        }
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    public static String step1(String varC) {
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

    public static ArrayList<Integer> step2(String step1Str) {
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

    public static String step3(ArrayList<Integer> step2Lst, String varParamX) {
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

    public static String step4(String step3Str) {
        String result = "";

        Pattern pattern = Pattern.compile("src=\"(.*?)\">");
        Matcher matcher = pattern.matcher(step3Str);

        int i = 1;
        while (matcher.find()) {
            String tab[] = matcher.group().split("\"");

            if (i == 2) {
                result = tab[1];
            }

            i++;
        }

        return result;
    }
}
