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
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import jd.OptionalPluginWrapper;
import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.swing.components.Balloon;
import jd.nutils.encoding.Encoding;
import jd.nutils.nativeintegration.LocalBrowser;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rs-layer.com" }, urls = { "http://[\\w\\.]*?rs-layer\\.com/(.+)\\.html" }, flags = { 0 })
public class RsLrCm extends PluginForDecrypt {

    private static Pattern linkPattern = Pattern.compile("onclick=\"getFile\\('([^;]*)'\\)", Pattern.CASE_INSENSITIVE);
    private static String strCaptchaPattern = "<img src=\"(captcha-[^\"]*\\.png)\" ";
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?rs-layer\\.com/(.+)\\.html", Pattern.CASE_INSENSITIVE);
    private static long LATEST_OPENED_CNL_TIME = 0;
    private static HashMap<String, Boolean> CNL_URL_MAP = new HashMap<String, Boolean>();

    public RsLrCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    private boolean add_container(String cryptedLink, String ContainerFormat, ArrayList<DownloadLink> decryptedLinks) throws IOException {
        String link_id = new Regex(cryptedLink, patternSupported).getMatch(0);
        String container_link = "http://rs-layer.com/" + link_id + ContainerFormat;
        if (br.containsHTML(container_link)) {
            File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ContainerFormat);

            br.cloneBrowser().downloadConnection(container, br.openGetConnection(container_link));
            decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));
            container.delete();
            return true;
        }
        return false;
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("-jd.html", ".html");

        br.getPage(parameter);

        if (isExternInterfaceActive()) {

            // CNL Dummy
            if ((System.currentTimeMillis() - LATEST_OPENED_CNL_TIME) > 60 * 1000 && !CNL_URL_MAP.containsKey(parameter)) {
                if (br.getRegex("\\<\\!\\-\\- cnl \\-\\-\\>").matches()) {
                    LATEST_OPENED_CNL_TIME = System.currentTimeMillis();

                    LocalBrowser.openDefaultURL(new URL(parameter.substring(0, parameter.lastIndexOf(".")) + "-jd.html"));
                    CNL_URL_MAP.put(parameter, Boolean.TRUE);
                    Balloon.show(JDL.L("jd.controlling.CNL2.checkText.title", "Click'n'Load"), null, JDL.L("jd.controlling.CNL2.checkText.message", "Click'n'Load URL opened"));
                    return decryptedLinks;

                }
            }
        }
        if (parameter.indexOf("/link-") != -1) {
            String link = br.getRegex("<frame.*?src=\"(.*?)\".*?>").getMatch(0);
            if (link == null) return null;
            decryptedLinks.add(createDownloadlink(link));
        } else if (parameter.indexOf("/directory-") != -1) {
            boolean cont = false;
            for (int i = 1; i < 6; i++) {
                Form[] forms = br.getForms();
                if (forms != null && forms.length != 0 && forms[0] != null && br.getRegex(linkPattern).getColumn(0).length == 0&&!br.containsHTML("\\/img\\/button")) {
                    Form captchaForm = forms[0];
                    String captchaFileName = br.getRegex(strCaptchaPattern).getMatch(0);
                    if (captchaFileName == null) { 
                        return null; }
                    String captchaUrl = "http://rs-layer.com/" + captchaFileName;
                    String captchaCode = getCaptchaCode(captchaUrl, param);
                    captchaForm.put("captcha_input", captchaCode);
                    br.submitForm(captchaForm);

                    if (br.containsHTML("Sicherheitscode<br />war nicht korrekt")) {
                        logger.info(JDL.L("plugins.decrypt.general.captchacodewrong", "Wrong CAPTCHA code"));
                        continue;
                    }
                    if (br.containsHTML("Gültigkeit für den<br> Sicherheitscode<br>ist abgelaufen")) {
                        logger.info(JDL.L("plugins.decrypt.rslayer.captchaexpired", "CAPTCHA code is too old"));
                        continue;
                    }
                } else {
                    cont = true;
                    break;
                }
            }
            if (cont == false) { throw new DecrypterException(DecrypterException.CAPTCHA); }
            String layerLinks[] = br.getRegex(linkPattern).getColumn(0);
            System.out.println(layerLinks.length);
            if (layerLinks.length == 0) {
                if (!add_container(parameter, ".dlc", decryptedLinks)) {
                    if (!add_container(parameter, ".rsdf", decryptedLinks)) {
                        add_container(parameter, ".ccf", decryptedLinks);
                    }
                }
            } else {
                progress.setRange(layerLinks.length);
                for (String element : layerLinks) {
                    br.getPage("http://rs-layer.com/link-" + element + ".html");
                    String link = Encoding.htmlDecode(br.getRegex("<i?frame.*?src=\"(.*?)\".*?>").getMatch(0));

                    if (link != null) {
                        if (link.startsWith("http://")) {
                            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(link)));
                        } else {

                            String dllink = "http://rs-layer.com/" + link;
                            br.getPage(dllink);
                            String finallink = br.getRegex("src=\"(.*?)\"").getMatch(0);
                            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(finallink)));
                        }
                    }
                    progress.increase(1);
                }
            }
        }

        return decryptedLinks;
    }

    private static boolean isExternInterfaceActive() {
        final OptionalPluginWrapper plg = JDUtilities.getOptionalPlugin("externinterface");
        return (plg != null && plg.isLoaded() && plg.isEnabled());
    }

}