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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bc.vc" }, urls = { "https?://(?:www\\.)?(?:bc\\.vc|bcvc\\.live|bcvc\\.xyz)/(\\d+/.+|[A-Za-z0-9]{5,7})" })
public class BcVc extends antiDDoSForDecrypt {
    public BcVc(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        {
            final String linkInsideLink = new Regex(param.getCryptedUrl(), "(?:bc\\.vc|bcvc\\.live|bcvc\\.xyz)/\\d+/(.+)").getMatch(0);
            if (linkInsideLink != null) {
                final String finalLinkInsideLink;
                if (StringUtils.startsWithCaseInsensitive(linkInsideLink, "http") || StringUtils.startsWithCaseInsensitive(linkInsideLink, "ftp")) {
                    finalLinkInsideLink = linkInsideLink;
                } else {
                    finalLinkInsideLink = "http://" + linkInsideLink;
                }
                if (!StringUtils.containsIgnoreCase(finalLinkInsideLink, getHost() + "/")) {
                    final DownloadLink link = createDownloadlink(finalLinkInsideLink);
                    link.setProperty("Referer", param.toString());
                    ret.add(link);
                    return ret;
                } else {
                    param.setCryptedUrl(linkInsideLink);
                }
            }
        }
        /**
         * we have to rename them here because we can damage urls within urls.</br> URLs containing www. will always be offline.
         */
        param.setCryptedUrl(param.getCryptedUrl().replaceFirst("://www.", "://").replaceFirst("http://", "https://"));
        br.setFollowRedirects(false);
        getPage(param.getCryptedUrl());
        /* Check for direct redirect */
        String redirect = br.getRedirectLocation();
        if (redirect == null) {
            redirect = br.getRegex("top\\.location\\.href = \"((?:https?|ftp)[^<>\"]*?)\"").getMatch(0);
        }
        if (redirect != null && !this.canHandle(redirect)) {
            ret.add(createDownloadlink(redirect));
            return ret;
        } else if (redirect != null) {
            getPage(redirect);
        }
        if (StringUtils.endsWithCaseInsensitive(redirect, "//bc.vc/7") || br.getURL().matches("https?://(?:www\\.)?bc.vc/") || br.containsHTML("top\\.location\\.href = \"https?://(?:www\\.)?bc\\.vc/\"") || br.containsHTML(">404 Not Found<") || br.containsHTML(">Sorry the page you are looking for does not exist")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.toString().length() <= 100) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("Unable to connect to database")) {
            logger.info("Link can't be decrypted because of server problems: " + param.getCryptedUrl());
            return ret;
        }
        ret.add(this.crawlBcbclive(param));
        return ret;
    }

    private DownloadLink crawlBcbclive(final CryptedLink param) throws IOException, PluginException, InterruptedException {
        final String token1 = br.getRegex("let tkn = \\'([^<>\"\\']+)").getMatch(0);
        final String xyz = br.getRegex("let xyz = '([a-f0-9]{32})';").getMatch(0);
        if (token1 == null || xyz == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Form form1 = new Form();
        br.setCookie(br.getHost(), "_kei_", "1");
        /* e.pageX + ',' + e.pageY + ':' + (e.pageX - posX)+ ',' + (e.pageY - posY) + new Date().getTime() - start_button */
        final String timeBlock1 = new Random().nextInt(1000) + "," + new Random().nextInt(1000);
        final String timeBlock2 = new Random().nextInt(1000) + "." + new Random().nextInt(10000) + "," + new Random().nextInt(1000) + "." + new Random().nextInt(10000);
        final String timeBlock3 = new Random().nextInt(10000) + "";
        // String time = "851,262:261.171875,35.609375:" + new Random().nextInt(10000);
        String time = timeBlock1 + ":" + timeBlock2 + ":" + timeBlock3;
        form1.setAction("/ln.php?wds=" + xyz + "&time=" + Encoding.urlEncode(time));
        form1.setMethod(MethodType.POST);
        /* See https://bcvc.live/dist/js/bcvcv3.js */
        form1.put(Encoding.urlEncode("xdf[afg]"), new Random().nextInt(300) + ""); // new Date().getTimezoneOffset() / -1
        form1.put(Encoding.urlEncode("xdf[bfg]"), new Random().nextInt(10000) + ""); // document.documentElement.clientWidth;
        form1.put(Encoding.urlEncode("xdf[cfg]"), new Random().nextInt(10000) + ""); // document.documentElement.clientHeight;
        form1.put(Encoding.urlEncode("xdf[jki]"), Encoding.urlEncode(token1));
        /* Monitor resolution dimensions */
        form1.put(Encoding.urlEncode("xdf[dfg]"), new Random().nextInt(10000) + ""); // window.screen.width;
        form1.put(Encoding.urlEncode("xdf[efg]"), new Random().nextInt(10000) + ""); // window.screen.height;
        form1.put(Encoding.urlEncode("ojk"), "jfhg");
        br.getHeaders().put("Origin", "https://" + br.getHost());
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept", "*/*");
        /* 2021-07-19: Waittime is skippable */
        // this.sleep(5001l, param);
        this.br.submitForm(form1);
        /* On error, browser will redirect to random ad website. */
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final Object errorO = entries.get("error");
        if (errorO == null || (Boolean) errorO) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String url = (String) JavaScriptEngineFactory.walkJson(entries, "message/url");
        final String b64 = UrlQuery.parse(url).get("cr");
        final String finallink = Encoding.Base64Decode(b64);
        return this.createDownloadlink(finallink);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}