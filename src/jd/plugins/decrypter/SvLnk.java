//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jd.DecryptPluginWrapper;
import jd.HostPluginWrapper;
import jd.PluginWrapper;
import jd.controlling.DistributeData;
import jd.controlling.ProgressController;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Base64;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDHexUtils;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "savelink1", "savelink2" }, urls = { "http://.*?\\..*?/.*?/sl/.*", "http://.*?\\..*?/.*?[\\?\\&]sl=1.*" }, flags = { 0, 0 })
public class SvLnk extends PluginForDecrypt {

    private static final String RECAPTCHA = "recaptcha";
    private static final ArrayList<DownloadLink> NULL = new ArrayList<DownloadLink>();
    private static final String LINK = "link";
    private static final String AES = "aes";

    public SvLnk(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(parameter.getCryptedUrl());
        try {
            String[] slParameters = null;
            String sl = br.getRegex("<meta\\s+?name\\s*?=\\s*?\"savelink\"\\s*?content=\"(.+?)\"\\s*?/?\\s*?>").getMatch(0);
            if (sl != null) {
                slParameters = sl.split(":");
            }
            if (slParameters != null && slParameters[0].equalsIgnoreCase(RECAPTCHA)) {
                return decryptRecaptcha(parameter, progress);
            } else if (slParameters != null && slParameters[0].equalsIgnoreCase(LINK)) {
                return followLink(slParameters[1]);

            } else if (slParameters != null && slParameters[0].equalsIgnoreCase(AES)) {

                byte[] byteKey = JDHexUtils.getByteArray(slParameters[1]);
                SecretKeySpec skeySpec = new SecretKeySpec(byteKey, "AES");
                IvParameterSpec ivSpec = new IvParameterSpec(byteKey);
                Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
                c.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);

                byte[] dec = c.doFinal(Base64.decode(slParameters[2].trim()));

                String decoded = new String(dec);

                String[] urls = HTMLParser.getHttpLinks(decoded, null);
                StringBuilder sb = new StringBuilder();
                for (String s : urls) {
                    if (!s.equalsIgnoreCase(parameter.getCryptedUrl())) {
                        sb.append(s + "\r\n");
                    }
                }
                ArrayList<DownloadLink> links = new DistributeData(sb.toString()).findLinks();
                for (Iterator<DownloadLink> it = links.iterator(); it.hasNext();) {
                    if (it.next().getDownloadURL().equalsIgnoreCase(parameter.getCryptedUrl())) it.remove();
                }

                return links;
            } else {
                String[] redirect = br.getRegex("<meta http-equiv=\"refresh\" content=\"(\\d+)\\; url=(.*?)\">").getRow(0);
                if (redirect != null) {
                    // metaredirects
                    return single(redirect[1]);

                } else {
                    // check source for links
                    String[] urls = HTMLParser.getHttpLinks(br.toString(), br.getBaseURL());
                    StringBuilder sb = new StringBuilder();
                    for (String s : urls) {
                        if (!s.equalsIgnoreCase(parameter.getCryptedUrl())) {
                            sb.append(s + "\r\n");
                        }
                    }
                    ArrayList<DownloadLink> links = new DistributeData(sb.toString()).findLinks();
                    for (Iterator<DownloadLink> it = links.iterator(); it.hasNext();) {
                        if (it.next().getDownloadURL().equalsIgnoreCase(parameter.getCryptedUrl())) it.remove();
                    }
                    return links;
                }

            }

        } catch (Exception e) {
            return NULL;
        }
    }

    private ArrayList<DownloadLink> followLink(String linktitle) throws BrowserException {
        String follow = br.getRegex("<a.*?href=\"([^\"]*)\"[^>]*>\\s*" + linktitle + "\\s*<").getMatch(0);
        follow = br.getURL(follow);

        return single(follow);
    }

    /**
     * Returns a list with 1 entry: DownloadLink(follow...)
     * 
     * @param follow
     * @return
     */
    private ArrayList<DownloadLink> single(String follow) {
        ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
        links.add(this.createDownloadlink(follow));
        return links;

    }

    private ArrayList<DownloadLink> decryptRecaptcha(CryptedLink parameter, ProgressController progress) throws Exception {

        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        rc.parse();
        rc.load();
        File captchaFile = this.getLocalCaptchaFile();
        rc.downloadCaptcha(captchaFile);
        String code = getCaptchaCode("recaptcha", captchaFile, parameter);
        rc.setCode(code);

        String[] urls = HTMLParser.getHttpLinks(br.toString(), br.getBaseURL());
        StringBuilder sb = new StringBuilder();
        for (String s : urls) {
            if (!s.equalsIgnoreCase(parameter.getCryptedUrl()) && (HostPluginWrapper.hasPlugin(s) || DecryptPluginWrapper.hasPlugin(s))) {
                sb.append(s + "\r\n");
            }
        }
        ArrayList<DownloadLink> links = new DistributeData(sb.toString()).findLinks();
        for (Iterator<DownloadLink> it = links.iterator(); it.hasNext();) {
            if (it.next().getDownloadURL().equalsIgnoreCase(parameter.getCryptedUrl())) it.remove();
        }
        return links;

    }
}
