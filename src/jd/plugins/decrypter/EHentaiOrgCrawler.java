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
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.appwork.storage.TypeRef;
import org.appwork.utils.Files;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.config.EhentaiConfig;
import org.jdownloader.plugins.components.config.EhentaiConfig.GalleryCrawlMode;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.EHentaiOrg;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "e-hentai.org" }, urls = { "https?://(?:[a-z0-9\\-]+\\.)?(?:e-hentai\\.org|exhentai\\.org)/(g|mpv)/(\\d+)/([a-z0-9]+).*" })
public class EHentaiOrgCrawler extends PluginForDecrypt {
    public EHentaiOrgCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY, LazyPlugin.FEATURE.XXX };
    }

    private EhentaiConfig cfg = PluginJsonConfig.get(EhentaiConfig.class);

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final EHentaiOrg hostplugin = (EHentaiOrg) this.getNewPluginForHostInstance("e-hentai.org");
        // final String url_host = Browser.getHost(param.getCryptedUrl());
        final Account account = AccountController.getInstance().getValidAccount(hostplugin);
        if (account != null) {
            hostplugin.login(this.br, account, false);
        }
        final Regex urlinfo = new Regex(param.getCryptedUrl(), this.getSupportedLinks());
        final String galleryid = urlinfo.getMatch(1);
        final String galleryhash = urlinfo.getMatch(2);
        /*
         * 2020-11-10: Do not modify URL based on account availability. Not all account owners have access to exhentai.org! Accessing the
         * wrong domain will result in a blank page!
         */
        // if (aa == null) {
        // parameter = "https://e-hentai.org/" + gallerytype + "/" + galleryid + "/" + galleryhash + "/";
        // } else {
        // parameter = "https://exhentai.org/" + gallerytype + "/" + galleryid + "/" + galleryhash + "/";
        // }
        final String contenturl = param.getCryptedUrl();
        if (galleryid == null || galleryhash == null) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (Browser.getHost(contenturl).equals("exhentai.org") && account == null) {
            /* Account required to crawl URLs of this host! */
            throw new AccountRequiredException();
        }
        EHentaiOrg.prepBR(br);
        br.getPage(contenturl);
        if (EHentaiOrg.isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("Key missing, or incorrect key provided") || br.containsHTML("class=\"d\"") || br.toString().matches("Your IP address has been temporarily banned for excessive pageloads.+")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getRequest().getHttpConnection().getCompleteContentLength() == 0) {
            /* 2020-11-10: Rare case */
            logger.warning("Blank page --> Are you trying to access exhentai.org without the appropriate rights?");
            throw new AccountRequiredException();
        }
        String title = hostplugin.getTitle(br);
        if (title == null) {
            /* Fallback */
            title = br._getURL().getPath();
        }
        title = Encoding.htmlDecode(title).trim();
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.setPackageKey(getHost() + "://gallery/" + galleryid);
        final GalleryCrawlMode mode = cfg.getGalleryCrawlMode();
        if (mode == GalleryCrawlMode.ZIP_ONLY || mode == GalleryCrawlMode.ZIP_AND_IMAGES) {
            /* Crawl process can take some time so let's add this always existing URL first */
            final DownloadLink ziparchive = this.createDownloadlink("ehentaiarchive://" + galleryid + "/" + galleryhash);
            ziparchive.setProperty(EHentaiOrg.PROPERTY_GALLERY_URL, br.getURL());
            ziparchive.setContentUrl(br.getURL());
            ziparchive.setFinalFileName(title + ".zip");
            final String archiveFileSize = br.getRegex("(?i)>\\s*File Size\\s*:\\s*</td><td[^>]+>([^<>\"]+)</td>").getMatch(0);
            if (archiveFileSize != null) {
                ziparchive.setDownloadSize(SizeFormatter.getSize(archiveFileSize));
            } else {
                logger.warning("Failed to find archive-size in html code");
            }
            ziparchive.setAvailable(true);
            ziparchive._setFilePackage(fp);
            ret.add(ziparchive);
            if (mode == GalleryCrawlMode.ZIP_ONLY) {
                return ret;
            } else {
                distribute(ziparchive);
            }
        }
        final String uploaderName = br.getRegex("<a href=\"https://[^/]+/uploader/([^<>\"]+)\">([^<>\"]+)</a>\\&nbsp; <a href=\"[^\"]+\"><img class=\"ygm\" src=\"[^\"]+\" alt=\"PM\" title=\"Contact Uploader\" />").getMatch(0);
        final String tagsCommaSeparated = br.getRegex("<meta name=\"description\" content=\"[^\"]+ - Tags: ([^\"]+)\" />").getMatch(0);
        /* Crawl all single images */
        int pagemax = 0;
        final String[] pages = br.getRegex("/?p=(\\d+)\" onclick=").getColumn(0);
        if (pages != null && pages.length != 0) {
            for (final String aPage : pages) {
                final int pageint = Integer.parseInt(aPage);
                if (pageint > pagemax) {
                    pagemax = pageint;
                }
            }
        }
        final UrlQuery query = UrlQuery.parse(contenturl);
        final String startPageStr = query.get("p");
        final int startPage;
        if (startPageStr != null && startPageStr.matches("\\d+")) {
            logger.info("Using page given in URL as start page: " + startPageStr);
            startPage = Integer.parseInt(startPageStr);
        } else {
            startPage = 0;
        }
        /* Check if the user has activated "Multi page View" in his account --> Switch to required URL if needed. */
        final boolean isMultiPageViewActive = br.containsHTML("/mpv/\\d+/[^/]+/#page\\d+");
        final String mpv_url = br.getURL("/mpv/" + galleryid + "/" + galleryhash + "/").toExternalForm();
        boolean isMultiPageURL = StringUtils.containsIgnoreCase(br.getURL(), "/mpv/");
        if (isMultiPageViewActive && !isMultiPageURL) {
            logger.info("Switching to multi page view ...");
            br.getPage(mpv_url);
        }
        /* Check again as we could also get redirected to a normal gallery URL containing '/g/' */
        isMultiPageURL = StringUtils.containsIgnoreCase(br.getURL(), "/mpv/");
        int imagecounter = 1;
        int numberofImages = -1;
        for (int page = startPage; page <= pagemax; page++) {
            // if (isMultiPageViewActive) {
            // logger.info("Multi-Page-View active --> Trying to deactivate it");
            // final String previousURL = br.getURL();
            // if (aa == null) {
            // logger.warning("WTF no account available");
            // }
            // br.getPage("https://e-hentai.org/uconfig.php");
            // final Form[] forms = br.getForms();
            // Form settings = new Form();
            // settings = forms[forms.length - 1];
            // // settings.setMethod(MethodType.POST);
            // // settings.setAction(br.getURL());
            // settings.remove("qb");
            // settings.put("qb", "0");
            // settings.put("apply", "Apply");
            // br.submitForm(settings);
            // br.getPage(previousURL);
            // }
            final boolean isLastPage = page == pagemax;
            final Browser br2 = br.cloneBrowser();
            final Regex paginationinfo = br.getRegex(">\\s*Showing (\\d+) - (\\d+) of (\\d+) images");
            if (paginationinfo.patternFind()) {
                imagecounter = Integer.parseInt(paginationinfo.getMatch(0));
                numberofImages = Integer.parseInt(paginationinfo.getMatch(2));
            } else {
                /* Not great, not terrible */
                logger.warning("Failed to find paginationInfo in html code");
            }
            if (isMultiPageURL) {
                /* 2020-05-21: New feature of the websites which some users can activate in their account */
                final String mpvkey = br.getRegex("var mpvkey\\s*=\\s*\"([a-z0-9]+)\";").getMatch(0);
                if (mpvkey == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String json_imagelist = br.getRegex("imagelist\\s*=\\s*(\\[\\{.*?\\])").getMatch(0);
                final List<Map<String, Object>> ressourcelist = (List<Map<String, Object>>) restoreFromString(json_imagelist, TypeRef.OBJECT);
                for (final Map<String, Object> entries : ressourcelist) {
                    final String originalFilename = (String) entries.get("n");
                    final String imagekey = (String) entries.get("k");
                    if (StringUtils.isEmpty(originalFilename) || StringUtils.isEmpty(imagekey)) {
                        /* Skip invalid items (this should never happen) */
                        continue;
                    }
                    final String url = mpv_url + "#page" + page;
                    final DownloadLink dl = getDownloadlink(url, galleryid, uploaderName, tagsCommaSeparated, title, originalFilename, numberofImages, imagecounter);
                    dl.setProperty(EHentaiOrg.PROPERTY_MPVKEY, mpvkey);
                    dl.setProperty(EHentaiOrg.PROPERTY_IMAGEKEY, imagekey);
                    fp.add(dl);
                    distribute(dl);
                    ret.add(dl);
                    imagecounter++;
                }
                logger.info("Stopping because: mpv URLs have all objects on the first page");
                break;
            } else {
                final String[][] links = br2.getRegex("\"(https?://(?:(?:g\\.)?e-hentai|exhentai)\\.org/s/[a-z0-9]+/" + galleryid + "-\\d+)\">\\s*<img[^<>]*title\\s*=\\s*\"(.*?)\"[^<>]*src\\s*=\\s*\"(.*?)\"").getMatches();
                if (links == null || links.length == 0 || title == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                for (final String link[] : links) {
                    final String singleLink = link[0];
                    final String originalFilename = new Regex(link[1], "\\s*(?:Page\\s*\\d+\\s*:?)?\\s+(.*?\\.(jpe?g|png|gif))").getMatch(0);
                    final DownloadLink dl = getDownloadlink(singleLink, galleryid, uploaderName, tagsCommaSeparated, title, originalFilename, numberofImages, imagecounter);
                    fp.add(dl);
                    distribute(dl);
                    ret.add(dl);
                    imagecounter++;
                }
            }
            logger.info("Crawled page " + (page + 1) + "/" + (pagemax + 1) + " | Found items so far: " + ret.size() + "/" + numberofImages);
            if (this.isAbort()) {
                logger.info("Stopping because: Decryption aborted by user: " + contenturl);
                return ret;
            }
            /* Continue to next page unless current page is the last page. */
            if (!isLastPage) {
                final int sleepBeforeNextPageMillis = new Random().nextInt(5000);
                logger.info("Sleep before accessing next page: " + sleepBeforeNextPageMillis);
                sleep(sleepBeforeNextPageMillis, param);
                query.addAndReplace("p", Integer.toString(page));
                br2.getPage(br._getURL().getPath() + "?" + query.toString());
            }
        }
        return ret;
    }

    final Set<String> dupes = new HashSet<String>();

    private DownloadLink getDownloadlink(final String url, final String galleryID, final String uploaderName, final String tagsCommaSeparated, final String fpName, final String originalFilename, final int numberofItems, final int imagePos) {
        final DownloadLink dl = createDownloadlink(url);
        final int padLength;
        if (numberofItems == -1) {
            /* We don't know how many items to expect -> Use fixed pad length */
            padLength = 4;
        } else {
            padLength = StringUtils.getPadLength(numberofItems);
        }
        final String imgposition = StringUtils.formatByPadLength(padLength, imagePos);
        final String namepart = fpName + "_" + galleryID + "-" + imgposition;
        final String extension;
        if (StringUtils.isNotEmpty(originalFilename) && Files.getExtension(originalFilename) != null) {
            extension = Plugin.getFileNameExtensionFromString(originalFilename);
        } else {
            /* Fallback */
            extension = ".jpg";
        }
        dl.setProperty("namepart", namepart);
        dl.setProperty("imageposition", imgposition);
        /* Additional properties (e.g. for usage via packagizer) */
        if (uploaderName != null) {
            dl.setProperty("uploader", uploaderName);
        }
        if (tagsCommaSeparated != null) {
            dl.setProperty("tags_comma_separated", tagsCommaSeparated);
        }
        final String name;
        if (cfg.isPreferOriginalFilename() && StringUtils.isNotEmpty(originalFilename)) {
            if (dupes.add(originalFilename)) {
                name = originalFilename;
            } else {
                int num = 1;
                while (true) {
                    final String newName = originalFilename.replaceFirst("(\\.)([^\\.]+$)", "_" + (num++) + ".$2");
                    if (dupes.add(newName)) {
                        name = newName;
                        break;
                    }
                }
            }
        } else {
            name = namepart + extension;
        }
        dl.setName(name);
        dl.setAvailable(true);
        return dl;
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* Too many processes = server hates us */
        return 1;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}