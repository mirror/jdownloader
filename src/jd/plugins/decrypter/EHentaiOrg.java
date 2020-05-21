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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.appwork.utils.Files;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ExtensionsFilterInterface;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "e-hentai.org" }, urls = { "https?://(?:www\\.)?(?:(?:g\\.)?e-hentai\\.org|exhentai\\.org)/(?:g|mpv)/(\\d+)/([a-z0-9]+)" })
public class EHentaiOrg extends PluginForDecrypt {
    public EHentaiOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final PluginForHost hostplugin = JDUtilities.getPluginForHost("e-hentai.org");
        final Account aa = AccountController.getInstance().getValidAccount(hostplugin);
        if (aa != null) {
            ((jd.plugins.hoster.EHentaiOrg) hostplugin).login(this.br, aa, false);
        }
        // links are transferable between the login enforced url and public, but may not be available on public
        final String galleryid = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        final String galleryhash = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(1);
        final String parameter;
        if (aa == null) {
            parameter = "https://e-hentai.org/g/" + galleryid + "/" + galleryhash + "/";
        } else {
            parameter = "https://exhentai.org/g/" + galleryid + "/" + galleryhash + "/";
        }
        if (galleryid == null || galleryhash == null) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.br.setFollowRedirects(true);
        br.setCookie(Browser.getHost(parameter), "nw", "1");
        br.getPage(parameter);
        if (jd.plugins.hoster.EHentaiOrg.isOffline(br) || br.containsHTML("Key missing, or incorrect key provided") || br.containsHTML("class=\"d\"") || br.toString().matches("Your IP address has been temporarily banned for excessive pageloads.+")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String archiveFileSize = br.getRegex(">File Size:</td><td[^>]+>([^<>\"]+)</td>").getMatch(0);
        final String uploaderName = br.getRegex("<a href=\"https://[^/]+/uploader/([^<>\"]+)\">([^<>\"]+)</a>\\&nbsp; <a href=\"[^\"]+\"><img class=\"ygm\" src=\"[^\"]+\" alt=\"PM\" title=\"Contact Uploader\" />").getMatch(0);
        final String tagsCommaSeparated = br.getRegex("<meta name=\"description\" content=\"[^\"]+ - Tags: ([^\"]+)\" />").getMatch(0);
        String fpName = ((jd.plugins.hoster.EHentaiOrg) hostplugin).getTitle(br);
        if (fpName == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "fpName can not be found");
        }
        fpName = Encoding.htmlDecode(fpName.trim());
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        fp.setProperty(FilePackage.PROPERTY_PACKAGE_KEY, galleryid);
        if (getPluginConfig().getBooleanProperty(jd.plugins.hoster.EHentaiOrg.SETTING_DOWNLOAD_ZIP, jd.plugins.hoster.EHentaiOrg.default_ENABLE_DOWNLOAD_ZIP)) {
            /* Crawl process can take some time so let's add this always existing URL first */
            final DownloadLink galleryArchive = this.createDownloadlink("ehentaiarchive://" + galleryid + "/" + galleryhash);
            galleryArchive.setContentUrl(parameter);
            galleryArchive.setFinalFileName(fpName + ".zip");
            if (archiveFileSize != null) {
                galleryArchive.setDownloadSize(SizeFormatter.getSize(archiveFileSize));
            }
            galleryArchive.setAvailable(true);
            decryptedLinks.add(galleryArchive);
            distribute(galleryArchive);
        }
        /* Now add all single images */
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
        final DecimalFormat df = new DecimalFormat("0000");
        final Set<String> dupes = new HashSet<String>();
        int counter = 1;
        final boolean preferOriginalFilename = getPluginConfig().getBooleanProperty(jd.plugins.hoster.EHentaiOrg.PREFER_ORIGINAL_FILENAME, jd.plugins.hoster.EHentaiOrg.default_PREFER_ORIGINAL_FILENAME);
        for (int page = 0; page <= pagemax; page++) {
            // final boolean isMultiPageViewActive = br.containsHTML("/mpv/\\d+/[^/]+/#page1");
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
            final Browser br2 = br.cloneBrowser();
            if (page > 0) {
                sleep(new Random().nextInt(5000), param);
                br2.getPage(parameter + "/?p=" + page);
            }
            final String[][] links = br2.getRegex("\"(https?://(?:(?:g\\.)?e-hentai|exhentai)\\.org/s/[a-z0-9]+/" + galleryid + "-\\d+)\">\\s*<img[^<>]*title\\s*=\\s*\"(.*?)\"[^<>]*src\\s*=\\s*\"(.*?)\"").getMatches();
            if (links == null || links.length == 0 || fpName == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (final String link[] : links) {
                final String singleLink = link[0];
                final DownloadLink dl = createDownloadlink(singleLink);
                final String imgposition = df.format(counter);
                final String namepart = fpName + "_" + galleryid + "-" + imgposition;
                final String originalFileName = new Regex(link[1], "\\s*(?:Page\\s*\\d+\\s*:?)?\\s+(.*?\\.(jpe?g|png|gif))").getMatch(0);
                final String extension;
                if (StringUtils.isNotEmpty(originalFileName) && Files.getExtension(originalFileName) != null) {
                    extension = "." + Files.getExtension(originalFileName);
                } else {
                    extension = getFileNameExtensionFromURL(link[2], ".jpg");
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
                if (preferOriginalFilename && StringUtils.isNotEmpty(originalFileName)) {
                    if (dupes.add(originalFileName)) {
                        name = originalFileName;
                    } else {
                        int num = 1;
                        while (true) {
                            final String newName = originalFileName.replaceFirst("(\\.)([^\\.]+$)", "_" + (num++) + ".$2");
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
                final ExtensionsFilterInterface mimeHint = CompiledFiletypeFilter.getExtensionsFilterInterface(Files.getExtension(name));
                dl.setMimeHint(mimeHint);
                dl.setAvailable(true);
                fp.add(dl);
                distribute(dl);
                decryptedLinks.add(dl);
                counter++;
            }
            if (this.isAbort()) {
                logger.info("Decryption aborted by user: " + parameter);
                return decryptedLinks;
            }
        }
        return decryptedLinks;
    }

    /* NOTE: no override to keep compatible to old stable */
    public int getMaxConcurrentProcessingInstances() {
        /* Too many processes = server hates us */
        return 1;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}