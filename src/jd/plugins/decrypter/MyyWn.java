//jDownloader - Downloadmanager
//Copyright (C) 2016  JD-Team support@jdownloader.org
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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Base64;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.components.PluginJSonUtils;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

/**
 * @author raztoki
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "myegy.win" }, urls = { "https?://(?:www\\.)?myegy\\.win/link/(\\d+)" })
public class MyyWn extends antiDDoSForDecrypt {

    public MyyWn(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("http://", "https://");
        final String uid = new Regex(parameter, getSupportedLinks()).getMatch(0);
        br = new Browser();
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        int repeat = 3;
        for (int i = 1; i <= repeat; i++) {
            final String captcha = br.getRegex("<div class=\"captcha\">.*?</div>").getMatch(-1);
            if (captcha != null) {
                final String[] imageBase64 = new Regex(captcha, "<img src=\"data:image/([0-9a-zA-Z]+);base64,(.*?)\"").getRow(0);
                // image to file
                final File imageFile = getLocalCaptchaFile("." + imageBase64[0]);
                // ensure that the file is created
                if (!imageFile.exists()) {
                    imageFile.createNewFile();
                }
                final byte[] decoded = Base64.decode(imageBase64[1]);
                // write to file
                Files.write(imageFile.toPath(), decoded, StandardOpenOption.WRITE);
                String code = null;
                code = getCaptchaCode(imageFile, param);
                // it's crazy i know we return "" from captcha refresher/reloader AND empty submission...
                if ("".equals(code)) {
                    repeat++;
                    getPage(br.getURL());
                    continue;
                }
                // they don't use form, they submit json via javascript
                final String json = findAndFormatJson(code);
                final Browser cbr = br.cloneBrowser();
                cbr.getHeaders().put("Accept", "text/plain, */*; q=0.01");
                cbr.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                postPageRaw(cbr, "/!/link/" + uid + (i > 1 ? "?incorrect=true" : ""), json, false);
                // result is singular
                final String link = PluginJSonUtils.getJsonValue(cbr, "result");
                // if incorrect
                if ("false".equals(link)) {
                    if (i + 1 >= repeat) {
                        throw new DecrypterException(DecrypterException.CAPTCHA);
                    }
                    // reload
                    getPage(br.getURL() + "?incorrect=true");
                    continue;
                } else if (link != null) {
                    decryptedLinks.add(createDownloadlink(link));
                    break;
                }

            }
        }
        return decryptedLinks;
    }

    private String findAndFormatJson(String code) throws Exception {
        final String json = br.getRegex("Neaf\\.Block\\.Init\\((.*?)\\)\\s*</script>").getMatch(0);
        if (json == null) {
            throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
        }
        // format json
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
        final String captchaId = (String) JavaScriptEngineFactory.walkJson(entries, "content/captcha");
        // hashes seem to be static...
        final String jsonoutput = "{\"jsonrpc\":\"2.0\",\"method\":\"GetLink\",\"state\":{\"header\":{\"user\":0,\"@\":\"4e565bc8\"},\"subheader\":{\"@\":\"b6142355\"},\"content\":{\"@\":\"b340c225\"},\"footer\":{\"@\":\"4c4004c8\"}},\"params\":{\"captcha\":\"" + captchaId + "\",\"word\":\"" + code + "\"},\"id\":1}";
        return jsonoutput;
    }

}