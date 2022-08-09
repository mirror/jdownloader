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
import java.util.Map;
import java.util.Random;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class FotografDe extends PluginForDecrypt {
    public FotografDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "fotograf.de" });
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
            ret.add("https?://[\\w\\-]+\\." + buildHostsPatternPart(domains) + "/[^/]+");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        /* 2022-06-11: No matter which URL the user adds we will most likely be redirected to /login */
        br.getPage(param.getCryptedUrl());
        final boolean looksLikeBadLink = br.getHttpConnection().getResponseCode() == 404;
        if (looksLikeBadLink || getPassForm(br) == null) {
            /* Fallback in case user adds bad URL. */
            logger.info("Executing fallback");
            br.getPage("/login");
        }
        /* 2022-06-11: All galleries are password protected and it seems like passwords are case insensitive. */
        String passCode = null;
        String photoOverviewURL = null;
        for (int i = 0; i <= 2; i++) {
            final Form passform = getPassForm(br);
            if (passform == null) {
                if (looksLikeBadLink) {
                    logger.info("Link is offline");
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    /* E.g. https://www.fotograf.de/agb/ or https://www.fotograf.de/preise */
                    logger.info("Link seems to be offline");
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            }
            passCode = getUserInput("Password?", param);
            passform.put(Encoding.urlEncode("data[GuestAccess][code][]"), Encoding.urlEncode(passCode));
            br.submitForm(passform);
            photoOverviewURL = br.getRegex("(/funnel/overview/\\d+)").getMatch(0);
            if (photoOverviewURL != null || getPassForm(br) == null) {
                logger.info("Correct password is: " + passCode);
                break;
            } else {
                logger.info("Wrong password: " + passCode);
                continue;
            }
        }
        if (br.containsHTML("(?i)Wir verleihen Ihren Fotos gerade den letzten Schliff")) {
            throw new DecrypterRetryException(RetryReason.FILE_NOT_FOUND, "GALLERY_IS_NOT_YET_READY_" + passCode, "This gallery is not yet ready. Try again later. Password: " + passCode);
        } else if (photoOverviewURL == null) {
            throw new DecrypterException(DecrypterException.PASSWORD);
        }
        br.getPage(photoOverviewURL);
        String packagename = null;
        String galleryTitle = br.getRegex("class=\"ws-title\">([^<]+)</div>").getMatch(0);
        if (galleryTitle != null) {
            final String photographerName = br.getRegex("<title>([^<]+)</title>").getMatch(0);
            galleryTitle = Encoding.htmlDecode(galleryTitle).trim();
            String galleryTitleSecondary = br.getRegex("class=\"ws-secondary\">([^<]+)</div>").getMatch(0);
            if (galleryTitleSecondary != null) {
                galleryTitleSecondary = Encoding.htmlDecode(galleryTitleSecondary).trim();
            }
            if (StringUtils.equals(galleryTitle, galleryTitleSecondary)) {
                packagename = galleryTitle;
            } else {
                packagename = galleryTitle + " - " + galleryTitleSecondary;
            }
            if (photographerName != null) {
                packagename = Encoding.htmlDecode(photographerName).trim() + " - " + packagename;
            }
        }
        /* Get list of all photoIDs */
        final String[] photoIDs = br.getRegex("/shop/photo/([a-f0-9\\-]+)\"").getColumn(0);
        /*
         * Pick randm photoID and access single photo page to get json containing information about all images of whole gallery -> This way
         * we don't have to deal with pagination.
         */
        final String randomChosenPhotoID = photoIDs[new Random().nextInt(photoIDs.length)];
        br.getPage(getPhotoContentURL(randomChosenPhotoID));
        final String photoJson = br.getRegex("Photo\\.setData\\((\\[.*?\\])\\);\\n").getMatch(0);
        final List<Map<String, Object>> photos = (List<Map<String, Object>>) JavaScriptEngineFactory.jsonToJavaObject(photoJson);
        for (final Map<String, Object> photoinfo : photos) {
            final String photoID = photoinfo.get("id").toString();
            String url = photoinfo.get("photoUrlL").toString();
            if (url.contains("_l.jpg")) {
                /* Get best quality */
                url = url.replaceFirst("_l\\.jpg", "_xl.jpg");
            } else {
                logger.warning("Unable to find best quality for image: " + url + " | ID: " + photoID);
            }
            String title = photoinfo.get("photoName").toString();
            final String photoDescription = (String) photoinfo.get("photoDescription");
            final DownloadLink photo = this.createDownloadlink(url);
            if (!StringUtils.isEmpty(title)) {
                title = title.trim();
                /* 2022-06-11: File-extension should always be .jpg */
                String ext = Plugin.getFileNameExtensionFromURL(url);
                if (ext == null) {
                    ext = ".jpg";
                }
                final String finalFilename = this.correctOrApplyFileNameExtension(title, ext);
                photo.setFinalFileName(finalFilename);
                photo.setProperty(DirectHTTP.FIXNAME, finalFilename);
            }
            photo.setAvailable(true);
            if (!StringUtils.isEmpty(photoDescription)) {
                photo.setComment(photoDescription);
            } else {
                photo.setComment(br.getURL(getPhotoContentURL(photoID)).toString());
            }
            /* Do not set contentURL because we need to login in browser to access it. Rather use directURLs here. */
            // final String contentURL = getPhotoContentURL(photoID);
            // photo.setContentUrl("");
            // photo.setContentUrl(url);
            /* We do not really need this password anymore but let's set it for completion to be able to view it later. */
            if (passCode != null) {
                photo.setDownloadPassword(passCode);
            }
            photo.setLinkID(this.getHost() + "://" + photoID);
            ret.add(photo);
        }
        if (packagename != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(packagename);
            if (passCode != null) {
                fp.setComment("Gallery password: " + passCode);
            }
            fp.addLinks(ret);
        }
        return ret;
    }

    private static String getPhotoContentURL(final String photoID) {
        return "/shop/photo/" + photoID;
    }

    private Form getPassForm(final Browser br) {
        return br.getFormbyProperty("id", "GuestAccessGuestLoginForm");
    }
}
