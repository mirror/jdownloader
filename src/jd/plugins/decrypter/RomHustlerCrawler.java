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
package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;

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
import jd.plugins.hoster.RomHustler;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class RomHustlerCrawler extends PluginForDecrypt {
    public RomHustlerCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "romhustler.org", "romhustler.net" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/rom/[^<>\"/]+/[^<>\"/]+(/[^<>\"/]+)?");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String addedurl = param.getCryptedUrl().replaceFirst("http://", "https://");
        final RomHustler rhPlugin = (RomHustler) this.getNewPluginForHostInstance(this.getHost());
        br.getPage(addedurl);
        if (isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String urlSlug = new Regex(addedurl, "/rom/(.+)").getMatch(0);
        String title = br.getRegex("<h1 [^>]*?itemprop=\"name\"[^>]*?>([^<>\"]*?)</h1>").getMatch(0);
        if (title == null) {
            title = urlSlug.replace("-", " ").trim();
        }
        final String[] urls = br.getRegex("<a[^>]+(/roms/(?:file|download)/[^>]*/[A-Za-z0-9/\\+=%]*)\"[^>]+>").getColumn(0);
        if (urls == null || urls.length == 0 || title == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(title).trim());
        for (final String url : urls) {
            br.getPage(url);
            final String partOverviewURL = br.getURL();
            final String[][] dlinfos = br.getRegex("<a href=\"(https?://dl\\.[^/]+/files/guest/[^\"]+)\">([^<]*) - \\((\\d+[^\\)]+)\\)\\s*</a>").getMatches();
            int position = 0;
            if (dlinfos != null && dlinfos.length > 0) {
                for (final String[] dlinfo : dlinfos) {
                    final String partDirectDownloadurl = dlinfo[0];
                    final String partTitle = dlinfo[1];
                    final String partFilesizeStr = dlinfo[2];
                    final String temporaryFilename = title + "_" + (position + 1) + "_" + Encoding.htmlDecode(partTitle).trim();
                    final DownloadLink dl = new DownloadLink(rhPlugin, temporaryFilename, this.getHost(), partOverviewURL, true);
                    dl.setName(temporaryFilename);
                    dl.setDownloadSize(SizeFormatter.getSize(partFilesizeStr));
                    dl.setProperty(RomHustler.PROPERTY_MAIN_CONTENT_URL, addedurl);
                    dl.setProperty(RomHustler.PROPERTY_PARTS_OVERVIEW_URL, partOverviewURL);
                    dl.setProperty(RomHustler.PROPERTY_DIRECTURL, partDirectDownloadurl);
                    dl.setProperty(RomHustler.PROPERTY_POSITION, position);
                    dl.setProperty(RomHustler.PROPERTY_NUMBEROF_PARTS, urls.length);
                    dl.setAvailable(true);
                    dl._setFilePackage(fp);
                    ret.add(dl);
                    distribute(dl);
                    position++;
                }
            } else {
                /* Single file download */
                final String temporaryFilename = title + "_" + (position + 1);
                final String filesizeStr = br.getRegex("(?i)Filesize\\s*</strong></td>\\s*<td>([^<]+)</td>").getMatch(0);
                final DownloadLink dl = new DownloadLink(rhPlugin, temporaryFilename, this.getHost(), partOverviewURL, true);
                dl.setName(temporaryFilename);
                if (filesizeStr != null) {
                    dl.setDownloadSize(SizeFormatter.getSize(filesizeStr));
                }
                dl.setProperty(RomHustler.PROPERTY_MAIN_CONTENT_URL, addedurl);
                dl.setProperty(RomHustler.PROPERTY_PARTS_OVERVIEW_URL, partOverviewURL);
                dl.setProperty(RomHustler.PROPERTY_POSITION, position);
                dl.setAvailable(true);
                dl._setFilePackage(fp);
                ret.add(dl);
                distribute(dl);
                position++;
            }
            logger.info("Crawled item " + (position + 1) + "/" + urls.length + " | Results so far: " + ret.size());
        }
        return ret;
    }

    public static boolean isOffline(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(?i)>\\s*404 \\- Page got lost|>\\s*This is a ESA protected rom|>Administrators only")) {
            return true;
        } else {
            return false;
        }
    }
}