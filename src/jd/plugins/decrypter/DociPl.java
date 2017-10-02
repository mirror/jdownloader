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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "doci.pl" }, urls = { "https?://(?:www\\.)?doci\\.pl/.+" })
public class DociPl extends PluginForDecrypt {
    public DociPl(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String host_plugin_string = "docidecrypted://";
        jd.plugins.hoster.DociPl.prepBR(this.br);
        br.getPage(parameter);
        if (jd.plugins.hoster.DociPl.isOffline(this.br)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String fpName = br.getRegex("<title>([^<>\"]+)(?:\\- Doci\\.pl)?</title>").getMatch(0);
        final String docid = jd.plugins.hoster.DociPl.getDocumentID(this.br);
        if (docid != null) {
            /* The current url is a single file ... */
            final DownloadLink dl = this.createDownloadlink(parameter.replaceAll("https?://", host_plugin_string));
            jd.plugins.hoster.DociPl.setDownloadlinkInformation(this.br, dl);
            dl.setAvailable(true);
            dl.setLinkID(docid);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        /* Crawl subfolders */
        final String[] folders = br.getRegex("class=\"CssTreeValue\"[^>]*?><a href=\"(/[^<>\"]+)\"").getColumn(0);
        for (String singleLink : folders) {
            singleLink = "http://" + this.br.getHost() + singleLink;
            decryptedLinks.add(createDownloadlink(singleLink));
        }
        /* Crawl files */
        final String[][] files = br.getRegex("class=\"text\\-ellipsis elipsis\\-file\"[^>]*?><a href=\"(/[^<>\"]+)\"\\s*>\\s*(.*?)\\s*<.*?Rozmiar\\s*:\\s*([0-9\\.]+\\s*[GKM]*B)").getMatches();
        if (files == null || files.length == 0) {
            if (folders != null && folders.length > 0) {
                return decryptedLinks;
            }
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String singleLink[] : files) {
            final String url = host_plugin_string + this.br.getHost() + singleLink[0];
            final DownloadLink link = createDownloadlink(url);
            link.setAvailable(true);
            link.setMimeHint(CompiledFiletypeFilter.DocumentExtensions.PDF);
            link.setName(singleLink[1]);
            link.setDownloadSize(SizeFormatter.getSize(singleLink[2]));
            decryptedLinks.add(link);
        }
        if (decryptedLinks.size() == 0) {
            logger.info("Possible empty folder");
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
