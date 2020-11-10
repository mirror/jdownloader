package jd.plugins.decrypter;
//jDownloader - Downloadmanager

//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.ArrayList;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gogo-stream.com" }, urls = { "https?://(?:www\\.)?(?:gogo-stream\\.com|vidstreaming\\.io)/download\\?id=\\w+" })
@SuppressWarnings("deprecation")
public class GoGoStream extends antiDDoSForDecrypt {
    public GoGoStream(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String page = br.toString();
        String fpName = br.getRegex("id=\"title\"\\s*>\\s*([^<]+)\\s*</span>").getMatch(0);
        ArrayList<String> links = new ArrayList<String>();
        String[] directLinks = br.getRegex("class=\"dowload\"\\s*>\\s*<a\\s+href\\s*=\\s*\"([^\"]+)\"").getColumn(0);
        if (directLinks.length > 0) {
            for (String directLink : directLinks) {
                if (new Regex(directLink, "https?://(?:www\\.)?(?:gogo-stream\\.com|vidstreaming\\.io)/goto\\.php\\?url=[\\w=]+").matches()) {
                    links.add("directhttp://" + directLink);
                } else {
                    links.add(directLink);
                }
            }
        }
        for (String link : links) {
            link = Encoding.htmlDecode(link).replaceAll("^//", "https://");
            DownloadLink dl = createDownloadlink(link);
            if (StringUtils.isNotEmpty(fpName)) {
                dl.setFinalFileName(Encoding.htmlDecode(fpName.trim().replaceAll("\\s+", " ")) + ".mp4");
            }
            decryptedLinks.add(dl);
        }
        if (StringUtils.isNotEmpty(fpName)) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim().replaceAll("\\s+", " ")));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}