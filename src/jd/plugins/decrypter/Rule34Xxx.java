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
import java.util.ArrayList;
import java.util.HashSet;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ExtensionsFilterInterface;
import org.jdownloader.plugins.components.config.Rule34xxxConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.SiteType.SiteTemplate;

/**
 *
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rule34.xxx" }, urls = { "https?://(?:www\\.)?rule34\\.xxx/index\\.php\\?page=post\\&s=(view\\&id=\\d+|list\\&tags=.+)" })
public class Rule34Xxx extends PluginForDecrypt {
    private final String prefixLinkID = getHost().replaceAll("[\\.\\-]+", "") + "://";

    public Rule34Xxx(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void init() {
        super.init();
        Browser.setRequestIntervalLimitGlobal(getHost(), 250);
    }

    @Override
    public Class<? extends Rule34xxxConfig> getConfigInterface() {
        return Rule34xxxConfig.class;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String parameter = Encoding.htmlDecode(param.getCryptedUrl());
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(?i)>\\s*No Images Found\\s*<|>\\s*This post was deleted")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("<h1>\\s*Nobody here but us chickens")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getURL().endsWith("/index.php?page=post&s=list&tags=all")) {
            // redirect to base list page of all content/tags.. we don't want to crawl the entire website
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final boolean preferServerFilenames = PluginJsonConfig.get(this.getConfigInterface()).isPreferServerFilenamesOverPluginDefaultFilenames();
        if (parameter.contains("&s=view&")) {
            // from list to post page
            final String imageParts[] = br.getRegex("'domain'\\s*:\\s*'(.*?)'\\s*,.*?'dir'\\s*:\\s*(\\d+).*?'img'\\s*:\\s*'(.*?)'.*?'base_dir'\\s*:\\s*'(.*?)'").getRow(0);
            String image = null;
            if (imageParts != null) {
                image = imageParts[0] + "/" + imageParts[3] + "/" + imageParts[1] + "/" + imageParts[2];
            } else {
                image = br.getRegex("<img[^>]+\\s+src=('|\")([^>]+)\\1 id=('|\")image\\3").getMatch(1);
                // can be video (Webm)
                if (image == null) {
                    image = br.getRegex("<source\\s+[^>]*src=('|\"|)(.*?)\\1").getMatch(1);
                }
            }
            if (image != null) {
                // these should linkcheck as single event... but if from list its available = true.
                // now core has changed we have to evaluate differently, as it doesn't re-enter decrypter if availablestatus is true.
                final boolean isFromMassEvent = this.getCurrentLink().getSourceLink() != null && this.getCurrentLink().getSourceLink().getDownloadLink() != null && this.getCurrentLink().getSourceLink().getDownloadLink().getDownloadURL().contains("&s=view&");
                final String link = HTMLEntities.unhtmlentities(image);
                String url = Request.getLocation(link, br.getRequest());
                // 2022-05-16: rewrite us location to wimg because us is missing some files(404 not found)
                url = url.replaceFirst("(?i)/(us|wimg)\\.rule34\\.xxx/", "/wimg.rule34.xxx/");
                final DownloadLink dl = createDownloadlink(url);
                if (isFromMassEvent) {
                    dl.setAvailable(true);
                }
                final String id = new Regex(parameter, "id=(\\d+)").getMatch(0);
                // set by decrypter from list, but not set by view!
                try { // Pevent NPE: https://svn.jdownloader.org/issues/84419
                    if (!StringUtils.equals(this.getCurrentLink().getSourceLink().getLinkID(), prefixLinkID + id)) {
                        dl.setLinkID(prefixLinkID + id);
                    }
                } catch (Exception e) {
                    dl.setLinkID(id);
                }
                final String extension = getFileNameExtensionFromString(image);
                final ExtensionsFilterInterface fileType = CompiledFiletypeFilter.getExtensionsFilterInterface(extension.replaceFirst("^\\.", ""));
                if (fileType != null) {
                    dl.setMimeHint(fileType);
                } else if (".webm".equals(extension)) {
                    dl.setMimeHint(CompiledFiletypeFilter.VideoExtensions.WEBM);
                } else {
                    dl.setMimeHint(CompiledFiletypeFilter.ImageExtensions.BMP);
                }
                if (preferServerFilenames) {
                    final String filename = getFileNameFromURL(new URL(dl.getPluginPatternMatcher()));
                    dl.setFinalFileName(filename);
                } else {
                    dl.setFinalFileName("rule34xxx-" + id + extension);
                }
                dl.setContentUrl(parameter);
                ret.add(dl);
            }
        } else {
            /* Crawl tags */
            String fpName = new Regex(parameter, "tags=(.+)&?").getMatch(0);
            FilePackage fp = null;
            if (fpName != null) {
                fp = FilePackage.getInstance();
                fp.setName(fpName);
            }
            final HashSet<String> pages = new HashSet<String>();
            final HashSet<String> dupes = new HashSet<String>();
            loop: do {
                // from list to post page
                final String[] links = br.getRegex("<a id=\"p\\d+\" href=('|\")(/?index\\.php\\?page=post&(:?amp;)?s=view&(:?amp;)?id=\\d+)\\1").getColumn(1);
                if (links == null || links.length == 0) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                int numberofNewItems = 0;
                for (String link : links) {
                    link = HTMLEntities.unhtmlentities(link);
                    if (dupes.add(link)) {
                        numberofNewItems++;
                        final DownloadLink dl = createDownloadlink(Request.getLocation(link, br.getRequest()));
                        if (fp != null) {
                            fp.add(dl);
                        }
                        // we should set temp filename also
                        final String id = new Regex(link, "id=(\\d+)").getMatch(0);
                        dl.setLinkID(prefixLinkID + id);
                        dl.setName(id);
                        distribute(dl);
                        ret.add(dl);
                    }
                }
                logger.info("Crawled page " + br.getURL() + " | Found items so far: " + ret.size());
                final String nexts[] = br.getRegex("<a href=\"(\\?page=post&(:?amp;)?s=list&(:?amp;)?tags=[a-zA-Z0-9_\\-%\\.\\+]+&(:?amp;)?pid=\\d+)\"").getColumn(0);
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user");
                    break;
                } else if (numberofNewItems == 0) {
                    logger.info("Stopping because: Failed to find any new items on current page");
                    break;
                } else if (nexts == null || nexts.length == 0) {
                    logger.info("Stopping because: Failed to find next page -> Reached end(?)");
                    break;
                } else {
                    for (final String next : nexts) {
                        if (pages.add(next)) {
                            sleep(1000, param);
                            br.getPage(HTMLEntities.unhtmlentities(next));
                            continue loop;
                        }
                    }
                }
            } while (true);
        }
        return ret;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Danbooru;
    }
}