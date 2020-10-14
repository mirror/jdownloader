package jd.plugins.decrypter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import sun.misc.IOUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fuskator.com" }, urls = { "https?://(?:www\\.)?fuskator\\.com/(thumbs|expanded).*" })
public class FuskatorCom extends PluginForDecrypt {

    private enum RequestType {
        AUTH,
        IMAGES_JSON
    }

    public FuskatorCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<>();
        final String parameter = param.toString();

        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }

        String hash = new Regex(parameter, "(thumbs|expanded)/([^/]+)/[^/]+").getMatch(1);
        String filePackageName = getFilePackageName(hash);

        /*
         * fuskator performs these XHR and then updates the page HTML with the info from the JSON:
         *
         * 1. POST https://fuskator.com/ajax/auth.aspx -> WTzR0liw
         * 
         * 2. POST https://fuskator.com/ajax/gal.aspx?X-Auth=WTzR0liw&hash=eZ3ETEmf4Dy ->
         * {"fuskerUrl":"https://www.imagefap.com/photo/2106672792/","hash":"eZ3ETEmf4Dy","hits":1908,"images":
         * [{"imageUrl":"//i10.fuskator.com/large/eZ3ETEmf4Dy/image-1.jpg","index":1,"height":1500,"width":889}, ....],
         * "imageServer":"i10.fuskator.com","path":"/eZ3ETEmf4Dy/index","quality":22,"rating":3.83, "tags":"imagefap,wangdan,Wang
         * Dan,chinese,litu,solo,outdoor","votes":6}
         */

        String auth = performAjaxRequest(RequestType.AUTH, null, null);
        if ("".equals(auth)) {
            logger.warning("could not get auth");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        String imagesJson = performAjaxRequest(RequestType.IMAGES_JSON, auth, hash);
        if ("".equals(imagesJson)) {
            logger.warning("could not get images json");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        populateDecryptedLinks(decryptedLinks, parameter, imagesJson);

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(filePackageName));
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    private String performAjaxRequest(RequestType type, String auth, String hash) throws PluginException {
        URLConnectionAdapter con = null;
        try {
            if (type == RequestType.AUTH) {
                con = br.openPostConnection("https://fuskator.com/ajax/auth.aspx", "");
            } else if (type == RequestType.IMAGES_JSON) {
                con = br.openGetConnection("https://fuskator.com/ajax/gal.aspx?X-Auth=" + Encoding.urlEncode(auth) + "&hash=" + hash);
            } else {
                logger.warning("unknown request type " + type);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            int responseCode = con.getResponseCode();
            if (responseCode != 200) {
                logger.warning(type + " request returned response code " + responseCode);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            int contentLength = (int) con.getContentLength();
            byte[] bytes = IOUtils.readFully(con.getInputStream(), contentLength, true);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new PluginException(LinkStatus.ERROR_RETRY);
        } finally {
            if (con != null) {
                try {
                    con.disconnect();
                } catch (Exception e) {
                    logger.warning("could not disconnect: " + e);
                }
            }
        }
    }

    private void populateDecryptedLinks(ArrayList<DownloadLink> decryptedLinks, String parameter, String json) throws Exception {
        List<Map<String, String>> images;
        try {
            Map<String, Object> pictures = JavaScriptEngineFactory.jsonToJavaMap(json);
            images = (List<Map<String, String>>) pictures.get("images");
        } catch (Exception e) {
            logger.exception("problem with images json: " + json, e);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        if (images == null || images.size() == 0) {
            logger.warning("found 0 images for " + parameter);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        for (Map<String, String> imageInfo : images) {
            String imageUrl = imageInfo.get("imageUrl");
            if (imageUrl.startsWith("//")) {
                imageUrl = imageUrl.substring(2);
            }
            if (!imageUrl.startsWith("http") && !imageUrl.startsWith("https")) {
                imageUrl = "https://" + imageUrl;
            }
            final DownloadLink dl = createDownloadlink(imageUrl);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
    }

    private String getFilePackageName(String hash) {
        String title = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        if (title == null) {
            title = "fuskator gallery " + hash;
        }
        return title.trim();
    }
}
