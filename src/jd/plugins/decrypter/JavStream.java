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

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "javstream.in" }, urls = { "https?://javstream\\.(?:us|in)/[^/]+?html(\\?mirror=\\d)?" })
public class JavStream extends antiDDoSForDecrypt {
    public JavStream(PluginWrapper wrapper) {
        super(wrapper);
    }

    /** 2021-02-18: New main domain = javstream.in */
    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String filename = br.getRegex("<title>([^<]+)\\s+\\|[\\s\\w]+</title>").getMatch(0);
        logger.info("filename: " + filename);
        if (filename != null) {
            filename = Encoding.htmlDecode(filename.trim());
        }
        String iframe = br.getRegex("<iframe[^<>]*?src=\"([^\"]+)\"[^<>]+?allowfullscreen").getMatch(0);
        if (iframe != null) {
            getPage(iframe);
            // file: 'https://6tz2f0sdh.tiffany-smith.xyz/6S71E3Rzl3Le6SDveipqDiZbO8buDlFUotxY7SeyLSCtLqWK7I/0/playlist.m3u8',
            String file = br.getRegex("file: '([^']+)'").getMatch(0);
            if (file != null) {
                getPage(file);
                DownloadLink dl = createDownloadlink(Encoding.htmlDecode(file));
                decryptedLinks.add(dl);
                logger.info("Decrypter output: " + dl);
            }
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return decryptedLinks;
    }
}