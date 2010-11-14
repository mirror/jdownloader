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
import java.util.Random;
import java.util.regex.Pattern;

import org.appwork.utils.Regex;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.FilePackage;
import jd.plugins.DecrypterException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ddl-music.org" }, urls = { "http://(www\\.)?ddl-music\\.org/(download/\\d+/.*?/|download/links/[a-z0-9]+/(mirror/\\d+/)?)" }, flags = { 0 })
public class DDLMscrg extends PluginForDecrypt {
    private static final String DECRYPTER_DDLMSC_MAIN  = "http://(www\\.)?ddl-music\\.org/download/\\d+/.*?/";
    private static final String DECRYPTER_DDLMSC_CRYPT = "http://(www\\.)?ddl-music\\.org/download/links/[a-z0-9]+/(mirror/\\d+/)?";
    private static final String CAPTCHATEXT            = "captcha\\.php\\?id=";

    public DDLMscrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (parameter.matches(DECRYPTER_DDLMSC_CRYPT)) {
            logger.info("The user added a DECRYPTER_DDLMSC_CRYPT link...");
            int add = 0;
            for (int i = 1; i < 10; i++) {
                br.getPage(parameter);
                try {
                    Thread.sleep(3000 + add);
                } catch (InterruptedException e) {
                }
                if (!br.containsHTML(CAPTCHATEXT)) return null;
                String captchaUrl = "http://ddl-music.org/captcha.php?id=" + new Regex(parameter, "ddl-music\\.org/download/links/([a-z0-9]+)/").getMatch(0) + "&rand=" + new Random().nextInt(1000);
                String code = getCaptchaCode(captchaUrl, param);
                br.postPage(parameter, "sent=1&captcha=" + code);
                if (!br.containsHTML(">Sicherheitscode nicht korrekt\\!<") && !br.containsHTML("captcha.php?id=")) {
                    break;
                } else {
                    if (i >= 8) throw new DecrypterException(DecrypterException.CAPTCHA);
                }
                add += 500;
            }
            String[] allLinks = br.getRegex("<tr class=\"download_links_parts\">.*?<a href=\"(.*?)\"").getColumn(0);
            if (allLinks == null || allLinks.length == 0) {
                logger.warning("Could not find the links...");
                return null;
            }
            for (String aLink : allLinks)
                decryptedLinks.add(createDownloadlink(aLink));
        } else if (parameter.matches(DECRYPTER_DDLMSC_MAIN)) {
            String fpName = br.getRegex("<title>DDL-Music v3.0 // Eric Clapton - (.*?)</title>").getMatch(0);
            if (fpName == null) fpName = br.getRegex("\\'\\);\">\\&gt;\\&gt; (.*?)</div>").getMatch(0);
            logger.info("The user added a DECRYPTER_DDLMSC_MAIN link...");
            br.getPage(parameter);
            String password = br.getRegex(Pattern.compile("<b>Passwort:</b> <i>(.*?)</i><br />", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
            if (password != null && password.contains("(kein Passwort)")) {
                password = null;
            }
            String allLinks[] = br.getRegex("\"(download/links/[a-z0-9]+/(mirror/\\d+/)?)\"").getColumn(0);
            if (allLinks == null || allLinks.length == 0) {
                logger.warning("Couldn't find any links...");
                return null;
            }
            for (String singleLink : allLinks) {
                DownloadLink dLink = createDownloadlink("http://ddl-music.org/" + singleLink);
                if (password != null) dLink.addSourcePluginPassword(password);
                decryptedLinks.add(dLink);
            }
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                fp.addLinks(decryptedLinks);
            }
        }

        return decryptedLinks;
    }

}
