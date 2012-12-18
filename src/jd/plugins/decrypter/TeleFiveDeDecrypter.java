//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.requests.PostRequest;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDHexUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tele5.de" }, urls = { "http://(www\\.)?tele5\\.de/\\w+" }, flags = { 0 })
public class TeleFiveDeDecrypter extends PluginForDecrypt {

    public TeleFiveDeDecrypter(final PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        final String query = new Regex(parameter, "/(\\w+)$").getMatch(0);
        if (query != null) {
            DownloadLink link = null;
            Browser amf = new Browser();
            getAMFRequest(amf, createAMFRequest(query));
            HashMap<String, String> values = new HashMap<String, String>();
            values = AMFParser(amf);
            if (values == null) return decryptedLinks;

            String type = values.get("type");

            if (type != null && !"stream".equals(type)) {
                String source = values.get("quelle");
                if (source != null) {
                    source = new Regex(source, "src=\"([^\"]+)\"").getMatch(0);
                    if (source != null) {
                        link = createDownloadlink(source);
                    }
                } else {
                    String path = values.get("path");
                    if (path != null) {
                        String ext = path.substring(path.lastIndexOf("."));
                        if (ext == null || ext.length() > 5) ext = ".mov";
                        if (!path.startsWith("http://")) path = "http://www.tele5.de" + path;
                        link = createDownloadlink("directhttp://" + path);
                        link.setFinalFileName(values.get("subline") + ext);
                    }
                }
            } else {
                parameter = parameter.replace("http://", "decrypted://");
                link = createDownloadlink(parameter);
            }
            decryptedLinks.add(link);
        }
        return decryptedLinks;
    }

    private byte[] createAMFRequest(String query) {
        if (query == null) return null;
        String data = "0A000000010200";
        data += getHexLength(query) + JDHexUtils.getHexString(query);
        return JDHexUtils.getByteArray("000300000001001674656C65352E676574436F6E74656E74506C6179657200022F31000000" + getHexLength(JDHexUtils.toString(data)) + data);
    }

    private String getHexLength(final String s) {
        String result = Integer.toHexString(s.length());
        return result.length() % 2 > 0 ? "0" + result : result;
    }

    private HashMap<String, String> AMFParser(final Browser amf) {
        /* Parsing key/value pairs from binary amf0 response message to HashMap */
        String t = amf.toString();
        /* workaround for browser in stable version */
        t = t.replaceAll("\r\n", "\n");
        char[] content = null;
        try {
            content = t.toCharArray();
        } catch (Throwable e) {
            return null;
        }
        HashMap<String, String> result = new HashMap<String, String>();
        for (int i = 0; i < content.length; i++) {
            if (content[i] != 3) continue;// Object 0x03
            i = i + 2;
            for (int j = i; j < content.length; j++) {
                int keyLen = content[j];
                if (keyLen == 0 || keyLen + j >= content.length) {
                    i = content.length;
                    break;
                }
                String key = "";
                int k;
                for (k = 1; k <= keyLen; k++) {
                    key = key + content[k + j];
                }
                String value = "";
                int v = j + k;
                int vv = 0;
                if (content[v] == 2) {// String 0x02
                    v = v + 2;
                    int valueLen = content[v];
                    if (valueLen == 0) value = null;
                    for (vv = 1; vv <= valueLen; vv++) {
                        value = value + content[v + vv];
                    }
                } else if (content[v] == 0) {// Number 0x00
                    String r;
                    for (vv = 1; vv <= 8; vv++) {
                        r = Integer.toHexString(content[v + vv]);
                        r = r.length() % 2 > 0 ? "0" + r : r;
                        value = value + r;
                    }
                    /*
                     * Encoded as 64-bit double precision floating point number IEEE 754 standard
                     */
                    value = value != null ? String.valueOf((int) Double.longBitsToDouble(new BigInteger(value, 16).longValue())) : value;
                } else {
                    continue;
                }
                j = v + vv;
                result.put(key, value);
            }
        }
        return result;
    }

    private void getAMFRequest(final Browser amf, final byte[] b) {
        amf.getHeaders().put("Content-Type", "application/x-amf");
        try {
            PostRequest request = (PostRequest) amf.createPostRequest("http://www.tele5.de/gateway/gateway.php", (String) null);
            request.setPostBytes(b);
            amf.openRequestConnection(request);
            amf.loadConnection(null);
        } catch (Throwable e) {
            /* does not exist in 09581 */
        }
    }

}