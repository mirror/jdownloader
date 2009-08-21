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
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import javax.imageio.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;

//Captcha recognition by external coder (JTB)
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rom-news.org" }, urls = { "http://[\\w\\.]*?download\\.rom-news\\.org/[\\w]+" }, flags = { 0 })
public class RmNwsrg extends PluginForDecrypt {

    private BufferedImage pic;
    private int[][] selected = new int[20001][2];
    private boolean smiley = false;
    private Color color; 
    private final Color colorsmiley = new Color(50, 50, 50);
    private int max_x = 0;
    private int max_y = 0;
    private int min_x = 300;
    private int min_y = 100;
    
    public RmNwsrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);
        if (br.getRedirectLocation() != null) {
            decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));
            return decryptedLinks;
        }
        for (int i = 1; i <= 5; i++) {
            File file = this.getLocalCaptchaFile();
            Regex colorhex = br.getRegex(br.getRegex("<h2>.*?class=\"(.*?)\".*?</h2>").getMatch(0)+" . color: .(..)(..)(..); .");
            Color color = new Color(Integer.parseInt(colorhex.getMatch(0), 16),Integer.parseInt(colorhex.getMatch(1), 16),Integer.parseInt(colorhex.getMatch(2), 16));
            String cap = br.getRegex("\"image\" src=\"(.*?png.*?)\"").getMatch(0);
            Form form = br.getForm(0);
            Browser.download(file, br.cloneBrowser().openGetConnection(cap));
            int[] p = Point_romnewsorg(file, color);
            if (p == null) throw new DecrypterException(DecrypterException.CAPTCHA);
            form.remove("x");
            form.remove("y");
            form.put("name.x", p[0] + "");
            form.put("name.y", p[1] + "");
            br.submitForm(form);
            if (br.getRedirectLocation() != null) break;
        }

        decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));

        return decryptedLinks;
    }

    public int[] Point_romnewsorg(File file, Color color) {
        try {
            this.pic = ImageIO.read(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int r, g, b;
        if (color.getRed() > 205)
            r = 205;
        else if (color.getRed() < 50)
            r = 50;
        else
            r = color.getRed();
        if (color.getGreen() > 205)
            g = 205;
        else if (color.getGreen() < 50)
            g = 50;
        else
            g = color.getGreen();
        if (color.getBlue() > 205)
            b = 205;
        else if (color.getBlue() < 50)
            b= 50;
        else
            b = color.getBlue();
        this.color = new Color(r, g, b);
        this.selected[0][0] = 1;
        int x = 3;
        int y = 3;
        int[] result = new int[2];
        while(y+3 < this.pic.getHeight()){
            if (result[0] == 0 && colcom(new Color(this.pic.getRGB(x, y)),this.color) && colcom(new Color(this.pic.getRGB(x, y-3)),this.color) && colcom(new Color(this.pic.getRGB(x, y+3)),this.color) && colcom(new Color(this.pic.getRGB(x-3, y)),this.color) && colcom(new Color(this.pic.getRGB(x+3, y)),this.color)){
                select(x, y);
                if (smiley == true){
                    result[0] = (this.max_x-this.min_x)/2+this.min_x;
                    result[1] = (this.max_y-this.min_y)/2+this.min_y;
                } else {
                    max_x = 0;
                    max_y = 0;
                    min_x = 300;
                    min_y = 100;
                }
            }
            
            x++;
            if (x+4 >= this.pic.getWidth()){
                x=0;
                y++;
            }
        }
        return result;
    }
    
    public void select(int x, int y) {
        boolean vorhanden = false;
        for(int i = 1; i < this.selected.length; i++){
            if (this.selected[i][0] == x && this.selected[i][1] == y)
                vorhanden = true;
        }
        if (vorhanden == false){
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
            
            if(colcom(new Color(this.pic.getRGB(x+1, y)),this.color))
                select(x+1, y);
            else if(colcom(new Color(this.pic.getRGB(x+1, y)),this.colorsmiley) && this.smiley == false)
                    this.smiley = true;
            if(colcom(new Color(this.pic.getRGB(x, y+1)),this.color))
                select(x, y+1);
            else if(colcom(new Color(this.pic.getRGB(x, y+1)),this.colorsmiley) && this.smiley == false)
                    this.smiley = true;
            if(colcom(new Color(this.pic.getRGB(x-1, y)),this.color))
                select(x-1, y);
            else if(colcom(new Color(this.pic.getRGB(x-1, y)),this.colorsmiley) && this.smiley == false)
                    this.smiley = true;
            if(colcom(new Color(this.pic.getRGB(x, y-1)),this.color))
                select(x, y-1); 
            else if(colcom(new Color(this.pic.getRGB(x, y-1)),this.colorsmiley) && this.smiley == false)
                    this.smiley = true;
        }
    }
    
    public static boolean colcom(Color color1, Color color2) { // colcom = color-compare
        int unterschied = 50;
        if((color1.getRed()+unterschied > color2.getRed() && color1.getRed()-unterschied < color2.getRed()) &&
            (color1.getBlue()+unterschied > color2.getBlue() && color1.getBlue()-unterschied < color2.getBlue()) &&
            (color1.getGreen()+unterschied > color2.getGreen() && color1.getGreen()-unterschied < color2.getGreen()))
            return true;
        else
            return false;
    }
    
    // @Override

}
