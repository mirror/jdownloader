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
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "file.karelia.ru" }, urls = { "http://(www\\.)?file\\.karelia\\.ru/[a-z0-9]+/" }, flags = { 0 })
public class FileKareliaRuDecrypter extends PluginForDecrypt {

    public FileKareliaRuDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        final String fid = new Regex(parameter, "([a-z0-9]+)/$").getMatch(0);
        br.getPage(parameter);
        if (br.containsHTML(">Файла не существует или он был удалён с сервера")) {
            logger.info("Link offline: " + parameter);
            final DownloadLink dl = createDownloadlink(parameter.replace("file.karelia.ru/", "file.kareliadecrypted.ru/") + System.currentTimeMillis() + new Random().nextInt(100000));
            dl.setFinalFileName(fid + ".zip");
            dl.setAvailable(false);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        final String[][] fileInfo = br.getRegex("<a  href=\"http://[a-z0-9]+\\.file\\.karelia\\.ru/" + fid + "/[a-z0-9]+/[a-z0-9]+/([^<>\"/]*?)\">[\t\n\r ]+<span class=\"filename binary\">[^<>\"/]*?</span><i>\\&mdash;\\&nbsp;\\&nbsp;([^<>\"/]*?)</i></a>").getMatches();
        // Decrypter could also be broken but we can't know it so no exception!
        if (fileInfo != null && fileInfo.length != 0) {
            for (final String singleLink[] : fileInfo) {
                final DownloadLink dl = createDownloadlink(parameter.replace("file.karelia.ru/", "file.kareliadecrypted.ru/") + System.currentTimeMillis() + new Random().nextInt(100000));
                dl.setFinalFileName(Encoding.htmlDecode(singleLink[0]));
                dl.setProperty("plainfilename", singleLink[0]);
                dl.setDownloadSize(SizeFormatter.getSize(Encoding.htmlDecode(singleLink[1].replace("Гбайта", "GB").replace("Мбайта", "MB").replace("Кбайта", "kb"))));
                dl.setProperty("partlink", true);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        }
        final DownloadLink dl = createDownloadlink(parameter.replace("file.karelia.ru/", "file.kareliadecrypted.ru/") + System.currentTimeMillis() + new Random().nextInt(100000));
        dl.setFinalFileName(fid + ".zip");
        final String filesize = br.getRegex("общим размером <strong id=\"totalSize\">([^<>\"]*?)</strong>").getMatch(0);
        if (filesize != null) dl.setDownloadSize(SizeFormatter.getSize(Encoding.htmlDecode(filesize).replace("Гбайта", "GB").replace("Мбайт", "MB").replace("Кбайта", "kb")));
        dl.setAvailable(true);
        decryptedLinks.add(dl);
        FilePackage fp = FilePackage.getInstance();
        fp.setName(fid);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}