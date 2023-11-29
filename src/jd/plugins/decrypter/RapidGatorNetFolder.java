//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.RapidGatorNet;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
@SuppressWarnings("deprecation")
@PluginDependencies(dependencies = { RapidGatorNet.class })
public class RapidGatorNetFolder extends PluginForDecrypt {
    public RapidGatorNetFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public static List<String[]> getPluginDomains() {
        return jd.plugins.hoster.RapidGatorNet.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/folder/(\\d+)/.*");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String contenturl = param.getCryptedUrl().replaceFirst("(?i)http://", "https://");
        final String folderID = new Regex(contenturl, this.getSupportedLinks()).getMatch(0);
        br.getPage(contenturl);
        String folderTitle = br.getRegex("(?i)Downloading\\s*:\\s*</strong>(.*?)</p>").getMatch(0);
        if (folderTitle == null) {
            folderTitle = br.getRegex("(?i)<title>Download file (.*?)</title>").getMatch(0);
        }
        final String titleForEmptyOrOfflineFolder;
        if (folderTitle != null) {
            folderTitle = Encoding.htmlDecode(folderTitle).trim();
            titleForEmptyOrOfflineFolder = folderID + "_" + folderTitle;
        } else {
            /* Fallback */
            folderTitle = folderID;
            titleForEmptyOrOfflineFolder = folderTitle;
        }
        if (br.containsHTML("E_FOLDERNOTFOUND") || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("class=\"empty\"")) {
            throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, titleForEmptyOrOfflineFolder);
        }
        final FilePackage fp = FilePackage.getInstance();
        if (folderTitle != null) {
            fp.setName(Encoding.htmlDecode(folderTitle).trim());
        } else {
            /* Fallback */
            fp.setName(br._getURL().getPath());
        }
        fp.addLinks(ret);
        int page = 1;
        int maxPage = 1;
        final String[] pagenumbers = br.getRegex("/folder/" + folderID + "/[^>]*\\?page=(\\d+)").getColumn(0);
        for (final String pagenumberStr : pagenumbers) {
            final int pagenumberTmp = Integer.parseInt(pagenumberStr);
            if (pagenumberTmp > maxPage) {
                maxPage = pagenumberTmp;
            }
        }
        final String baseurl = br.getURL();
        final HashSet<String> dupes = new HashSet<String>();
        do {
            final String[] subfolderurls = br.getRegex("<td><a href=\"(/folder/\\d+/[^<>\"/]+\\.html)\">").getColumn(0);
            final String[][] links = br.getRegex("\"(/file/([a-z0-9]{32}|\\d+)/([^\"]+))\".*?>([\\d\\.]+ (KB|MB|GB))").getMatches();
            if ((links == null || links.length == 0) && (subfolderurls == null || subfolderurls.length == 0)) {
                /* This should never happen as we check for offline/empty folder in beforehand. */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            int numberofNewItems = 0;
            if (links != null && links.length != 0) {
                for (String[] dl : links) {
                    final String url = Request.getLocation(dl[0], br.getRequest());
                    if (!dupes.add(url)) {
                        continue;
                    }
                    final DownloadLink link = createDownloadlink(url);
                    link.setName(dl[2].replaceFirst("\\.html$", ""));
                    link.setDownloadSize(SizeFormatter.getSize(dl[3]));
                    link.setAvailable(true);
                    if (fp != null) {
                        fp.add(link);
                    }
                    ret.add(link);
                    distribute(link);
                    numberofNewItems++;
                }
            }
            if (subfolderurls != null && subfolderurls.length != 0) {
                for (final String subfolderurl : subfolderurls) {
                    final String url = Request.getLocation(subfolderurl, br.getRequest());
                    if (!dupes.add(url)) {
                        continue;
                    }
                    final DownloadLink link = createDownloadlink(url);
                    if (fp != null) {
                        fp.add(link);
                    }
                    ret.add(link);
                    distribute(link);
                    numberofNewItems++;
                }
            }
            logger.info("Crawled page " + page + "/" + maxPage + " | Found items so far: " + ret.size());
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (page == maxPage) {
                logger.info("Stopping because: Reached last page");
                break;
            } else if (numberofNewItems == 0) {
                logger.info("Stopping because: Failed to find any new items on current page");
                break;
            } else {
                /* Continue to next page */
                /* Small delay between requests */
                sleep(500, param);
                page++;
                br.getPage(baseurl + "?page=" + page);
            }
        } while (true);
        return ret;
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    public boolean hasCaptcha(final CryptedLink link, final jd.plugins.Account acc) {
        return false;
    }
}