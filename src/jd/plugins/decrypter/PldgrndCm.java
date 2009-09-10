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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploadground.com" }, urls = { "http://[\\w\\.]*?uploadground\\.com/files/[0-9A-Z]+/" }, flags = { 0 })
public class PldgrndCm extends PluginForDecrypt {

    public PldgrndCm(PluginWrapper wrapper) {
      super(wrapper);
    }
    
    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
      ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
      String parameter = param.toString();
      String id = new Regex(parameter, "files/([0-9A-Z]+)").getMatch(0);
      parameter = "http://uploadground.com/status.php?uid=" + id;
      br.getPage(parameter);
      String[] redirectLinks = br.getRegex("download_td\"><a href=(.*?)target").getColumn(0);
      
      if (redirectLinks.length == 0) return null;
      for (String link : redirectLinks) {
          decryptedLinks.add(createDownloadlink(link));
      }
          
      return decryptedLinks;
    }
    
}