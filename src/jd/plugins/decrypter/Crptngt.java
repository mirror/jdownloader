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

import jd.PluginWrapper;
import jd.controlling.JDController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "crypting.it" }, urls = { "http://[\\w\\.]*?crypting\\.it/(s/[\\w]+|index\\.php\\?p=show(usrfolders)?(&user=.+)?&id=[\\w]+)" }, flags = { 0 })
public class Crptngt extends PluginForDecrypt {

    public Crptngt(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static int getCode(String code) {
        try {
            int ind = code.indexOf('p');
            if (ind == -1) {
                ind = code.indexOf('m');
                return Integer.parseInt(code.substring(0, ind)) - Integer.parseInt(code.substring(ind + 1));
            }
            return Integer.parseInt(code.substring(0, ind)) + Integer.parseInt(code.substring(ind + 1));
        } catch (Exception e) {
        }
        return 0;
    }

    // @Override
    @SuppressWarnings("static-access")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        setBrowserExclusive();
        // No language cookie
        br.getPage("http://www.crypting.it/index.php?l=en&p=home");
        br.getPage(parameter);

        // Password
        Form form = br.getForm(1);
        if (form == null) return null;
        if (br.containsHTML("password to access this folder")) {
            for (int i = 1; i <= 5; i++) {
                String folderPass = getUserInput(null, param);
                form.put("opass", folderPass);
                br.submitForm(form);
                if (br.containsHTML("maximum number of password attempts")) throw new DecrypterException("You have entered the maximum number of password attempts");
                if (!br.containsHTML("Wrong password")) break;
            }
            if (br.containsHTML("Wrong password")) throw new DecrypterException("Wrong Password");
        }

        // Captcha (browser split, so only 1 captcha is needed for several
        // mirrors)
        for (int i = 1; i <= 5; i++) {
            String code = getCaptchaCode("http://www.crypting.it/captcha.php", param);
            form.put("AnimCaptcha", "" + getCode(code));
            Browser br3 = br.cloneBrowser();
            br3.submitForm(form); // Wrong answer!
            String error = br3.getRegex("alert\\(\"(.*?)\"\\);").getMatch(0);
            if (error == null) break;
            if (error != null && i == 5) throw new DecrypterException("Wrong Captcha Code!");
        }

        String password = br.getRegex("<td valign=\"top\" style=\"border-bottom: 1px dotted #C8C8C8;\"><div align=\"center\">(.*?)</div></td>").getMatch(0, 2);
        if (password != null) password = password.trim();
        String[] mirrors = br.getRegex("mirrorvalue\" value=\"(.*?)\"").getColumn(0);
        if (mirrors.length == 0) return null;
        for (String mirror : mirrors) {
            ArrayList<DownloadLink> tempDecryptedLinks = new ArrayList<DownloadLink>();
            Browser br2 = br.cloneBrowser();
            while (form.hasInputFieldByName("mirrorvalue"))
                form.remove("mirrorvalue");
            form.put("mirrorvalue", mirror);
            br2.submitForm(form);

            // Container
            /* THERE SEEMS TO BE NO VALID CONTAINER, EVEN IN BROWSER */
            String[][] containers = br2.getRegex("(http://www\\.crypting\\.it/files/download\\.php\\?fileid=[a-zA-Z0-9]+-m1(\\..*?))'").getMatches();
            for (String[] container : containers) {
                Browser br3 = br2.cloneBrowser();
                File containerFile = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + container[1]);
                br3.getHeaders().put("Referer", "http://www.crypting.it/greybox/loader_frame.html?s=0");
                br3.getDownload(containerFile, container[0]);
                /*
                 * TODO: Change to static method call after next major update!
                 */
                if (!JDController.getInstance().isContainerFile(containerFile)) {
                    containerFile.delete();
                    continue;
                }
                ArrayList<DownloadLink> dLinks = JDUtilities.getController().getContainerLinks(containerFile);
                containerFile.delete();
                if (dLinks.size() != 0) {
                    if (password != null && password.length() != 0) {
                        for (DownloadLink dLink : dLinks) {
                            dLink.addSourcePluginPassword(password);
                            tempDecryptedLinks.add(dLink);
                        }
                    } else
                        tempDecryptedLinks.addAll(dLinks);
                }
                if (tempDecryptedLinks.size() != 0) break;
            }

            // No container
            if (tempDecryptedLinks.size() == 0) {
                String[] links = br2.getRegex("(http://www\\.crypting\\.it/follow\\.php\\?url=.*?)'}").getColumn(0);
                if (links.length == 0) continue;
                progress.setRange(mirrors.length * links.length);
                for (String link : links) {
                    br2.getHeaders().put("Referer", "http://www.crypting.it/greybox/loader_frame.html?s=0");
                    br2.getPage(link);
                    /*
                     * crypting.it fakes a rapidshare site. but uploaded.to
                     * links are shown in a iframe. no other mirrors found.
                     */
                    String rdLink = br2.getRegex("iframe\\ssrc=\\s*\"(.*?)\"").getMatch(0);
                    if (rdLink == null) {
                        Form rsForm = br2.getForm(0);
                        if (rsForm != null) {
                            rdLink = rsForm.getAction();
                        } else
                            rdLink = br2.getRegex("downloadlink\">(.*?)<").getMatch(0);
                    }
                    if (rdLink == null || rdLink.trim().length() == 0)
                        continue;
                    else
                        rdLink = rdLink.trim();

                    DownloadLink dLink;
                    dLink = createDownloadlink(rdLink);
                    if (password != null && password.length() != 0) dLink.addSourcePluginPassword(password);
                    tempDecryptedLinks.add(dLink);
                    progress.increase(1);
                }
            }

            decryptedLinks.addAll(tempDecryptedLinks);
        }

        return decryptedLinks;
    }

    // @Override

}
