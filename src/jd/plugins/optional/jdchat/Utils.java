package jd.plugins.optional.jdchat;

import java.awt.Color;

import jd.utils.HTMLEntities;

public class Utils {
    public static String getRandomColor() {
        
        String col = Integer.toHexString((int)new Color((int) (Math.random() * 0xffffff)).darker().getRGB());
        while (col.length() < 6)
            col = "0" + col;
        return col.substring(col.length()-6);
    }
    public static String prepareMsg(String msg){
        msg=HTMLEntities.htmlAngleBrackets(msg);
        return msg.replaceAll("((http://)|(www\\.))([^\\s\"]+)", "<a href=\"http://$3$4\">$3$4</a>");
       
    }
}
