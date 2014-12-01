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
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "lolabits.es" }, urls = { "http://([a-z0-9]+\\.)?lolabits\\.es/.+" }, flags = { 0 })
public class LolaBitsEsDecrypter extends PluginForDecrypt {

    public LolaBitsEsDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        try {
            br.getPage(parameter);
        } catch (final BrowserException e) {
            final DownloadLink dl = createDownloadlink("http://lolabitsdecrypted.es/" + System.currentTimeMillis() + new Random().nextInt(1000000));
            dl.setFinalFileName(parameter);
            dl.setProperty("mainlink", parameter);
            dl.setProperty("offline", true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        final String server = new Regex(parameter, "(https?://[a-z0-9]+\\.lolabits\\.es/)").getMatch(0);
        if (server != null) {
            /* Find normal links of online viewable doc links */
            parameter = br.getRegex("\"(https?://[\\w\\.]*lolabits\\.es/[^<>\"]*?)\" id=\"dnLink\"").getMatch(0);
            if (parameter == null) {
                parameter = br.getRegex("\"(?:https?://[\\w\\.]*lolabits\\.es)?/([^<>\"]+)\" class=\"redButtonCSS downloadAction\"").getMatch(0);
                if (parameter != null) {
                    parameter = server + parameter;
                } else {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
            }
            br.getPage(parameter);
        }

        /* empty folder | no folder */
        if (br.containsHTML("class=\"noFile\"") || !br.containsHTML("name=\"FolderId\"|id=\"fileDetails\"")) {
            final DownloadLink dl = createDownloadlink("http://lolabitsdecrypted.es/" + System.currentTimeMillis() + new Random().nextInt(1000000));
            dl.setFinalFileName(parameter);
            dl.setProperty("mainlink", parameter);
            dl.setProperty("offline", true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        /* Password protected link --> Not yet supported --> And this code is not yet tested either :D */
        // if (br.containsHTML(">Digite senha:</label>")) {
        // final DownloadLink dl = createDownloadlink("http://abelhasdecrypted.pt/" + System.currentTimeMillis() + new
        // Random().nextInt(1000000));
        // dl.setFinalFileName(parameter);
        // dl.setProperty("mainlink", parameter);
        // dl.setProperty("offline", true);
        // decryptedLinks.add(dl);
        // return decryptedLinks;
        // }

        /* Differ between single links and folders */
        if (br.containsHTML("id=\"fileDetails\"")) {
            String filename = br.getRegex("Descargar: <b>([^<>\"]*?)</b>").getMatch(0);
            final String filesize = br.getRegex("class=\"fileSize\">([^<>\"]*?)</p>").getMatch(0);
            final String fid = br.getRegex("name=\"FileId\" value=\"(\\d+)\"").getMatch(0);
            if (filename == null || filesize == null || fid == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            filename = Encoding.htmlDecode(filename).trim();
            final DownloadLink dl = createDownloadlink("http://lolabitsdecrypted.es/" + System.currentTimeMillis() + new Random().nextInt(1000000));

            dl.setProperty("plain_filename", filename);
            dl.setProperty("plain_filesize", filesize);
            dl.setProperty("plain_fid", fid);
            dl.setProperty("mainlink", parameter);
            dl.setProperty("LINKDUPEID", fid + filename);

            try {
                dl.setContentUrl(parameter);
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
                dl.setBrowserUrl(parameter);
            }

            dl.setName(filename);
            dl.setDownloadSize(SizeFormatter.getSize(filesize));
            dl.setAvailable(true);

            decryptedLinks.add(dl);
        } else {
            final String fpName = br.getRegex("class=\"T_selected\">([^<>\"]*?)<").getMatch(0);
            String[] linkinfo = br.getRegex("<div class=\"fileinfo tab\">(.*?)<span class=\"filedescription\"").getColumn(0);
            if (linkinfo == null || linkinfo.length == 0) {
                linkinfo = br.getRegex("<li class=\"fileItemContainer\">(.*?)class=\"directFileLink\"").getColumn(0);
            }
            if (linkinfo == null || linkinfo.length == 0 || fpName == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String lnkinfo : linkinfo) {
                String contenturl = new Regex(lnkinfo, "\"(/[^<>\"]*?)\"").getMatch(0);
                final String fid = new Regex(lnkinfo, "rel=\"(\\d+)\"").getMatch(0);
                final Regex finfo = new Regex(lnkinfo, "<span class=\"bold\">([^<>\"]*?)</span>([^<>\"]*?)</a>");
                String filename = new Regex(lnkinfo, "alt=\"([^<>\"]*?)\" style=\"\"").getMatch(0);
                if (filename == null || filename.equals("")) {
                    filename = finfo.getMatch(0);
                }
                final String ext = finfo.getMatch(1);
                String filesize = new Regex(lnkinfo, "(\\d+(,\\d+)? (B|KB|MB|GB))([\t\n\r ]+)?<").getMatch(0);
                if (fid == null || filename == null || ext == null || filesize == null || contenturl == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                filesize = Encoding.htmlDecode(filesize).trim();
                filename = Encoding.htmlDecode(filename).trim() + Encoding.htmlDecode(ext).trim();
                contenturl = "http://lolabits.es" + contenturl;

                final DownloadLink dl = createDownloadlink("http://lolabitsdecrypted.es/" + System.currentTimeMillis() + new Random().nextInt(1000000));

                dl.setProperty("plain_filename", filename);
                dl.setProperty("plain_filesize", filesize);
                dl.setProperty("plain_fid", fid);
                dl.setProperty("mainlink", parameter);
                dl.setProperty("LINKDUPEID", fid + filename);

                try {
                    dl.setContentUrl(contenturl);
                } catch (final Throwable e) {
                    /* Not available in old 0.9.581 Stable */
                    dl.setBrowserUrl(contenturl);
                }

                dl.setName(filename);
                dl.setDownloadSize(SizeFormatter.getSize(filesize));
                dl.setAvailable(true);

                decryptedLinks.add(dl);
            }

            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

}
