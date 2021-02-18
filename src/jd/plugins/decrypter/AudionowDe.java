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

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "audionow.de" }, urls = { "https?://(?:www\\.)?audionow\\.de/podcast/(.+)" })
public class AudionowDe extends PluginForDecrypt {
    public AudionowDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        /* 2021-02-18: E.g. redirect to "audionow.de/404" without actual 404 http code */
        if (br.getHttpConnection().getResponseCode() == 404 || !this.canHandle(br.getURL())) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String[] htmls = br.getRegex("<li[^>]*class=\"episode-list-item\">(.*?)</div>\\s*</li>").getColumn(0);
        if (htmls == null || htmls.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        String fpName = br.getRegex("data-podTitle=\"([^<>\"]+)\"").getMatch(0);
        if (fpName == null) {
            fpName = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        }
        fpName = Encoding.htmlDecode(fpName);
        int index = 0;
        for (final String html : htmls) {
            index++;
            String eptitle = new Regex(html, "class=\"episode-title\"[^>]*>([^<>\"]+)<").getMatch(0);
            final String directurl = new Regex(html, "data-audiolink=\"(https://[^<>\"]+)").getMatch(0);
            final String filesize = new Regex(html, "<span[^>]*class=\"text-size\"[^>]*>(\\d+ [A-Za-z]+)</span>").getMatch(0);
            // final String date = new Regex(html, "class=\"text-date\">(\\d{2}\\.\\d{2}\\.\\d{4})<").getMatch(0);
            if (StringUtils.isEmpty(directurl)) {
                /* Skip invalid items */
                continue;
            }
            if (StringUtils.isEmpty(eptitle)) {
                /* Fallback */
                eptitle = index + "";
            }
            eptitle = Encoding.htmlDecode(eptitle);
            final DownloadLink dl = createDownloadlink("directhttp://" + directurl);
            dl.setFinalFileName(fpName + " - " + eptitle + ".mp3");
            if (filesize != null) {
                dl.setDownloadSize(SizeFormatter.getSize(filesize));
            }
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
        if (decryptedLinks.size() == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}
