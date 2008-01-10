package jd.plugins;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jd.utils.JDUtilities;

public class Form {
    public static final int METHOD_POST = 0;
    public static final int METHOD_GET = 1;
    public static final int METHOD_PUT = 2;
    public static final int METHOD_UNKNOWN = 99;
    public boolean withHtmlCode = true;
    /**
     * Methode der Form POST = 0, GET = 1 ( PUT = 2 wird jedoch bei
     * getRequestInfo nicht unterstützt )
     */
    public int method;
    /**
     * Action der Form entspricht auch oft einer URL
     */
    public String action;
    /**
     * Die eigenschaften der Form z.B. id oder name (ohne method und action)
     * kann zur Identifikation verwendet werden
     */
    public HashMap<String, String> formProperties = new HashMap<String, String>();
    /**
     * Value und name von Inputs/Textareas/Selectoren HashMap<name, value>
     * Achtung müssen zum teil noch ausgefüllt werden
     */
    public HashMap<String, String> vars = new HashMap<String, String>();
    /**
     * Wird bei der Benutzung von getForms automatisch gesetzt
     */
    private RequestInfo baseRequest;
    private static String[] getNameValue(String data) {
        Matcher matcher = Pattern.compile("name=['\"]([^'\"]*?)['\"]",
                Pattern.CASE_INSENSITIVE).matcher(data);
        String key, value;
        key = value = null;
        if (matcher.find()) {
            key = matcher.group(1);
        } else {
            matcher = Pattern.compile("name=(.*)", Pattern.CASE_INSENSITIVE)
                    .matcher(data + " ");
            if (matcher.find())
                key = matcher.group(1).replaceAll(" [^\\s]+\\=.*", "").trim();
        }
        if (key == null) return null;
        matcher = Pattern.compile("value=['\"]([^'\"]*?)['\"]",
                Pattern.CASE_INSENSITIVE).matcher(data);
        if (matcher.find())
            value = matcher.group(1);
        else {
            matcher = Pattern.compile("value=(.*)", Pattern.CASE_INSENSITIVE)
                    .matcher(data + " ");
            if (matcher.find())
                value = matcher.group(1).replaceAll(" [^\\s]+\\=.*", "").trim();
        }
        if (value != null && value.matches("[\\s]*")) value = null;
        return new String[] { key, value };
    }
    /**
     * Gibt alle Input fields zurück Object[0]=vars Object[1]=varsWithoutValue
     */
    private static HashMap<String, String> getInputFields(String data) {
        HashMap<String, String> ret = new HashMap<String, String>();
        Matcher matcher = Pattern.compile(
                "(?s)<[\\s]*(input|textarea|select)(.*?)>",
                Pattern.CASE_INSENSITIVE).matcher(data);
        while (matcher.find()) {
            String[] nv = getNameValue(matcher.group(2));
            if (nv != null) {
                if (!ret.containsKey(nv[0]) || ret.get(nv[0]).equals(""))
                    ret.put(nv[0], ((nv[1] == null) ? "" : nv[1]));
            }
        }
        return ret;
    }
    public static Form[] getForms(String url)
    {
        try {
            return getForms(new URL(url));
        }
        catch (MalformedURLException e) {
            // TODO Automatisch erstellter Catch-Block
            e.printStackTrace();
        }
        return null;
    }
    public static Form[] getForms(URL url)
    {
        try {
            return getForms(Plugin.getRequest(url));
        }
        catch (IOException e) {
            // TODO Automatisch erstellter Catch-Block
            e.printStackTrace();
        }
        return null;
    }
    /**
     * Ein Array mit allen Forms einer Seite
     */
    public static Form[] getForms(RequestInfo requestInfo) {
        return getForms(requestInfo, ".*");
    }
    /**
     * Ein Array mit allen Forms dessen Inhalt dem matcher entspricht. Achtung
     * der Matcher bezieht sich nicht auf die Properties einer Form sondern auf
     * den Text der zwischen der Form steht. Dafür gibt es die formProperties
     */
    public static Form[] getForms(RequestInfo requestInfo, String matcher) {
        ArrayList<Form> forms = new ArrayList<Form>();
        Pattern pattern = Pattern.compile(
                "<[\\s]*form(.*?)>(.*?)<[\\s]*/[\\s]*form[\\s]*>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher formmatcher = pattern.matcher(requestInfo.getHtmlCode()
                .replaceAll("(?s)<!--.*?-->", ""));
        while (formmatcher.find()) {
            String formPropertie = formmatcher.group(1);
            String inForm = formmatcher.group(2);
            // System.out.println(inForm);
            if (inForm.matches("(?s)" + matcher)) {
                Form form = new Form();
                form.baseRequest = requestInfo;
                form.method = METHOD_GET;
                Pattern patternfp = Pattern.compile(
                        " ([^\\s]+)\\=[\"'](.*?)[\"']",
                        Pattern.CASE_INSENSITIVE);
                Matcher matcherfp = patternfp.matcher(formPropertie);
                while (matcherfp.find()) {
                    String pname = matcherfp.group(1);
                    if (pname.toLowerCase().equals("action"))
                        form.action = matcherfp.group(2);
                    else if (pname.toLowerCase().equals("method")) {
                        String meth = matcherfp.group(2).toLowerCase();
                        if (meth.matches(".*post.*"))
                            form.method = METHOD_POST;
                        else if (meth.matches(".*get.*"))
                            form.method = METHOD_GET;
                        else if (meth.matches(".*put.*"))
                            form.method = METHOD_PUT;
                        else form.method = METHOD_UNKNOWN;
                    } else form.formProperties.put(pname, matcherfp.group(2));
                }
                patternfp = Pattern.compile(" ([^\\s]+)\\=[^\"'](.*?)[\\s>]",
                        Pattern.CASE_INSENSITIVE);
                matcherfp = patternfp.matcher(formPropertie);
                while (matcherfp.find()) {
                    String pname = matcherfp.group(1);
                    if (pname.toLowerCase().equals("action"))
                        form.action = matcherfp.group(2);
                    else if (pname.toLowerCase().equals("method")) {
                        String meth = matcherfp.group(2).toLowerCase();
                        if (meth.matches(".*post.*"))
                            form.method = METHOD_POST;
                        else if (meth.matches(".*get.*"))
                            form.method = METHOD_GET;
                        else if (meth.matches(".*put.*"))
                            form.method = METHOD_PUT;
                        else form.method = METHOD_UNKNOWN;
                    } else form.formProperties.put(pname, matcherfp.group(2));
                }
                if (form.action == null)
                    form.action = requestInfo.getConnection().getURL()
                            .toString();
                form.vars.putAll(getInputFields(inForm));
                forms.add(form);
            }
        }
        return forms.toArray(new Form[forms.size()]);
    }
    @SuppressWarnings("deprecation")
    public URLConnection getConnection() {
        if (method == METHOD_UNKNOWN) {
            JDUtilities.getLogger().severe("Unknown method");
            return null;
        } else if (method == METHOD_PUT) {
            JDUtilities.getLogger().severe("PUT is not supported");
            return null;
        }
        if (baseRequest == null) return null;
        URL baseurl = baseRequest.getConnection().getURL();
        if (action == null || action.matches("[\\s]*")) {
            if (baseurl == null) return null;
            action = baseurl.toString();
        } else if (!action.matches("http://.*")) {
            if (baseurl == null) return null;
            if (action.charAt(0) == '/')
                action = "http://" + baseurl.getHost() + action;
            else {
                String base = baseurl.toString();
                if (base.matches("http://.*/"))
                    action = base.substring(0, base.lastIndexOf("/")) + "/"
                            + action;
                else action = base + "/" + action;
            }
        }
        StringBuffer buffer = new StringBuffer();
        boolean first = true;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            if (first)
                first = false;
            else buffer.append("&");
            buffer.append(entry.getKey());
            buffer.append("=");
            buffer.append(URLEncoder.encode(entry.getValue()));
        }
        String varString = buffer.toString();
        if (method == METHOD_GET) {
            if (varString != null && !varString.matches("[\\s]*")) {
                if (action.matches(".*\\?.+"))
                    action += "&";
                else if (action.matches("[^\\?]*")) action += "?";
                action += varString;
            }
            try {
                URLConnection urlConnection = new URL(action).openConnection();
                urlConnection.setRequestProperty("Accept-Language",
                        Plugin.ACCEPT_LANGUAGE);
                urlConnection
                        .setRequestProperty(
                                "User-Agent",
                                "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
                urlConnection.setRequestProperty("Cookie", baseRequest
                        .getCookie());
                urlConnection.setRequestProperty("Referer", baseurl.toString());
                return urlConnection;
            }
            catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else if (method == METHOD_POST) {
            try {
                URLConnection urlConnection = new URL(action).openConnection();
                urlConnection.setRequestProperty("Accept-Language",
                        Plugin.ACCEPT_LANGUAGE);
                urlConnection
                        .setRequestProperty(
                                "User-Agent",
                                "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
                urlConnection.setRequestProperty("Cookie", baseRequest
                        .getCookie());
                urlConnection.setRequestProperty("Referer", baseurl.toString());
                urlConnection.setDoOutput(true);
                OutputStreamWriter wr = new OutputStreamWriter(urlConnection
                        .getOutputStream());
                wr.write(varString);
                wr.flush();
                wr.close();
                return urlConnection;
            }
            catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return null;
    }
    /**
     * Erzeugt aus der Form eine RequestInfo
     */
    public RequestInfo getRequestInfo() {
        return getRequestInfo(true);
    }
    public RequestInfo getRequestInfo(boolean redirect) {
        HttpURLConnection connection = (HttpURLConnection) getConnection();
        if(connection==null)
            return null;
        connection.setInstanceFollowRedirects(redirect);
        RequestInfo ri = null;
        int responseCode = HttpURLConnection.HTTP_NOT_IMPLEMENTED;
        try {
            responseCode = connection.getResponseCode();
        }
        catch (IOException e) {}
        if (withHtmlCode) {
            try {
                ri = Plugin.readFromURL(connection);
            }
            catch (IOException e) {
                // TODO Automatisch erstellter Catch-Block
                e.printStackTrace();
            }
        } else ri = new RequestInfo("", connection.getHeaderField("Location"),
                Plugin.getCookieString(connection), connection
                        .getHeaderFields(), responseCode);
        if (ri != null) {
            ri.setConnection(connection);
            return ri;
        }
        return null;
    }
    public String toString() {
        String ret = "";
        ret += "Action: " + action + "\n";
        if (method == METHOD_POST)
            ret += "Method: POST\n";
        else if (method == METHOD_GET)
            ret += "Method: GET\n";
        else if (method == METHOD_PUT)
            ret += "Method: PUT is not supported\n";
        else if (method == METHOD_UNKNOWN) ret += "Method: Unknown\n";
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            ret += "var: " + entry.getKey() + "=" + entry.getValue() + "\n";
        }
        for (Map.Entry<String, String> entry : formProperties.entrySet()) {
            ret += "formProperty: " + entry.getKey() + "=" + entry.getValue()
                    + "\n";
        }
        return ret;
    }
    public void put(String key, String value) {
        vars.put(key, value);
    }
    public void remove(String key) {
        vars.remove(key);
    }
}
