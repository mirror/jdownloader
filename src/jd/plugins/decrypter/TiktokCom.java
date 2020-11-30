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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tiktok.com" }, urls = { "https?://[A-Za-z0-9]+\\.tiktok\\.com/.+" })
public class TiktokCom extends PluginForDecrypt {
    public TiktokCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_USER = ".+tiktok\\.com/(?:@|share/user/\\d+)(.+)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final PluginForHost plg = JDUtilities.getPluginForHost(this.getHost());
        if (parameter.matches("https?://vm\\..+")) {
            /* Single redirect URLs */
            br.setFollowRedirects(false);
            br.getPage(parameter);
            if (br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            final String finallink = br.getRedirectLocation();
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        } else if (new Regex(parameter, plg.getSupportedLinks()).matches()) {
            /* Single URL for host plugin */
            decryptedLinks.add(this.createDownloadlink(parameter));
            return decryptedLinks;
        } else if (parameter.matches(TYPE_USER)) {
            crawlProfile(parameter, decryptedLinks);
        } else {
            logger.info("Unsupported URL: " + parameter);
        }
        return decryptedLinks;
    }

    public ArrayList<DownloadLink> crawlProfile(final String parameter, final ArrayList<DownloadLink> decryptedLinks) throws Exception {
        br.setFollowRedirects(true);
        final String username_url = new Regex(parameter, TYPE_USER).getMatch(0);
        if (username_url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        } else if (jd.plugins.hoster.TiktokCom.isBotProtectionActive(this.br)) {
            throw new DecrypterRetryException(RetryReason.CAPTCHA, "Bot protection active, cannot crawl any items of user " + username_url, null, null);
        }
        final String websiteJson = br.getRegex("window\\.__INIT_PROPS__ = (\\{.*?\\})</script>").getMatch(0);
        Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(websiteJson);
        entries = (Map<String, Object>) entries.get("/@:uniqueId");
        final Map<String, Object> user_data = (Map<String, Object>) entries.get("userData");
        final String secUid = (String) user_data.get("secUid");
        final String userId = (String) user_data.get("userId");
        final String username = (String) user_data.get("uniqueId");
        // final String username = new Regex(parameter, "/@([^/\\?\\&]+)").getMatch(0);
        if (StringUtils.isEmpty(secUid) || StringUtils.isEmpty(userId) || StringUtils.isEmpty(username)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        List<Object> ressourcelist;
        final boolean api_broken = true;
        if (api_broken) {
            logger.warning("Plugin not yet finished, API signing is missing");
            /* We can only return the elements we find on their website when we cannot use their API! */
            ressourcelist = (ArrayList<Object>) entries.get("itemList");
            for (final Object videoO : ressourcelist) {
                final String videoURL = (String) videoO;
                if (!StringUtils.isEmpty(videoURL)) {
                    decryptedLinks.add(this.createDownloadlink(videoURL));
                }
            }
            return decryptedLinks;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(username);
        fp.addLinks(decryptedLinks);
        boolean hasMore = true;
        int index = 0;
        int page = 1;
        // final int maxItemsPerPage = 48;
        String maxCursor = "0";
        do {
            logger.info("Current page: " + page);
            logger.info("Current index: " + index);
            br.getPage("https://m." + this.getHost() + "/share/item/list?secUid=" + secUid + "&id=" + userId + "&type=1&count=48&minCursor=0&maxCursor=" + maxCursor + "&_signature=TODO_FIXME");
            if (jd.plugins.hoster.TiktokCom.isBotProtectionActive(this.br)) {
                throw new DecrypterRetryException(RetryReason.CAPTCHA, "Bot protection active, cannot crawl more items of user " + username_url, null, null);
            }
            entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            entries = (Map<String, Object>) entries.get("body");
            hasMore = ((Boolean) entries.get("hasMore")).booleanValue();
            maxCursor = (String) entries.get("maxCursor");
            ressourcelist = (List<Object>) entries.get("itemListData");
            for (final Object videoO : ressourcelist) {
                entries = (Map<String, Object>) videoO;
                entries = (Map<String, Object>) entries.get("itemInfos");
                final String videoID = (String) entries.get("id");
                final long createTimestamp = JavaScriptEngineFactory.toLong(entries.get("createTime"), 0);
                if (StringUtils.isEmpty(videoID) || createTimestamp == 0) {
                    /* This should never happen */
                    return null;
                }
                final DownloadLink dl = this.createDownloadlink("https://www.tiktok.com/@" + username + "/video/" + videoID);
                final String date_formatted = formatDate(createTimestamp);
                dl.setFinalFileName(date_formatted + "_@" + username + "_" + videoID + ".mp4");
                dl.setAvailable(true);
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
                distribute(dl);
                index++;
            }
            page++;
        } while (!this.isAbort() && hasMore && !StringUtils.isEmpty(maxCursor));
        return decryptedLinks;
    }

    public static String formatDate(final long date) {
        if (date <= 0) {
            return null;
        }
        String formattedDate = null;
        final String targetFormat = "yyyy-MM-dd";
        Date theDate = new Date(date * 1000);
        try {
            final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
            formattedDate = formatter.format(theDate);
        } catch (Exception e) {
            /* prevent input error killing plugin */
            formattedDate = Long.toString(date);
        }
        return formattedDate;
    }
}
