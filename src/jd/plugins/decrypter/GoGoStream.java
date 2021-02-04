package jd.plugins.decrypter;
//jDownloader - Downloadmanager

import java.io.IOException;
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
import java.util.Collections;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gogo-stream.com" }, urls = { "https?://(?:www\\.)?(?:gogo-stream\\.com|vidstreaming\\.io|gogo-play\\.net)/(?:download\\?id=|loadserver\\.php?|streaming\\.php?|videos/)[^/]+" })
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
        if (StringUtils.isEmpty(fpName) && StringUtils.containsIgnoreCase(parameter, "/videos/")) {
            fpName = br.getRegex("\"og:title\"[^>]+content\\s*=\\s*\"(?:Watch\\s+)?([^\"]+)\\s+online\\s+at").getMatch(0);
        }
        ArrayList<String> links = new ArrayList<String>();
        String[] directLinks = br.getRegex("class=\"dowload\"\\s*>\\s*<a\\s+href\\s*=\\s*\"([^\"]+)\"").getColumn(0);
        if (directLinks != null && directLinks.length > 0) {
            for (String directLink : directLinks) {
                if (new Regex(directLink, "\\.+/goto\\.php\\?url=[\\w=]+").matches()) {
                    links.add("directhttp://" + directLink);
                } else {
                    links.add(directLink);
                }
            }
        }
        String[] embedLinks = br.getRegex("class\\s*=\\s*\"play-video\"[^>]*>\\s*<iframe[^>]+src\\s*=\\s*\"([^\"]+)").getColumn(0);
        if (embedLinks != null && embedLinks.length > 0) {
            Collections.addAll(links, embedLinks);
        }
        String downloadLink = br.getRegex("playerInstance\\.addButton[^$]+\"([^\"]+/download?[^\"]+)\"").getMatch(0);
        if (StringUtils.isNotEmpty(downloadLink)) {
            links.add(downloadLink);
        }
        String[] serverLinks = br.getRegex("<li[^>]+class=\"[^\"]*linkserver\"[^>]+data-video=\"([^\"]+)\"").getColumn(0);
        Collections.addAll(links, serverLinks);
        for (String link : links) {
            link = Encoding.htmlDecode(link);
            DownloadLink dl = createDownloadlink(processPrefixSlashes(link));
            if (StringUtils.isNotEmpty(fpName)) {
                if (directLinks != null && directLinks.length > 0) {
                    dl.setFinalFileName(Encoding.htmlDecode(fpName.trim().replaceAll("\\s+", " ")) + ".mp4");
                }
            }
            decryptedLinks.add(dl);
        }
        if (StringUtils.isNotEmpty(fpName)) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim().replaceAll("\\s+", " ")));
            fp.setProperty(LinkCrawler.PACKAGE_ALLOW_MERGE, true);
            fp.setProperty(LinkCrawler.PACKAGE_ALLOW_INHERITANCE, true);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private String processPrefixSlashes(String link) throws IOException {
        link = link.trim().replaceAll("^//", "https://");
        if (link.startsWith("/")) {
            link = this.br.getURL(link).toString();
        }
        return link;
    }
}