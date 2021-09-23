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

import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.StringUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class PicsVc extends PluginForDecrypt {
    public PicsVc(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "pics.vc" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/watch\\?g=([a-f0-9]{32})");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String galleryID = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("class=.gall_not_found")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String fpName = br.getRegex("<title>\\s*([^<>\"]+)\\s*-\\s*PICS\\.VC\\s*</title>").getMatch(0);
        int offset = 0;
        int lastNextOffset = 0;
        int page = 0;
        final DecimalFormat df = new DecimalFormat("000");
        final FilePackage fp = FilePackage.getInstance();
        if (fpName != null) {
            final String lines[] = StringUtils.getLines(fpName);
            fp.setName(Encoding.htmlDecode(lines[0].trim()));
            if (lines.length > 1) {
                fp.setComment(StringUtils.join(Arrays.asList(lines).subList(1, lines.length), ","));
            }
        } else {
            /* Fallback */
            fp.setName(galleryID);
        }
        List<String> dupes = new ArrayList<String>();
        do {
            page += 1;
            logger.info("Crawling page: " + page);
            /* 2021-03-24: Avoid grabbing "related" pictures listed below the actual pictures belonging to a category! */
            String[] links = br.getRegex("(https?://s\\d+\\.pics\\.vc/pics/s/[^\"\\']+\\.jpg)[^>]*pic_loader\\(this\\)").getColumn(0);
            if (links == null || links.length == 0) {
                links = br.getRegex("(/cdn/s\\d+/[^\"\\']+\\.jpg)[^>]*pic_loader\\(this\\)").getColumn(0);
            }
            if (links == null || links.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (String singleLink : links) {
                if (dupes.contains(singleLink)) {
                    continue;
                }
                dupes.add(singleLink);
                if (singleLink.startsWith("/cdn/")) {
                    final String server = new Regex(singleLink, "/cdn/(s\\d+)").getMatch(0);
                    final String rest = new Regex(singleLink, "\\d+/s/(.+)").getMatch(0);
                    if (server == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else if (rest == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    singleLink = "https://" + server + ".pics.vc/pics/n/" + rest;
                } else {
                    singleLink = singleLink.replaceFirst("/pics/s/", "/pics/o/");
                }
                String filename = Plugin.getFileNameFromURL(new URL(singleLink));
                filename = df.format(offset + 1) + "_" + filename;
                final DownloadLink dl = createDownloadlink("directhttp://" + singleLink);
                dl.setFinalFileName(filename);
                dl._setFilePackage(fp);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
                distribute(dl);
                offset += 1;
            }
            final String nextOffsetStr = br.getRegex(galleryID + "\\&off=(\\d+)'[^>]*><div[^>]*class='next_page clip'").getMatch(0);
            if (nextOffsetStr != null && Integer.parseInt(nextOffsetStr) > lastNextOffset) {
                // 2021-06-07 website given offset is missing about 2 images per page, use smaller steps (16 instead of website 36) to avoid
                // missing
                // images
                final int nextOffset = Math.min(lastNextOffset + 16, Integer.parseInt(nextOffsetStr));
                lastNextOffset = nextOffset;
                br.getPage(parameter + "&off=" + nextOffset);
            } else {
                logger.info("Stopping because failed to find next page");
                break;
            }
        } while (!this.isAbort());
        return decryptedLinks;
    }
}
