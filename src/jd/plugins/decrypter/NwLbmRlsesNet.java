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
import java.util.List;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class NwLbmRlsesNet extends PluginForDecrypt {
    public NwLbmRlsesNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "newalbumreleases.net" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            // old: (?!(about|category|date|feed|comments|pic)/)[A-Za-z0-9=/]+/?
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/\\d+/[a-z0-9\\-]+/?");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (param.getCryptedUrl().matches("https?://(?:www\\.)?newalbumreleases\\.net/\\d+(/?.+)?")) {
            br.setFollowRedirects(true);
            br.getPage(param.getCryptedUrl());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String[] links = br.getRegex("\"(https?://[^<>\"]*?)\">DOWNLOAD</a>").getColumn(0);
            br.setFollowRedirects(false);
            int counter = 1;
            for (final String singleLink : links) {
                logger.info("Crawling item " + counter + "/" + links.length);
                String finallink;
                if (singleLink.contains("newalbumreleases.net/")) {
                    br.getPage(singleLink);
                    finallink = br.getRedirectLocation();
                    if (finallink == null) {
                        logger.warning("Decrypter broken for link: " + param.getCryptedUrl());
                        return null;
                    }
                } else {
                    finallink = singleLink;
                }
                decryptedLinks.add(createDownloadlink(finallink));
                if (this.isAbort()) {
                    break;
                }
            }
            /* 2022-05-18: Look for embedded youtube videos */
            final String[] ytVideos = br.getRegex("(https?://(?:www\\.)?youtube\\.com/embed/[A-Za-z0-9]+)").getColumn(0);
            for (final String ytVideo : ytVideos) {
                decryptedLinks.add(createDownloadlink(ytVideo));
            }
            if (decryptedLinks.isEmpty()) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else {
            br.setFollowRedirects(false);
            br.getPage(param.getCryptedUrl());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String finallink = br.getRedirectLocation();
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + param.getCryptedUrl());
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        }
        return decryptedLinks;
    }
}
