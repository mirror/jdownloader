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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "flameupload.com" }, urls = { "http://[\\w\\.]*?flameupload\\.com/files/[0-9A-Z]{8}/\\w+" }, flags = { 0 })
public class FlmpldCm extends PluginForDecrypt {

    public FlmpldCm(PluginWrapper wrapper) {
      super(wrapper);
    }
    
    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
      ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
      String parameter = param.toString();
      String id = new Regex(parameter, "files/([0-9A-Z]{8})").getMatch(0);
      parameter = "http://flameupload.com/status.php?uid=" + id;
      br.getPage(parameter);
      String[] redirectLinks = br.getRegex("<a href=(/redirect/[0-9A-Z]{8}/\\d+) target=_blank><img src=/images/click.gif border='0' /></a>").getColumn(0);
      
      if (redirectLinks.length == 0) return null;
      for (String link : redirectLinks) {
          br.getPage(link);
          String dllink = br.getRegex("<frame name=\"main\" src=\"(.*?)\">").getMatch(0);
          decryptedLinks.add(createDownloadlink(dllink));
      }
          
      return decryptedLinks;
    }
    
}