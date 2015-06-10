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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.ImgUrCom;
import jd.utils.JDUtilities;

/*Only accept single-imag URLs with an LID-length or either 5 OR 7 - everything else are invalid links or thumbnails*/
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imgur.com" }, urls = { "https?://(www\\.)?imgur\\.com/(gallery|a)/[A-Za-z0-9]{5,7}|https?://i\\.imgur\\.com/(download/)?([A-Za-z0-9]{7}|[A-Za-z0-9]{5})|https?://(www\\.)?imgur\\.com/(download/)?([A-Za-z0-9]{7}|[A-Za-z0-9]{5})" }, flags = { 0 })
public class ImgurCom extends PluginForDecrypt {

    public ImgurCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String            TYPE_ALBUM                    = "https?://(www\\.)?imgur\\.com/a/[A-Za-z0-9]{5,7}";
    private final String            TYPE_GALLERY                  = "https?://(www\\.)?imgur\\.com/gallery/[A-Za-z0-9]{5,7}";
    private static Object           ctrlLock                      = new Object();
    private static AtomicBoolean    pluginLoaded                  = new AtomicBoolean(false);

    /* User settings */
    private static final String     SETTING_USE_API               = "SETTING_USE_API";
    private static final String     SETTING_GRAB_SOURCE_URL_VIDEO = "SETTING_GRAB_SOURCE_URL_VIDEO";
    private static final String     API_FAILED                    = "API_FAILED";

    /* Constants */
    private static long             view_filesizelimit            = 0;

    private ArrayList<DownloadLink> decryptedLinks                = new ArrayList<DownloadLink>();
    private String                  PARAMETER                     = null;
    private String                  LID                           = null;
    private String                  AUTHOR                        = null;
    private String                  VIDEOSOURCE                   = null;
    private boolean                 grabVideoSource               = false;

