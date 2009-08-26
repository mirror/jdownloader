package jd.captcha.specials;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Captcha Recognition for gmd,uploadr
 * 
 * @author JTB
 */
public class GmdMscCm {
    
    private int max_x = 0;
    private int max_y = 0;
    private int min_x = 300;
    private int min_y = 100;
    private int[][] selected = new int[5001][2];
    private BufferedImage pic = null;
    private int pixel = 0;
    private File file = null;
    
    public GmdMscCm(File file) {
        this.file = file;
    }

    public int[] getResult() {
        try {
            pic = ImageIO.read(this.file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        int x = 3;
        int y = 3;
        this.selected[0][0] = 1;
        Color color;
        int[] result = {0, 0};
        
        while(y+3 < pic.getHeight()) {
            color = new Color(pic.getRGB(x, y));
            
            if (result[0] == 0 && colorCompare(color,new Color(0, 0, 0)) == false
                    && colorCompare(color, new Color(255, 255, 255)) == false
                    && ((colorCompare2(new Color(pic.getRGB(x, y - 1)), color)
                            || colorCompare2(new Color(pic.getRGB(x, y + 1)), color))
                    && (colorCompare2(new Color(pic.getRGB(x - 1, y)), color)
                            || colorCompare2(new Color(pic.getRGB(x + 1, y)), color)))) {
                
                select(x, y, color);
                
                if(this.pixel > 4) {
                    result[0] = (this.max_x - this.min_x) / 2 + this.min_x;
                    result[1] = (this.max_y - this.min_y) / 2 + this.min_y;
                } else {
                    this.pixel = 0;
                    this.max_x = 0;
                    this.min_x = 0;
                    this.max_y = 0;
                    this.min_y = 0;
                }
            }
            
            x++;
            
            if (x+4 >= pic.getWidth()) {
                x = 3;
                y++;
            }
        }
        return result;
    }

    private boolean colorCompare(Color color1,Color color2) {
      int unterschied = 75;
      
      if((color1.getRed() + unterschied) >= color2.getRed()
              && (color1.getRed() - unterschied) <= color2.getRed()
              && (color1.getGreen() + unterschied) >= color2.getGreen()
              && (color1.getGreen() - unterschied) <= color2.getGreen()
              && (color1.getBlue() + unterschied) >= color2.getBlue()
              && (color1.getBlue() - unterschied) <= color2.getBlue()) {
          return true;
      } else {
          return false;
      }  
    }
    
    private boolean colorCompare2(Color color1, Color color2) {
        if ((Color.RGBtoHSB(color1.getRed(), color1.getGreen(), color1.getBlue(), null)[0] == Color.RGBtoHSB(color2.getRed(), color2.getGreen(), color2.getBlue(), null)[0] || colorCompare(color1, color2))
                && !colorCompare(color1,new Color(255, 255, 255))
                && !colorCompare(color1,new Color(0, 0, 0))) {
            return true;
        } else {
            return false;
        }  
     }

    private void select(int x, int y, Color color) {
        boolean vorhanden = false;
        
        for(int i = 1; i < this.selected.length; i++){
            if (this.selected[i][0] == x && this.selected[i][1] == y) {
                vorhanden = true;
            }   
        }
        
        if (vorhanden == false) {
            this.pixel++;
            this.selected[this.selected[0][0]][0] = x;
            this.selected[this.selected[0][0]][1] = y;
            this.selected[0][0]++;
            
            if (x > this.max_x) {
                this.max_x = x;
            } else {
                if (x < this.min_x) {
                    this.min_x = x;
                }   
            }
            
            if (y > this.max_y) {
                this.max_y = y;
            } else {
                if (y < this.min_y) {
                    this.min_y = y; 
                }    
            }
            
            if(colorCompare2(new Color(this.pic.getRGB(x + 1, y)), color))
                select(x + 1, y, color);
            if(colorCompare(new Color(this.pic.getRGB(x, y + 1)), color))
                select(x, y + 1, color);
            if(colorCompare2(new Color(this.pic.getRGB(x - 1, y)), color))
                select(x - 1, y, color);
            if(colorCompare2(new Color(this.pic.getRGB(x, y -1 )), color))
                select(x, y - 1, color); 
        }
    }
}
