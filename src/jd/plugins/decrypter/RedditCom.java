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
import java.util.LinkedHashMap;

import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "reddit.com" }, urls = { "https?://(?:www\\.)?reddit\\.com/r/[^/]+/comments/([a-z0-9]+)/[A-Za-z0-9\\-_]+" })
public class RedditCom extends PluginForDecrypt {
    public RedditCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* 2020-07-21: Let's be gentle and avoid doing too many API requests. */
        return 1;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String commentID = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        // final Account acc = AccountController.getInstance().getValidAccount(this.getHost());
        // if (acc == null) {
        // throw new AccountRequiredException();
        // }
        // final PluginForHost plugin = JDUtilities.getPluginForHost(this.getHost());
        // plugin.setBrowser(this.br);
        // ((jd.plugins.hoster.RedditCom) plugin).login(acc, false);
        /* According to: https://www.reddit.com/r/redditdev/comments/b8yd3r/reddit_api_possible_to_get_posts_by_id/ */
        br.getPage("https://www.reddit.com/comments/" + commentID + "/.json");
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        LinkedHashMap<String, Object> entries;
        final ArrayList<Object> ressourcelist = (ArrayList<Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        /* https://www.reddit.com/dev/api/#fullnames */
        /* [0] = post/"first comment" */
        /* [1] = Comments */
        for (final Object postO : ressourcelist) {
            entries = (LinkedHashMap<String, Object>) postO;
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "data/children/{0}/data");
            /* Look for single URLs e.g. single pictures (e.g. often imgur.com URLs) */
            final String externalURL = (String) entries.get("url");
            if (externalURL != null) {
                decryptedLinks.add(this.createDownloadlink(externalURL));
            }
            /* Look for embedded content from external sources */
            final Object embeddedMediaO = entries.get("media_embed");
            if (embeddedMediaO != null) {
                logger.info("Found embedded media");
                LinkedHashMap<String, Object> embeddedMediaInfo = (LinkedHashMap<String, Object>) embeddedMediaO;
                String media_embedStr = (String) embeddedMediaInfo.get("content");
                final String[] links = HTMLParser.getHttpLinks(media_embedStr, this.br.getURL());
                for (final String url : links) {
                    decryptedLinks.add(this.createDownloadlink(url));
                }
            }
            /* Only grab first post - stop then! */
            break;
        }
        if (decryptedLinks.size() == 0) {
            logger.info("Failed to find any downloadable content");
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        return decryptedLinks;
    }
}
