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
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "solidfiles.com" }, urls = { "https?://(?:www\\.)?solidfiles\\.com/folder/[a-z0-9]+/" }, flags = { 0 })
public class SolidFilesComFolder extends PluginForDecrypt {

    public SolidFilesComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("http://", "https://");
        br.getPage(parameter);
        if (br.containsHTML(">Not found<|>We couldn\\'t find the file you requested")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        String fpName = br.getRegex("<title>([^<>\"]*?)\\- Solidfiles</title>").getMatch(0);
        if (fpName == null) {
            fpName = new Regex(parameter, "([a-z0-9]+)/$").getMatch(0);
        }
        final PluginForHost solidfiles_host = JDUtilities.getPluginForHost("solidfiles.com");
        final boolean decryptFolders = solidfiles_host.getPluginConfig().getBooleanProperty(jd.plugins.hoster.SolidFilesCom.DECRYPTFOLDERS, false);
        final String[] finfo = br.getRegex("rel=\"file\">(.*?)</article>").getColumn(0);
        final String[] folders = br.getRegex("<a href=\"(/folder/[a-z0-9]+/?)\"").getColumn(0);
        if ((folders == null || folders.length == 0) && (finfo == null || finfo.length == 0)) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        if (finfo != null && finfo.length != 0) {
            for (final String finfos : finfo) {
                final Regex urlfilename = new Regex(finfos, "<a href=\"(/d/[^<>\"]*?)\">([^<>\"]*?)</a>");
                String url = urlfilename.getMatch(0);
                String filename = urlfilename.getMatch(1);
                final String filesize = new Regex(finfos, "(\\d+(?:\\.\\d+)? ?(KB|MB|GB))").getMatch(0);
                if (url == null || filename == null || filesize == null) {
                    return null;
                }
                url = "http://www.solidfiles.com" + url;
                filename = Encoding.htmlDecode(filename);

                final DownloadLink dl = createDownloadlink(url);
                dl.setName(filename);
                dl.setDownloadSize(SizeFormatter.getSize(filesize));
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        }
        if (decryptFolders && (folders != null && folders.length != 0)) {
            for (final String singleLink : folders) {
                decryptedLinks.add(createDownloadlink("http://www.solidfiles.com" + singleLink));
            }
        }
        if (!decryptFolders && (folders != null && folders.length != 0) && decryptedLinks.size() == 0) {
            logger.info(folders.length + " folders are available but folder decrypt is deactivated.");
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}