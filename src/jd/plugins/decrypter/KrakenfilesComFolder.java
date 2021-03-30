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

import org.appwork.utils.Regex;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class KrakenfilesComFolder extends PluginForDecrypt {
    public KrakenfilesComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "krakenfiles.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/profiles/(\\w+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String profileName = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(profileName);
        fp.addLinks(decryptedLinks);
        String next = null;
        int page = 0;
        do {
            page += 1;
            logger.info("Crawling page: " + page);
            final String[] fileIDs = br.getRegex("/view/([a-z0-9]+)/file\\.html").getColumn(0);
            if (fileIDs.length == 0) {
                if (decryptedLinks.isEmpty()) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    /* This should never happen! */
                    logger.info("Stopping because: Current page doesn't contain any items");
                    break;
                }
            }
            for (final String fileID : fileIDs) {
                final DownloadLink dl = createDownloadlink("https://" + this.getHost() + "/view/" + fileID + "/file.html");
                final String filename = br.getRegex("/view/" + fileID + "/file\\.html\">\\s*<div class=\"sl-content\">\\s*<span[^>]*>([^>]+)<").getMatch(0);
                if (filename != null) {
                    dl.setName(filename);
                } else {
                    /* Fallback */
                    dl.setName(fileID);
                }
                dl.setAvailable(true);
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
                distribute(dl);
            }
            next = br.getRegex("<a rel=\"next\" href=\"(/profiles/" + Regex.escape(profileName) + "\\?page=" + (page + 1) + ")\"").getMatch(0);
            if (next == null) {
                logger.info("Aborted because: Reached end");
                break;
            } else {
                br.getPage(next);
            }
        } while (!this.isAbort());
        return decryptedLinks;
    }
}
