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

import java.util.logging.Logger;

import jd.captcha.utils.UTILITIES;
import jd.update.WebUpdater;









/**
 * JAC Updater

 * 
 * @author JD-Team
 */
public class JACUpdater {
    private Logger logger = UTILITIES.getLogger();
    /**
     * @param args
     */
    public static void main(String args[]){
  
        JACUpdater main = new JACUpdater();
        main.go();
    }
    private void go(){ 
        
//        JAntiCaptcha.updateMethods();
        WebUpdater web= new WebUpdater("http://lagcity.de/~JDownloaderFiles/autoUpdate");
        logger.info("New files: "+web.getUpdateNum());

   
    }
}