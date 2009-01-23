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

package jd.parser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.http.Browser;
import org.lobobrowser.html.io.WritableLineReader;
import org.lobobrowser.html.js.Executor;
import org.lobobrowser.html.test.SimpleUserAgentContext;
import org.lobobrowser.js.JavaFunctionObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.xml.sax.SAXException;

import jd.http.Encoding;

import jd.http.Cookie;

public final class JavaScript {
    public String javaScript;
    public Browser br;
    private ArrayList<String> executed = new ArrayList<String>();
    private WritableLineReader wis;
    public boolean debug = false;
    private Context cx;
    private Scriptable scope;
    private Document d;
    public JavaScript( Browser br) {
        this.br = br;
    }

    public String callFunction(String functionName) throws SAXException, IOException {
        return callFunction(functionName, null);
    }

    public String callFunction(String functionName, String[] parameters) throws SAXException, IOException {
        runPage();
        StringBuilder parameter = new StringBuilder();
        if (parameters != null) {
            if (parameters.length > 0) {
                parameter.append('\'');
                parameter.append(parameters[0]);
                parameter.append('\'');
                for (int i = 1; i < parameters.length; i++) {
                    parameter.append(new char[] { ',', ' ', '\'' });
                    parameter.append(parameters[i]);
                    parameter.append('\'');
                }
            }
        }
        String fun = "function f(){ " + javaScript + "\nreturn " + functionName + "(" + parameter.toString() + ")} f()";
        Object result = cx.evaluateString(scope, fun, "<cmd>", 1, null);
        String ret = Context.toString(result);
        Context.exit();
        return ret;
    }

    private String parseJS(String js) {
        return js.replaceAll("document\\.all\\.(.*?)\\.", "document.getElementById('$1').");
    }

