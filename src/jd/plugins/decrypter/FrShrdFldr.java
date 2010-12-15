//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.util.ArrayList;
import java.util.Arrays;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "4shared.com" }, urls = { "http://[\\w\\.]*?4shared(-china)?\\.com/dir/[\\w\\._-]+/[\\w\\._-]+/?" }, flags = { 0 })
public class FrShrdFldr extends PluginForDecrypt {

    public FrShrdFldr(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        String pass = "";

        // check for password
        this.br.getPage(parameter);
        if (this.br.containsHTML("enter a password to access")) {
            final Form form = this.br.getFormbyProperty("name", "theForm");
            if (form == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            for (int retry = 5; retry > 0; retry--) {
                pass = Plugin.getUserInput(null, param);
                form.put("userPass2", pass);
                this.br.submitForm(form);
                if (!this.br.containsHTML("enter a password to access")) {
                    break;
                } else {
                    if (retry == 1) {
                        logger.severe("Wrong Password!");
                        throw new DecrypterException("Wrong Password!");
                    }
                }
            }
        }

        // allow choice scan only root directory or all subdirectories too
        final int result = UserIO.getInstance().requestConfirmDialog(UserIO.STYLE_LARGE | UserIO.NO_COUNTDOWN, JDL.L("jd.plugins.decrypter.frshrdfldr.dir.title", "Directory scan"), JDL.L("jd.plugins.decrypter.frshrdfldr.dir.message", "Do you want scan all subdirectory or only files in this directory?"), null, JDL.L("jd.plugins.decrypter.frshrdfldr.dir.ok", "Subdirectory"), JDL.L("jd.plugins.decrypter.frshrdfldr.dir.cancel", "File"));

        final String script = this.br.getRegex("src=\"(/account/homeScript.*?)\"").getMatch(0);
        this.br.cloneBrowser().getPage("http://" + this.br.getHost() + script);
        final String burl = this.br.getRegex("var bUrl = \"(/account/changedir.jsp\\?sId=.*?)\";").getMatch(0);
        final String name = this.br.getRegex("hidden\" name=\"defaultZipName\" value=\"(.*?)\">").getMatch(0).trim();

        if (result == 4) {
            this.scanFile(decryptedLinks, pass, burl, name, name);
        }
        if (result == 2) {
            this.scanDirectory(decryptedLinks, pass, burl, name, progress);
        }

        return decryptedLinks;
    }

    // replace ID directory with correct name
    private void replaceA(final ArrayList<String> names, final String oldS, final String newS) throws Exception {
        String rpl;
        int pos;

        for (int x = 0; x < names.size(); x++) {
            rpl = "\\" + names.get(x) + "\\";
            pos = rpl.indexOf("\\" + oldS + "\\");
            if (pos != -1) {
                names.set(x, rpl.substring(0, pos) + "\\" + newS + "\\" + rpl.substring(pos + 1 + oldS.length()));
            }
        }
    }

    // scan all ID directory and make simple tree structure
    private void scanDirectory(final ArrayList<DownloadLink> decryptedLinks, final String pass, final String burl, final String defName, final ProgressController progress) throws Exception {
        final ArrayList<String> links = new ArrayList<String>();
        final ArrayList<String> names = new ArrayList<String>();
        String name;

        links.addAll(Arrays.asList(this.br.getRegex("new WebFXTreeItem\\('(.+?)','javascript:changeDirLeft\\((\\d+)\\)',false\\)").getColumn(1)));
        for (int i = 0; i < links.size(); i++) {
            name = this.br.getRegex("tree(\\w+)\\.add\\(tree" + links.get(i) + "\\)").getMatch(0);
            if (name == null) {
                names.add(defName + "\\" + links.get(i));
            } else {
                names.add(names.get(links.indexOf(name)) + "\\" + links.get(i));
            }
        }

        // scan files in actual directory
        this.scanFile(decryptedLinks, pass, burl, defName, defName);
        progress.increase(1);

        progress.setRange(links.size());
        for (int i = 0; i < links.size(); i++) {
            final String url = "http://" + this.br.getHost() + burl + "&ajax=true&changedir=" + links.get(i) + "&sortsMode=NAME&sortsAsc=true&random=0.1863370989474954";
            this.br.getPage(url);
            name = this.br.getRegex("<b style=\"font-size:larger;\">\\s*(.+?)\\s*</b>").getMatch(0);
            this.replaceA(names, links.get(i), name);
            this.scanFile(decryptedLinks, pass, burl, name, names.get(i));
            progress.increase(1);
        }
    }

    // scan file in actual directory and put in filepackage with setting correct
    // save directory
    private void scanFile(final ArrayList<DownloadLink> decryptedLinks, final String pass, final String burl, final String name, final String path) throws Exception {
        final ArrayList<DownloadLink> dL = new ArrayList<DownloadLink>();
        final String[] pages = this.br.getRegex("javascript:pagerShowFiles\\((\\d+)\\);").getColumn(0);
        String[] links;

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(name + "-" + path);

        // scan
        links = this.br.getRegex("<a href=\"(http://[\\w\\.]*?4shared(-china)?\\.com/(get|file|document|photo|video|audio)/.+?/.*?)(\\?dirPwdVerified|.)\"").getColumn(0);
        for (final String dl : links) {
            DownloadLink dlink;
            dlink = this.createDownloadlink(dl);
            if (pass.length() != 0) {
                dlink.setProperty("pass", pass);
            }
            decryptedLinks.add(dlink);
            dL.add(dlink);
        }

        // scan all possible tabs
        for (int i = 0; i < pages.length - 1; i++) {
            final String url = "http://" + this.br.getHost() + burl + "&ajax=true&firstFileToShow=" + pages[i] + "&sortsMode=NAME&sortsAsc=&random=0.9519735221243086";
            this.br.getPage(url);

            // scan
            links = this.br.getRegex("<a href=\"(http://[\\w\\.]*?4shared(-china)?\\.com/(get|file|document|photo|video|audio)/.+?/.*?)(\\?dirPwdVerified|.)\"").getColumn(0);
            for (final String dl : links) {
                DownloadLink dlink;
                dlink = this.createDownloadlink(dl);
                if (pass.length() != 0) {
                    dlink.setProperty("pass", pass);
                }
                decryptedLinks.add(dlink);
                dL.add(dlink);
            }
        }
        if (dL.size() > 0) {
            fp.addLinks(dL);
        }
    }
}
