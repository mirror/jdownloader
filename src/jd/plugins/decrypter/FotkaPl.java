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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fotka.pl" }, urls = { "http://[\\w\\.]*?fotka\\.pl/(profil/\\w+/albumy/\\d+,\\w+(/\\d+)?|duza_fotka\\.php\\?fotka_id=\\d+\\&owner_id=\\d+)" }, flags = { 0 })
public class FotkaPl extends PluginForDecrypt {

    public FotkaPl(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String MAINPAGE        = "http://www.fotka.pl";
    private static final String FINALLINKREGEX1 = "id=\"zdjecie\" src=\"(http.*?)\"";
    private static final String FINALLINKREGEX2 = "\"(http://\\w+\\.asteroid\\.pl/.*?fotka\\.pl\\.s\\d+.*?/.*?)\"";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("Nie ma profilu, którego szukasz")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String profileName = new Regex(parameter, "fotka\\.pl/profil/(.*?)/albumy").getMatch(0);
        if (parameter.matches(".*?fotka\\.pl/profil/\\w+/albumy/\\d+,.*?/\\d+")) {
            logger.info("The user added a link that contains only one picture, starting to decrypt...");
            String finallink = br.getRegex(FINALLINKREGEX1).getMatch(0);
            if (finallink == null) finallink = br.getRegex(FINALLINKREGEX2).getMatch(0);
            if (finallink == null) return null;
            decryptedLinks.add(createDownloadlink("directhttp://" + Encoding.htmlDecode(finallink)));
        } else if (parameter.matches(".*?")) {
            logger.info("The user added a link that contains only one picture, starting to decrypt...");
            String finallink = br.getRegex("\\}\\)\\.attr\\(\\'src\\', \\'(http://.*?)\\?AWSAccessKeyId").getMatch(0);
            if (finallink == null) finallink = br.getRegex("id=\"fotka\" src=\"(http://.*?)\\?AWSAccessKeyId").getMatch(0);
            if (finallink == null) return null;
            decryptedLinks.add(createDownloadlink("directhttp://" + Encoding.htmlDecode(finallink)));
        } else {
            logger.info("The user added a link which should contain multiple links, starting to decrypt...");
            String[] links = br.getRegex("class=\"podpis html\" onmouseover=\"show_podglad\\(this, false\\);\" onclick=\"location\\.href=\\'(/profil/\\d+/albumy/.*?/\\d+(,)?)\\'").getColumn(0);
            if (links == null || links.length == 0) links = br.getRegex("px auto 2px auto;\">[\t\n\r ]+<a href=\"(/profil/\\w+/albumy/.*?/\\d+(,)?)\"").getColumn(0);
            if (links == null || links.length == 0) return null;
            progress.setRange(links.length);
            for (String singlelink : links) {
                br.getPage(MAINPAGE + singlelink);
                String finallink = br.getRegex(FINALLINKREGEX1).getMatch(0);
                if (finallink == null) finallink = br.getRegex(FINALLINKREGEX2).getMatch(0);
                if (finallink == null) return null;
                decryptedLinks.add(createDownloadlink("directhttp://" + Encoding.htmlDecode(finallink)));
                progress.increase(1);
            }
        }
        if (profileName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(profileName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
