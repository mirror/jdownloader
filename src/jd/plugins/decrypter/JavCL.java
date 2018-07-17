package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.Base64;
import org.appwork.utils.formatter.HexFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "javcl.com" }, urls = { "https?://(?:www\\.)?javcl\\.com/([^/]+)/" })
public class JavCL extends PluginForDecrypt {
    public JavCL(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getPage(parameter.getCryptedUrl());
        br.followRedirect();
        final String filename = br.getRegex("<span class=\"title2\">([a-zA-Z0-9-]+)</span>").getMatch(0);
        final String data_id = br.getRegex("<div id='videoPlayer' data-id=\"([0-9]+)\" data-ep=\"([0-9]+)\">").getMatch(0);
        final String data_links[] = br.getRegex("<li data-sv=\"([0-9]+)\" data-link=\"([a-zA-Z0-9/+=-]+)\"(?: class=\"active\")?>\\s*([^</]+)\\s*</li>").getColumn(1);
        if (data_links == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        int index = 0;
        for (String data_link : data_links) {
            final String url = decodejav(data_link, data_id);
            final String name;
            if (data_links.length > 1) {
                name = filename + "_a" + index;
            } else {
                name = filename;
            }
            final DownloadLink downloadLink;
            if (StringUtils.isEmpty(name)) {
                downloadLink = createDownloadlink(url);
            } else {
                downloadLink = createDownloadlink(url + "#javclName=" + HexFormatter.byteArrayToHex(name.getBytes("UTF-8")));
            }
            ret.add(downloadLink);
            index++;
        }
        return ret;
    }

    private String decodejav(String data_link, String data_id) {
        String key = Base64.encode(data_id + "decodejav");
        key = new StringBuilder(key).reverse().toString();
        byte[] link = Base64.decode(data_link);
        StringBuilder sb = new StringBuilder();
        int k = 0;
        for (int i = 0; i < link.length; i++) {
            k = i % key.length();
            sb.append((char) (link[i] ^ key.codePointAt(k)));
        }
        return new String(Base64.decode(sb.toString()));
    }
}
