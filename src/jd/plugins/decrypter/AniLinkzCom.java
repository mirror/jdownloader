//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "anilinkz.com" }, urls = { "http://(www\\.)?anilinkz\\.com/(?!get|img|dsa|series|forums|files).+/.+" }, flags = { 0 })
public class AniLinkzCom extends PluginForDecrypt {

    private static final Pattern PATTERN_SUPPORTED_HOSTER         = Pattern.compile("(youtube\\.com|veoh\\.com)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_UNSUPPORTED_HOSTER       = Pattern.compile("(facebook\\.com|google\\.com)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_SUPPORTED_FILE_EXTENSION = Pattern.compile("(\\.mp4|\\.flv|\\.fll)", Pattern.CASE_INSENSITIVE);

    public AniLinkzCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getHeaders().put("Referer", null);
        br.getPage(parameter);
        if (br.containsHTML(">Page Not Found<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final Browser br2 = br.cloneBrowser();
        // set filepackage
        final String filepackage = br.getRegex("<h3>(.*?)</h3>").getMatch(0);
        // get Mirrors
        int mirrorCount = 0;
        final String[] img = br.getRegex("(/images/src\\d+\\.png)").getColumn(0);
        for (int i = img.length - 1; i > 0; i--) {
            br2.openGetConnection(img[i]);
            if (br2.getRequest().getHttpConnection().getResponseCode() == 404) {
                mirrorCount = i;
            } else {
                mirrorCount = i;
                break;
            }
        }
        final List<String> unescape = new ArrayList<String>();
        String[] dllinks;
        String mirror = null;
        // get dllinks
        for (int i = 0; i <= mirrorCount; i++) {
            String escapeAll = br.getRegex("escapeall\\('(.*)'\\)\\)\\);").getMatch(0);
            if (escapeAll != null) {
                escapeAll = escapeAll.replaceAll("[A-Z~!@#\\$\\*\\{\\}\\[\\]\\-\\+\\.]?", "");
            } else {
                logger.warning("Decrypter out of date for link: " + parameter);
                return null;
            }
            unescape.add(Encoding.htmlDecode(escapeAll));
            dllinks = new Regex(unescape.get(i), "(href|url|file)=\"?(.*?)\"").getColumn(1);
            if (dllinks == null || dllinks.length == 0) {
                dllinks = new Regex(unescape.get(i), "src=\"(.*?)\"").getColumn(0);
            }
            if (dllinks.length > 0) {
                for (String dllink : dllinks) {
                    try {
                        if (dllink.contains("anilinkz.com/get")) {
                            br2.openGetConnection(dllink);
                            if (br2.getRedirectLocation() != null) {
                                dllink = "directhttp://" + br2.getRedirectLocation().toString();
                            } else {
                                break;
                            }
                        }
                        if (dllink.contains("embed.novamov.com")) {
                            br2.getPage(dllink);
                            dllink = br2.getRegex("flashvars.file=\"(.*?)\";").getMatch(0);
                            if (dllink == null) {
                                break;
                            }
                        }
                        if (dllink.contains("upload2.com")) {
                            br2.getPage(dllink);
                            dllink = br2.getRegex("video=(.*?)&rating").getMatch(0);
                            if (dllink == null) {
                                break;
                            }
                            dllink = "directhttp://" + dllink;
                            mirror = "upload2.com";
                        }
                        if (dllink.contains("youtube.com")) {
                            dllink = new Regex(dllink, "(http://[\\w\\.]*?youtube\\.com/v/\\w+)&").getMatch(0);
                            if (dllink != null) {
                                dllink = dllink.replace("v/", "watch?v=");
                            } else {
                                break;
                            }
                        }
                    } catch (final Exception e) {
                        logger.log(Level.SEVERE, e.getMessage(), e);
                        continue;
                    }
                    if (mirror == null) {
                        mirror = new Regex(dllink, "http://.*?\\.(\\w+\\.\\w+)/.*?").getMatch(0);
                    }
                    if (mirrorCount == 0 && new Regex(mirror, PATTERN_UNSUPPORTED_HOSTER).count() == 1) {
                        logger.warning(mirror + " is not supported yet! Link: " + parameter);
                        return null;
                    }
                    if (new Regex(dllink, PATTERN_SUPPORTED_FILE_EXTENSION).count() == 1 || new Regex(dllink, PATTERN_SUPPORTED_HOSTER).count() == 1) {
                        String ext = dllink.substring(dllink.lastIndexOf("."));
                        if (ext.length() > 3) {
                            ext = ext.substring(0, 4);
                        }
                        final DownloadLink dl = createDownloadlink(dllink.trim());
                        final FilePackage fp = FilePackage.getInstance();
                        fp.setName(filepackage.trim());
                        fp.add(dl);
                        dl.setProperty("removeReferer", true);
                        if (ext != null && new Regex(dllink, PATTERN_SUPPORTED_HOSTER).count() == 0) {
                            final String filename = filepackage + "_mirror_" + (i + 1) + "_" + mirror + ext;
                            dl.setFinalFileName(filename.trim());
                        }
                        mirror = null;
                        decryptedLinks.add(dl);
                    }
                }
            }
            if (mirrorCount > i) {
                br.getPage(parameter + (i + 2) + "/");
            }
        }
        if (decryptedLinks.size() == 0) {
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }
}