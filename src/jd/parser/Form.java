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

package jd.parser;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.http.Encoding;

public class Form {
    public static final int METHOD_FILEPOST = 3;

    public static final int METHOD_GET = 1;

    public static final int METHOD_POST = 0;

    public static final int METHOD_PUT = 2;

    public static final int METHOD_UNKNOWN = 99;

    /**
     * Ein Array mit allen Forms einer Seite
     */
    public static Form[] getForms(Object requestInfo) {
        return Form.getForms(requestInfo, ".*");
    }
public boolean equals(Form f){
    return this.toString().equalsIgnoreCase(f.toString());
}
    /**
     * Ein Array mit allen Forms dessen Inhalt dem matcher entspricht. Achtung
     * der Matcher bezieht sich nicht auf die Properties einer Form sondern auf
     * den Text der zwischen der Form steht. Dafür gibt es die formProperties
     */
    public static Form[] getForms(Object requestInfo, String matcher) {
        LinkedList<Form> forms = new LinkedList<Form>();

        Pattern pattern = Pattern.compile("<[\\s]*form(.*?)>(.*?)<[\\s]*/[\\s]*form[\\s]*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher formmatcher = pattern.matcher(requestInfo.toString().replaceAll("(?s)<!--.*?-->", ""));
        while (formmatcher.find()) {
            String formPropertie = formmatcher.group(1);
            String inForm = formmatcher.group(2);
            // System.out.println(inForm);
            if (inForm.matches("(?s)" + matcher)) {
                Form form = new Form();
                // form.baseRequest = requestInfo;
                form.method = METHOD_GET;
                Pattern patternfp = Pattern.compile(" ([^\\s]+)\\=[\"'](.*?)[\"']", Pattern.CASE_INSENSITIVE);
                Matcher matcherfp = patternfp.matcher(formPropertie);
                while (matcherfp.find()) {
                    String pname = matcherfp.group(1);
                    if (pname.toLowerCase().equals("action")) {
                        form.action = matcherfp.group(2);
                    } else if (pname.toLowerCase().equals("method")) {
                        String meth = matcherfp.group(2).toLowerCase();
                        if (meth.matches(".*post.*")) {
                            form.method = METHOD_POST;
                        } else if (meth.matches(".*get.*")) {
                            form.method = METHOD_GET;
                        } else if (meth.matches(".*put.*")) {
                            form.method = METHOD_PUT;
                        } else {
                            form.method = METHOD_UNKNOWN;
                        }
                    } else {
                        form.formProperties.put(pname, matcherfp.group(2));
                    }
                }
                patternfp = Pattern.compile(" ([^\\s]+)\\=([^\"'][^\\s>]*)", Pattern.CASE_INSENSITIVE);
                matcherfp = patternfp.matcher(formPropertie);
                while (matcherfp.find()) {
                    String pname = matcherfp.group(1);
                    if (pname.toLowerCase().equals("action")) {
                        form.action = matcherfp.group(2);
                    } else if (pname.toLowerCase().equals("method")) {
                        String meth = matcherfp.group(2).toLowerCase();
                        if (meth.matches(".*post.*")) {
                            form.method = METHOD_POST;
                        } else if (meth.matches(".*get.*")) {
                            form.method = METHOD_GET;
                        } else if (meth.matches(".*put.*")) {
                            form.method = METHOD_PUT;
                        } else {
                            form.method = METHOD_UNKNOWN;
                        }
                    } else {
                        form.formProperties.put(pname, matcherfp.group(2));
                    }
                }
                // if (form.action == null) {
                // form.action =
                // requestInfo.getConnection().getURL().toString();
                // }
                form.vars.putAll(form.getInputFields(inForm));
                forms.add(form);
            }
        }
        return forms.toArray(new Form[forms.size()]);
    }

    /**
     * Action der Form entspricht auch oft einer URL
     */
    public String action;

    /**
     * Fals es eine Uploadform ist, kann man hier die Dateien setzen die
     * hochgeladen werden sollen
     */
    private File fileToPost = null;

    private String filetoPostName = null;

    /**
     * Die eigenschaften der Form z.B. id oder name (ohne method und action)
     * kann zur Identifikation verwendet werden
     */
    public HashMap<String, String> formProperties = new HashMap<String, String>();

    /**
     * Methode der Form POST = 0, GET = 1 ( PUT = 2 wird jedoch bei
     * getRequestInfo nicht unterstützt ), FILEPOST = 3 (Ist eigentlich ein Post
     * da aber dateien Gesendet werden hab ich Filepost draus gemacht)
     */
    public int method;

    /**
     * Value und name von Inputs/Textareas/Selectoren HashMap<name, value>
     * Achtung müssen zum teil noch ausgefüllt werden
     */
    private HashMap<String, String> vars = new HashMap<String, String>();



    public String getAction(String baseURL) {
        URL baseurl = null;
        try {
            baseurl = new URL(baseURL);
        } catch (MalformedURLException e) {

            e.printStackTrace();
        }
        String ret = action;
        if (action == null || action.matches("[\\s]*")) {
            if (baseurl == null) { return null; }
            ret = baseurl.toString();
        } else if (!ret.matches("http://.*")) {
            if (baseurl == null) { return null; }
            if (ret.charAt(0) == '/') {
                ret = "http://" + baseurl.getHost() + ret;
            } else if (ret.charAt(0) == '&') {
                String base = baseurl.toString();
                if (base.matches("http://.*/.*")) {
                    ret = base + ret;
                } else {
                    ret = base + "/" + ret;
                }
            } else if (ret.charAt(0) == '?') {
                String base = baseurl.toString();
                if (base.matches("http://.*/.*")) {
                    ret = base.replaceFirst("\\?.*", "") + ret;
                } else {
                    ret = base + "/" + ret;
                }
            } else {
                String base = baseurl.toString();
                if (base.matches("http://.*/.*")) {
                    ret = base.substring(0, base.lastIndexOf("/")) + "/" + ret;
                } else {
                    ret = base + "/" + ret;
                }
            }
        }
        return ret;
    }

    // @SuppressWarnings("deprecation")
    // public HTTPConnection getConnection() {
    // if (method == METHOD_UNKNOWN) {
    // JDUtilities.getLogger().severe("Unknown method");
    // return null;
    // } else if (method == METHOD_PUT) {
    // JDUtilities.getLogger().severe("PUT is not Supported");
    // return null;
    // }
    // if (baseRequest == null) { return null; }
    // URL baseurl = baseRequest.getConnection().getURL();
    // if (action == null || action.matches("[\\s]*")) {
    // if (baseurl == null) { return null; }
    // action = baseurl.toString();
    // } else if (!action.matches("http://.*")) {
    // if (baseurl == null) { return null; }
    // if (action.charAt(0) == '/') {
    // action = "http://" + baseurl.getHost() + action;
    // } else if (action.charAt(0) == '&') {
    // String base = baseurl.toString();
    // if (base.matches("http://.*/.*")) {
    // action = base + action;
    // } else {
    // action = base + "/" + action;
    // }
    // } else if (action.charAt(0) == '?') {
    // String base = baseurl.toString();
    // if (base.matches("http://.*/.*")) {
    // action = base.replaceFirst("\\?.*", "") + action;
    // } else {
    // action = base + "/" + action;
    // }
    // } else {
    // String base = baseurl.toString();
    // if (base.matches("http://.*/.*")) {
    // action = base.substring(0, base.lastIndexOf("/")) + "/" + action;
    // } else {
    // action = base + "/" + action;
    // }
    // }
    // }
    // StringBuffer stbuffer = new StringBuffer();
    // boolean first = true;
    // for (Map.Entry<String, String> entry : vars.entrySet()) {
    // if (first) {
    // first = false;
    // } else {
    // stbuffer.append("&");
    // }
    // stbuffer.append(entry.getKey());
    // stbuffer.append("=");
    // stbuffer.append(Encoding.urlEncode(entry.getValue()));
    // }
    // String varString = stbuffer.toString();
    // if (method == METHOD_GET) {
    // if (varString != null && !varString.matches("[\\s]*")) {
    // if (action.matches(".*\\?.+")) {
    // action += "&";
    // } else if (action.matches("[^\\?]*")) {
    // action += "?";
    // }
    // action += varString;
    // }
    // try {
    // HTTPConnection HTTPConnection = new HTTPConnection(new
    // URL(action).openConnection());
    // HTTPConnection.setRequestProperty("Accept-Language",
    // Plugin.ACCEPT_LANGUAGE);
    // HTTPConnection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible;
    // MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
    // HTTPConnection.setRequestProperty("Cookie", baseRequest.getCookie());
    // for (Map.Entry<String, String> entry : requestPoperties.entrySet()) {
    // HTTPConnection.setRequestProperty(entry.getKey(), entry.getValue());
    // }
    // HTTPConnection.setRequestProperty("Referer", baseurl.toString());
    // return HTTPConnection;
    // } catch (MalformedURLException e) {
    //
    // e.printStackTrace();
    // } catch (IOException e) {
    //
    // e.printStackTrace();
    // }
    // } else if (method == METHOD_POST) {
    // try {
    // Logger logger = JDUtilities.getLogger();
    // HTTPConnection connection = new HTTPConnection(new
    // URL(action).openConnection());
    // connection.setRequestProperty("Accept-Language", Plugin.ACCEPT_LANGUAGE);
    // connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible;
    // MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
    // connection.setRequestProperty("Cookie", baseRequest.getCookie());
    // for (Map.Entry<String, String> entry : requestPoperties.entrySet()) {
    // connection.setRequestProperty(entry.getKey(), entry.getValue());
    // logger.info(entry.getKey() + " : " + entry.getValue());
    // }
    // connection.setRequestProperty("Referer", baseurl.toString());
    // connection.setDoOutput(true);
    // OutputStreamWriter wr = new
    // OutputStreamWriter(connection.getOutputStream());
    // wr.write(varString);
    // wr.flush();
    // wr.close();
    // return connection;
    // } catch (MalformedURLException e) {
    //
    // e.printStackTrace();
    // } catch (IOException e) {
    //
    // e.printStackTrace();
    // }
    // } else if (method == METHOD_FILEPOST) {
    // try {
    // // JOptionPane.showMessageDialog(null,
    // // "Dateiname:"+exsistingFileName );
    // String boundary = MultiPartFormOutputStream.createBoundary();
    // HTTPConnection urlConn = new
    // HTTPConnection(MultiPartFormOutputStream.createConnection(new
    // URL(action)));
    // urlConn.setRequestProperty("Accept", "*/*");
    // urlConn.setRequestProperty("Accept-Language", Plugin.ACCEPT_LANGUAGE);
    // urlConn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE
    // 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
    // urlConn.setRequestProperty("Cookie", baseRequest.getCookie());
    // for (Map.Entry<String, String> entry : requestPoperties.entrySet()) {
    // urlConn.setRequestProperty(entry.getKey(), entry.getValue());
    // }
    // urlConn.setRequestProperty("Referer", baseurl.toString());
    // urlConn.setRequestProperty("Content-Type",
    // MultiPartFormOutputStream.getContentType(boundary));
    // urlConn.setRequestProperty("Connection", "Keep-Alive");
    // urlConn.setRequestProperty("Cache-Control", "no-cache");
    // MultiPartFormOutputStream out = new
    // MultiPartFormOutputStream(urlConn.getOutputStream(), boundary);
    // for (Map.Entry<String, String> entry : vars.entrySet()) {
    // out.writeField(entry.getKey(), URLEncoder.encode(entry.getValue()));
    // }
    // out.writeFile(filetoPostName, null, fileToPost);
    // out.close();
    // return urlConn;
    // } catch (IOException e) {
    //
    // e.printStackTrace();
    // }
    //
    // }
    // return null;
    // }

    /**
     * Gibt alle Input fields zurück Object[0]=vars Object[1]=varsWithoutValue
     */
    private HashMap<String, String> getInputFields(String data) {
        HashMap<String, String> ret = new HashMap<String, String>();
        Matcher matcher = Pattern.compile("(?s)<[\\s]*(input|textarea|select)(.*?)>", Pattern.CASE_INSENSITIVE).matcher(data);
        while (matcher.find()) {
            String[] nv = getNameValue(matcher.group(2));
            if (nv != null) {
                if (!ret.containsKey(nv[0]) || ret.get(nv[0]).equals("")) {
                    ret.put(nv[0], (nv[1] == null ? "" : nv[1]));
                }
            }
        }
        return ret;
    }

    private String[] getNameValue(String data) {
        Matcher matcher = Pattern.compile("name=['\"]([^'\"]*?)['\"]", Pattern.CASE_INSENSITIVE).matcher(data);
        String key, value;
        key = value = null;
        if (matcher.find()) {
            key = matcher.group(1);
        } else {
            matcher = Pattern.compile("name=(.*)", Pattern.CASE_INSENSITIVE).matcher(data + " ");
            if (matcher.find()) {
                key = matcher.group(1).replaceAll(" [^\\s]+\\=.*", "").trim();
            }
        }
        if (key == null) {

            if (data.toLowerCase().matches(".*type=[\"']?file.*")) {
                method = METHOD_FILEPOST;
                setFiletoPostName("");
                return null;
            }
            return null;
        }

        matcher = Pattern.compile("value=['\"]([^'\"]*?)['\"]", Pattern.CASE_INSENSITIVE).matcher(data);
        if (matcher.find()) {
            value = matcher.group(1);
        } else {
            matcher = Pattern.compile("value=(.*)", Pattern.CASE_INSENSITIVE).matcher(data + " ");
            if (matcher.find()) {
                value = matcher.group(1).replaceAll(" [^\\s]+\\=.*", "").trim();
            }
        }
        if (value != null && value.matches("[\\s]*")) {
            value = null;
        }
        if (value == null && data.toLowerCase().matches(".*type=[\"']?file.*")) {
            method = METHOD_FILEPOST;
            setFiletoPostName(key);
            return null;
        }

        return new String[] { key, value };
    }

    public void put(String key, String value) {
        vars.put(key, value);
    }

    public void remove(String key) {
        vars.remove(key);
    }

    public String setVariable(int i, String value) {

        for (Iterator<String> it = vars.keySet().iterator(); it.hasNext();) {

            if (--i <= 0) {
                String key = it.next();
                vars.put(key, value);
                return key;
            }
        }
        return null;

    }

    public String toString() {
        String ret = "";
        ret += "Action: " + action + "\n";
        if (method == METHOD_POST) {
            ret += "Method: POST\n";
        } else if (method == METHOD_GET) {
            ret += "Method: GET\n";
        } else if (method == METHOD_PUT) {
            ret += "Method: PUT is not supported\n";
        } else if (method == METHOD_FILEPOST) {
            ret += "Method: FILEPOST\n";
            ret += "filetoPostName:" + getFiletoPostName() + "\n";
            if (getFileToPost() == null) {
                ret += "Warning: you have to set the fileToPost\n";
            }
        } else if (method == METHOD_UNKNOWN) {
            ret += "Method: Unknown\n";
        }
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            ret += "var: " + entry.getKey() + "=" + entry.getValue() + "\n";
        }
        for (Map.Entry<String, String> entry : formProperties.entrySet()) {
            ret += "formProperty: " + entry.getKey() + "=" + entry.getValue() + "\n";
        }

        return ret;
    }

    public String getPropertyString() {
        StringBuffer stbuffer = new StringBuffer();
        boolean first = true;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            if (first) {
                first = false;
            } else {
                stbuffer.append("&");
            }
            stbuffer.append(entry.getKey());
            stbuffer.append("=");
            stbuffer.append(Encoding.urlEncode(entry.getValue()));
        }
        return stbuffer.toString();

    }

    public HashMap<String, String> getVars() {
        return vars;
    }

    public void setVars(HashMap<String, String> vars) {
        this.vars = vars;
    }

    public void setFiletoPostName(String filetoPostName) {
        this.filetoPostName = filetoPostName;
    }

    public String getFiletoPostName() {
        return filetoPostName;
    }

    public void setFileToPost(File fileToPost) {
        this.fileToPost = fileToPost;
    }

    public File getFileToPost() {
        return fileToPost;
    }
}
