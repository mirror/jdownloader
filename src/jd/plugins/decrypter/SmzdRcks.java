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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDUtilities;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;

@DecrypterPlugin(revision = "$Revision: 28619 $", interfaceVersion = 3, names = { "smoozed.rocks" }, urls = { "https?://(www\\.)?smoozed\\.rocks/folder/[A-Za-z0-9\\-_]+" }, flags = { 0 })
public class SmzdRcks extends PluginForDecrypt {

    private String ssid;

    public SmzdRcks(PluginWrapper wrapper) {
        super(wrapper);

        // some kind of canvas browser id?
        // seems like a random number works pretty fine
        long min = 1000000000l;
        long max = 9999999999l;
        ssid = (min + (int) (Math.random() * ((max - min) + 1))) + "";

    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        final String rcID = br.getRegex("challenge\\?k=([^\"]+)").getMatch(0);
        // Form[] forms = br.getForms();
        final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        rc.setId(rcID);
        rc.load();
        String secretKey = null;

        for (int i = 0; i <= 15; i++) {
            final File cf = rc.downloadCaptcha(getLocalCaptchaFile());

            String c = getCaptchaCode("recaptcha", cf, param);

            if (c == null || c.length() == 0) {
                rc.reload();
                continue;
            }

            Form rcForm = br.getFormbyKey("recaptcha_response_field");
            rcForm.getInputField("ssid").setValue(ssid);
            rcForm.getInputField("recaptcha_response_field").setValue(c);

            rcForm.addInputField(new InputField("recaptcha_challenge_field", rc.getChallenge()));
            rcForm.addInputField(new InputField("mode", "free"));
            rcForm.setAction(parameter + "/access");
            Cookies cookies = br.getCookies("https://www.smoozed.rocks");

            cookies.add(new Cookie("smoozed.rocks", "sid", ssid));
            cookies.add(new Cookie("smoozed.rocks", "jid", ssid));

            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            // waittime is not evaluated
            // Thread.sleep(15000);
            br.submitForm(rcForm);
            secretKey = br.getRegex("\"secretKey\"\\s*\\:\\s*\"([^\"]+)").getMatch(0);
            if (StringUtils.isNotEmpty(secretKey)) {
                break;
            }

        }
        if (StringUtils.isEmpty(secretKey)) {
            throw new DecrypterException(DecrypterException.CAPTCHA);
        }

        br.getPage(parameter + "/" + secretKey);
        parse(br.getRequest().getHtmlCode(), decryptedLinks);
        return decryptedLinks;
    }

    private void parse(String htmlCode, ArrayList<DownloadLink> decryptedLinks) throws IOException {

        ArrayList<Object> obj = JSonStorage.restoreFromString(br.toString(), new TypeRef<ArrayList<Object>>() {
        });
        // String cnl = (String) obj.get(0);
        // String dlc = (String) obj.get(1);
        String accessKey = (String) obj.get(3);

        Map<String, List<List<String>>> mirrorMap = (Map<String, List<List<String>>>) obj.get(2);

        for (Entry<String, List<List<String>>> es : mirrorMap.entrySet()) {
            for (List<String> linkInfo : es.getValue()) {
                // String status = linkInfo.get(0);
                // String name = linkInfo.get(1);
                // String name_short = linkInfo.get(2);
                // String size = linkInfo.get(3);
                String id = linkInfo.get(4);

                String link = "https://www.smoozed.rocks/dl/" + id + "/" + accessKey + "/" + ssid + "?direct=1";
                Browser clone = br.cloneBrowser();
                clone.setFollowRedirects(false);
                clone.getPage(link);
                String redirect = clone.getRedirectLocation();
                if (StringUtils.isNotEmpty(redirect)) {
                    decryptedLinks.add(createDownloadlink(redirect));
                }
            }

        }

    }
}
