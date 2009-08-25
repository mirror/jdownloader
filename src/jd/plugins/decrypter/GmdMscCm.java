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

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gmd-music.com" }, urls = { "http://[\\w\\.]*?(gmd-music\\.com|uploadr\\.eu)/download/\\d+_[\\w-]+/" }, flags = { 0 })
public class GmdMscCm extends PluginForDecrypt {

  private String domain = null;
  
  /* attributes required by the anti captcha method*/
  private int max_x = 0;
  private int max_y = 0;
  private int min_x = 300;
  private int min_y = 100;
  private int[][] selected = new int[5001][2];
  private BufferedImage pic = null;
  private int pixel = 0;
    
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
    String pass = br.getRegex("Passwort:.*?<td align='left' width='50%'>(.*?)</td>").getMatch(0);   
    ArrayList<String> passwords = new ArrayList<String>();
    passwords.add("gmd-music.com");
    if (pass != null && !pass.equals("kein Passwort")) {
        passwords.add(pass);
    }
    for (String redlnk : redirectLinks) {
        br.getPage(this.domain + redlnk);
        handleCaptcha();
        String[] hostLinks = br.getRegex("<textarea name='links' rows='12' cols='104'>(http://.*?)</textarea>").getColumn(0);
        for (String hstlnk : hostLinks) {
          DownloadLink dl = createDownloadlink(hstlnk);
          dl.setSourcePluginPasswordList(passwords);
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
              int[] p = get_point(file);
              if (p == null) throw new DecrypterException(DecrypterException.CAPTCHA);
              captcha.put("button", "Send");
              captcha.put("button.x", p[0] + "");
              captcha.put("button.y", p[1] + "");
              br.submitForm(captcha);
          } else {
              valid = true;
              break;
          }
      }
      if (valid == false)
          throw new DecrypterException(DecrypterException.CAPTCHA);
  }
  
  public int[] get_point(File file) {
      try {
          pic = ImageIO.read(file);
      } catch (IOException e) {
          e.printStackTrace();
      }
      int x = 3;
      int y = 3;
      this.selected[0][0] = 1;
      Color color;
      int[] result = {0,0};
      while(y+3 < pic.getHeight()){
          color = new Color(pic.getRGB(x, y));
          if (result[0] == 0 && colcom(color,new Color(0,0,0)) == false && colcom(color,new Color(255,255,255))==false && ((colcom2(new Color(pic.getRGB(x, y-1)),color) || colcom2(new Color(pic.getRGB(x, y+1)),color)) && (colcom2(new Color(pic.getRGB(x-1, y)),color) || colcom2(new Color(pic.getRGB(x+1, y)),color)))){
              select(x, y, color);
              if(this.pixel > 4){
                  result[0] = (this.max_x-this.min_x)/2+this.min_x;
                  result[1] = (this.max_y-this.min_y)/2+this.min_y;
              } else {
                  this.pixel = 0;
                  this.max_x = 0;
                  this.min_x = 0;
                  this.max_y = 0;
                  this.min_y = 0;
              }
          }
          x++;
          if (x+4 >= pic.getWidth()){
              x=3;
              y++;
          }
      }
      return result;
  }

  public static boolean colcom(Color color1, Color color2) { // colcom = color-compare
    int unterschied = 75;
    if((color1.getRed()+unterschied >= color2.getRed() && color1.getRed()-unterschied <= color2.getRed()) &&
        (color1.getGreen()+unterschied >= color2.getGreen() && color1.getGreen()-unterschied <= color2.getGreen()) &&
        (color1.getBlue()+unterschied >= color2.getBlue() && color1.getBlue()-unterschied <= color2.getBlue()))
        return true;
    else
        return false;
  }
  
  public static boolean colcom2(Color color1, Color color2) {
      if ((Color.RGBtoHSB(color1.getRed(), color1.getGreen(), color1.getBlue(), null)[0] == Color.RGBtoHSB(color2.getRed(), color2.getGreen(), color2.getBlue(), null)[0] || colcom(color1, color2)) && colcom(color1,new Color(255,255,255))==false && colcom(color1,new Color(0,0,0))==false)
          return true;
      else
          return false;
   }

  public void select(int x, int y, Color color) {
      boolean vorhanden = false;
      for(int i = 1; i < this.selected.length; i++){
          if (this.selected[i][0] == x && this.selected[i][1] == y)
              vorhanden = true;
      }
      
      if (vorhanden == false) {
          this.pixel++;
          this.selected[this.selected[0][0]][0] = x;
          this.selected[this.selected[0][0]][1] = y;
          this.selected[0][0]++;
          
          if (x > this.max_x)
              this.max_x = x;
          else{
              if (x < this.min_x)
                  this.min_x = x;
          }
          if (y > this.max_y)
              this.max_y = y;
          else{
              if (y < this.min_y)
                  this.min_y = y;
          }
          
          if(colcom2(new Color(this.pic.getRGB(x+1, y)),color))
              select(x+1, y, color);
          if(colcom2(new Color(this.pic.getRGB(x, y+1)),color))
              select(x, y+1, color);
          if(colcom2(new Color(this.pic.getRGB(x-1, y)),color))
              select(x-1, y, color);
          if(colcom2(new Color(this.pic.getRGB(x, y-1)),color))
              select(x, y-1, color); 
      }
  }
  
}