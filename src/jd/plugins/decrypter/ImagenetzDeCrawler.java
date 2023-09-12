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
import java.util.regex.Pattern;

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.ImageNetzDe;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ImagenetzDeCrawler extends PluginForDecrypt {
    public ImagenetzDeCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "imagenetz.de" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([A-Za-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final String folderID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        br.getPage(param.getCryptedUrl());
        checkOffline(br, folderID);
        final ImageNetzDe hosterplugin = (ImageNetzDe) this.getNewPluginForHostInstance(this.getHost());
        final String[] filelinks = br.getRegex("href='(https?://(?:www\\.)?imagenetz.de/[A-Za-z0-9]+)' class='btn btn-success btn-download-file'").getColumn(0);
        if (filelinks != null && filelinks.length > 0) {
            String fpName = br.getRegex("<h3 class='panel-title text-strong'[^>]*>([^<]+)</h3>").getMatch(0);
            for (final String singleLink : filelinks) {
                final Regex fileinfo = br.getRegex(">([^<]+)</strong></td>\\s*<td class='text-muted text-right'>([^<]+)</td>\\s*<td class='text-right'><a href='" + Pattern.quote(singleLink));
                final String filename = fileinfo.getMatch(0);
                final String filesizeStr = fileinfo.getMatch(1);
                final DownloadLink file = new DownloadLink(hosterplugin, this.getHost(), singleLink);
                if (filename != null) {
                    file.setName(filename);
                }
                if (filesizeStr != null) {
                    file.setDownloadSize(SizeFormatter.getSize(filesizeStr));
                }
                file.setAvailable(true);
                ret.add(file);
            }
            if (fpName != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName).trim());
                fp.addLinks(ret);
            }
        } else {
            /* Looks like single file */
            final DownloadLink file = new DownloadLink(hosterplugin, this.getHost(), br.getURL());
            ImageNetzDe.parseFileInfo(br, file);
            file.setAvailable(true);
            ret.add(file);
        }
        return ret;
    }

    public static void checkOffline(final Browser br, final String contentID) throws PluginException {
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("Diese Datei existiert nicht mehr")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (contentID != null && !br.getURL().contains(contentID)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }
}