    private void runString(String data) throws IOException {
        {
            data = data.replaceAll("(?s)<!--.*?-->", "");
            String url = br.getURL().toString();
            String basename = "";
            String host = "";
            Pattern[] basePattern = new Pattern[] { Pattern.compile("(?s)<[ ]?base[^>]*?href='(.*?)'", Pattern.CASE_INSENSITIVE), Pattern.compile("(?s)<[ ]?base[^>]*?href=\"(.*?)\"", Pattern.CASE_INSENSITIVE), Pattern.compile("(?s)<[ ]?base[^>]*?href=([^'\"][^\\s]*)", Pattern.CASE_INSENSITIVE), };
            Matcher m;
            for (Pattern element : basePattern) {
                m = element.matcher(data);
                if (m.find()) {
                    url = Encoding.htmlDecode(m.group(1));
                    break;
                }
            }
            if (url != null) {
                url = url.replace("http://", "");
                int dot = url.lastIndexOf('/');
                if (dot != -1) {
                    basename = url.substring(0, dot + 1);
                } else {
                    basename = "http://" + url + "/";
                }
                dot = url.indexOf('/');
                if (dot != -1) {
                    host = "http://" + url.substring(0, dot);
                } else {
                    host = "http://" + url;
                }
                url = "http://" + url;
            } else {
                url = "";
            }
            String[][] reg = new Regex(data, "<[ ]?script(.*?)(/>|>(.*?)<[ ]?/script>)").getMatches();
            // buff.append("var document[];\r\n");
            for (int i = 0; i < reg.length; i++) {
                if (reg[i][0].toLowerCase().contains("javascript")) {
                    if (reg[i].length == 3 && reg[i][2] != null && reg[i][2].length() > 0 && !executed.contains(reg[i][2])) {
                        try {
                           cx.evaluateString(scope, parseJS(reg[i][2]), "<cmd>", 1, null);
                        } catch (Exception e) {
                            if(debug)
                            {
                                e.printStackTrace();
                                System.err.println(reg[i][2]);
                            }
                        }

                        String data2 = d.getInnerHTML().replaceAll("(?s)<!--.*?-->", "");
                        executed.add(reg[i][2]);
                        runString(d.content.toString());
                        // System.out.println(data2);
                        if (!data2.equals(data) && !d.content.toString().equals(data)) {
                            runString(data2);
                            return;
                        }
                    }
                    Pattern[] linkAndFormPattern = new Pattern[] { Pattern.compile(".*?src=\"(.*?)\"", Pattern.CASE_INSENSITIVE), Pattern.compile(".*?src='(.*?)'", Pattern.CASE_INSENSITIVE), Pattern.compile(".*?src=([^'\"][^\\s]*)", Pattern.CASE_INSENSITIVE) };
                    for (Pattern element : linkAndFormPattern) {
                        m = element.matcher(reg[i][0]);
                        while (m.find()) {
                            String link = Encoding.htmlDecode(m.group(1));
                            if (!(link.length() > 6 && link.matches("(?is)https?://.*")) && link.length() > 0) {
                                if (link.length() > 2 && link.substring(0, 3).equals("www")) {
                                    link = "http://" + link;
                                }
                                if (link.charAt(0) == '/') {
                                    link = host + link;
                                } else if (link.charAt(0) == '#') {
                                    link = url + link;
                                } else {
                                    link = basename + link;
                                }
                            }
                            if (!executed.contains(link)) {
                                // set.add(link);runString(String data, Context
                                // cx, Scriptable scope, HTMLDocumentImpl d) {
                                String page = br.cloneBrowser().getPage(link);
                                executed.add(link);
                                try {
                                    cx.evaluateString(scope, parseJS(page), "<cmd>", 1, null);
                                } catch (Exception e) {
                                    if(debug)
                                    {
                                        e.printStackTrace();
                                        System.err.println(link);
                                    }
                                }
                                String data2 = d.getInnerHTML().replaceAll("(?s)<!--.*?-->", "");

                                runString(d.content.toString());
                                // System.out.println(data2);
                                if (!data2.equals(data) && !d.content.toString().equals(data)) {
                                    runString(data2);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    private void runPage() throws SAXException, IOException {
        if(cx!=null)return;
        if (br == null) return;
        String data = br.toString();
//        Logger.getLogger(HTMLDocumentImpl.class.getName()).setLevel(Level.OFF);
        if(debug)
            Logger.getLogger(JavaFunctionObject.class.getName()).setLevel(Level.WARNING);
        else
            Logger.getLogger(JavaFunctionObject.class.getName()).setLevel(Level.OFF);
        ByteArrayInputStream ba = new ByteArrayInputStream(data.getBytes());
        SimpleUserAgentContext uacontext = new SimpleUserAgentContext();
        uacontext.setExternalCSSEnabled(false);
        uacontext.setScriptingEnabled(false);
        String host2 = br.getHost();

        if (host2.matches(".*\\..*\\..*")) host2 = host2.replaceFirst(".*?\\.", "");
        Set<Entry<String, Cookie>> cookies = br.getCookies().get(host2).entrySet();
        StringBuilder c = new StringBuilder();
        boolean b = false;
        for (Entry<String, Cookie> entry : cookies) {
            if (b == true) {
                c.append("; ");
            } else
                b = true;
            c.append(entry.getValue().getKey() + "=" + entry.getValue().getValue());
        }
        uacontext.setCookie(new URL(br.getURL()), c.toString());
        Reader reader = new InputStreamReader(ba);
        wis = new WritableLineReader(reader);
        
        d = new Document(uacontext, null, wis, br.getURL());
        d.load();
        d.setCookie(c.toString());
        cx = Executor.createContext(new URL(br.getURL()), uacontext);
        scope = (Scriptable) d.getUserData(Executor.SCOPE_KEY);
        if (scope == null) { throw new IllegalStateException("Scriptable (scope) instance was expected to be keyed as UserData to document using " + Executor.SCOPE_KEY); }
        // System.out.println(data);
        runString(d.getInnerHTML());
        // TODO document ersetzen
        // ret.replaceAll("document\\.([^\\s;=]*)", "");
    }
    public String getVar(String varname) throws SAXException, IOException
    {
        runPage();
        String result = (String) scope.get(varname, scope);
        return result;
    }
    public String runJavaScript() throws SAXException, IOException {

        runPage();
        Object result = cx.evaluateString(scope, javaScript, "<cmd>", 1, null);
        String ret = Context.toString(result);
        return ret;
    }

    public String toString() {
        try {
            runPage();
            return d.getInnerHTML();
        } catch (SAXException e) {
        } catch (IOException e) {
        }
        return null;
    }

    /**
     * TODO muss noch überarbeitet werden
     * 
     * @param args
     */
    public static void main(String[] args) {
        try {
            Browser b = new Browser();
            b.getPage("http://www.sendspace.com/file/ueknde");
            JavaScript js = b.getJavaScript();
            System.out.println(b.getJavaScript().getVar("link_dec"));
            System.out.println("----------");
            b.getPage("http://dwdhome.ath.cx/test.html");
            System.out.println(b);
            System.out.println("----------");
            js = b.getJavaScript();
            js.callFunction("Aendern");
            System.out.println(js);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
