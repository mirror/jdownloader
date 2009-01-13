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

package jd.captcha;

import java.io.File;
import java.util.logging.Logger;

import jd.captcha.utils.UTILITIES;
import jd.utils.JDUtilities;

/**
 * Jac Training
 * 
 * @author JD-Team
 */
public class JACTestrun {
    /**
     * @param args
     */
    public static void main(String args[]) {

        JACTestrun main = new JACTestrun();
        main.go();
    }

    @SuppressWarnings("unused")
    private Logger logger = UTILITIES.getLogger();

    private void go() {

        @SuppressWarnings("unused")
        String methodsPath = UTILITIES.getFullPath(new String[] { JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath(), "jd", "captcha", "methods" });
        JAntiCaptcha.testMethod(new File("C:\\Users\\coalado\\.jd_home\\jd\\captcha\\methods\\share-online.biz/"));

        // File[] methods= JAntiCaptcha.getMethods(methodsPath);
        // logger.info("Found "+methods.length+" Methods");
        // JAntiCaptcha.testMethods(methods);

    }
}