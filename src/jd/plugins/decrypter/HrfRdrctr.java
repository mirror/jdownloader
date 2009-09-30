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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "forex-fileupload.co.cc", "r1one.co.cc" }, urls = { "http://[\\w\\.]*?forex-fileupload\\.co\\.cc/\\?\\w+", "http://[\\w\\.]*?r1one\\.co\\.cc/\\d+" }, flags = { 0, 0 })
public class HrfRdrctr extends PluginForDecrypt {

  String[] regxps = { 
          "<a href=\"(.*?)\"><img src=\".*?dwn_btn.gif\"></a>",
          "<a href=\"(.*?)\"><img src=\"http://r1one\\.co\\.cc/aa\\.png\"></a>"
  };

  public HrfRdrctr(PluginWrapper wrapper) {
      super(wrapper);
  }

  // @Override
  public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
      ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
      String link = null;
      String parameter = param.toString();
      br.getPage(parameter);
     
      for (int i = 0; i < regxps.length; i++) {
          if (link == null) {
              link = br.getRegex(regxps[i]).getMatch(0);
          } else {
              break;
          }
      }

      if (link == null) return null;

      decryptedLinks.add(createDownloadlink(link));

      return decryptedLinks;
  }

  // @Override

}