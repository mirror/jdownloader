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
import java.util.ArrayList;
import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gmd-music.com" }, urls = { "http://[\\w\\.]*?(gmd-music\\.com|uploadr\\.eu)/download/\\d+_[\\w-]+/" }, flags = { 0 })
public class GmdMscCm extends PluginForDecrypt {

  private String domain = null;
    
  public GmdMscCm(PluginWrapper wrapper) {
    super(wrapper);
  }
  
  // @Override
  public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
    ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
    String parameter = param.toString();
    
    if (parameter.matches(".*?gmd-music\\.com.*?")) domain = "http://gmd-music.com/";
    else if (parameter.matches(".*?uploadr\\.eu.*?")) domain = "http://uploadr.eu/";
    
    br.getPage(parameter);
    String[] redirectLinks = br.getRegex("onclick='self.window.location = \"(redirect/\\d+_[\\w-]+/)\";").getColumn(0);
    if (redirectLinks.length == 0) return null;
    FilePackage fp = FilePackage.getInstance();
    String password = br.getRegex("Passwort:.*?<td align='left' width='50%'>(.*?)</td>").getMatch(0);
    String name = br.getRegex("<div class='paneltitle'>.*?<h1>(.*?)</h1>").getMatch(0);
    fp.setPassword(password);
    fp.setName(name);
    
    for (String redlnk : redirectLinks) {
        br.getPage(this.domain + redlnk);
        handleCaptcha();
        String[] hostLinks = br.getRegex("<textarea name='links' rows='12' cols='104'>(http://.*?)</textarea>").getColumn(0);
        for (String hstlnk : hostLinks) {
          DownloadLink dl = createDownloadlink(hstlnk);
          dl.setFilePackage(fp);
          decryptedLinks.add(dl); 
        }
        
    } 
    return decryptedLinks;
  }
  
  public void handleCaptcha() throws Exception {
      boolean valid = true;
      for (int i = 0; i < 5; ++i) {
          if (br.containsHTML("Klicken Sie auf den ge&ouml;ffneten Kreis!")) {
              Form captcha = br.getFormbyProperty("name", "captcha");
              captcha.setAction(this.domain + captcha.getAction());
              valid = false;
              File file = this.getLocalCaptchaFile();
              String url = this.domain + captcha.getRegex("input type='image' src='(.*?)'").getMatch(0);
              Browser.download(file, br.cloneBrowser().openGetConnection(url));
              Point p = UserIO.getInstance().requestClickPositionDialog(file, JDL.L("plugins.decrypt.gmd-music.captcha.title", "Captcha"), JDL.L("plugins.decrypt.gmd-music.captcha", "Please click on the Circle with a gap"));
              if (p == null) throw new DecrypterException(DecrypterException.CAPTCHA);
              captcha.put("button", "Send");
              captcha.put("button.x", p.x + "");
              captcha.put("button.y", p.y + "");
              br.submitForm(captcha);
          } else {
              valid = true;
              break;
          }
      }
      if (valid == false)
          throw new DecrypterException(DecrypterException.CAPTCHA);
  }
  
}