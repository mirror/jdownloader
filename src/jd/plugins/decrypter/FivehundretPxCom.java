package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "500px.com" }, urls = { "https?://(?:www\\.)?500px\\.com/(?!editors|about|studio|login|signup|licensing|popular|upgrade|business|photo|settings|city|terms|search|pro|jobs|fresh|privacy)[^/]+(/galleries/[^/]+|/featured)?" })
public class FivehundretPxCom extends antiDDoSForDecrypt {
    public FivehundretPxCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        if (parameter.toString().matches(".+/galleries/.+") || parameter.toString().matches(".+/featured")) {
            getPage(br, parameter.toString());
            String curatorID = br.getRegex("App\\.CuratorId\\s*=\\s*(\\d+)").getMatch(0);
            if (curatorID == null) {
                curatorID = br.getRegex("500px.com/user/(\\d+)").getMatch(0);
                if (curatorID == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            final String csrfToken = br.getRegex("\"csrf-token\"\\s*content\\s*=\\s*\"(.*?)\"").getMatch(0);
            if (csrfToken == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final Browser brc = br.cloneBrowser();
            brc.setHeader("X-CSRF-Token", csrfToken);
            String galleryName = null;
            if (parameter.toString().matches(".+/featured")) {
                galleryName = br.getRegex("<option selected value='\\d+'>\\s*(.*?)\\s*</option").getMatch(0);
                if (galleryName != null) {
                    galleryName = galleryName.replace(" ", "").replace("-", "_");
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } else {
                galleryName = new Regex(parameter.toString(), ".+/galleries/(.+)").getMatch(0);
            }
            getPage(brc, "https://api.500px.com/v1/users/" + curatorID + "/galleries/" + galleryName + "?include_user=true&include_cover=1&cover_size=2048");
            final String galleryID = brc.getRegex("\"id\"\\s*:\\s*(\\d+)").getMatch(0);
            if (galleryID == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String username = brc.getRegex("\"username\"\\s*:\\s*\"(.*?)\"").getMatch(0);
            final String name = brc.getRegex("\"name\"\\s*:\\s*\"(.*?)\"").getMatch(0);
            final FilePackage fp = FilePackage.getInstance();
            String fpName = "";
            if (StringUtils.isNotEmpty(username)) {
                fpName = username;
            } else {
                fpName = galleryID;
            }
            if (StringUtils.isNotEmpty(name)) {
                if (fpName.length() > 0) {
                    fpName = fpName + "_" + name;
                }
            }
            fp.setName(fpName);
            int page = 0;
            while (!isAbort()) {
                getPage(brc, "https://api.500px.com/v1/users/" + curatorID + "/galleries/" + galleryID + "/items?rpp=50&image_size[]=1&image_size[]=2&image_size[]=32&image_size[]=31&image_size[]=33&image_size[]=34&image_size[]=35&image_size[]=36&image_size[]=2048&image_size[]=4&image_size[]=14&include_licensing=true&formats=jpeg,lytro&sort=position&sort_direction=asc&page=" + page++ + "&rpp=50");
                final Map<String, Object> map = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
                final List<Map<String, Object>> photos = (List<Map<String, Object>>) map.get("photos");
                if (photos == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                for (final Map<String, Object> photo : photos) {
                    if (isAbort()) {
                        break;
                    }
                    final Number id = (Number) photo.get("id");
                    final String url = photo.get("url") != null ? (String) photo.get("url") : null;
                    final DownloadLink downloadLink;
                    if (StringUtils.isNotEmpty(url)) {
                        downloadLink = createDownloadlink(br.getURL(url).toString());
                    } else {
                        downloadLink = createDownloadlink(br.getURL("/photo/" + id.intValue()).toString());
                    }
                    if (id != null) {
                        downloadLink.setLinkID(getHost() + "://" + id);
                    }
                    fp.add(downloadLink);
                    final String photoName = photo.get("name") != null ? (String) photo.get("name") : null;
                    if (StringUtils.isNotEmpty(photoName)) {
                        if (StringUtils.isNotEmpty(username)) {
                            downloadLink.setName(username + " - " + photoName + ".jpg");
                        } else {
                            downloadLink.setName(photoName + ".jpg");
                        }
                    }
                    downloadLink.setAvailable(true);
                    distribute(downloadLink);
                    ret.add(downloadLink);
                }
                final Number total_pages = (Number) map.get("total_pages");
                if (total_pages == null || page > total_pages.intValue()) {
                    break;
                }
            }
        } else {
            getPage(br, parameter.toString());
            if (br.containsHTML(">Sorry, no such page")) {
                ret.add(createOfflinelink(parameter.toString()));
                return ret;
            }
            String curatorID = br.getRegex("App\\.CuratorId\\s*=\\s*(\\d+)").getMatch(0);
            if (curatorID == null) {
                curatorID = br.getRegex("500px.com/user/(\\d+)").getMatch(0);
                if (curatorID == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            final String csrfToken = br.getRegex("\"csrf-token\"\\s*content\\s*=\\s*\"(.*?)\"").getMatch(0);
            if (csrfToken == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final Browser brc = br.cloneBrowser();
            brc.setHeader("X-CSRF-Token", csrfToken);
            final String username = br.getRegex("\"username\"\\s*:\\s*\"(.*?)\"").getMatch(0);
            final FilePackage fp = FilePackage.getInstance();
            if (StringUtils.isNotEmpty(username)) {
                fp.setName(username);
            }
            int page = 0;
            while (!isAbort()) {
                getPage(brc, "https://api.500px.com/v1/photos?feature=user&stream=photos&user_id=" + curatorID + "&include_states=true&image_size[]=1&image_size[]=2&image_size[]=32&image_size[]=31&image_size[]=33&image_size[]=34&image_size[]=35&image_size[]=36&image_size[]=2048&image_size[]=4&image_size[]=14&include_licensing=true&page=" + page++ + "&rpp=50");
                final Map<String, Object> map = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
                final List<Map<String, Object>> photos = (List<Map<String, Object>>) map.get("photos");
                if (photos == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                for (final Map<String, Object> photo : photos) {
                    if (isAbort()) {
                        break;
                    }
                    final Number id = (Number) photo.get("id");
                    final String url = photo.get("url") != null ? (String) photo.get("url") : null;
                    final DownloadLink downloadLink;
                    if (StringUtils.isNotEmpty(url)) {
                        downloadLink = createDownloadlink(br.getURL(url).toString());
                    } else {
                        downloadLink = createDownloadlink(br.getURL("/photo/" + id.intValue()).toString());
                    }
                    if (id != null) {
                        downloadLink.setLinkID(getHost() + "://" + id);
                    }
                    fp.add(downloadLink);
                    final String photoName = photo.get("name") != null ? (String) photo.get("name") : null;
                    if (StringUtils.isNotEmpty(photoName)) {
                        if (StringUtils.isNotEmpty(username)) {
                            downloadLink.setName(username + " - " + photoName + ".jpg");
                        } else {
                            downloadLink.setName(photoName + ".jpg");
                        }
                    }
                    downloadLink.setAvailable(true);
                    distribute(downloadLink);
                    ret.add(downloadLink);
                }
                final Number total_pages = (Number) map.get("total_pages");
                if (total_pages == null || page > total_pages.intValue()) {
                    break;
                }
            }
        }
        return ret;
    }
}
