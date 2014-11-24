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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fshare.vn" }, urls = { "http://(?:www\\.)?(?:mega\\.1280\\.com|fshare\\.vn)/folder/([A-Z0-9]+)" }, flags = { 0 })
public class FShareVnFolder extends PluginForDecrypt {

    public FShareVnFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("mega.1280.com", "fshare.vn");
        boolean failed = false;
        br.getPage(parameter);
        if (!br.containsHTML("filename")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String uid = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        final String fpName = br.getRegex("data-id=\"" + uid + "\" data-path=\"/(.*?)\"").getMatch(0);
        String[] linkinformation = br.getRegex("<li>(\\s*<div[^>]+class=\"[^\"]+file_name[^\"]*.*?)</li>").getColumn(0);
        linkinformation = null;
        if (linkinformation == null || linkinformation.length == 0) {
            failed = true;
            linkinformation = br.getRegex("(https?://(www\\.)?fshare\\.vn/file/[A-Z0-9]+)").getColumn(0);
        }
        if (linkinformation == null || linkinformation.length == 0) {
            return null;
        }
        for (String data : linkinformation) {
            if (failed) {
                decryptedLinks.add(createDownloadlink(data));
            } else {
                final String filename = new Regex(data, "title=\"(.*?)\"").getMatch(0);
                final String filesize = new Regex(data, "file_size align-right\">(.*?)</div>").getMatch(0);
                final String dlink = new Regex(data, "(http://(www\\.)?fshare\\.vn/file/[A-Z0-9]+)").getMatch(0);
                if (filename == null && filesize == null && dlink == null) {
                    continue;
                }
                if (dlink == null) {
                    return null;
                }
                final DownloadLink aLink = createDownloadlink(dlink);
                if (filename != null) {
                    aLink.setName(filename.trim());
                    aLink.setAvailable(true);
                }
                if (filesize != null) {
                    aLink.setDownloadSize(SizeFormatter.getSize(filesize));
                }
                decryptedLinks.add(aLink);
            }
        }
        if (!decryptedLinks.isEmpty() && fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}