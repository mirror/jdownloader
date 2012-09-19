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
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "files.mail.ru" }, urls = { "http://[\\w\\.]*?files\\.mail\\.ru/[A-Z0-9]{6}" }, flags = { 0 })
public class FlsMailRu extends PluginForDecrypt {

    public FlsMailRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String DLLINKREGEX  = "\"(http://[a-z0-9-]+\\.files\\.mail\\.ru/.*?/.*?)\"";
    private final String UNAVAILABLE1 = ">В обработке<";
    private final String UNAVAILABLE2 = ">In process<";
    private final String INFOREGEX    = "<td class=\"name\">(.*?<td class=\"do\">.*?)</td>";
    private String       LINKREPLACE  = "wge4zu4rjfsdehehztiuxw";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final PluginForHost filesMailRuPlugin = JDUtilities.getPluginForHost("files.mail.ru");
        String parameter = param.toString().replace("www.", "");
        // At the moment jd gets the russian version of the site. Errorhandling
        // also works for English but filesize handling doesn't so if this
        // plugin get's broken that's on of the first things to check
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.getPage(parameter);
        // Errorhandling for offline folders
        if (br.containsHTML(jd.plugins.hoster.FilesMailRu.LINKOFFLINE)) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        // New kind of links
        if (br.containsHTML(jd.plugins.hoster.FilesMailRu.DLMANAGERPAGE)) {
            String filelink = br.getRegex(DLLINKREGEX).getMatch(0);
            String filesize = br.getRegex("</div>[\t\n\r ]+</div>[\t\n\r ]+</td>[\t\n\r ]+<td title=\"(\\d+(\\.\\d+)? [^<>\"]*?)\">").getMatch(0);
            final String filename = br.getRegex("<title>([^<>\"]*?)  скачать [^<>\"]*?@Mail\\.Ru</title>").getMatch(0);
            if (filename == null || filesize == null || filelink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
            }
            filesize = ((jd.plugins.hoster.FilesMailRu) filesMailRuPlugin).fixFilesize(filesize, br);
            filelink = ((jd.plugins.hoster.FilesMailRu) filesMailRuPlugin).fixLink(filelink, br);
            final DownloadLink finallink = createDownloadlink(filelink.replace("files.mail.ru", LINKREPLACE));
            finallink.setFinalFileName(filename);
            finallink.setAvailable(true);
            finallink.setDownloadSize(SizeFormatter.getSize(filesize));
            finallink.setProperty("folderID", parameter);
            finallink.setProperty("realfilename", filename);
            finallink.setProperty("MRDWNLD", br.getRegex("\"http://dlm\\.mail\\.ru/downloader_fmr_[0-9a-f]+\\.exe\"").matches());
            decryptedLinks.add(finallink);
        } else {
            String[] linkinformation = br.getRegex(INFOREGEX).getColumn(0);
            if (linkinformation == null || linkinformation.length == 0) return null;
            for (String info : linkinformation) {
                String statusText = null;
                String directlink = new Regex(info, "<div id=\"dlinklinkOff\\d+\".*?<a href=\"(http[^<>\"]*?)\"").getMatch(0);
                if ((info.contains(UNAVAILABLE1) || info.contains(UNAVAILABLE2)) && directlink == null) {
                    directlink = parameter;
                    statusText = JDL.L("plugins.hoster.FilesMailRu.InProcess", "Datei steht noch im Upload");
                }
                String filename = new Regex(info, "href=\".*?onclick=\"return.*?\">(.*?)<").getMatch(0);
                if (filename == null) filename = new Regex(info, "class=\"str\">(.*?)</div>").getMatch(0);
                if (directlink == null || filename == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                String filesize = new Regex(info, "<td>(.*?{1,15})</td>").getMatch(0);
                directlink = ((jd.plugins.hoster.FilesMailRu) filesMailRuPlugin).fixLink(directlink, br);
                DownloadLink finallink = createDownloadlink(directlink.replace("files.mail.ru", LINKREPLACE));
                if (statusText != null) finallink.getLinkStatus().setStatusText(statusText);
                // Maybe that helps id jd gets the english version of the site!
                if (filesize != null) {
                    filesize = ((jd.plugins.hoster.FilesMailRu) filesMailRuPlugin).fixFilesize(filesize, br);
                    finallink.setDownloadSize(SizeFormatter.getSize(filesize));
                }
                finallink.setProperty("folderID", parameter);
                finallink.setProperty("realfilename", filename);
                filename = Encoding.htmlDecode(filename.trim());
                finallink.setFinalFileName(filename);
                finallink.setAvailable(true);
                decryptedLinks.add(finallink);
            }
        }

        return decryptedLinks;
    }

}
