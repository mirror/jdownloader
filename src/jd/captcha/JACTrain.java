
//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://wnu.org/licenses/>.


package jd.captcha;



import java.util.logging.Logger;

import jd.captcha.utils.UTILITIES;
import jd.utils.JDUtilities;

/**
 * Jac Training
 *

 * 
 * @author JD-Team
 */
public class JACTrain {
    private Logger logger = UTILITIES.getLogger();
    /**
     * @param args
     */
    public static void main(String args[]){
  
        JACTrain main = new JACTrain();
        main.go();
    }
    private void go(){

        String methodsPath=UTILITIES.getFullPath(new String[] { JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath(), "jd", "captcha", "methods"});
        
        String hoster="wii-reloaded.ath.cx";

        JAntiCaptcha jac= new JAntiCaptcha(methodsPath,hoster);
        //jac.runTestMode(new File("1186941165349_captcha.jpg"));
     jac.displayLibrary();
       // jac.setShowDebugGui(true);
      // jac.showPreparedCaptcha(new File("/home/dwd/.jd_home/captchas/datenklo.net/08.01.2008_18.08.52.gif"));
      jac.trainAllCaptchas("C:\\Users\\coalado\\.jd_home\\jd\\captcha\\methods\\"+hoster+"\\captchas");
       // jac.saveMTHFile();
        logger.info("Training Ende");
        //jac.addLetterMap();
           // jac.saveMTHFile();
    }
//    private static class FilterJAR implements FileFilter{
//        public boolean accept(File f) {
//            if(f.getName().endsWith(".jar"))
//                return true;
//            else
//                return false;
//        }
//    }
}