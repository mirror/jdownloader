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

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "file.karelia.ru" }, urls = { "https?://(?:www\\.)?file\\.(?:karelia|sampo)\\.ru/[a-z0-9]+/" })
public class FileKareliaRuDecrypter extends PluginForDecrypt {
    public FileKareliaRuDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static boolean isOffline(final Browser br) {
        return br.containsHTML("(?i)>\\s*Файла не существует или он был удалён с сервера") || br.getHttpConnection().getResponseCode() == 404;
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String folderid = new Regex(param.toString(), "([a-z0-9]+)/$").getMatch(0);
        final String parameter = String.format("http://file.karelia.ru/%s/", folderid);
        br.getPage(parameter);
        if (isOffline(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String[] fileHtmls = this.br.getRegex("<a href=\"#\"\\s*data-href.*?select_to_zip selected").getColumn(-1);
        final boolean singlefile = fileHtmls.length == 1;
        for (final String filehtml : fileHtmls) {
            final String singleLink = new Regex(filehtml, "data\\-href=\"(http[^<>\"]+)").getMatch(0);
            String filename = new Regex(filehtml, "title=\"([^<>\"]+)").getMatch(0);
            final String filesize = new Regex(filehtml, "data\\-filesize=\"([^<>\"]+)").getMatch(0);
            if (singleLink == null || filename == null) {
                continue;
            }
            final DownloadLink dl = createDownloadlink(parameter.replace("file.karelia.ru/", "file.kareliadecrypted.ru/") + System.currentTimeMillis() + new Random().nextInt(100000));
            dl.setContentUrl(parameter);
            filename = Encoding.htmlDecode(filename).trim();
            dl.setFinalFileName(filename);
            dl.setProperty("plainfilename", filename);
            dl.setLinkID(folderid + filename);
            if (filesize != null) {
                jd.plugins.hoster.FileKareliaRu.setFilesize(dl, filesize);
            }
            dl.setProperty("partlink", true);
            if (singlefile) {
                dl.setProperty("singlefile", true);
            }
            dl.setAvailable(true);
            ret.add(dl);
        }
        /* Only add zip url if we found nothing else */
        final boolean allowAddSingleZipFile = false; // 2022-10-20: Single zip file handling is broken and not needed anymore
        if (ret.isEmpty() && allowAddSingleZipFile) {
            final DownloadLink dl = createDownloadlink(parameter.replace("file.karelia.ru/", "file.kareliadecrypted.ru/") + System.currentTimeMillis() + new Random().nextInt(100000));
            dl.setFinalFileName(folderid + ".zip");
            dl.setLinkID(folderid);
            final String filesize = br.getRegex("общим размером <strong id=\"totalSize\">([^<>\"]*?)</strong>").getMatch(0);
            if (filesize != null) {
                dl.setDownloadSize(SizeFormatter.getSize(Encoding.htmlDecode(filesize).replace("Гбайта", "GB").replace("Мбайт", "MB").replace("Кбайта", "kb")));
            }
            dl.setAvailable(true);
            ret.add(dl);
        }
        if (ret.size() > 1) {
            /* Only set packagename if we got multiple items */
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(folderid);
            fp.addLinks(ret);
        }
        return ret;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}