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
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filecrypt.cc" }, urls = { "https?://(?:www\\.)?filecrypt\\.cc/Container/([A-Z0-9]{10})\\.html" }, flags = { 0 })
public class FileCryptCc extends PluginForDecrypt {

    public FileCryptCc(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        final String uid = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        // skip text captcha
        br.postPage(parameter, "recaptcha_response_field=");
        if (br.getURL().contains("filecrypt.cc/404.html")) {
            try {
                decryptedLinks.add(createOfflinelink(parameter));
            } catch (final Throwable t) {
                logger.info("OfflineLink :" + parameter);
            }
            return decryptedLinks;
        }
        int counter = 0;
        Form captchaform = null;
        while (counter++ < 3 && br.containsHTML("class=\"safety\"")) {
            final Form[] allForms = br.getForms();
            if (allForms != null && allForms.length != 0) {
                for (final Form aForm : allForms) {
                    if (aForm.containsHTML("captcha")) {
                        captchaform = aForm;
                        break;
                    }
                }
            }
            final String captcha = br.getRegex("(/captcha/[^<>\"]*?)\"").getMatch(0);
            if (captcha == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            if (captcha.contains("circle.php")) {
                final File file = this.getLocalCaptchaFile();
                br.cloneBrowser().getDownload(file, captcha);
                final Point p = UserIO.getInstance().requestClickPositionDialog(file, "Click on the open circle", null);
                if (p == null) {
                    throw new DecrypterException(DecrypterException.CAPTCHA);
                }
                captchaform.put("button.x", String.valueOf(p.x));
                captchaform.put("button.y", String.valueOf(p.y));
                br.submitForm(captchaform);
            }
        }
        if (br.containsHTML("class=\"safety\"")) {
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
            final String dlc_id = br.getRegex("DownloadDLC\\(\\'([^<>\"]*?)\\'\\)").getMatch(0);
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
        final String[] links = br.getRegex("openLink\\(\\'([^<>\"]*?)\\'").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        br.setFollowRedirects(false);
        for (final String singleLink : links) {
            br.getPage("http://filecrypt.cc/Link/" + singleLink + ".html");
            String finallink = null;
            final String first_rd = br.getRedirectLocation();
            if (first_rd != null && first_rd.contains("filecrypt.cc/")) {
                br.getPage(first_rd);
                finallink = br.getRedirectLocation();
            } else if (first_rd != null && !first_rd.contains("filecrypt.cc/")) {
                finallink = first_rd;
            } else {
                final String nextlink = br.getRegex("\"(https?://(www\\.)?filecrypt\\.cc/index\\.php\\?Action=(G|g)o[^<>\"]*?)\"").getMatch(0);
                if (nextlink != null) {
                    br.getPage(nextlink);
                    finallink = br.getRedirectLocation();
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
}