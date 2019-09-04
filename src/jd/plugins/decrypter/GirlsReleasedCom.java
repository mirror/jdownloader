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

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision: 41211 $", interfaceVersion = 3, names = { "girlsreleased.com" }, urls = { "https?://(www\\.)?girlsreleased\\.com/#(set|site|model)s?/?.*" })
public class GirlsReleasedCom extends antiDDoSForDecrypt {
    public GirlsReleasedCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private enum PageType {
        GR_SET,
        GR_SITES,
        GR_SITE,
        GR_MODEL,
        GR_MODELS,
        GR_UNKNOWN;
        private static PageType parse(final String link) {
            if (StringUtils.containsIgnoreCase(link, "/#set/")) {
                return GR_SET;
            } else if (StringUtils.containsIgnoreCase(link, "/#site/")) {
                return GR_SITE;
            } else if (StringUtils.containsIgnoreCase(link, "/#sites")) {
                return GR_SITES;
            } else if (StringUtils.containsIgnoreCase(link, "/#model/")) {
                return GR_MODEL;
            } else if (StringUtils.containsIgnoreCase(link, "/#models")) {
                return GR_MODELS;
            } else {
                return GR_UNKNOWN;
            }
        }
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        final PageType pageType = PageType.parse(parameter);
        if (pageType == PageType.GR_UNKNOWN) {
            getLogger().warning("Unable to determine page type!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        getPage(parameter);
        String fpName = null; // Sill be set further down, most logical approach is to group by sets.
        String[] links = null;
        // Build API payload
        String[][] idList = null;
        String payload = null;
        idList = new Regex(parameter, "#\\w+/([^$\\/\\?]+)").getMatches();
        if (pageType == PageType.GR_SET) {
            if (idList != null && idList.length > 0) {
                String timestamp = br.getRegex("var w = \'([^\']+)\';").getMatch(0);
                if (timestamp != null && timestamp.length() > 0) {
                    payload = "{\"tasks\":[\"getset\"],\"set\":{\"id\":\"" + idList[0][0] + "\"},\"w\":\"" + timestamp + "\"}";
                }
            }
        } else if (pageType == PageType.GR_SITE) {
            if (idList != null && idList.length > 0) {
                payload = "{\"tasks\":[\"getsets\"],\"sets\":{\"count\":999999999,\"site\":\"" + idList[0][0] + "\"}}";
            }
        } else if (pageType == PageType.GR_SITES) {
            payload = "{\"tasks\":[\"getsites\"],\"sites\":{\"page\":0,\"count\":999999999,\"model\":0,\"modelname\":null,\"search\":null,\"modelid\":null}}";
        } else if (pageType == PageType.GR_MODEL) {
            if (idList != null && idList.length > 0) {
                idList = new Regex(parameter, "/#\\w+/([^$\\/]+)/([^$\\/\\s\\?]+)").getMatches();
                if (idList != null && idList.length > 0 && idList[0].length > 1) {
                    payload = "{\"tasks\":[\"getsets\"],\"sets\":{\"count\":999999999,\"model\":\"" + idList[0][0] + "\"},\"modelname\":\"" + idList[0][1] + "\"}";
                }
            }
        } else if (pageType == PageType.GR_MODELS) {
            payload = "{\"tasks\":[\"getmodels\"],\"models\":{\"page\":0,\"count\":999999999,\"site\":null,\"sort\":null,\"search\":null}}";
        }
        if (payload == null) {
            getLogger().warning("Unable to build payload!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // Get API result
        final String postURL = "https://girlsreleased.com/";
        final Browser br2 = br.cloneBrowser();
        String apiResult = null;
        final PostRequest post = new PostRequest(postURL);
        post.getHeaders().put("Origin", "https://openloadtvstream.me");
        post.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        post.setContentType("application/json");
        post.setPostDataString(payload);
        br2.setRequest(post);
        postPage(br2, postURL, payload);
        apiResult = br2.toString();
        // Filter/build links
        if (apiResult != null && apiResult.length() > 0) {
            apiResult = apiResult.replaceAll("\\\\/", "/");
            if (pageType == PageType.GR_SET) {
                links = new Regex(apiResult, "\\[[^,\\]]+,[^,\\]]+,[^,\\]]+,[^,\\]]+,\"([^\"]+)\"").getColumn(0);
                String[] fpLookup = new Regex(apiResult, "\"site\":\"([^\"]+)\",\"models\":\\[\\[(\\d+),\"([^\"]+)\"").getRow(0);
                if (fpLookup != null && fpLookup.length > 2) {
                    fpName = fpLookup[0] + " - " + fpLookup[2] + " - " + "Set " + fpLookup[1];
                }
            } else if (pageType == PageType.GR_SITE || pageType == PageType.GR_MODELS) {
                String[][] rawLlinks = new Regex(apiResult, "\\[(\\d+),\"([^,\\]]+)\"").getMatches();
                links = new String[rawLlinks.length];
                for (int i = 0; i < rawLlinks.length; i++) {
                    links[i] = "https://girlsreleased.com/#model/" + rawLlinks[i][0] + "/" + rawLlinks[i][1];
                }
            } else if (pageType == PageType.GR_SITES) {
                String[] rawLlinks = new Regex(apiResult, "\\[[^,\\]]+,\"([^,\\]]+)\"").getColumn(0);
                links = new String[rawLlinks.length];
                for (int i = 0; i < rawLlinks.length; i++) {
                    links[i] = "https://girlsreleased.com/#set/" + rawLlinks[i];
                }
            } else if (pageType == PageType.GR_MODEL) {
                String[] rawLlinks = new Regex(apiResult, "\\[(\\d+)").getColumn(0);
                links = new String[rawLlinks.length];
                for (int i = 0; i < rawLlinks.length; i++) {
                    links[i] = "https://girlsreleased.com/#set/" + rawLlinks[i];
                }
            }
        }
        if (links != null && links.length > 0) {
            for (String link : links) {
                link = Encoding.htmlDecode(link);
                if (link.startsWith("/")) {
                    link = br.getURL(link).toString();
                }
                decryptedLinks.add(createDownloadlink(link));
            }
            if (fpName != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.setProperty("ALLOW_MERGE", true);
                fp.addLinks(decryptedLinks);
            }
        }
        return decryptedLinks;
    }
}