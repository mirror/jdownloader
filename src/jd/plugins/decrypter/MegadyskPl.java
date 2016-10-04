package jd.plugins.decrypter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.encoding.Base64;
import org.jdownloader.plugins.controller.host.HostPluginController;

@DecrypterPlugin(revision = "$Revision: 35014 $", interfaceVersion = 3, names = { "megadysk.pl" }, urls = { "https?://(?:www\\.)?megadysk\\.pl/(f|s)/[A-Za-z0-9\\.]+(?:/n/[^/]+)?" })
public class MegadyskPl extends PluginForDecrypt {

    public MegadyskPl(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getPage(parameter.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("Nothing has been found|Make sure that URL is proper|There are no files to be shown")) {
            return ret;
        }
        final String data = br.getRegex("window\\['.*?'\\]\\s*=\\s*\"(.*?)\"").getMatch(0);
        if (data == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Browser keyBr = br.cloneBrowser();
        keyBr.getPage("/dist/index.js");
        final String key = keyBr.getRegex("INITIAL_STATE_(FIELD|KEY)\\s*=\\s*\"(.*?)\"").getMatch(1);
        if (key == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String jsonString = xorDecode(key, data);
        final Map<String, Object> json = JSonStorage.restoreFromString(jsonString, TypeRef.HASHMAP);
        final Map<String, Object> app = (Map<String, Object>) json.get("app");
        List<Map<String, Object>> files = (List<Map<String, Object>>) ((Map<String, Object>) app.get("folderView")).get("files");
        if (files == null || files.size() == 0) {
            files = (List<Map<String, Object>>) ((Map<String, Object>) app.get("folderView")).get("items");
        }
        if (files == null || files.size() == 0) {
            files = (List<Map<String, Object>>) ((Map<String, Object>) app.get("folderView")).get("elements");
        }
        if (files.size() == 1) {
            final Map<String, Object> file = files.get(0);
            final DownloadLink link = createDownloadlink(parameter.getCryptedUrl());
            if (file.get("size") != null) {
                final Number number = (Number) file.get("size");
                link.setVerifiedFileSize(number.longValue());
            }
            if (file.get("name") != null) {
                link.setFinalFileName((String) file.get("name"));
            }
            if (!Boolean.FALSE.equals(file.get("uploaded")) && file.get("downloadUrl") != null) {
                link.setAvailable(true);
            }
            ret.add(link);
        } else {
            final PluginForHost defaultPlugin = HostPluginController.getInstance().get(getHost()).getPrototype(null);
            for (final Map<String, Object> file : files) {
                if (file.get("nameSafe") != null) {
                    final DownloadLink link = createDownloadlink(parameter.getCryptedUrl() + "/n/" + file.get("nameSafe"));
                    link.setHost(getHost());
                    link.setDefaultPlugin(defaultPlugin);
                    if (file.get("size") != null) {
                        final Number number = (Number) file.get("size");
                        link.setVerifiedFileSize(number.longValue());
                    }
                    if (file.get("name") != null) {
                        link.setFinalFileName((String) file.get("name"));
                    }
                    if (!Boolean.FALSE.equals(file.get("uploaded")) && file.get("downloadUrl") != null) {
                        link.setAvailable(true);
                    }
                    ret.add(link);
                }
            }
        }
        return ret;
    }

    private String xorDecode(final String key, final String data) throws IOException {
        byte[] input = Base64.decode(data);
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        for (int index = 0; index < input.length; index++) {
            byte in = input[index];
            byte out = (byte) (0xff & (in ^ key.charAt(index % key.length())));
            os.write(out);
        }
        return URLDecoder.decode(os.toString(), "UTF-8");
    }

}
