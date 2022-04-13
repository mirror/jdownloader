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

import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class VideoOneLife extends PornEmbedParser {
    public VideoOneLife(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "video-one.com", "video-one.life" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/((?:[a-z]+/)?pornvideo/[a-z0-9]+|player/[^/]+/)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String fuid = new Regex(param.getCryptedUrl(), "/([^/]+)/?$").getMatch(0);
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("embedframe")) {
            String xvl = br.getRegex("src=\"(https://www.xvideos.com/embedframe/\\d+)\"").getMatch(0);
            if (xvl != null) {
                decryptedLinks.add(createDownloadlink(xvl));
                return decryptedLinks;
            }
        }
        if (br.containsHTML("<source src=\\'[^']+m3u8\\'")) {
            /* --> To hosterplugin - most of all '/player/' URLs will have this but also 'pornvideo/' URLs . */
            final DownloadLink dl = createDownloadlink(param.getCryptedUrl());
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        br.getPage("https://m.8-d.com/prein");
        final Regex th = br.getRegex("\\&t=(\\d+)\\&h=([a-z0-9]+)\"");
        String t = th.getMatch(0);
        String h = th.getMatch(1);
        if (t == null || h == null) {
            logger.warning("Decrypter broken for link: " + param.getCryptedUrl());
            return null;
        }
        br.getPage("http://m.8-d.com/in?r=&p=http://video-one.com/video/" + fuid + ".html&t=" + t + "&h=" + h);
        t = br.getRegex("var t=\\'(\\d+)\\';").getMatch(0);
        h = br.getRegex("var h=\\'([a-z0-9]+)\\';").getMatch(0);
        if (t == null || h == null) {
            logger.warning("Decrypter broken for link: " + param.getCryptedUrl());
            return null;
        }
        br.getPage("http://video-one.com/newvid/" + fuid + "?t=" + t + "&h=" + h + "&p=video-one.com/eval/seq/2");
        if (br.containsHTML(">Video Content Not Available<|No htmlCode read")) {
            logger.info("Link offline: " + param.getCryptedUrl());
            return decryptedLinks;
        }
        String externID = null;
        String fuu = null;
        final String continueURL = br.getRegex("\"(http://(\\d+\\.\\d+\\.\\d+\\.\\d+|[a-z0-9\\.]+)/visions/[^<>\"]*?)\"").getMatch(0);
        /** Maybe crypted/abnormal? */
        if (continueURL != null) {
            br.getPage(continueURL);
            fuu = jsString();
            if (fuu != null) {
                externID = new Regex(fuu, "starturl = \"(http[^<>\"]*?)\"").getMatch(0);
            } else {
                externID = br.getRegex("(http://seabliss\\.com/evideo/720p/)").getMatch(0);
            }
            final String embedID = new Regex(continueURL, "\\.html\\?(.+)$").getMatch(0);
            if (externID != null && embedID == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (externID == null) {
                logger.warning("Decrypter broken for link: " + param.getCryptedUrl());
                return null;
            }
            if (externID.contains("seabliss.com/")) {
                externID = "directhttp://" + externID + "/" + embedID + ".mp4";
                decryptedLinks.add(createDownloadlink(externID));
                return decryptedLinks;
            } else if (externID.contains("redtube.com/")) {
                externID += "?" + embedID;
            } else {
                externID += embedID;
            }
            if (externID.matches("http://media\\.8\\-d\\.com/getcode\\.php\\?id=\\d+\\&code=\\w+")) {
                br.getPage(externID);
                externID = br.getRegex("<url>([^<>]*?)</url>").getMatch(0);
                if (externID == null) {
                    /* Chances are very high that that url is simply offline. */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (!externID.startsWith("http")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                externID = "directhttp://" + externID;
            }
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        externID = this.br.getRegex("iframe src=\"(ftp://[^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        /** Or not crypted... */
        decryptedLinks.addAll(findEmbedUrls(fuid));
        if (!decryptedLinks.isEmpty()) {
            return decryptedLinks;
        }
        // final failover!
        // they are all shown within iframe, return it incase findEmbedUrls doens't provide assistance.
        final String iframe = br.getRegex("<iframe[^>]*\\s*('|\")(https?://.*?)\\1").getMatch(1);
        if (iframe == null) {
            if (this.br.containsHTML("xmlns=")) {
                /*
                 * E.g. empty document: <html xmlns="http://www.w3.org/1999/xhtml"><head> <meta http-equiv="Content-Type"
                 * content="text/html; charset=iso-8859-1" /></body></html>
                 */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return null;
        }
        decryptedLinks.add(createDownloadlink(iframe));
        return decryptedLinks;
    }

    @Override
    protected boolean isOffline(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404) {
            return true;
        } else if (!this.canHandle(br.getURL())) {
            return false;
        } else {
            return false;
        }
    }

    private String jsString() {
        try {
            String var1 = br.getRegex("\"JavaScript\"> var [A-Za-z0-9]+ = \\'([^<>\"]*?)\\';").getMatch(0);
            String var2 = "";
            String var4 = br.getRegex("[A-Za-z0-9]+ = \"\"; function [A-Za-z0-9]+\\(\\) \\{[A-Za-z0-9]+ = \\'(.*?)\\';[A-Za-z0-9]+\\(\\);").getMatch(0);
            for (int i = 0; i < var4.length(); i++) {
                char indexofVar4 = var4.charAt(i);
                int indexofvar1 = var1.indexOf(indexofVar4);
                if (var1.indexOf(var4.charAt(i)) != -1) {
                    char charAtVar1 = var1.charAt(indexofvar1 - 1);
                    var2 += charAtVar1;
                } else {
                    var2 = var2 + var4.charAt(i);
                }
            }
            return var2;
        } catch (final Exception e) {
            return null;
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}