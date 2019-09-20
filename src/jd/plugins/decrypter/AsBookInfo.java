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
import java.util.Locale;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "asbook.info" }, urls = { "https?://(www\\.)?asbook\\.info/[^/]+/?" })
public class AsBookInfo extends antiDDoSForDecrypt {
    public AsBookInfo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = null;
        String embedURL = br.getRegex("<script[^>]+defer\\s+src=\\s*'\\s*([^']+)\\s*'[^>]+>\\s*(?:</\\s*script\\s*>)?\\s*<table[^>]+class\\s*=\\s*'[^']*xframe-meta[^']*'").getMatch(0);
        fpName = br.getRegex("<title>\\s*([^<]+)\\s+слушать бесплатно онлайн").getMatch(0);
        if (StringUtils.isNotEmpty(embedURL)) {
            Browser br2 = br.cloneBrowser();
            embedURL = Encoding.htmlDecode(embedURL);
            getPage(br2, embedURL);
            String iframeURL = br2.getRegex("<iframe[^>]+id\\s*=\\s*\\\\\"[^\"]*xframe[^\"]*\"[^>]+src\\s*=\\s*\\\\\"([^\"]*)\"[^>]*>").getMatch(0);
            if (StringUtils.isNotEmpty(iframeURL)) {
                iframeURL = Encoding.htmlOnlyDecode(iframeURL.replace("\\", ""));
                getPage(br2, iframeURL);
                String[] tracks = br2.getRegex("<a[^>]+href\\s*=\\s*\"([^\"]+)\"[^>]+data-code\\s*=[^>]+>").getColumn(0);
                if (tracks != null && tracks.length > 0) {
                    int trackcount = tracks.length;
                    int trackNumber = 1;
                    String trackNumber_suffix = null;
                    String ext = null;
                    int padlength = getPadLength(tracks.length);
                    for (String track : tracks) {
                        String decodedLink = br.getURL(Encoding.htmlDecode(track)).toString();
                        DownloadLink dl = createDownloadlink(decodedLink);
                        if (StringUtils.isNotEmpty(fpName)) {
                            String trackNumber_formatted = String.format(Locale.US, "%0" + padlength + "d", trackNumber);
                            trackNumber_suffix = trackcount > 1 ? (" - " + trackNumber_formatted) : "";
                            if (ext == null) {
                                /* No general extension given? Get it from inside the URL. */
                                ext = getFileNameExtensionFromURL(decodedLink, ".mp3");
                            }
                            final String album_name = Encoding.htmlDecode(fpName.trim());
                            dl.setFinalFileName(album_name + trackNumber_suffix + ext);
                        }
                        decryptedLinks.add(dl);
                        trackNumber++;
                    }
                }
            }
        }
        if (StringUtils.isNotEmpty(fpName)) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private final int getPadLength(final int size) {
        return String.valueOf(size).length();
    }
}