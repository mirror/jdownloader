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
import jd.http.URLConnectionAdapter;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "videacesky.cz" }, urls = { "https?://(?:www\\.)?(?:videacesky\\.cz)/(?:video)/[A-Za-z0-9-]+" })
public class VideaCesky extends PluginForDecrypt {
    public VideaCesky(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String link = this.br.getRegex("file:\\s*'(http[^<>\"]*?)'").getMatch(0);
        final String title = this.br.getRegex("title:\\s*'(.*?)'").getMatch(0);
        final String srtfile = this.br.getRegex("file:\\s*\"(.*?)\"").getMatch(0);
        final String srtlabel = this.br.getRegex("label:\\s*\"(.*?)\"").getMatch(0);
        if (link == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        // Add link to youtube video
        decryptedLinks.add(createDownloadlink(link));
        // Add link to srt file for player
        final String srt_link = "http://videacesky.cz" + srtfile;
        DownloadLink dl2 = createDownloadlink(srt_link);
        dl2.setContentUrl(srt_link);
        final String srt_file = title + "." + srtlabel + ".srt";
        dl2.setName(srt_file);
        dl2.setFinalFileName(srt_file);
        dl2.setAvailable(true);
        decryptedLinks.add(dl2);
        for (DownloadLink d : decryptedLinks) {
            logger.info(d.getContentUrl());
        }
        long filesize = -1;
        URLConnectionAdapter con = null;
        try {
            con = br.openHeadConnection(srt_link);
            if (!con.getContentType().contains("html")) {
                filesize = con.getLongContentLength();
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        dl2.setDownloadSize(filesize);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}