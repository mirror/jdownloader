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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "vidnow.to" }, urls = { "https?://(?:www\\.)?vidnow\\.to/file/[A-Za-z0-9\\-_]+" })
public class VidnowTo extends PluginForDecrypt {

    public VidnowTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        /* 2017-04-07: Offline urls will also show the html like 'Download will be available soon' --> Bullshit, they're simply offline! */
        if (br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("Download link will be available soon")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String fpName = br.getRegex("<title>([^<>\"]+)</title>").getMatch(0);
        final String[] links = br.getRegex("href=\"(http[^<>\"]+)\" target=\"_blank\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink : links) {
            if (singleLink.contains(this.getHost())) {
                continue;
            }
            decryptedLinks.add(createDownloadlink(singleLink));
        }

        /*
         * Try to find direct-download (2017-04-20: Seems to be officially only available for users from England) [GEO-block is possibly to
         * skip]
         */
        try {
            final Regex js_arguments = this.br.getRegex("downloadLink\\((\\d+),\\'([^<>\"\\']+)\\'");
            final String link_id = js_arguments.getMatch(0);
            final String title = js_arguments.getMatch(1);
            if (link_id != null && title != null) {
                this.br.postPage("http://vidnow.to/file/vidnow/", "downloadLink=1&link_id=" + link_id + "&title=" + title);
                /* Usually we'll get googlevideo urls here. */
                final String[] directurls = this.br.getRegex("<a href=\"(https?://[^<>\"\\']+)").getColumn(0);
                for (final String singleLink : directurls) {
                    if (singleLink.contains(this.getHost())) {
                        continue;
                    }
                    decryptedLinks.add(createDownloadlink(singleLink));
                }
            }
        } catch (final Throwable e) {
        }

        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

}
