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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "anilinkz.com" }, urls = { "http://(www\\.)?anilinkz\\.com/[^<>\"/]+(/[^<>\"/]+)?" }, flags = { 0 })
public class AniLinkzCom extends PluginForDecrypt {

    private static final Pattern PATTERN_SUPPORTED_HOSTER         = Pattern.compile("(youtube\\.com|veoh\\.com|nowvideo\\.eu|videobam\\.com|mp4upload\\.com|gorillavid\\.in|putlocker\\.com|veevr\\.com|yourupload\\.com|videoweed\\.com)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_UNSUPPORTED_HOSTER       = Pattern.compile("(facebook\\.com|google\\.com)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_SUPPORTED_FILE_EXTENSION = Pattern.compile("(\\.mp4|\\.flv|\\.fll)", Pattern.CASE_INSENSITIVE);
    private static final String  INVALIDLINKS                     = "http://(www\\.)?anilinkz\\.com/(search|affiliates|get|img|dsa|series|forums|files|category|\\?page=|faqs|.*?\\-list|.*?\\-info|\\?random).*?";

    public AniLinkzCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        if (parameter.matches(INVALIDLINKS)) {
            logger.info("Link invalid: " + parameter);
            return decryptedLinks;
        }
        br.getHeaders().put("Referer", null);
        // Not done yet...
        // if (!cloudflare(parameter)) {
        // logger.warning("Decrypter out of date for link: " + parameter);
        // return null;
        // }
        if (br.containsHTML(">Page Not Found<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        // https://pastee.org/e84rh
        // if (br.containsHTML(">DDoS protection by CloudFlare<")) {
        // final String importantValue = br.getRegex("name=\"jschl_vc\" value=\"([a-z0-9]+)\"").getMatch(0);
        // boolean notDoneYet = true;
        // if (importantValue == null || notDoneYet) {
        // logger.warning("Decrypter out of date for link: " + parameter);
        // return null;
        // }
        // // Needs this code: https://pastee.org/e84rh
        // br.postPage(br.getURL(), "act=jschl&jschl_vc=" + importantValue + "&jschl_answer=171");
        // }
        final Browser br2 = br.cloneBrowser();
        // set filepackage
        final String filepackage = br.getRegex("<h3>(.*?)</h3>").getMatch(0);
        if (filepackage == null) {
            logger.warning("filepackage == null: " + parameter);
            logger.warning("Please report issue to JDownloader Development team!");
            return null;
        }
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
        boolean foundOffline = false;
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
            // we should change this to an array as these can pick up false positives like javascript:void
            dllinks = new Regex(unescape.get(i), "<iframe src=\"(http://[^<>\"]*?)\"").getColumn(0);
            // this is for dailymotion.com embeded links
            if (dllinks == null || dllinks.length == 0) dllinks = new Regex(unescape.get(i), "src=\"(http[^\"]+dailymotion\\.com/swf/[^\"]+)").getColumn(0);
            // for youtube.com links
            if (dllinks == null || dllinks.length == 0) dllinks = new Regex(unescape.get(i), "src=\"(http://(www\\.)?youtube\\.com/v/[^<>\"]*?)\"").getColumn(0);
            if (dllinks == null || dllinks.length == 0) dllinks = new Regex(unescape.get(i), "(href|url|file)=\"?(.*?)\"").getColumn(1);
            if (dllinks == null || dllinks.length == 0) dllinks = new Regex(unescape.get(i), "src=\"(.*?)\"").getColumn(0);

            if (dllinks.length > 0) {
                for (String dllink : dllinks) {
                    try {
                        if (dllink.contains("auengine.com/embed.php")) {
                            br2.getPage(dllink);
                            dllink = br2.getRegex("url:\\s+'([^']+auengine\\.com%2Fvideos%2F[^']+)").getMatch(0);
                            if (dllink != null) {
                                dllink = "directhttp://" + Encoding.htmlDecode(dllink);
                            } else {
                                break;
                            }
                        } else if (dllink.contains("dailymotion.com/swf/")) {
                            decryptedLinks.add(createDownloadlink(dllink));
                        } else if (dllink.contains("videofun.me/embed/")) {
                            br2.getPage(dllink);
                            dllink = br2.getRegex("url:\\s+\"(https?://[^\"]+videofun\\.me%2Fvideos%2F[^\"]+)").getMatch(0);
                            if (dllink != null) {
                                dllink = "directhttp://" + Encoding.htmlDecode(dllink);
                            } else {
                                break;
                            }
                        } else if (dllink.contains("anilinkz.com/get")) {
                            br2.openGetConnection(dllink);
                            if (br2.getRedirectLocation() != null) {
                                dllink = "directhttp://" + br2.getRedirectLocation().toString();
                            } else {
                                break;
                            }
                        } else if (dllink.contains("embed.novamov.com")) {
                            br2.getPage(dllink);
                            dllink = br2.getRegex("flashvars\\.file=\"(.*?)\";").getMatch(0);
                            if (dllink == null) {
                                break;
                            }
                        } else if (dllink.contains("upload2.com")) {
                            br2.getPage(dllink);
                            dllink = br2.getRegex("video=(.*?)&rating").getMatch(0);
                            if (dllink == null) {
                                break;
                            }
                            dllink = "directhttp://" + dllink;
                            mirror = "upload2.com";
                        } else if (dllink.contains("youtube.com")) {
                            dllink = new Regex(dllink, "(http://[\\w\\.]*?youtube\\.com/v/\\w+)&").getMatch(0);
                            if (dllink != null) {
                                dllink = dllink.replace("v/", "watch?v=");
                            } else {
                                break;
                            }
                        } else if (dllink.contains("vidzur.com/embed")) {
                            br.getPage(dllink);
                            dllink = br.getRegex("url: \\'(http://[a-z0-9]+\\.vidzur\\.com[^<>\"]*?)\\',").getMatch(0);
                            if (dllink == null) break;
                            dllink = "directhttp://" + Encoding.htmlDecode(dllink);
                        } else if (dllink.contains("videobam.com/widget/")) {
                            dllink = dllink.replace("videobam.com/widget/", "videobam.com/");
                        } else if (dllink.contains("gorillavid.in/")) {
                            dllink = "http://gorillavid.in/" + new Regex(dllink, "gorillavid\\.in/embed\\-([a-z0-9]{12})").getMatch(0);
                        } else if (dllink.contains("http://www./media")) {
                            logger.info("Found offline link: " + dllink);
                            foundOffline = true;
                            continue;
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
            if (foundOffline) {
                logger.info("Found no downloadable content in link: " + parameter);
                return decryptedLinks;
            }
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(filepackage.trim());
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    /**
     * performs silly cloudflare anti DDoS crapola, made by Raztoki
     */
    private boolean cloudflare(String url) throws Exception {
        try {
            /* not available in old stable */
            br.setAllowedResponseCodes(new int[] { 503 });
        } catch (Throwable e) {
        }
        // we need to reload the page, as first time it failed and allowedResponseCodes wasn't set to allow 503
        br.getPage(url);
        final Form cloudflare = br.getFormbyProperty("id", "ChallengeForm");
        if (cloudflare != null) {
            String math = br.getRegex("\\$\\(\\'#jschl_answer\\'\\)\\.val\\(([^\\)]+)\\);").getMatch(0);
            if (math == null) {
                String variableName = br.getRegex("(\\w+)\\s*=\\s*\\$\\(\'#jschl_answer\'\\);").getMatch(0);
                if (variableName != null) variableName = variableName.trim();
                math = br.getRegex(variableName + "\\.val\\(([^\\)]+)\\)").getMatch(0);
            }
            if (math == null) {
                logger.warning("Couldn't find 'math'");
                return false;
            }
            // use js for now, but change to Javaluator as the provided string doesn't get evaluated by JS according to Javaluator author.
            ScriptEngineManager mgr = new ScriptEngineManager();
            ScriptEngine engine = mgr.getEngineByName("JavaScript");
            cloudflare.put("jschl_answer", String.valueOf(((Double) engine.eval("(" + math + ") + 13")).longValue()));
            Thread.sleep(5500);
            br.submitForm(cloudflare);
            if (br.getFormbyProperty("id", "ChallengeForm") != null) {
                logger.warning("Possible plugin error within cloudflare(). Continuing....");
                return false;
            }
        }
        // remove the setter
        try {
            /* not available in old stable */
            br.setAllowedResponseCodes(new int[] {});
        } catch (Throwable e) {
        }
        return true;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}