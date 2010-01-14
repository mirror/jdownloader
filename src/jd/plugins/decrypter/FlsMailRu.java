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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "files.mail.ru" }, urls = { "http://[\\w\\.]*?files\\.mail\\.ru/[A-Z0-9]+" }, flags = { 0 })
public class FlsMailRu extends PluginForDecrypt {

    public FlsMailRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        // At the moment jd gets the russian version of the site. Errorhandling
        // also works for English but filesize handling doesn't so if this
        // plugin get's broken that's on of the first things to check
        br.getPage(parameter);
        // Errorhandling for offline folders
        if (br.containsHTML("(was not found|were deleted by sender|Не найдено файлов, отправленных с кодом|<b>Ошибка</b>)")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String[] linkinformation = br.getRegex("<td class=\"name\">(.*?)<td class=\"do\">").getColumn(0);
        if (linkinformation.length == 0) return null;
        for (String info : linkinformation) {
            String directlink = new Regex(info, "\"(http://.*?\\.files\\.mail\\.ru/.*?/.*?)\"").getMatch(0);
            String filename = new Regex(info, "href=\".*?onclick=\"return.*?\">(.*?)<").getMatch(0);
            if (directlink == null || filename == null) return null;
            String filesize = new Regex(info, "<td>(.*?{1,15})</td>").getMatch(0);
            DownloadLink finallink = createDownloadlink(directlink.replace("files.mail.ru", "wge4zu4rjfsdehehztiuxw"));
            // Maybe that helps id jd gets the english version of the site!
            if (filesize != null) {
                if (!filesize.contains("MB")) {
                    filesize = filesize.replace("Г", "G");
                    filesize = filesize.replace("М", "M");
                    filesize = filesize.replace("к", "k");
                    filesize = filesize.replace("Б", "");
                    filesize = filesize + "b";
                }
                finallink.setDownloadSize(Regex.getSize(filesize));
            }
            finallink.setFinalFileName(filename);
            finallink.setAvailable(true);
            // Property is still unused...
            finallink.setProperty("name2search", filename);
            decryptedLinks.add(finallink);
        }

        return decryptedLinks;
    }

}
