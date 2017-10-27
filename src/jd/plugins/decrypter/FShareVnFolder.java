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
import java.util.LinkedHashSet;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "fshare.vn" }, urls = { "https?://(?:www\\.)?(?:mega\\.1280\\.com|fshare\\.vn)/folder/([A-Z0-9]+)" })
public class FShareVnFolder extends PluginForDecrypt {
    public FShareVnFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("mega.1280.com", "fshare.vn");
        final LinkedHashSet<String> dupe = new LinkedHashSet<String>();
        boolean failed = false;
        br.getPage("https://www.fshare.vn/location/en");
        br.getPage(parameter);
        if (!br.containsHTML("filename")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        } else if (br.containsHTML(">No results found<")) {
            logger.info("Empty folder");
            return decryptedLinks;
        }
        final String uid = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        final String fpName = br.getRegex("data-id=\"" + uid + "\" data-path=\"/(.*?)\"").getMatch(0);
        String[] linkinformation = new String[0];
        for (int i = 0; i <= linkinformation.length; i++) {
            final Browser br = this.br.cloneBrowser();
            if (i != 0 && linkinformation.length > 0) {
                if (true) {
                    // at this stage no spanning page support
                    break;
                }
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.getHeaders().put("Accept", "*/*");
                br.getPage(parameter + "?pageIndex=" + i);
            }
            linkinformation = br.getRegex("<li[^>]*>(\\s*<div[^>]+class=\"[^\"]+file_name[^\"]*.*?)</li>").getColumn(0);
            if (linkinformation == null || linkinformation.length == 0) {
                failed = true;
                linkinformation = br.getRegex("(https?://(www\\.)?fshare\\.vn/file/[A-Z0-9]+)").getColumn(0);
                if (linkinformation == null || linkinformation.length == 0) {
                    if (i == 0) {
                        return null;
                    }
                    break;
                }
            }
            for (final String data : linkinformation) {
                if (failed) {
                    decryptedLinks.add(createDownloadlink(data));
                } else {
                    // check if folder
                    final String folder = new Regex(data, "class=\"filename folder\" data-id=\"(.*?)\"").getMatch(0);
                    if (folder != null) {
                        final String fder = parameter.replace(uid, folder);
                        if (!dupe.add(fder)) {
                            // this is a way to break page getting since now they return same links over and over, creates infinite loop.
                            linkinformation = null;
                            break;
                        }
                        decryptedLinks.add(createDownloadlink(fder));
                        continue;
                    }
                    final String filename = new Regex(data, "title=\"(.*?)\"").getMatch(0);
                    final String fileSizeBytes = new Regex(data, "file_size align-right\"\\s*data-size=\"(\\d+)\"").getMatch(0);
                    final String filesizeString = new Regex(data, "file_size align-right\"\\s*(?:data-size=\"\\d+\")?>(.*?)</div>").getMatch(0);
                    final String dlink = new Regex(data, "(https?://(www\\.)?fshare\\.vn/file/[A-Z0-9]+)").getMatch(0);
                    if (filename == null && dlink == null) {
                        continue;
                    }
                    if (dlink == null) {
                        logger.info("Failed to get dlink from data: " + data);
                        return null;
                    }
                    if (!dupe.add(dlink)) {
                        // this is a way to break page getting since now they return same links over and over, creates infinite loop.
                        linkinformation = null;
                        break;
                    }
                    final DownloadLink aLink = createDownloadlink(dlink);
                    if (filename != null) {
                        aLink.setName(filename.trim());
                        aLink.setAvailable(true);
                    }
                    if (fileSizeBytes != null) {
                        aLink.setVerifiedFileSize(Long.parseLong(fileSizeBytes));
                    } else if (filesizeString != null) {
                            aLink.setDownloadSize(SizeFormatter.getSize(filesizeString));
                        }
                    decryptedLinks.add(aLink);
                }
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