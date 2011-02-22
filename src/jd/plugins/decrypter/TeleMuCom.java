//    jDownloader - Downloadmanager
//    Copyright (C) 2011  JD-Team support@jdownloader.org
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
//
package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.DecryptPluginWrapper;
import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision: 10500 $", interfaceVersion = 2, names = { "telechargementmu.com" }, urls = { "http://[\\w\\.]*telechargementmu\\.com/[musique]*/([pop]*/|)[0-9]+-.*?\\.html" }, flags = { 0 })
public class TeleMuCom extends PluginForDecrypt {

    public TeleMuCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        // The site needs authentication to works (It seems that the browser
        // cookie does not work, then I add it here)
        br.setCookie(parameter, "dle_user_id", "9756");
        br.setCookie(parameter, "dle_password", "23d45b337ff85d0a326a79082f7c6f50");
        br.getPage(parameter);
        if (br.containsHTML("(You must  register before you can view this text.)")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "You may be logon on the web site to available links."));
        String fpName = br.getRegex("<title>(.*?)</title>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("<h3>(.*?)</h3>").getMatch(0);
        }

        // On this site, when an accent is set, the result is as extended ASCII
        // character which is bad in the name of the package
        fpName = br.normalizeExtendedASCII(fpName);
        fpName = RemoveCharacter(fpName);

        int iLinkImage = 0;
        String[] TabImage = br.getRegex("<img[^>]+src\\s*=\\s*['\"](http://[^'\"]+)\\.jpg['\"][^>]*>").getColumn(0);

        if (TabImage != null) {
            for (String strImageLink : TabImage) {
                TabImage[iLinkImage] = strImageLink + ".jpg";
                iLinkImage++;
            }
        }

        String[] TabPassword = br.getRegex("</a>[ \t]+([A-Z0-9][^ ]*)[ \t]+<a href").getColumn(0);

        String[] TabTemp = br.getRegex("<a href=\"http://protect-my-links\\.com/(.*?)\"").getColumn(0);
        if (TabTemp != null) {
            for (int iIndex = 0; iIndex < TabTemp.length; iIndex++) {
                TabTemp[iIndex] = "http://protect-my-links.com/" + TabTemp[iIndex];
            }
        }
        if (TabTemp == null || TabTemp.length == 0) {
            TabTemp = br.getRegex("http://[\\w\\.]*jheberg\\.com/(.*?)\\.html").getColumn(0);
            if (TabTemp == null || TabTemp.length == 0) {
                return null;
            } else {
                for (int iIndex = 0; iIndex < TabTemp.length; iIndex++) {
                    if (TabTemp[iIndex].indexOf("...") == -1)
                        TabTemp[iIndex] = "http://www.jheberg.com/" + TabTemp[iIndex] + ".html";
                    else
                        TabTemp[iIndex] = null;
                }
            }
        }

        String strLink = null;
        String[] linksTemp = new String[TabTemp.length];

        ArrayList<DecryptPluginWrapper> AllDecrypters = new ArrayList<DecryptPluginWrapper>(DecryptPluginWrapper.getDecryptWrapper());

        int iLink = 0;
        for (int iIndex = 0; iIndex < TabTemp.length; iIndex++) {
            if (TabTemp[iIndex] != null) {
                strLink = TabTemp[iIndex].toLowerCase();
                for (DecryptPluginWrapper wrapper : AllDecrypters) {
                    if (wrapper.getHost() != this.getHost()) {
                        if (wrapper.canHandle(strLink)) {
                            linksTemp[iLink] = strLink;
                            iLink++;
                            break;
                        }
                    }
                }
            }
        }

        String[] links = new String[iLink];
        for (int iIndex = 0; iIndex < links.length; iIndex++) {
            links[iIndex] = linksTemp[iIndex];
        }
        int iImage = TabImage == null ? 0 : TabImage.length;

        progress.setRange(links.length + iImage);

        for (String redirectlink : links) {
            decryptedLinks.add(createDownloadlink(redirectlink));
            progress.increase(1);
        }

        if (TabImage != null) {
            for (String strImageLink : TabImage) {
                decryptedLinks.add(createDownloadlink(strImageLink));
                progress.increase(1);
            }
        }

        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);

            if (TabPassword.length == 1 && TabPassword[0] != "AUCUN") {
                fp.setPassword(TabPassword[0].trim());
            }
        }
        return decryptedLinks;
    }

    /**
     * Allows to remove some character to have a nice name
     * 
     * @param strName
     *            The name of the package
     * @return the name of the package normalized.
     */
    private String RemoveCharacter(String strName) {
        String strRemover = "( - )|( · )";
        String[] strTemp = strName.split(strRemover);
        if (strTemp.length >= 2) {
            strName = strTemp[0].trim() + " - " + strTemp[1].trim();
        }

        strName = strName.replace("", "-");
        strName = strName.replace("", "'");
        strName = strName.replace(":", ",");

        strName = strName.replace("VA - ", "");
        strName = strName.replace("FLAC", "");
        return strName;
    }
}
