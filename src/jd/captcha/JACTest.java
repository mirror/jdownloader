//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.captcha;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeSet;
import java.util.logging.Logger;

import jd.captcha.gui.BasicWindow;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.utils.UTILITIES;
import jd.plugins.Plugin;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

/**
 * JAC Tester
 * 
 * 
 * @author JD-Team
 */
public class JACTest {
    /**
     * @param args
     */
    public static void main(String args[]) {

        JACTest main = new JACTest();
        main.go();
    }

    private void go() {
        String methodsPath = UTILITIES.getFullPath(new String[] { JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath(), "jd", "captcha", "methods" });
        String hoster = "rapidshare.com";

        JAntiCaptcha jac = new JAntiCaptcha(methodsPath, hoster);
        // sharegullicom47210807182105.gif
//        Logger logger = JDUtilities.getLogger();
//        HashMap<Integer,Integer> avg= new  HashMap<Integer,Integer>();
//
//        avg.put(4, 0);
//        avg.put(5, 0);
//        avg.put(6, 0);
//        avg.put(7, 0);
//        HashMap<Integer,Integer> max= new  HashMap<Integer,Integer>();
//        
//        max.put(4, Integer.MAX_VALUE);
//        max.put(5, Integer.MAX_VALUE);
//        max.put(6, Integer.MAX_VALUE);
//        max.put(7, Integer.MAX_VALUE);
//        
//        HashMap<Integer,Integer> min= new  HashMap<Integer,Integer>();
//        
//        min.put(4, 0);
//        min.put(5, 0);
//        min.put(6, 0);
//        min.put(7, 0);
//        
//    HashMap<Integer,Integer> count= new  HashMap<Integer,Integer>();
//        
//    count.put(4, 0);
//    count.put(5, 0);
//    count.put(6, 0);
//    count.put(7, 0);
//    
//    
//    min: {4=163, 5=219, 6=244, 7=302}
//    avg: {4=217, 5=255, 6=291, 7=345}
//    max: {4=280, 5=369, 6=412, 7=491}
//    4:117
//    5:150
//    6:168
//    7:189
//        
//    int breaks=0;
//        int i = 1;
//        while (true) {
//            switch(i){
//            case 36:
//            case 31:
//                i++;
//                continue;
//                
//            
//            }
//          
//            RequestInfo ri;
//            try {
//                ri = Plugin.getRequest(new URL("http://scsoft.sc.funpic.de/reproduce.php?id=" + i));
//            String[] res = Plugin.getSimpleMatches(ri.getHtmlCode(), "onclick=\"myClicker(event);\" style=\"width:°; height:°; background-image:url('./captchas/°.jpg'); background-repeat:no-repeat;°<table><tr><th>Buchstaben:</th><td>°</td></tr>°<tr><th>Katzen:</th><td>°</td>");
//            logger.info(ri+"");
//            if(res==null||res[4]==null||res[6]==null||res[6].length()<4){
//                i++;
//                continue;
//            }
//            String url="http://scsoft.sc.funpic.de/captchas/"+res[2]+".jpg";
//            String code=res[4];
//            String cats=res[6];
//            File file = JDUtilities.getResourceFile("test/rscap/"+res[2]+"_"+code+"_"+cats+".jpg");
//            JDUtilities.downloadBinary(file.getAbsolutePath(), url);
//            Image captchaImage = UTILITIES.loadImage(file);
//            Captcha captcha = jac.createCaptcha(captchaImage);
// 
//            captcha.toBlackAndWhite();
//            int end=0;
//            all:
//           for(int x=captcha.getWidth()-1;x>=0;x--){
//               for(int y=0;y<captcha.getHeight();y++){
//                   if(captcha.grid[x][y]==0){
//                       captcha.grid[x][y]=0xff0000;
//                       end=x;
//                       break all;
//                   }
//               }
//               
//           }
//            count.put(cats.length(),count.get(cats.length())+1);
//            int c=count.get(cats.length());
//            if(end==385){
//                
//                BasicWindow.showImage(captcha.getImage(2), +i+"  "+code);
//            }
//            
//            if(c>5&&Math.abs(avg.get(cats.length())-end)>80){
//                i++;
//                breaks++;
//                logger.info("Breaks; "+breaks+"/"+i+"()"+((breaks*100)/i));
//               // BasicWindow.showImage(captcha.getImage(2), code);
//                continue;
//                
//            }
//            logger.info(avg.get(cats.length())+"* "+(c-1)+"+"+end+" = "+(avg.get(cats.length())*(c-1)+end)+"/"+c+"="+((avg.get(cats.length())*(c-1)+end)/c));
//                avg.put(cats.length(), (avg.get(cats.length())*(c-1)+end)/c);
//            
//                if(end<max.get(cats.length())){
//                    max.put(cats.length(),end);
//                   
//                }
//                
//                if(end>min.get(cats.length())){
//                    min.put(cats.length(),end);
//                   
//                }
//                logger.info("min: "+max);
//                logger.info("avg: "+avg);
//                logger.info("max: "+min);
//          //  BasicWindow.showImage(captcha.getImage(2), code);
//            } catch (MalformedURLException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            } catch (IOException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//
//            i++;
//           if(false) break;
//        }

       // jac.setShowDebugGui(true);
     //LetterComperator.CREATEINTERSECTIONLETTER = true;
        //
       //jac.exportDB();
        // UTILITIES.getLogger().info("has method:
        // "+JAntiCaptcha.hasMethod(methodsPath, hoster));

        //

     // jac.importDB();

      

   // jac.displayLibrary();

        // jac.getJas().set("preScanFilter", 0);
        // jac.trainCaptcha(new
        // File(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath()+"/jd/captcha/methods"+"/"+hoster+"/captchas/"+"securedin1730080724541.jpg"),
        // 4);
        jac.showPreparedCaptcha(new File("C:/Users/coalado/.jd_home/jd/captcha/methods/" + hoster + "/captchas/" + "captcha30_04_2008_23_40_13" + ".jpg"));

        // UTILITIES.getLogger().info(JAntiCaptcha.getCaptchaCode(UTILITIES.loadImage(new
        // File(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath()+"/jd/captcha/methods"+"/rapidshare.com/captchas/rapidsharecom24190807214810.jpg")),
        // null, "rapidshare.com"));
        // jac.removeBadLetters();
        // jac.addLetterMap();
        // jac.saveMTHFile();

    }
}