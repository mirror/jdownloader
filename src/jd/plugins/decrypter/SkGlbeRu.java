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
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "skyglobe.ru" }, urls = { "http://[\\w\\.]*?skyglobe\\.ru/mp3/album/\\d+" }, flags = { 0 })
public class SkGlbeRu extends PluginForDecrypt {

    public SkGlbeRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setCustomCharset("windows-1251");
        br.setFollowRedirects(true);
        boolean failed = false;
        br.getPage(parameter);
        if (br.containsHTML("No htmlCode read")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String fpName = br.getRegex("<TITLE>Скачать альбом(.*?)бесплатной в формате mp3").getMatch(0);
        System.out.print(br.toString());
        String[] linkinformation = br.getRegex("<tr(.*?class=\"right_border\".*?)</tr>").getColumn(0);
        if (linkinformation == null || linkinformation.length == 0) {
            failed = true;
            linkinformation = br.getRegex("\"(/mp3/track/\\d+)").getColumn(0);
        }
        if (linkinformation.length == 0) return null;
        for (String data : linkinformation) {
            if (failed) {
                decryptedLinks.add(createDownloadlink("http://skyglobe.ru" + data));
            } else {
                String filename = new Regex(data, "href=\"/mp3/track.*?\">(.*?)</a>").getMatch(0);
                String filesize = new Regex(data, "<td class=\"right_border\">(.*?)</td>").getMatch(0);
                String dlink = new Regex(data, "\"(/mp3/track/\\d+)").getMatch(0);
                if (dlink == null) return null;
                DownloadLink aLink = createDownloadlink("http://skyglobe.ru" + dlink);
                if (filename != null) aLink.setName(filename.trim() + ".mp3");
                if (filesize != null) aLink.setDownloadSize(SizeFormatter.getSize(filesize));
                if (filename != null && filesize != null) aLink.setAvailable(true);
                decryptedLinks.add(aLink);
            }
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
