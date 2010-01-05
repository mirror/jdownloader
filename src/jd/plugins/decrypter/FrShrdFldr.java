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

import java.util.*;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.userio.UserIOGui;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "4shared.com" }, urls = { "http://[\\w\\.]*?(4shared|4shared-china)\\.com/dir/\\d+/[\\w]+/?" }, flags = { 0 })
public class FrShrdFldr extends PluginForDecrypt {

    public FrShrdFldr(PluginWrapper wrapper) {
        super(wrapper);
    }

    //scan file in actual directory and put in filepackage with setting correct save directory 
    private void scanFile(ArrayList<DownloadLink> decryptedLinks, String pass, String burl, String name, String path) throws Exception {
        ArrayList<DownloadLink> dL = new ArrayList<DownloadLink>();
        String[] pages = br.getRegex("javascript:pagerShowFiles\\((\\d+)\\);").getColumn(0);
        String[] links;

        FilePackage fp = FilePackage.getInstance();
        fp.setName(name);
        fp.setDownloadDirectory(fp.getDownloadDirectory() + '\\' + path);

        //if (!br.containsHTML("dirPwdVerified="))  -- Don't work correctly on all 4shared folders :( 
        
        //scan simple 
        links = br.getRegex("<a href=\"(http://[\\w\\.]*?(4shared|4shared-china)\\.com/file/.*?)\"").getColumn(0);
        for (String dl : links) {
            DownloadLink dlink;
            dlink = createDownloadlink(dl);
            if (pass.length() != 0) dlink.setProperty("pass", pass);
            decryptedLinks.add(dlink);
            dL.add(dlink);
        }

        //scan verified
        links = br.getRegex("<a href=\"(http://[\\w\\.]*?(4shared|4shared-china)\\.com/file/.*?)\\?dirPwdVerified").getColumn(0);
        for (String dl : links) {
            DownloadLink dlink;
            dlink = createDownloadlink(dl);
            if (pass.length() != 0) dlink.setProperty("pass", pass);
            decryptedLinks.add(dlink);
            dL.add(dlink);
        }
        
        //scan all possible tabs
        for (int i = 0; i < pages.length - 1; i++) {
            String url =  "http://" + br.getHost() + burl + "&ajax=true&firstFileToShow=" + pages[i] + "&sortsMode=NAME&sortsAsc=&random=0.9519735221243086";
            br.getPage(url);

            //scan simple 
            links = br.getRegex("<a href=\"(http://[\\w\\.]*?(4shared|4shared-china)\\.com/file/.*?)\"").getColumn(0);
            for (String dl : links) {
                DownloadLink dlink;
                dlink = createDownloadlink(dl);
                if (pass.length() != 0) dlink.setProperty("pass", pass);
                decryptedLinks.add(dlink);
                dL.add(dlink);
            }

            //scan verified
            links = br.getRegex("<a href=\"(http://[\\w\\.]*?(4shared|4shared-china)\\.com/file/.*?)\\?dirPwdVerified").getColumn(0);
            for (String dl : links) {
                DownloadLink dlink;
                dlink = createDownloadlink(dl);
                if (pass.length() != 0) dlink.setProperty("pass", pass);
                decryptedLinks.add(dlink);
                dL.add(dlink);
            }
        }
        if (dL.size() > 0) fp.addLinks(dL);
    }
    
    //replace ID directory with correct name 
    private void replaceA(ArrayList<String> names, String oldS, String newS) throws Exception {
        String rpl;
        int pos;
        
        for (int x = 0; x < names.size(); x++) {
            rpl = "\\" + names.get(x) + "\\";
            pos = rpl.indexOf("\\" + oldS + "\\"); 
            if (pos != -1) names.set(x, rpl.substring(0, pos) + "\\" + newS + "\\" + rpl.substring(pos + 1 + oldS.length()));  
        }
    }
    
    //scan all ID directory and make simple tree structure  
    private void scanDirectory(ArrayList<DownloadLink> decryptedLinks, String pass, String burl, String defName, ProgressController progress) throws Exception {
        ArrayList<String> links = new ArrayList<String>(); 
        ArrayList<String> names = new ArrayList<String>(); 
        String name;

        links.addAll(Arrays.asList(br.getRegex("new WebFXTreeItem\\(\"(.+?)\",'javascript:changeDirLeft\\((\\d+)\\)',false\\)").getColumn(1)));
        for (int i = 0; i < links.size(); i++) {
          name = br.getRegex("tree(\\w+)\\.add\\(tree" + links.get(i)).getMatch(0);  
          if (name == null) names.add(defName + "\\" + links.get(i)); 
          else names.add(names.get(links.indexOf(name)) + "\\" + links.get(i));
        }

        //scan files in actual directory
        scanFile(decryptedLinks, pass, burl, defName, defName);
        progress.increase(1);
        
        progress.setRange(links.size());
        for (int i = 0; i < links.size(); i++) {
            String url = "http://" + br.getHost() + burl + "&ajax=true&changedir=" + links.get(i) + "&sortsMode=NAME&sortsAsc=&random=0.1863370989474954";
            br.getPage(url);
            name = br.getRegex("<input type=\"text\" class=\"fnamefieldinv\" id=\"fnamefield1\"\\s+readonly=\"\\w+\"\\s+style=\".+?\"\\s+value=\"(.+?)\"\\s+/>").getMatch(0).trim();            
            replaceA(names, links.get(i), name);
            scanFile(decryptedLinks, pass, burl, name, names.get(i));
            progress.increase(1);
        }
    }
    
    
    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        String pass = "";

        //allow choice scan only root directory or all subdirectories too
        int result = UserIOGui.getInstance().requestConfirmDialog(UserIO.STYLE_LARGE | UserIO.NO_COUNTDOWN, 
                                                                  JDL.L("jd.plugins.decrypter.frshrdfldr.dir.title", "Directory scan"), 
                                                                  JDL.L("jd.plugins.decrypter.frshrdfldr.dir.message", "Do you want scan all subdirectory or only files in this directory?"), null, 
                                                                  JDL.L("jd.plugins.decrypter.frshrdfldr.dir.ok", "Subdirectory"), 
                                                                  JDL.L("jd.plugins.decrypter.frshrdfldr.dir.cancel", "File"));
        
        //check for password 
        br.getPage(parameter);
        if (br.containsHTML("enter a password to access")) {
            Form form = br.getFormbyProperty("name", "theForm");
            if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            for (int retry = 1; retry <= 5; retry++) {
                pass = getUserInput("Password:", param);
                form.put("userPass2", pass);
                br.submitForm(form);
                if (!br.containsHTML("enter a password to access")) {
                    break;
                } else if (retry == 5) logger.severe("Wrong Password!");
            }
        }

        String script = br.getRegex("src=\"(/account/homeScript.*?)\"").getMatch(0);
        br.cloneBrowser().getPage("http://" + br.getHost() + script);
        String burl = br.getRegex("var bUrl = \"(/account/changedir.jsp\\?sId=.*?)\";").getMatch(0);
        String name = br.getRegex("hidden\" name=\"defaultZipName\" value=\"(.*?)\">").getMatch(0).trim();
        
        if (result == 4)
            scanFile(decryptedLinks, pass, burl, name, name);
        if (result == 2)
            scanDirectory(decryptedLinks, pass, burl, name, progress);
        
        return decryptedLinks;
    }

}
