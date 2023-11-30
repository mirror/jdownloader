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
import java.util.HashSet;
import java.util.List;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.WebShareCz;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { WebShareCz.class })
public class WebShareCzFolder extends PluginForDecrypt {
    public WebShareCzFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public static List<String[]> getPluginDomains() {
        return WebShareCz.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:#/)?folder/[a-z0-9]{8,}");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String folderid = new Regex(param.getCryptedUrl(), "([a-z0-9]+)$").getMatch(0);
        if (folderid == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        FilePackage fp = null;
        int offset = 0;
        final int maxItemsPerPage = 100;
        int page = 1;
        final HashSet<String> dupes = new HashSet<String>();
        do {
            int numberofNewItemsThisPage = 0;
            br.postPage("https://" + this.getHost() + "/api/folder/", "ident=" + folderid + "&offset=" + offset + "&limit=" + maxItemsPerPage + "&wst=");
            if (br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("Folder not found")) {
                /*
                 * <response><status>FATAL</status><code>FOLDER_FATAL_1</code><message>Folder not
                 * found.</message><app_version>26</app_version></response>
                 */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (fp == null) {
                fp = FilePackage.getInstance();
                /* Very very cheap solution: First "name" tag in XML is name of the folder. */
                fp.setName(br.getRegex("<name>([^<]+)</name>").getMatch(0));
            }
            final String[] xmls = br.getRegex("<file>(.*?)</file>").getColumn(0);
            if (xmls == null || xmls.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (final String singleXML : xmls) {
                final String fileid = new Regex(singleXML, "<ident>([^<]+)</ident>").getMatch(0);
                if (!dupes.add(fileid)) {
                    /* Skip dupes */
                    continue;
                }
                final String filesize = new Regex(singleXML, "<size>([^<]+)</size>").getMatch(0);
                final String filename = new Regex(singleXML, "<name>([^<]+)</name>").getMatch(0);
                final String content_url = "https://webshare.cz/#/file/" + fileid;
                final DownloadLink dl = createDownloadlink(content_url);
                dl.setContentUrl(content_url);
                dl.setLinkID(fileid);
                dl.setName(filename);
                dl.setDownloadSize(Long.parseLong(filesize));
                dl.setAvailable(true);
                dl._setFilePackage(fp);
                ret.add(dl);
                distribute(dl);
                numberofNewItemsThisPage++;
                offset++;
            }
            logger.info("Crawled page " + page + " | Found items so far: " + ret.size());
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (numberofNewItemsThisPage < maxItemsPerPage) {
                logger.info("Stopping because: Current page contains less items than full page pagination");
                break;
            } else {
                /* Continue to next page */
                page++;
            }
        } while (true);
        return ret;
    }
}