    /* IMPORTANT: Make sure that we're always using the current version of their API: https://api.imgur.com/ */
    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final SubConfiguration cfg = SubConfiguration.getConfig("imgur.com");
        PARAMETER = param.toString().replace("https://", "http://").replaceFirst("/all$", "");
        LID = new Regex(PARAMETER, "([A-Za-z0-9]+)$").getMatch(0);
        grabVideoSource = cfg.getBooleanProperty(SETTING_GRAB_SOURCE_URL_VIDEO, ImgUrCom.defaultSOURCEVIDEO);
        synchronized (ctrlLock) {
            if (!pluginLoaded.get()) {
                // load plugin!
                JDUtilities.getPluginForHost("imgur.com");
                pluginLoaded.set(true);
            }
            view_filesizelimit = jd.plugins.hoster.ImgUrCom.view_filesizelimit;
            String galleryTitle = null;
            String fpName = null;
            if (PARAMETER.matches(TYPE_ALBUM) || PARAMETER.matches(TYPE_GALLERY)) {
                try {
                    if (!cfg.getBooleanProperty(SETTING_USE_API, false)) {
                        logger.info("User prefers not to use the API");
                        throw new DecrypterException(API_FAILED);
                    }
                    br.getHeaders().put("Authorization", jd.plugins.hoster.ImgUrCom.getAuthorization());
                    try {
                        /* Gallery (single image) */
                        if (PARAMETER.matches(TYPE_GALLERY)) {
                            br.getPage("https://api.imgur.com/3/gallery/:" + LID);
                            if (br.getHttpConnection().getResponseCode() == 404) {
                                /*
                                 * Either it is a gallery with a single photo or it is offline. Seems like there is no way to know this
                                 * before!
                                 */
                                final DownloadLink dl = createDownloadlink(getHostpluginurl(LID));
                                dl.setProperty("imgUID", LID);
                                decryptedLinks.add(dl);
                                return decryptedLinks;
                            }
                            boolean is_album = false;
                            final String is_albumo = getJson(this.br.toString(), "is_album");
                            if (is_albumo != null) {
                                is_album = Boolean.parseBoolean(is_albumo);
                            }
                            if (PARAMETER.matches(TYPE_GALLERY) && is_album) {
                                /* We have a single picture and not an album. */
                                final DownloadLink dl = createDownloadlink(getHostpluginurl(LID));
                                dl.setProperty("imgUID", LID);
                                decryptedLinks.add(dl);
                                return decryptedLinks;
                            }
                        }
                        /* We knopw that we definitly have an album --> Crawl it */
                        br.getPage("https://api.imgur.com/3/album/" + LID);
                        if (br.getHttpConnection().getResponseCode() == 404) {
                            /* Either it is a gallery with a single photo or it is offline. Seems like there is no way to know this before! */
                            final DownloadLink dl = createDownloadlink(getHostpluginurl(LID));
                            dl.setProperty("imgUID", LID);
                            decryptedLinks.add(dl);
                            return decryptedLinks;
                        }
                    } catch (final BrowserException e) {
                        if (br.getHttpConnection().getResponseCode() == 429) {
                            logger.info("API limit reached, using site");
                            throw new DecrypterException(API_FAILED);
                        }
                        logger.info("Server problems: " + PARAMETER);
                        return decryptedLinks;
                    }
                    if (br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404) {
                        return createOfflineLink(PARAMETER);
                    }
                    br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
                    galleryTitle = getJson(br.toString(), "title");
                    if ("null".equals(galleryTitle)) {
                        galleryTitle = null;
                    }
                    api_decrypt();
                } catch (final DecrypterException e) {
                    /* Make sure we only continue if the API failed or was disabled by the user. */
                    if (!e.getMessage().equals(API_FAILED)) {
                        throw e;
                    }
                    try {
                        br.setLoadLimit(br.getLoadLimit() * 2);
                    } catch (final Throwable eFUstable) {
                        /* Not available in old 0.9.581 Stable */
                    }
                    try {
                        br.getPage(PARAMETER);
                    } catch (final BrowserException ebr) {
                        logger.info("Server problems: " + PARAMETER);
                        return decryptedLinks;
                    }
                    if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("class=\"textbox empty\"|<h1>Zoinks! You've taken a wrong turn\\.</h1>|it's probably been deleted or may not have existed at all\\.</p>")) {
                        return createOfflineLink(PARAMETER);
                    }
                    AUTHOR = this.br.getRegex("property=\"author\" content=\"([^<>\"]*?)\"").getMatch(0);
                    galleryTitle = br.getRegex("<title>([^<>\"]*?) \\- Imgur</title>").getMatch(0);
                    final String album_info = br.getRegex("\"album_images\":\\{(.+)").getMatch(0);
                    if (album_info != null) {
                        final String count_pics_str = new Regex(album_info, "\"count\":(\\d+)").getMatch(0);
                        /* Only load that if needed - it won't work for galleries with only 1 picture e.g. */
                        if (count_pics_str != null && Long.parseLong(count_pics_str) >= 10) {
                            logger.info("siteDecrypt: loading json to get all pictures");
                            br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                            br.getPage("http://imgur.com/gallery/" + LID + "/album_images/hit.json?all=true");
                            br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
                        }
                    }
                    site_decrypt();
                }

                if (galleryTitle != null) {
                    galleryTitle = Encoding.htmlDecode(galleryTitle).trim();
                }
                fpName = jd.plugins.hoster.ImgUrCom.getFormattedPackagename(AUTHOR, galleryTitle, LID);
                fpName = encodeUnicode(fpName);
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                fp.addLinks(decryptedLinks);
            } else {
                final DownloadLink dl = createDownloadlink(getHostpluginurl(LID));
                dl.setProperty("imgUID", LID);
                decryptedLinks.add(dl);
            }
        }
        return decryptedLinks;
    }

    private String getHostpluginurl(final String lid) {
        return "http://imgurdecrypted.com/download/" + lid;
    }

    private void api_decrypt() throws DecrypterException, ParseException {
        if (br.containsHTML("\"status\":404")) {
            /* Well in case it's a gallery link it might be a single picture */
            if (PARAMETER.matches(TYPE_GALLERY)) {
                final DownloadLink dl = createDownloadlink(getHostpluginurl(LID));
                dl.setProperty("imgUID", LID);
                decryptedLinks.add(dl);
                return;
            }
            decryptedLinks.addAll(createOfflineLink(PARAMETER));
            return;
        }
        final int imgcount = Integer.parseInt(getJson(br.toString(), "images_count"));
        /* Either no images there or sometimes the number of wrong. */
        if (imgcount == 0 || br.containsHTML("images\":\\[\\]")) {
            decryptedLinks.addAll(createOfflineLink(PARAMETER));
            return;
        }

        AUTHOR = getJson(this.br.toString(), "account_url");
        /*
         * using links (i.imgur.com/imgUID(s)?.extension) seems to be problematic, it can contain 's' (imgUID + s + .extension), but not
         * always! imgUid.endswith("s") is also a valid uid, so you can't strip them!
         */
        final String jsonarray = br.getRegex("\"images\":\\[(\\{.*?\\})\\]").getMatch(0);
        String[] items = jsonarray.split("\\},\\{");
        /* We assume that the API is always working fine */
        if (items == null || items.length == 0) {
            logger.info("Empty album: " + PARAMETER);
            return;
        }
        for (final String item : items) {
            String directlink = getJson(item, "link");
            String title = getJson(item, "title");
            final String filesize_str = getJson(item, "size");
            final String imgUID = getJson(item, "id");
            /* TODO: */
            VIDEOSOURCE = null;
            final String filetype = new Regex(item, "\"type\":\"image/([^<>\"]*?)\"").getMatch(0);
            if (imgUID == null || filesize_str == null || directlink == null || filetype == null) {
                logger.warning("Decrypter broken for link: " + PARAMETER);
                throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
            }
            if (title != null && !title.equals("")) {
                title = Encoding.htmlDecode(title);
                title = HTMLEntities.unhtmlentities(title);
                title = HTMLEntities.unhtmlAmpersand(title);
                title = HTMLEntities.unhtmlAngleBrackets(title);
                title = HTMLEntities.unhtmlSingleQuotes(title);
                title = HTMLEntities.unhtmlDoubleQuotes(title);
                title = encodeUnicode(title);
            }
            final long filesize = Long.parseLong(filesize_str);
            final DownloadLink dl = createDownloadlink("http://imgurdecrypted.com/download/" + imgUID);
            dl.setAvailable(true);
            dl.setProperty("imgUID", imgUID);
            dl.setProperty("filetype", filetype);
            /*
             * Note that for pictures/especially GIFs over 20 MB, the "link" value will only contain a link which leads to a preview or low
             * quality version of the picture. This is why we need a little workaround for this case (works from 19.5++ MB).
             */
            if (filesize >= view_filesizelimit) {
                directlink = jd.plugins.hoster.ImgUrCom.getBigFileDownloadlink(dl);
            }
            dl.setProperty("directlink", directlink);
            dl.setProperty("decryptedfilesize", filesize);
            dl.setProperty("directtitle", title);
            dl.setProperty("directusername", AUTHOR);
            final String filename = jd.plugins.hoster.ImgUrCom.getFormattedFilename(dl);
            dl.setFinalFileName(filename);
            dl.setDownloadSize(filesize);
            /* No need to hide directlinks */
            try {
                /* JD2 only */
                dl.setContentUrl(jd.plugins.hoster.ImgUrCom.getURLContent(imgUID));
            } catch (Throwable e) {
                /* Stable */
                dl.setContentUrl(jd.plugins.hoster.ImgUrCom.getURLContent(imgUID));
            }
            if (VIDEOSOURCE != null && grabVideoSource) {
                decryptedLinks.add(this.createDownloadlink(VIDEOSOURCE));
            }
            decryptedLinks.add(dl);
        }
    }

    private void site_decrypt() throws DecrypterException, ParseException {
        /* Removed differentiation between two linktypes AFTER revision 26468 */
        String jsonarray = br.getRegex("\"images\":\\[(\\{.*?\\})\\]").getMatch(0);
        if (jsonarray == null) {
            jsonarray = br.getRegex("\"items\":\\[(.*?)\\]").getMatch(0);
        }
        /* Maybe it's just a single image in a gallery */
        if (jsonarray == null) {
            jsonarray = br.getRegex("image[\t\n\r ]+:[\t\n\r ]+(\\{.*?\\})").getMatch(0);
        }
        if (jsonarray == null) {
            throw new DecrypterException("Decrypter broken!");
        }
        String[] items = jsonarray.split("\\},\\{");
        /* We assume that the API is always working fine */
        if (items == null || items.length == 0) {
            logger.info("Empty album: " + PARAMETER);
            return;
        }
        for (final String item : items) {
            String directlink;
            String title = getJson(item, "title");
            final String filesize_str = getJson(item, "size");
            final String imgUID = getJson(item, "hash");
            VIDEOSOURCE = getJson(item, "video_source");
            String ext = getJson(item, "ext");
            if (imgUID == null || filesize_str == null || ext == null) {
                logger.warning("Decrypter broken for link: " + PARAMETER);
                throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
            }
            final long filesize = Long.parseLong(filesize_str);
            /* Correct sometimes broken ext TODO: Wait for a response of their support - this might be a serverside issue. */
            if (ext.contains("?")) {
                ext = ext.substring(0, ext.lastIndexOf("?"));
            }
            if (title != null && !title.equals("")) {
                title = Encoding.htmlDecode(title);
                title = HTMLEntities.unhtmlentities(title);
                title = HTMLEntities.unhtmlAmpersand(title);
                title = HTMLEntities.unhtmlAngleBrackets(title);
                title = HTMLEntities.unhtmlSingleQuotes(title);
                title = HTMLEntities.unhtmlDoubleQuotes(title);
                title = encodeUnicode(title);
            }
            final DownloadLink dl = createDownloadlink("http://imgurdecrypted.com/download/" + imgUID);
            dl.setDownloadSize(filesize);
            dl.setAvailable(true);
            dl.setProperty("imgUID", imgUID);
            dl.setProperty("filetype", ext.replace(".", ""));
            /*
             * Note that for pictures/especially GIFs over 20 MB, the "link" value will only contain a link which leads to a preview or low
             * quality version of the picture. This is why we need a little workaround for this case (works from 19.5++ MB).
             */
            if (filesize >= view_filesizelimit) {
                directlink = "http://imgur.com/download/" + imgUID;
                directlink = jd.plugins.hoster.ImgUrCom.getBigFileDownloadlink(dl);
            } else {
                directlink = "http://i.imgur.com/" + imgUID + ext;
            }
            dl.setProperty("directlink", directlink);
            dl.setProperty("decryptedfilesize", filesize);
            dl.setProperty("directtitle", title);
            dl.setProperty("directusername", AUTHOR);
            final String filename = jd.plugins.hoster.ImgUrCom.getFormattedFilename(dl);
            dl.setFinalFileName(filename);
            /* No need to hide directlinks */
            try {
                /* JD2 only */
                dl.setContentUrl(jd.plugins.hoster.ImgUrCom.getURLContent(imgUID));
            } catch (Throwable e) {
                /* Stable */
                dl.setContentUrl(jd.plugins.hoster.ImgUrCom.getURLContent(imgUID));
            }
            if (VIDEOSOURCE != null && grabVideoSource) {
                decryptedLinks.add(this.createDownloadlink(VIDEOSOURCE));
            }
            decryptedLinks.add(dl);
        }
    }

    private String getJson(final String source, final String parameter) {
        String result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?([0-9\\.]+)").getMatch(1);
        if (result == null) {
            result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?\"([^<>\"]*?)\"").getMatch(1);
        }
        return result;
    }

    /** Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    private ArrayList<DownloadLink> createOfflineLink(final String link) {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final DownloadLink offline = createDownloadlink("directhttp://" + link);
        offline.setAvailable(false);
        if (LID != null) {
            offline.setFinalFileName(LID);
        }
        offline.setProperty("OFFLINE", true);
        decryptedLinks.add(offline);
        return decryptedLinks;
    }

}
