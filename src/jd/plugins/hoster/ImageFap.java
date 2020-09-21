//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.text.ParseException;
import java.util.HashMap;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "imagefap.com" }, urls = { "https?://(www\\.)?imagefap.com/(imagedecrypted/\\d+|video\\.php\\?vid=\\d+)" })
public class ImageFap extends PluginForHost {
    public ImageFap(final PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        // this.setStartIntervall(500l);
    }

    private static final String CUSTOM_FILENAME = "CUSTOM_FILENAME";

    public void correctDownloadLink(DownloadLink link) {
        final String addedLink = link.getDownloadURL();
        if (addedLink.contains("imagedecrypted/")) {
            final String newurl = "https://www.imagefap.com/photo/" + new Regex(addedLink, "(\\d+)$").getMatch(0) + "/";
            link.setUrlDownload(newurl);
            link.setContentUrl(newurl);
        }
    }

    public static Browser prepBR(final Browser br) {
        br.setAllowedResponseCodes(new int[] { 429 });
        return br;
    }

    private static final String VIDEOLINK = "https?://(www\\.)?imagefap.com/video\\.php\\?vid=\\d+";

    private String DecryptLink(final String code) {
        try {
            final String s1 = Encoding.htmlDecode(code.substring(0, code.length() - 1));
            String t = "";
            for (int i = 0; i < s1.length(); i++) {
                // logger.info("decrypt4 " + i);
                // logger.info("decrypt5 " + ((int) (s1.charAt(i+1) - '0')));
                // logger.info("decrypt6 " +
                // (Integer.parseInt(code.substring(code.length()-1,code.length()
                // ))));
                final int charcode = s1.charAt(i) - Integer.parseInt(code.substring(code.length() - 1, code.length()));
                // logger.info("decrypt7 " + charcode);
                t = t + Character.valueOf((char) charcode).toString();
                // t+=new Character((char)
                // (s1.charAt(i)-code.charAt(code.length()-1)));
            }
            // logger.info(t);
            // var s1=unescape(s.substr(0,s.length-1)); var t='';
            // for(i=0;i<s1.length;i++)t+=String.fromCharCode(s1.charCodeAt(i)-s.
            // substr(s.length-1,1));
            // return unescape(t);
            // logger.info("return of DecryptLink(): " +
            // JDUtilities.htmlDecode(t));
            return Encoding.htmlDecode(t);
        } catch (final Exception e) {
            logger.log(e);
        }
        return null;
    }

    @Override
    public String getAGBLink() {
        return "http://imagefap.com/faq.php";
    }

