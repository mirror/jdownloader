//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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
package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "multiup.org" }, urls = { "https?://(www\\.)?multiup\\.(org|eu)/(?:en|fr/)?(fichiers/download/[a-z0-9]{32}_[^<> \"'&%]+|([a-z]{2}/)?(download|mirror)/[a-z0-9]{32}(/[^<> \"'&%]+)?|\\?lien=[a-z0-9]{32}_[^<> \"'&%]+|[a-f0-9]{32})" })
public class MultiupOrg extends antiDDoSForDecrypt {
    // DEV NOTES:
    // DO NOT REMOVE COMPONENTS YOU DONT UNDERSTAND! When in doubt ask raztoki to fix.
    //
    // break down of link formats, old and dead formats work with uid transfered into newer url structure.
    // /?lien=842fab872a0a9618f901b9f4ea986d47_bawls_doctorsdiary202.avi = dead url structure phased out
    // /fichiers/download/d249b81f92d7789a1233e500a0319906_FIQHwASOOL_75_rar = old url structure, but redirects
    // (/fr)?/download/d249b81f92d7789a1233e500a0319906/FIQHwASOOL_75_rar, = new link structure!
    //
    // uid and filename are required to be a valid links for all link structures!
    public MultiupOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        String parameter = param.toString().replaceFirst("://multiup", "://www.multiup");
        parameter = parameter.replaceFirst("\\.org/en/", ".org/");
        parameter = parameter.replaceFirst("\\.org/fr/", ".org/");
        parameter = parameter.replaceFirst("\\.eu/en/", ".eu/");
        parameter = parameter.replaceFirst("\\.eu/fr/", ".eu/");
        parameter = parameter.replaceFirst("http://", "https://");
        final String uid = getFUID(parameter);
        if (uid == null) {
            logger.info("URL is invalid, must contain 'uid' to be valid " + parameter);
            return decryptedLinks;
        }
        String filename = getFilename(parameter);
        String filesize = getFileSize(parameter);
        if (filename != null) {
            parameter = new Regex(parameter, "(https?://[^/]+)").getMatch(0) + "/en/download/" + uid + "/" + filename;
            param.setCryptedUrl(parameter);
        }
        getPage(parameter.replace("/en/download/", "/en/mirror/"));
        final String webSiteFilename = getWebsiteFileName(br);
        if (!StringUtils.isEmpty(webSiteFilename)) {
            filename = webSiteFilename;
        }
        final String webSiteFileSize = getWebsiteFileSize(br);
        if (filesize == null) {
            filesize = webSiteFileSize;
        }
        if (br.containsHTML("The file does not exist any more\\.<|<h1>The server returned a \"404 Not Found\"\\.</h2>|<h1>Oops! An Error Occurred</h1>|>File not found|>No link currently available")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (br.containsHTML("g-recaptcha")) {
            final Form form = br.getFormbyActionRegex("/mirror/");
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
            form.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            submitForm(form);
            if (br.containsHTML("g-recaptcha")) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
        }
        final String[] links = br.getRegex("\\s+href=\"([^\"]+)\"\\s+").getColumn(0);
        if (links == null || links.length == 0) {
            logger.info("Could not find links, please report this to JDownloader Development Team. " + parameter);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (String singleLink : links) {
            if (isAbort()) {
                break;
            }
            String finalURL = null;
            if (StringUtils.containsIgnoreCase(singleLink, "/redirect-to-host/")) {
                singleLink = br.getURL(singleLink).toString();
                final Browser brc = br.cloneBrowser();
                brc.setFollowRedirects(false);
                brc.getPage(singleLink);
                boolean retry = true;
                while (!isAbort()) {
                    finalURL = brc.getRedirectLocation();
                    if (finalURL == null) {
                        finalURL = brc.getRegex("http-equiv\\s*=\\s*\"refresh\"\\s*content\\s*=\\s*\"[^\"]*url\\s*=\\s*(.*?)\"").getMatch(0);
                        if (finalURL != null) {
                            if (retry && StringUtils.contains(finalURL, "multinews.me")) {
                                // first/randomly opens sort of *show some ads* website, on retry the real destination is given
                                retry = false;
                                brc.getPage(singleLink);
                                continue;
                            }
                            if (!StringUtils.containsIgnoreCase(finalURL, "/redirect-to-host/") || finalURL.startsWith("http")) {
                                break;
                            }
                        }
                        if (finalURL != null) {
                            if (finalURL.matches("^.+/\\d+$")) {
                                // seems we can skip the redirects
                                finalURL = finalURL.substring(0, finalURL.length() - 2);
                            }
                            sleep(1000, param);
                            finalURL = brc.getURL(finalURL).toString();
                            brc.setRequest(null);
                            brc.setCurrentURL(null);
                            brc.getPage(finalURL);
                        } else {
                            finalURL = brc.getRedirectLocation();
                            if (finalURL == null) {
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                        }
                    } else {
                        break;
                    }
                }
            } else if (singleLink.startsWith("http")) {
                finalURL = singleLink.trim().replaceFirst(":/+", "://");
            }
            if (finalURL != null) {
                final DownloadLink downloadLink = createDownloadlink(finalURL);
                if (filename != null) {
                    downloadLink.setFinalFileName(filename);
                }
                if (filesize != null) {
                    downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
                }
                distribute(downloadLink);
                decryptedLinks.add(downloadLink);
            }
        }
        return decryptedLinks;
    }

    private String getFileSize(String parameter) throws Exception {
        if (br.getRequest() == null) {
            getPage(parameter);
        }
        return getWebsiteFileSize(br);
    }

    private String getFilename(String parameter) throws Exception {
        String filename = new Regex(parameter, "/[0-9a-f]{32}(?:/|_)([^/]+)").getMatch(0);
        if (filename == null) {
            // here it can be present within html source
            getPage(parameter);
            return getWebsiteFileName(br);
        }
        return filename;
    }

    private String getWebsiteFileSize(Browser br) {
        String fileSize = br.getRegex("Size\\s*:\\s*([0-9\\.]+\\s*[GMK]iB)\\s*<br").getMatch(0);
        if (fileSize == null) {
            fileSize = br.getRegex("\"description\"\\s*content\\s*=\\s*\"\\s*(?:Mirror\\s*list|Download)\\s*.*?\\s*\\(([0-9\\.]+\\s*[GMK]iB)\\)").getMatch(0);
        }
        return fileSize;
    }

    private String getWebsiteFileName(Browser br) {
        String fileName = br.getRegex("<title>\\s*Download\\s*(.*?)\\s*-\\sMirror").getMatch(0);
        if (fileName == null) {
            fileName = br.getRegex("<meta name=\"description\" content=\"Download\\s*(.*?)\\s*\\(").getMatch(0);
            if (fileName == null) {
                fileName = br.getRegex("<title>\\s*Mirror list\\s*(.*?)\\s*-\\sMirror").getMatch(0);
            }
        }
        return fileName;
    }

    private String getFUID(String parameter) {
        final String fuid = new Regex(parameter, "(?:_|/)([a-f0-9]{32})").getMatch(0);
        return fuid;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}