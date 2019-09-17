package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import com.fasterxml.jackson.databind.ObjectMapper;

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
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "thingiverse.com" }, urls = { "https?://(www\\.)?thingiverse\\.com/(thing:\\d+|make:\\d+|[^/]+/(about|designs|collections(/[^/]+)?|makes|likes|things)|groups/[^/]+(/(things|about))?)" })
public class ThingiverseCom extends antiDDoSForDecrypt {
    public ThingiverseCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        getPage(param.getCryptedUrl());
        String fpName = br.getRegex("<title>\\s*([^<]+?)\\s*-\\s*Thingiverse").getMatch(0);
        if (new Regex(br.getURL(), "/([^/]+/(about|designs|collections(/[^/]+)?|makes|likes|things)|groups/[^/]+(/(things|about))?)").matches()) {
            String[] links = getAPISearchLinks(br);
            if (links != null && links.length > 0) {
                for (String link : links) {
                    decryptedLinks.add(createDownloadlink(br.getURL(link).toString()));
                }
            }
        } else if (StringUtils.containsIgnoreCase(br.getURL(), "/thing:")) {
            // a thing
            final String thingID = new Regex(br.getURL(), "thing:(\\d+).*").getMatch(0);
            final DownloadLink link = createDownloadlink("directhttp://https://www.thingiverse.com/thing:" + thingID + "/zip");
            if (fpName != null) {
                fpName = Encoding.htmlOnlyDecode(fpName);
                link.setFinalFileName(fpName + ".zip");
            }
            decryptedLinks.add(link);
            // Images to see what we've downloaded (in case the label doesn't make much sense in hindsight).
            final String[] imageLinks = br.getRegex("<div class=\"gallery-photo\"[^>]*data-full=\"([^\"]+)\"[^>]*>").getColumn(0);
            if (imageLinks != null && imageLinks.length > 0) {
                for (String imageLink : imageLinks) {
                    imageLink = Encoding.htmlDecode(imageLink);
                    DownloadLink imageDL = createDownloadlink(imageLink);
                    if (fpName != null) {
                        imageDL.setFinalFileName(fpName + "_" + imageLink.hashCode() + ".jpg");
                    }
                    decryptedLinks.add(imageDL);
                }
            }
        } else if (StringUtils.containsIgnoreCase(br.getURL(), "/make:")) {
            // a make
            final String thingID = br.getRegex("href=\"/thing:(\\d+)\"\\s*class=\"card-img-holder\"").getMatch(0);
            if (thingID != null) {
                final DownloadLink thing = createDownloadlink("https://www.thingiverse.com/thing:" + thingID);
                decryptedLinks.add(thing);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private String[] getAPISearchLinks(Browser br) throws Exception {
        String[] results = null;
        String sourceData = PluginJSonUtils.getJsonNested(br, "data");
        sourceData = new Regex(sourceData, "(\\{[^\\}]+\\})").getMatch(0);
        if (sourceData != null) {
            LinkedHashMap<String, String> searchValues = new ObjectMapper().readValue(sourceData, LinkedHashMap.class);
            if (searchValues != null && searchValues.keySet().size() > 0) {
                searchValues.put("page", "1");
                searchValues.put("per_page", "999999999");
                Browser br2 = br.cloneBrowser();
                String postURL = br2.getURL(searchValues.get("source")).toString();
                PostRequest post = new PostRequest(postURL);
                UrlQuery postQuery = new UrlQuery();
                for (String key : searchValues.keySet()) {
                    post.addVariable(key, String.valueOf(searchValues.get(key)));
                    postQuery.add(key, String.valueOf(searchValues.get(key)));
                }
                post.getHeaders().put("Origin", "https://www.thingiverse.com");
                post.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                post.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
                br2.setRequest(post);
                postPage(br2, postURL, postQuery.toString());
                results = br2.getRegex("a href=\"([^\"]+)\" class=\"card-img-holder").getColumn(0);
            }
        }
        return results;
    }
}