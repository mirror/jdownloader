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

package jd.utils;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import jd.gui.skins.simple.SimpleGUI;
import jd.http.Browser;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class CLRLoader {

    private static Vector<String> IDS;

    static Logger logger = JDUtilities.getLogger();

    public static String[] createLiveHeader(String CLR) {
        try {
            DocumentBuilderFactory factory;

            InputSource inSource;
            Document doc;

            factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringComments(true);

            factory.setValidating(false);
            inSource = new InputSource(new StringReader(CLR));

            doc = factory.newDocumentBuilder().parse(inSource);

            NodeList nodes = doc.getFirstChild().getChildNodes();
            String routerName = null;
            StringBuffer hlh = new StringBuffer();
            hlh.append("[[[HSRC]]]");
            hlh.append("\r\n");
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                short type = node.getNodeType();
                if (type != 1) {
                    continue;
                }
                // logger.info(node.getNodeName() + "");
                if (node.getNodeName().equalsIgnoreCase("router")) {
                    routerName = node.getAttributes().getNamedItem("name").getNodeValue().trim();
                } else if (node.getNodeName().equalsIgnoreCase("command")) {
                    hlh.append("    [[[STEP]]]" + "\r\n");
                    hlh.append("        [[[REQUEST]]]" + "\r\n");
                    String method = node.getAttributes().getNamedItem("method").getNodeValue().trim();
                    String action = node.getAttributes().getNamedItem("action").getNodeValue().trim();
                    String basicauth = null;
                    if (method.equalsIgnoreCase("post")) {
                        hlh.append("            " + method.toUpperCase() + " /" + action + " HTTP/1.1" + "\r\n");                        
                    } else if (method.equalsIgnoreCase("get")) {
                    } else if (method.equalsIgnoreCase("auth")) {
                        basicauth = action;
                    } else {
                        logger.severe("UNKNOWN METHOD: " + method);
                    }
                    NodeList params = node.getChildNodes();
                    HashMap<String, String> p = new HashMap<String, String>();
                    String post = "";
                    for (int ii = 0; ii < params.getLength(); ii++) {
                        Node param = params.item(ii);
                        try {
                            String key = param.getAttributes().getNamedItem("name").getNodeValue();
                            String value = param.getAttributes().getNamedItem("value").getNodeValue();
                            p.put(key, value);
                            if (post.equals("")) {
                                post += key + "=" + value;
                            } else {
                                post += "&" + key + "=" + value;
                            }
                        } catch (Exception ee) {
                            continue;
                        }

                    }

                    if (method.equalsIgnoreCase("post")) {
                        hlh.append("            Host: %%%routerip%%%" + "\r\n");
                        CLRLoader.inputAuth(hlh, basicauth);                        
                        hlh.append("\r\n");
                        hlh.append(post.trim());
                        hlh.append("\r\n");
                    } else {
                        if (post.equals("")) {
                            hlh.append("            " + method.toUpperCase() + " /" + action + " HTTP/1.1" + "\r\n");
                        } else {
                            hlh.append("            " + method.toUpperCase() + " /" + action + "?" + post.trim() + " HTTP/1.1" + "\r\n");
                        }
                        hlh.append("            Host: %%%routerip%%%" + "\r\n");
                        CLRLoader.inputAuth(hlh, basicauth);
                    }
                    hlh.append("        [[[/REQUEST]]]" + "\r\n");
                    hlh.append("    [[[/STEP]]]" + "\r\n");
                } else {
                    logger.info("UNKNOWN  command: " + node.getNodeName());
                }
            }
            hlh.append("[[[/HSRC]]]");
            // logger.info(hlh.toString());
            return new String[] { routerName, hlh.toString() };
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    private static void inputAuth(StringBuffer hlh, String basicauth) {
        if (basicauth != null) {
            if (basicauth.equalsIgnoreCase("")) {
                hlh.append("            Authorization: Basic %%%basicauth%%%" + "\r\n");
            } else {
                logger.severe("UNKNOWN AUTH TYPE");
            }
        }

    }

    public static void log(String arg) {
        System.out.println(arg);
    }

    public static void main(String args[]) {
        IDS = new Vector<String>();
        Vector<String[]> res = new Vector<String[]>();

        try {
            Browser br = new Browser();
            br.getPage("http://cryptload.info/clr/");

            String[][] matches = br.getRegex(Pattern.compile("<option value=\"(.*?)\">(.*?)</option>", Pattern.CASE_INSENSITIVE)).getMatches();

            for (int i = matches.length - 1; i >= 0; i--) {
                String[] next = matches[i];
                if (IDS.contains(next[0])) {
                    continue;
                }
                br.postPage("http://cryptload.info/clrfile/", "clrid=" + next[0] + "&submit=myRouter.clr+herunterladen");

                String[] ret = CLRLoader.createLiveHeader(br.toString());
                if (ret != null) {
                    res.add(ret);
                    IDS.add(next[0]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        CLRLoader.saveTolist(res, new File("c:/clrList.xml"));

    }

    @SuppressWarnings("unchecked")
    private static void saveTolist(Vector<String[]> list, File file) {
        if (file.exists()) {
            list.addAll((Collection<? extends String[]>) JDUtilities.loadObject(((SimpleGUI) JDUtilities.getGUI()).getFrame(), file, true));
            Collections.sort(list, new Comparator<String[]>() {
                public int compare(String[] a, String[] b) {
                    return (a[0] + " " + a[1]).compareToIgnoreCase(b[0] + " " + b[1]);
                }
            });

        }
        JDUtilities.saveObject(null, list, file, null, null, true);
    }

    public CLRLoader() {
    }
}
