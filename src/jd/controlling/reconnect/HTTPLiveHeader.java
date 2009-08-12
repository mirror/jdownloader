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

package jd.controlling.reconnect;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;

import jd.config.Configuration;
import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.http.JDProxy;
import jd.http.RequestHeader;
import jd.nutils.Formatter;
import jd.nutils.JDHash;
import jd.nutils.io.JDIO;
import jd.parser.Regex;
import jd.utils.CLRLoader;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



/**
 * Diese Klasse kann mehrere HTTPrequests durchführen. Um damit einen reconnect
 * zu simulieren
 */
public class HTTPLiveHeader extends ReconnectMethod {

    private Configuration configuration;

    private HashMap<String, String> headerProperties;

    private HashMap<String, String> variables;

    public HTTPLiveHeader() {
        configuration = JDUtilities.getConfiguration();
    }

    // @Override
    protected boolean runCommands(ProgressController progress) {
        String script;
        if (configuration.getIntegerProperty(ReconnectMethod.PARAM_RECONNECT_TYPE, ReconnectMethod.LIVEHEADER) == ReconnectMethod.CLR) {
            /* konvertiert CLR zu Liveheader */
            String[] ret = CLRLoader.createLiveHeader(configuration.getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS_CLR));
            if (ret != null) {
                script = ret[1];
            } else {
                script = null;
            }
        } else {
            script = configuration.getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS);
        }
        String user = configuration.getStringProperty(Configuration.PARAM_HTTPSEND_USER);
        String pass = configuration.getStringProperty(Configuration.PARAM_HTTPSEND_PASS);
        String ip = configuration.getStringProperty(Configuration.PARAM_HTTPSEND_IP);

        if (script == null || script.length() == 0) {
            progress.doFinalize();
            logger.severe("No LiveHeader Script found");
            return false;
        }

        // script = script.replaceAll("\\<", "&lt;");
        // script = script.replaceAll("\\>", "&gt;");
        script = script.replaceAll("\\[\\[\\[", "<");
        script = script.replaceAll("\\]\\]\\]", ">");
        script = script.replaceAll("<REQUEST(.*?)>", "<REQUEST$1><![CDATA[");
        script = script.replaceAll("</REQUEST>", "]]></REQUEST>");
        script = script.replaceAll("<RESPONSE(.*?)>", "<RESPONSE$1><![CDATA[");
        script = script.replaceAll("</RESPONSE.*>", "]]></RESPONSE>");
        Document xmlScript;
        variables = new HashMap<String, String>();
        variables.put("user", user);
        variables.put("pass", pass);
        variables.put("basicauth", Encoding.Base64Encode(user + ":" + pass));
        variables.put("routerip", ip);
        headerProperties = new HashMap<String, String>();

        Browser br = new Browser();

        br.setProxy(JDProxy.NO_PROXY);
        if (user != null && pass != null) {
            br.setAuth(ip, user, pass);
        }
        try {
            xmlScript = JDUtilities.parseXmlString(script, false);
            if (xmlScript == null) {
                progress.doFinalize();
                logger.severe("Error while parsing the xml string: " + script);
                return false;
            }
            Node root = xmlScript.getChildNodes().item(0);
            if (root == null || !root.getNodeName().equalsIgnoreCase("HSRC")) {
                progress.doFinalize();
                logger.severe("Root Node must be [[[HSRC]]]*[/HSRC]");
                return false;
            }

            NodeList steps = root.getChildNodes();
            progress.addToMax(steps.getLength());
            for (int step = 0; step < steps.getLength(); step++) {
                progress.setStatusText(JDL.L("interaction.liveHeader.progress.3_step", "(STEP)HTTPLiveHeader :") + step);
                progress.increase(1);
                Node current = steps.item(step);

                if (current.getNodeType() == 3) continue;

                if (!current.getNodeName().equalsIgnoreCase("STEP")) {
                    progress.doFinalize();
                    logger.severe("Root Node should only contain [[[STEP]]]*[[[/STEP]]] ChildTag: " + current.getNodeName());
                    return false;
                }
                NodeList toDos = current.getChildNodes();
                for (int toDoStep = 0; toDoStep < toDos.getLength(); toDoStep++) {
                    Node toDo = toDos.item(toDoStep);

                    progress.setStatusText(JDL.LF("interaction.liveHeader.progress.4_step", "(%s)HTTPLiveHeader", toDo.getNodeName()));

                    if (toDo.getNodeName().equalsIgnoreCase("DEFINE")) {

                        NamedNodeMap attributes = toDo.getAttributes();
                        for (int attribute = 0; attribute < attributes.getLength(); attribute++) {
                            String key = attributes.item(attribute).getNodeName();
                            String value = attributes.item(attribute).getNodeValue();
                            String[] tmp = value.split("\\%\\%\\%(.*?)\\%\\%\\%");
                            String[] params = new Regex(value, "%%%(.*?)%%%").getColumn(-1);
                            if (params.length > 0) {
                                StringBuilder req;
                                if (value.startsWith(params[0])) {
                                    req = new StringBuilder();
                                    logger.finer("Variables: " + variables);
                                    logger.finer("Headerproperties: " + headerProperties);
                                    for (int i = 0; i <= tmp.length; i++) {
                                        logger.finer("Replace variable: ********(" + params[i - 1] + ")");

                                        req.append(getModifiedVariable(params[i - 1]));
                                        if (i < tmp.length) {
                                            req.append(tmp[i]);
                                        }
                                    }
                                } else {
                                    req = new StringBuilder(tmp[0]);
                                    logger.finer("Variables: " + variables);
                                    logger.finer("Headerproperties: " + headerProperties);
                                    for (int i = 1; i <= tmp.length; i++) {
                                        if (i > params.length) {
                                            continue;
                                        }
                                        logger.finer("Replace variable: *********(" + params[i - 1] + ")");
                                        req.append(getModifiedVariable(params[i - 1]));
                                        if (i < tmp.length) {
                                            req.append(tmp[i]);
                                        }
                                    }
                                }

                                value = req.toString();
                            }

                            variables.put(key, value);
                        }

                        logger.finer("Variables set: " + variables);
                    }

                    if (toDo.getNodeName().equalsIgnoreCase("PARSE")) {
                        String[] parseLines = HTTPLiveHeader.splitLines(toDo.getChildNodes().item(0).getNodeValue().trim());
                        for (String parseLine : parseLines) {
                            String varname = new Regex(parseLine, "(.*?):").getMatch(0);
                            String pattern = new Regex(parseLine, ".*?:(.+)").getMatch(0);
                            if (varname != null && pattern != null) {
                                varname = varname.trim();
                                pattern = pattern.trim();
                                String found = br.getRegex(pattern).getMatch(0);
                                if (found != null) {
                                    found = found.trim();
                                    logger.finer("Parse: Varname=" + varname + " Pattern=" + pattern + "->" + found);
                                    variables.put(varname, found);
                                } else {
                                    logger.finer("Parse: Varname=" + varname + " Pattern=" + pattern + "->NOT FOUND!");
                                }
                            }
                        }
                    }

                    if (toDo.getNodeName().equalsIgnoreCase("REQUEST")) {
                        boolean ishttps = false;
                        boolean israw = false;
                        if (toDo.getChildNodes().getLength() != 1) {
                            progress.doFinalize();
                            logger.severe("A REQUEST Tag is not allowed to have childTags.");
                            return false;
                        }
                        NamedNodeMap attributes = toDo.getAttributes();
                        if (attributes.getNamedItem("https") != null) {
                            ishttps = true;
                        }
                        if (attributes.getNamedItem("raw") != null) {
                            israw = true;
                        }
                        Browser retbr = null;
                        try {
                            retbr = doRequest(toDo.getChildNodes().item(0).getNodeValue().trim(), br, ishttps, israw);
                        } catch (Exception e2) {
                            retbr = null;
                        }
                        try {
                            /* DDoS Schutz */
                            Thread.sleep(150);
                        } catch (Exception e) {
                        }
                        if (retbr == null || !retbr.getHttpConnection().isOK()) {
                            logger.severe("Request error!");
                        } else {
                            br = retbr;
                        }

                    }
                    if (toDo.getNodeName().equalsIgnoreCase("RESPONSE")) {
                        logger.finer("get Response");
                        if (toDo.getChildNodes().getLength() != 1) {
                            progress.doFinalize();
                            logger.severe("A RESPONSE Tag is not allowed to have childTags.");
                            return false;
                        }

                        NamedNodeMap attributes = toDo.getAttributes();
                        if (attributes.getNamedItem("keys") == null) {
                            progress.doFinalize();
                            logger.severe("A RESPONSE Node needs a Keys Attribute: " + toDo);
                            return false;
                        }

                        String[] keys = attributes.getNamedItem("keys").getNodeValue().split("\\;");
                        getVariables(toDo.getChildNodes().item(0).getNodeValue().trim(), keys, br);

                    }
                    if (toDo.getNodeName().equalsIgnoreCase("WAIT")) {
                        NamedNodeMap attributes = toDo.getAttributes();
                        Node item = attributes.getNamedItem("seconds");
                        if (item == null) {
                            logger.severe("A Wait Step needs a Waittimeattribute: e.g.: <WAIT seconds=\"15\"/>");
                            return false;
                        }
                        logger.finer("Wait " + item.getNodeValue() + " seconds");
                        int seconds = Formatter.filterInt(item.getNodeValue());
                        Thread.sleep(seconds * 1000);
                    }
                }
            }
        } catch (Exception e) {
            JDLogger.exception(e);
            progress.doFinalize();
            logger.severe(e.getCause() + " : " + e.getMessage());
            return false;
        }

        return true;
    }

    private Browser doRequest(String request, Browser br, boolean ishttps, boolean israw) {
        try {
            String requestType;
            String path;
            StringBuilder post = new StringBuilder();
            String host = null;
            String http = "http://";
            if (ishttps) http = "https://";
            if (logger.isLoggable(Level.FINEST)) {
                br.forceDebug(true);
            } else {
                br.forceDebug(false);
            }
            HashMap<String, String> requestProperties = new HashMap<String, String>();
            if (israw) br.setHeaders(new RequestHeader());
            String[] tmp = request.split("\\%\\%\\%(.*?)\\%\\%\\%");
            // ArrayList<String> params =
            // SimpleMatches.getAllSimpleMatches(request,
            // "%%%°%%%", 1);
            String[] params = new Regex(request, "%%%(.*?)%%%").getColumn(0);
            if (params.length > 0) {
                StringBuilder req;
                if (request.startsWith(params[0])) {
                    req = new StringBuilder();
                    logger.finer("Variables: " + variables);
                    logger.finer("Headerproperties: " + headerProperties);
                    for (int i = 0; i <= tmp.length; i++) {
                        logger.finer("Replace variable: " + getModifiedVariable(params[i - 1]) + "(" + params[i - 1] + ")");
                        req.append(getModifiedVariable(params[i - 1]));
                        if (i < tmp.length) {
                            req.append(tmp[i]);
                        }
                    }
                } else {
                    req = new StringBuilder(tmp[0]);
                    logger.finer("Variables: " + variables);
                    logger.finer("Headerproperties: " + headerProperties);
                    for (int i = 1; i <= tmp.length; i++) {
                        if (i > params.length) {
                            continue;
                        }
                        logger.finer("Replace variable: " + getModifiedVariable(params[i - 1]) + "(" + params[i - 1] + ")");
                        req.append(getModifiedVariable(params[i - 1]));
                        if (i < tmp.length) {
                            req.append(tmp[i]);
                        }
                    }
                }

                request = req.toString();
            }
            String[] requestLines = HTTPLiveHeader.splitLines(request);
            if (requestLines.length == 0) {
                logger.severe("Parse Fehler:" + request);
                return null;
            }
            // RequestType
            tmp = requestLines[0].split(" ");
            if (tmp.length < 2) {
                logger.severe("Konnte Requesttyp nicht finden: " + requestLines[0]);
                return null;
            }
            requestType = tmp[0];
            path = tmp[1];
            boolean headersEnd = false;
            // Zerlege request

            for (int li = 1; li < requestLines.length; li++) {

                if (headersEnd) {
                    post.append(requestLines[li]);
                    post.append(new char[] { '\r', '\n' });
                    continue;
                }
                if (requestLines[li].trim().length() == 0) {
                    headersEnd = true;

                    continue;
                }
                String[] p = requestLines[li].split("\\:");
                if (p.length < 2) {
                    logger.warning("Syntax Fehler in: " + requestLines[li] + "\r\n Vermute Post Parameter");
                    headersEnd = true;
                    li--;
                    continue;
                }
                requestProperties.put(p[0].trim(), requestLines[li].substring(p[0].length() + 1).trim());

                if (p[0].trim().equalsIgnoreCase("HOST")) {
                    host = requestLines[li].substring(p[0].length() + 1).trim();
                }
            }

            if (host == null) {
                logger.severe("Host nicht gefunden: " + request);
                return null;
            }
            try {
                br.setConnectTimeout(5000);
                br.setReadTimeout(5000);
                if (requestProperties != null) {
                    br.getHeaders().putAll(requestProperties);
                }
                if (requestType.equalsIgnoreCase("GET")) {
                    br.getPage(http + host + path);
                } else if (requestType.equalsIgnoreCase("POST")) {
                    String poster = post.toString().trim();

                    br.postPageRaw(http + host + path, poster);
                } else if (requestType.equalsIgnoreCase("AUTH")) {
                    logger.finer("Convert AUTH->GET");
                    br.getPage(http + host + path);
                } else {
                    logger.severe("Unknown requesttyp: " + requestType);
                    return null;
                }
                return br;
            } catch (IOException e) {
                logger.severe("IO Error: " + e.getLocalizedMessage());
                JDLogger.exception(e);
                return null;
            }
        } catch (Exception e) {
            JDLogger.exception(e);
            return null;
        }

    }

    @SuppressWarnings("unchecked")
    public ArrayList<String[]> getLHScripts() {
        File[] list = new File(new File(JDUtilities.getJDHomeDirectoryFromEnvironment(), "jd"), "router").listFiles();
        ArrayList<String[]> ret = new ArrayList<String[]>();
        for (File element : list) {
            if (element.isFile() && element.getName().toLowerCase().matches(".*\\.xml$")) {
                ret.addAll((Collection<? extends String[]>) JDIO.loadObject(new JFrame(), element, true));
            }
        }

        return ret;
    }

    private static String[] splitLines(String source) {
        return source.split("\r\n|\r|\n");
    }

    private String getModifiedVariable(String key) {

        if (key.indexOf(":::") == -1 && headerProperties.containsKey(key)) { return headerProperties.get(key); }
        if (key.indexOf(":::") == -1) { return variables.get(key); }
        String ret = variables.get(key.substring(key.lastIndexOf(":::") + 3));
        if (headerProperties.containsKey(key.substring(key.lastIndexOf(":::") + 3))) {
            ret = headerProperties.get(key.substring(key.lastIndexOf(":::") + 3));
        }
        if (ret == null) return "";
        int id;
        String fnc;
        while ((id = key.indexOf(":::")) >= 0) {
            fnc = key.substring(0, id);
            key = key.substring(id + 3);

            if (fnc.equalsIgnoreCase("URLENCODE")) {
                ret = Encoding.urlEncode(ret);
            } else if (fnc.equalsIgnoreCase("URLDECODE")) {
                ret = Encoding.htmlDecode(ret);
            } else if (fnc.equalsIgnoreCase("UTF8DECODE")) {
                ret = Encoding.UTF8Decode(ret);
            } else if (fnc.equalsIgnoreCase("UTF8ENCODE")) {
                ret = Encoding.UTF8Encode(ret);
            } else if (fnc.equalsIgnoreCase("MD5")) {
                ret = JDHash.getMD5(ret);
            } else if (fnc.equalsIgnoreCase("BASE64")) {
                ret = Encoding.Base64Encode(ret);
            }
        }
        return ret;
    }

    private void getVariables(String patStr, String[] keys, Browser br) {
        logger.info("GetVariables");
        if (br == null) return;
        // patStr="<title>(.*?)</title>";
        logger.finer(patStr);
        Pattern pattern = Pattern.compile(patStr, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

        // logger.info(requestInfo.getHtmlCode());
        Matcher matcher = pattern.matcher(br + "");
        logger.info("Matches: " + matcher.groupCount());
        if (matcher.find() && matcher.groupCount() > 0) {
            for (int i = 0; i < keys.length && i < matcher.groupCount(); i++) {
                variables.put(keys[i], matcher.group(i + 1));
                logger.info("Set Variable: " + keys[i] + " = " + matcher.group(i + 1));
            }
        } else {
            logger.severe("Regular Expression without matches: " + patStr);
        }

    }

    // @Override
    public void initConfig() {
    }

    // @Override
    public String toString() {
        return JDL.L("interaction.liveHeader.name", "HTTP Live Header");
    }

}
