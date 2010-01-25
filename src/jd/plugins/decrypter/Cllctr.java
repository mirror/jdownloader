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

import java.util.ArrayList;
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "collectr.net" }, urls = { "http://[\\w\\.]*?collectr\\.net/(out/(\\d+/)?\\d+|links/\\w+)" }, flags = { 0 })
public class Cllctr extends PluginForDecrypt {

    private static final String PAT_SUPPORTED_OUT = "http://[\\w\\.]*?collectr\\.net/out/(\\d+/)?\\d+";
    private static final String PATTERN_AB_18 = "Hast du das 18 Lebensjahr bereits abgeschlossen\\?.*";
    private static final String PAT_SUPPORTED_FOLDER = "http://[\\w\\.]*?collectr\\.net/links/(\\w+)";
    private static final String PATTERN_GETLINK = "<a href=\"javascript:getLink\\(lnk\\[(\\d+)\\]\\)\">(.+?)  #\\d+</a>";
    private static final String PATTERN_SAPCHA = "useSaptcha\\s+=\\s+(\\d+);";
    private static final String PATTERN_FOLDERNAME = "<span id=\"title\">(.+?)</span>";
    private static final String PATTERN_DURL = "<key>(.+?)</key>";
    private static final String JAMES_GETLINK = "http://collectr.net/james.php?do=getLink";
    private static final String JAMES_SAPTCHA = "http://collectr.net/james.php?do=saptcha";

    public Cllctr(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String url = param.toString();
        br.getPage(url);
        if (new Regex(url, PAT_SUPPORTED_OUT).matches()) {
            Form form = br.getForm(0);

            if (br.containsHTML(PATTERN_AB_18)) {
                form.put("o18", "o18=true");
                br.submitForm(form);
            }

            String links[] = br.getRegex("<iframe id=\"displayPage\" src=\"(.*?)\" name=\"displayPage\"").getColumn(0);
            progress.setRange(links.length);

            for (String element : links) {
                decryptedLinks.add(createDownloadlink(element));
                progress.increase(1);
            }
        } else if (new Regex(url, PAT_SUPPORTED_FOLDER).matches()) {
            String saptcha = br.getRegex(PATTERN_SAPCHA).getMatch(0);
            String ordner = new Regex(url, PAT_SUPPORTED_FOLDER).getMatch(0);
            FilePackage fp = FilePackage.getInstance();
            fp.setName(br.getRegex(PATTERN_FOLDERNAME).getMatch(0));
            String[] links = br.getRegex(PATTERN_GETLINK).getColumn(0);
            if (saptcha != null) {
                // Captcha on
                String captchaCode = getCaptchaCode("http://collectr.net/img/saptcha" + saptcha + ".gif", param);
                LinkedHashMap<String, String> post = new LinkedHashMap<String, String>();
                post.put("saptcha", captchaCode);
                post.put("id", saptcha);
                post.put("ordner", ordner);
                br.postPage(JAMES_SAPTCHA, post);
            }
            for (String link : links) {
                LinkedHashMap<String, String> post = new LinkedHashMap<String, String>();
                post.put("id", link);
                post.put("ordner", ordner);
                br.postPage(JAMES_GETLINK, post);
                String dUrl = br.getRegex(PATTERN_DURL).getMatch(0);
                DownloadLink dLink = createDownloadlink(dUrl);
                dLink.setFilePackage(fp);
                decryptedLinks.add(dLink);
            }
        }
        return decryptedLinks;
    }

}
