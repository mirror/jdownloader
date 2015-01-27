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

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

import org.jdownloader.captcha.utils.recaptcha.api2.Recaptcha2Helper;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "filecrypt.cc" }, urls = { "https?://(?:www\\.)?filecrypt\\.cc/Container/([A-Z0-9]{10})\\.html" }, flags = { 0 })
public class FileCryptCc extends PluginForDecrypt {

    public FileCryptCc(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br = new Browser();
        JDUtilities.getPluginForHost("mediafire.com");
        br.getHeaders().put("User-Agent", jd.plugins.hoster.MediafireCom.stringUserAgent());
        br.setFollowRedirects(true);
        final String uid = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        // not all captcha types are skipable (recaptchav2 isn't). I tried with new response value - raztoki
        getPage(parameter);
        if (br.getURL().contains("filecrypt.cc/404.html")) {
            try {
                decryptedLinks.add(createOfflinelink(parameter));
            } catch (final Throwable t) {
                logger.info("OfflineLink :" + parameter);
            }
            return decryptedLinks;
        }
        int counter = 0;
        final int retry = 3;
        while (counter++ < retry && containsCaptcha()) {
            Form captchaform = null;
            final Form[] allForms = br.getForms();
            if (allForms != null && allForms.length != 0) {
                for (final Form aForm : allForms) {
                    if (aForm.containsHTML("captcha")) {
                        captchaform = aForm;
                        break;
                    }
                }
            }
            final String captcha = captchaform != null ? captchaform.getRegex("(/captcha/[^<>\"]*?)\"").getMatch(0) : null;
            if (captcha != null && captcha.contains("circle.php")) {
                final File file = this.getLocalCaptchaFile();
                br.cloneBrowser().getDownload(file, captcha);
                final Point p = UserIO.getInstance().requestClickPositionDialog(file, "Click on the open circle", null);
                if (p == null) {
                    throw new DecrypterException(DecrypterException.CAPTCHA);
                }
                // captchaform.remove("button.x");
                // captchaform.remove("button.y");
                captchaform.put("button.x", String.valueOf(p.x));
                captchaform.put("button.y", String.valueOf(p.y));
                submitForm(captchaform);
                // br.postPage(br.getURL(), "button.x=" + p.x + "&button.y=" + p.y);

            } else if (captchaform != null && captchaform.containsHTML("=\"g-recaptcha\"")) {
                // recaptcha v2
                boolean success = false;
                String responseToken = null;
                do {
                    Recaptcha2Helper rchelp = new Recaptcha2Helper();
                    rchelp.init(this.br);
                    final File outputFile = rchelp.loadImageFile();
                    String code = getCaptchaCode("recaptcha", outputFile, param);
                    success = rchelp.sendResponse(code);
                    responseToken = rchelp.getResponseToken();
                    counter++;
                } while (!success && counter <= retry);
                if (!success) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                captchaform.put("g-recaptcha-response", Encoding.urlEncode(responseToken));
                submitForm(captchaform);
            } else if (captcha != null) {
                // they use recaptcha response field key for non recaptcha.. math sum and text =
                // http://filecrypt.cc/captcha/captcha.php?namespace=container
                // using bismarck original observation, this type is skipable.
                if (counter > 1) {
                    final String code = getCaptchaCode(captcha, param);
                    captchaform.put("recaptcha_response_field", Encoding.urlEncode(code));
                } else {
                    captchaform.put("recaptcha_response_field", "");
                }
                submitForm(captchaform);
            } else {
                break;
            }
        }
        if (counter == retry && containsCaptcha()) {
            throw new DecrypterException(DecrypterException.CAPTCHA);
        }
        final String fpName = br.getRegex("class=\"status (online|offline) shield\">([^<>\"]*?)<").getMatch(1);

        // mirrors
        String[] mirrors = br.getRegex("\"([^\"]*/Container/" + uid + "\\.html\\?mirror=\\d+)\"").getColumn(0);
        if (mirrors.length < 1) {
            mirrors = new String[1];
            mirrors[0] = parameter + "?mirror=0";
        }
        // first mirror shown should be mirror 0;
        Arrays.sort(mirrors);
        for (String mirror : mirrors) {
            // if 0 we don't need to get new page
            if (!mirror.endsWith("mirror=0")) {
                br.getPage(mirror);
            }
            /* First try DLC, then single links */
            final String dlc_id = br.getRegex("DownloadDLC\\('([^<>\"]*?)'\\)").getMatch(0);
            if (dlc_id != null) {
                logger.info("DLC found - trying to add it");
                decryptedLinks.addAll(loadcontainer("http://filecrypt.cc/DLC/" + dlc_id + ".dlc"));
            }
        }
        if (!decryptedLinks.isEmpty()) {
            logger.info("DLC successfully added");
            return decryptedLinks;
        }

        // this isn't always shown, see 104061178D - raztoki 20141118

        logger.info("Trying single link handling");
        final String[] links = br.getRegex("openLink\\('([^<>\"]*?)'").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        br.setFollowRedirects(false);
        for (final String singleLink : links) {
            final Browser br2 = br.cloneBrowser();
            br2.getPage("http://filecrypt.cc/Link/" + singleLink + ".html");
            String finallink = null;
            final String first_rd = br2.getRedirectLocation();
            if (first_rd != null && first_rd.contains("filecrypt.cc/")) {
                br2.getPage(first_rd);
                finallink = br2.getRedirectLocation();
            } else if (first_rd != null && !first_rd.contains("filecrypt.cc/")) {
                finallink = first_rd;
            } else {
                final String nextlink = br2.getRegex("\"(https?://(www\\.)?filecrypt\\.cc/index\\.php\\?Action=(G|g)o[^<>\"]*?)\"").getMatch(0);
                if (nextlink != null) {
                    br2.getPage(nextlink);
                    finallink = br2.getRedirectLocation();
                }
            }
            if (finallink == null || finallink.contains("filecrypt.cc/")) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        }

        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

    private final boolean containsCaptcha() {
        return new Regex(cleanHTML, containsCaptcha).matches();
    }

    private final String containsCaptcha = "class=\"safety\">Sicherheitsabfrage<";

    private String       cleanHTML       = null;

    private final void cleanUpHTML() {
        String toClean = br.toString();
        ArrayList<String> regexStuff = new ArrayList<String>();
        // generic cleanup
        regexStuff.add("<!(--.*?--)>");
        regexStuff.add("(<\\s*(\\w+)\\s+[^>]*style\\s*=\\s*(\"|')(?:(?:[\\w:;\\s#-]*(visibility\\s*:\\s*hidden;|display\\s*:\\s*none;|font-size\\s*:\\s*0;)[\\w:;\\s#-]*)|font-size\\s*:\\s*0|visibility\\s*:\\s*hidden|display\\s*:\\s*none)\\3[^>]*(>.*?<\\s*/\\2[^>]*>|/\\s*>))");

        for (String aRegex : regexStuff) {
            String results[] = new Regex(toClean, aRegex).getColumn(0);
            if (results != null) {
                for (String result : results) {
                    toClean = toClean.replace(result, "");
                }
            }
        }

        cleanHTML = toClean;
    }

    @SuppressWarnings("deprecation")
    private ArrayList<DownloadLink> loadcontainer(final String theLink) throws IOException, PluginException {
        ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
        final Browser brc = br.cloneBrowser();
        File file = null;
        URLConnectionAdapter con = null;
        try {
            con = brc.openGetConnection(theLink);
            if (con.getResponseCode() == 200) {
                file = JDUtilities.getResourceFile("tmp/filecryptcc/" + JDHash.getSHA1(theLink) + theLink.substring(theLink.lastIndexOf(".")));
                if (file == null) {
                    return links;
                }
                file.getParentFile().mkdirs();
                file.deleteOnExit();
                brc.downloadConnection(file, con);
                if (file != null && file.exists() && file.length() > 100) {
                    links.addAll(JDUtilities.getController().getContainerLinks(file));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
            if (file.exists()) {
                file.delete();
            }

        }
        return links;
    }

    private final void getPage(final String page) throws IOException, PluginException {
        if (page == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(page);
        cleanUpHTML();
    }

    private final void postPage(final String url, final String post) throws IOException, PluginException {
        if (url == null || post == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.postPage(url, post);
        cleanUpHTML();
    }

    private final void submitForm(final Form form) throws Exception {
        if (form == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.submitForm(form);
        cleanUpHTML();
    }

}