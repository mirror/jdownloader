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

import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class VepornNet extends antiDDoSForDecrypt {
    public VepornNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "titfap.com", "veporno.net", "veporn.net", "veporns.com" });
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
            ret.add("https?://(?:(?:www|m)\\.)?" + buildHostsPatternPart(domains) + "/video/([A-Za-z0-9\\-_]+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String urlSlug = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        final String url = param.getCryptedUrl().replaceFirst("https?://m\\.", "https://www.");
        br.setFollowRedirects(true);
        getPage(url);
        final String rateLimitRegex = "(?i)>\\s*Site is too crowded\\s*<";
        if (br.containsHTML(rateLimitRegex)) {
            for (int i = 1; i <= 3; i++) {
                sleep(i * 3 * 1001l, param);
                getPage(url);
                if (!br.containsHTML(rateLimitRegex)) {
                    break;
                }
            }
            if (br.containsHTML(rateLimitRegex)) {
                throw new DecrypterRetryException(RetryReason.HOST_RATE_LIMIT);
            }
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!this.canHandle(br.getURL())) {
            /* Redirect to unsupported URL / mainpage. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String title = urlSlug.replace("-", " ").trim();
        /* 2022-06-21: Most likely videos embedded on streamtape.com */
        final String[] embedURLs = br.getRegex("<iframe src=\"(https?://[^\"]+)\"").getColumn(0);
        if (embedURLs.length > 0) {
            for (final String embedURL : embedURLs) {
                ret.add(this.createDownloadlink(embedURL));
            }
        }
        final String hlsMaster = br.getRegex("<source type=\"application/x-mpegURL\" src=\"(https?://[^\"]+)").getMatch(0);
        if (hlsMaster != null) {
            ret.add(this.createDownloadlink(hlsMaster));
        }
        final boolean tryOldHandling = embedURLs == null || embedURLs.length == 0;
        final String[] links = br.getRegex("comment\\((\\d+)\\)").getColumn(0);
        if (links.length > 0 && tryOldHandling) {
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            int counter = 1;
            for (final String singleLink : links) {
                logger.info("Crawling video item " + counter + "/" + links.length);
                final Browser br = this.br.cloneBrowser();
                getPage(br, "/ajax.php?page=video_play&thumb&theme=&video=&id=" + singleLink + "&server=" + counter);
                if (br.containsHTML(">Site is too crowded<")) {
                    for (int i = 1; i <= 3; i++) {
                        sleep(i * 3 * 1001l, param);
                        getPage(br, "/ajax.php?page=video_play&thumb&theme=&video=&id=" + singleLink + "&server=" + counter);
                        if (!br.containsHTML(">Site is too crowded<")) {
                            break;
                        }
                    }
                }
                String finallink = br.getRegex("iframe src='(https?[^<>']+)'").getMatch(0);
                if (finallink == null) {
                    finallink = br.getRegex("iframe src=\"(https?[^<>\"]+)\"").getMatch(0);
                    if (finallink == null) {
                        continue;
                    }
                }
                ret.add(createDownloadlink(finallink));
                if (this.isAbort()) {
                    return ret;
                }
                counter++;
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(title).trim());
        fp.addLinks(ret);
        return ret;
    }
}
