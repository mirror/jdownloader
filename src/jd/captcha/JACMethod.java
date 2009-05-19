//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.io.FileFilter;
import java.util.Collections;
import java.util.Vector;
import java.util.logging.Logger;

import jd.controlling.JDLogger;
import jd.nutils.OSDetector;
import jd.nutils.io.JDIO;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class JACMethod implements Comparable<JACMethod> {

    private static Logger logger = JDLogger.getLogger();

    private static Vector<JACMethod> methods = null;

    public static String forServiceName(String service) {
        for (JACMethod method : getMethods()) {
            if (service.equalsIgnoreCase(method.getServiceName())) {
                logger.info("Found JAC method for the service " + service + " in directory " + method.getFileName());
                return method.getFileName();
            }
        }
        logger.info("There is no JAC method for the service " + service);
        return service;
    }

    public static Vector<JACMethod> getMethods() {
        if (methods != null) return methods;
        Vector<JACMethod> methods = new Vector<JACMethod>();
        for (File methodDir : getMethodDirs()) {
            methods.addAll(parseJACInfo(methodDir));
        }
        Collections.sort(methods);
        JACMethod.methods = methods;
        return methods;
    }

    private static File[] getMethodDirs() {
        File dir = JDUtilities.getResourceFile(JDUtilities.getJACMethodsDirectory());

        if (dir == null || !dir.exists()) {
            logger.severe("Konnte Methodenordner nicht finden: " + dir);
            return new File[0];
        }

        File[] entries = dir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                if (!pathname.isDirectory()) return false;
                File method = new File(pathname.getAbsoluteFile() + "/jacinfo.xml");
                return (method.exists() && isAvailableExternMethod(method));
            }
        });
        return entries;
    }

    private static Vector<JACMethod> parseJACInfo(File dir) {
        String filecontent = JDIO.getLocalFile(new File(dir.getAbsolutePath() + "/jacinfo.xml"));
        Document doc = JDUtilities.parseXmlString(filecontent, false);
        if (doc == null) return null;

        String[] services = null;
        String author = null;

        NodeList nl = doc.getFirstChild().getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node childNode = nl.item(i);

            if (childNode.getNodeName().equals("method")) {
                String servicelist = JDUtilities.getAttribute(childNode, "services");
                if (servicelist != null) {
                    services = servicelist.split(";");
                } else {
                    services = new String[] { JDUtilities.getAttribute(childNode, "name") };
                }

                String extern = JDUtilities.getAttribute(childNode, "type");
                if (extern != null && extern.equalsIgnoreCase("extern")) {
                    author = JDLocale.L("gui.config.jac.extern", "Externe Methode");
                } else {
                    author = JDUtilities.getAttribute(childNode, "author");
                }

                break;
            }
        }

        Vector<JACMethod> methods = new Vector<JACMethod>();
        for (String service : services) {
            methods.add(new JACMethod(dir.getName(), service, author));
        }
        return methods;
    }

    public static boolean hasMethod(String service) {
        String methodName = forServiceName(service);
        File method = JDUtilities.getResourceFile(JDUtilities.getJACMethodsDirectory() + "/" + methodName + "/jacinfo.xml");
        return (method.exists() && isAvailableExternMethod(method));
    }

    private static boolean isAvailableExternMethod(File jacinfo) {
        return isAvailableExternMethod(JDIO.getLocalFile(jacinfo));
    }

    private static boolean isAvailableExternMethod(String content) {
        if (content != null && content.contains("extern")) {
            if (OSDetector.isLinux() && !content.contains("linux")) {
                return false;
            } else if (OSDetector.isMac() && !content.contains("mac")) {
                return false;
            } else if (OSDetector.isWindows() && !content.contains("windows")) { return false; }
        }
        return true;
    }

    private String filename;

    private String servicename;

    private String author;

    public JACMethod(String filename, String servicename, String author) {
        this.filename = filename;
        this.servicename = servicename;
        this.author = author;
    }

    public String getFileName() {
        return filename;
    }

    public String getServiceName() {
        return servicename;
    }

    public String getAuthor() {
        return author;
    }

    public int compareTo(JACMethod o) {
        return (servicename + " " + filename).compareToIgnoreCase(o.getServiceName() + " " + o.getFileName());
    }

}
