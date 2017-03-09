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
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "file.karelia.ru" }, urls = { "https?://(?:www\\.)?file\\.(?:karelia|sampo)\\.ru/[a-z0-9]+/" })
public class FileKareliaRuDecrypter extends PluginForDecrypt {

    public FileKareliaRuDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static boolean isOffline(final Browser br) {
        return br.containsHTML(">Файла не существует или он был удалён с сервера") || br.getHttpConnection().getResponseCode() == 404;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String fid = new Regex(param.toString(), "([a-z0-9]+)/$").getMatch(0);
        final String parameter = String.format("http://file.karelia.ru/%s/", fid);
        br.getPage(parameter);
        if (isOffline(this.br)) {
            final DownloadLink dl = this.createOfflinelink(parameter);
            dl.setFinalFileName(fid);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        final String[] fileHtmls = this.br.getRegex("class=\"modernFileWrap avaliableFile\"(.*?)select_to_zip selected").getColumn(0);
        final boolean singlefile = fileHtmls.length == 1;
        for (final String filehtml : fileHtmls) {
            final String singleLink = new Regex(filehtml, "data\\-href=\"(http[^<>\"]+)").getMatch(0);
            String filename = new Regex(filehtml, "title=\"([^<>\"]+)").getMatch(0);
            final String filesize = new Regex(filehtml, "data\\-filesize=\"([^<>\"]+)").getMatch(0);
            if (singleLink == null || filename == null) {
                continue;
            }

            final DownloadLink dl = createDownloadlink(parameter.replace("file.karelia.ru/", "file.kareliadecrypted.ru/") + System.currentTimeMillis() + new Random().nextInt(100000));
            filename = Encoding.htmlDecode(filename);
            dl.setFinalFileName(filename);
            dl.setProperty("plainfilename", filename);
            dl.setLinkID(fid + filename);
            if (filesize != null) {
                jd.plugins.hoster.FileKareliaRu.setFilesize(dl, filesize);
            }
            dl.setProperty("partlink", true);
            if (singlefile) {
                dl.setProperty("singlefile", true);
            }
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }

        /* Only add general zip url if we found nothing above or more than 1 item. */
        if (decryptedLinks.size() != 1) {
            final DownloadLink dl = createDownloadlink(parameter.replace("file.karelia.ru/", "file.kareliadecrypted.ru/") + System.currentTimeMillis() + new Random().nextInt(100000));
            dl.setFinalFileName(fid + ".zip");
            dl.setLinkID(fid);
            final String filesize = br.getRegex("общим размером <strong id=\"totalSize\">([^<>\"]*?)</strong>").getMatch(0);
            if (filesize != null) {
                dl.setDownloadSize(SizeFormatter.getSize(Encoding.htmlDecode(filesize).replace("Гбайта", "GB").replace("Мбайт", "MB").replace("Кбайта", "kb")));
            }
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
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