    private String getGalleryName(final DownloadLink dl) {
        String galleryName = dl.getStringProperty("galleryname");
        if (galleryName == null) {
            // galleryName = br.getRegex("<font face=verdana size=3>([^<>\"]*?)<BR>").getMatch(0);
            galleryName = br.getRegex("<font[^<>]*?itemprop=\"name\"[^<>]*?>([^<>]+)<").getMatch(0);
            if (galleryName == null) {
                galleryName = br.getRegex("<title>.*? in gallery ([^<>\"]*?) \\(Picture \\d+\\) uploaded by").getMatch(0);
            }
        }
        return galleryName;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        String pfilename = downloadLink.getName();
        br.getPage(downloadLink.getDownloadURL());
        if (downloadLink.getDownloadURL().matches(VIDEOLINK)) {
            String configLink = br.getRegex("flashvars\\.config = escape\\(\"(https?://[^<>\"]*?)\"").getMatch(0);
            if (configLink == null) {
                /* 2020-03-23 */
                configLink = br.getRegex("url\\s*:\\s*'(http[^<>\"\\']+)\\'").getMatch(0);
            }
            if (configLink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(configLink);
            String finallink = br.getRegex("<videoLink>(https?://[^<>\"]*?)</videoLink>").getMatch(0);
            if (finallink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (Encoding.isHtmlEntityCoded(finallink)) {
                finallink = Encoding.htmlDecode(finallink);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else {
            String imagelink = br.getRegex("name=\"mainPhoto\".*src=\"(https?://[a-z0-9\\.\\-]+\\.imagefapusercontent\\.com/[^<>\"]+)\"").getMatch(0);
            // if (imagelink == null) {
            // String ID = new Regex(downloadLink.getDownloadURL(), "(\\d+)").getMatch(0);
            // imagelink = br.getRegex("href=\"http://img\\.imagefapusercontent\\.com/images/full/\\d+/\\d+/" + ID +
            // "\\.jpe?g\" original=\"(http://fap.to/images/full/\\d+/\\d+/" + ID + "\\.jpe?g)\"").getMatch(0);
            // }
            if (imagelink == null) {
                final String returnID = new Regex(br, Pattern.compile("return lD\\(\\'(\\S+?)\\'\\);", Pattern.CASE_INSENSITIVE)).getMatch(0);
                if (returnID != null) {
                    imagelink = DecryptLink(returnID);
                }
                if (imagelink == null) {
                    imagelink = br.getRegex("onclick=\"OnPhotoClick\\(\\);\" src=\"(https?://.*?)\"").getMatch(0);
                    if (imagelink == null) {
                        imagelink = br.getRegex("href=\"#\" onclick=\"javascript:window\\.open\\(\\'(https?://.*?)\\'\\)").getMatch(0);
                        if (imagelink == null) {
                            imagelink = br.getRegex("\"contentUrl\"\\s*>\\s*(https?://cdn\\.imagefap\\.com/images/full/.*?)\\s*<").getMatch(0);
                        }
                    }
                }
            }
            if (imagelink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // Only set subdirectory if it wasn't set before or we'll get
            // subfolders
            // in subfolders which is bad
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, imagelink, false, 1);
            final long t = dl.getConnection().getContentLength();
            if (dl.getConnection().getResponseCode() == 404 || (t != -1 && t < 107)) {
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (!pfilename.endsWith(new Regex(imagelink, "(\\.[A-Za-z0-9]+)($|\\?)").getMatch(0))) {
                pfilename += new Regex(imagelink, "(\\.[A-Za-z0-9]+)($|\\?)").getMatch(0);
            }
            downloadLink.setFinalFileName(pfilename);
        }
        dl.startDownload();
    }

    @Override
    public void init() {
        try {
            // Browser.setRequestIntervalLimitGlobal(getHost(), 600, 100, 60000);
            Browser.setRequestIntervalLimitGlobal(getHost(), 600);
        } catch (final Throwable e) {
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException {
        prepBR(this.br);
        try {
            br.getPage(link.getDownloadURL());
            if (br.getHttpConnection().getResponseCode() == 429) {
                /*
                 * 2020-09-21: Newly introduced rate limits:
                 * https://www.imagefaq.cc/forum/viewtopic.php?f=4&t=17675&sid=0ed66fda947338862f2cb3d32622e030
                 */
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Error 429 rate limit reached", 5 * 60 * 1000l);
            }
            if (link.getDownloadURL().matches(VIDEOLINK)) {
                final String filename = br.getRegex(">Title:</td>[\t\n\r ]+<td width=35%>([^<>\"]*?)</td>").getMatch(0);
                if (filename == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                link.setFinalFileName(Encoding.htmlDecode(filename) + ".flv");
            } else {
                final String location = br.getRedirectLocation();
                if (location != null) {
                    if (!location.contains("/photo/")) {
                        br.getPage(location);
                    }
                    logger.info("Setting new downloadUrl: " + location);
                    link.setUrlDownload(location);
                    br.getPage(location);
                }
                if (br.containsHTML("(>The image you are trying to access does not exist|<title> \\(Picture 1\\) uploaded by  on ImageFap\\.com</title>)")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                String picture_name = link.getStringProperty("original_filename");
                if (picture_name == null) {
                    picture_name = br.getRegex("<title>(.*?) in gallery").getMatch(0);
                    if (picture_name == null) {
                        picture_name = br.getRegex("<title>(.*?) uploaded by").getMatch(0);
                        if (picture_name == null) {
                            picture_name = br.getRegex("<title>(.*?) Porn Pic").getMatch(0);
                        }
                    }
                }
                String galleryName = getGalleryName(link);
                String username = link.getStringProperty("directusername");
                if (username == null) {
                    username = br.getRegex("<b><font size=\"4\" color=\"#CC0000\">(.*?)\\'s gallery</font></b>").getMatch(0);
                    if (username == null) {
                        username = br.getRegex("<td class=\"mnu0\"><a href=\"/profile\\.php\\?user=(.*?)\"").getMatch(0);
                        if (username == null) {
                            username = br.getRegex("jQuery\\.BlockWidget\\(\\d+,\"(.*?)\",\"left\"\\);").getMatch(0);
                            if (username == null) {
                                username = br.getRegex("Uploaded by ([^<>\"]+)</font>").getMatch(0);
                            }
                        }
                    }
                }
                if (galleryName == null || picture_name == null) {
                    logger.info("galleryName: " + galleryName + " picture_name: " + picture_name);
                    // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                galleryName = Encoding.htmlDecode(galleryName).trim();
                if (username != null) {
                    username = username.trim();
                }
                link.setProperty("galleryname", galleryName);
                link.setProperty("directusername", username);
                link.setProperty("original_filename", picture_name);
                link.setFinalFileName(getFormattedFilename(link));
                /* only set filepackage if not set yet */
                try {
                    if (FilePackage.isDefaultFilePackage(link.getFilePackage())) {
                        final FilePackage fp = FilePackage.getInstance();
                        fp.setName(username + " - " + galleryName);
                        fp.add(link);
                    }
                } catch (final Throwable e) {
                    /*
                     * does not work in stable 0.9580, can be removed with next major update
                     */
                    try {
                        if (link.getFilePackage() == FilePackage.getDefaultFilePackage()) {
                            final FilePackage fp = FilePackage.getInstance();
                            fp.setName(username + " - " + galleryName);
                            fp.add(link);
                        }
                    } catch (final Throwable e2) {
                    }
                }
            }
            return AvailableStatus.TRUE;
        } catch (final Exception e) {
            logger.log(e);
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    /** Returns either the original server filename or one that is very similar to the original */
    @SuppressWarnings("deprecation")
    public static String getFormattedFilename(final DownloadLink downloadLink) throws ParseException {
        final SubConfiguration cfg = SubConfiguration.getConfig("imagefap.com");
        final String username = downloadLink.getStringProperty("directusername", "-");
        final String original_filename = downloadLink.getStringProperty("original_filename", null);
        final String galleryname = downloadLink.getStringProperty("galleryname", null);
        final String orderid = downloadLink.getStringProperty("orderid", "-");
        /* Date: Maybe add this in the future, if requested by a user. */
        // final long date = getLongProperty(downloadLink, "originaldate", 0l);
        // String formattedDate = null;
        // /* Get correctly formatted date */
        // String dateFormat = "yyyy-MM-dd";
        // SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
        // Date theDate = new Date(date);
        // try {
        // formatter = new SimpleDateFormat(dateFormat);
        // formattedDate = formatter.format(theDate);
        // } catch (Exception e) {
        // /* prevent user error killing plugin */
        // formattedDate = "";
        // }
        // /* Get correctly formatted time */
        // dateFormat = "HHmm";
        // String time = "0000";
        // try {
        // formatter = new SimpleDateFormat(dateFormat);
        // time = formatter.format(theDate);
        // } catch (Exception e) {
        // /* prevent user error killing plugin */
        // time = "0000";
        // }
        String formattedFilename = cfg.getStringProperty(CUSTOM_FILENAME, defaultCustomFilename);
        if (!formattedFilename.contains("*username*") && !formattedFilename.contains("*title*") && !formattedFilename.contains("*galleryname*")) {
            formattedFilename = defaultCustomFilename;
        }
        formattedFilename = formattedFilename.replace("*orderid*", orderid);
        formattedFilename = formattedFilename.replace("*username*", username);
        formattedFilename = formattedFilename.replace("*galleryname*", galleryname);
        formattedFilename = formattedFilename.replace("*title*", original_filename);
        return formattedFilename;
    }

    private HashMap<String, String> phrasesEN = new HashMap<String, String>() {
                                                  {
                                                      put("SETTING_TAGS", "Explanation of the available tags:\r\n*username* = Name of the user who posted the content\r\n*title* = Original title of the picture including file extension\r\n*galleryname* = Name of the gallery in which the picture is listed\r\n*orderid* = Position of the picture in a gallery e.g. '0001'");
                                                      put("LABEL_FILENAME", "Define custom filename for pictures:");
                                                  }
                                              };
    private HashMap<String, String> phrasesDE = new HashMap<String, String>() {
                                                  {
                                                      put("SETTING_TAGS", "Erklärung der verfügbaren Tags:\r\n*username* = Name des Benutzers, der den Inhalt veröffentlicht hat \r\n*title* = Originaler Dateiname mitsamt Dateiendung\r\n*galleryname* = Name der Gallerie, in der sich das Bild befand\r\n*orderid* = Position des Bildes in einer Gallerie z.B. '0001'");
                                                      put("LABEL_FILENAME", "Gib das Muster des benutzerdefinierten Dateinamens für Bilder an:");
                                                  }
                                              };

    /**
     * Returns a German/English translation of a phrase. We don't use the JDownloader translation framework since we need only German and
     * English.
     *
     * @param key
     * @return
     */
    private String getPhrase(String key) {
        if ("de".equals(System.getProperty("user.language")) && phrasesDE.containsKey(key)) {
            return phrasesDE.get(key);
        } else if (phrasesEN.containsKey(key)) {
            return phrasesEN.get(key);
        }
        return "Translation not found!";
    }

    @Override
    public String getDescription() {
        return "JDownloader's imagefap.com plugin helps downloading videos and images from ImageFap. JDownloader provides settings for custom filenames.";
    }

    private static final String defaultCustomFilename = "*username* - *galleryname* - *orderid**title*";

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME, getPhrase("LABEL_FILENAME")).setDefaultValue(defaultCustomFilename));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, getPhrase("SETTING_TAGS")));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}