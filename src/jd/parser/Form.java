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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.config.Property;
import jd.http.Encoding;
import jd.utils.JDUtilities;

public class Form {
    public static final int METHOD_FILEPOST = 3;

    public static final int METHOD_GET = 1;

    public static final int METHOD_POST = 0;

    public static final int METHOD_PUT = 2;

    public static final int METHOD_UNKNOWN = 99;

    private String htmlcode = null;

    /**
     * Ein Array mit allen Forms einer Seite
     */
    public static Form[] getForms(Object requestInfo) {
        return Form.getForms(requestInfo, ".*");
    }

    private String[] submitValues;

    public String getHtmlCode() {
        return htmlcode;
    }

    public boolean equals(Form f) {
        return this.toString().equalsIgnoreCase(f.toString());
    }

    /**
     * Ein Array mit allen Forms dessen Inhalt dem matcher entspricht. Achtung
     * der Matcher bezieht sich nicht auf die Properties einer Form sondern auf
     * den Text der zwischen der Form steht. Dafür gibt es die formProperties
     */
    public static Form[] getForms(Object requestInfo, String matcher) {
        LinkedList<Form> forms = new LinkedList<Form>();

        Pattern pattern = Pattern.compile("<[\\s]*form(.*?)>(.*?)<[\\s]*/[\\s]*form[\\s]*>|<[\\s]*form(.*?)>(.+)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher formmatcher = pattern.matcher(requestInfo.toString().replaceAll("(?s)<!--.*?-->", ""));
        while (formmatcher.find()) {
            String formPropertie = formmatcher.group(1);
            String inForm = formmatcher.group(2);
            if (formPropertie == null) {
                /* falls die form nicht geschlossen wird */
                formPropertie = formmatcher.group(3);
                inForm = formmatcher.group(4);
            }
            // System.out.println(inForm);
            if (inForm.matches("(?s)" + matcher)) {
                Form form = new Form();
                form.htmlcode = inForm;
                String[] submits = new Regex(inForm, "<input.*?>").getColumn(-1);
                ArrayList<String> tmp = new ArrayList<String>();
                // 

                for (String submit : submits) {
                    if (Regex.matches(submit, "type[ ]*?=[ ]*?[\"|']submit[\"|']")) {
                        String submitvalue = new Regex(submit, "value[ ]*?=[ ]*?[\"|'](.*?)[\"|']").getMatch(0);
                        tmp.add(submitvalue);
                    }

                }
                form.submitValues = tmp.toArray(new String[] {});
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
    private HashMap<String, InputField> vars = new HashMap<String, InputField>();

    public boolean hasSubmitValue(String value) {
        for (String submit : this.submitValues) {
            try {
                if (submit == value || submit.equals(value)) return true;
            } catch (NullPointerException e) {
                // e.printStackTrace();
            }
        }
        return false;

    }

    public String getAction(String baseURL) {
        URL baseurl = null;
        if (baseURL == null) {
            baseurl = null;
        } else {
            try {
                baseurl = new URL(baseURL);
            } catch (MalformedURLException e) {

                e.printStackTrace();
            }
        }
        String ret = action;
        if (action == null || action.matches("[\\s]*")) {
            if (baseurl == null) { return null; }
            ret = baseurl.toString();
        } else if (!ret.matches("https?://.*")) {
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
            } else if (ret.charAt(0) == '#') {
                String base = baseurl.toString();
                if (base.matches("http://.*/.*")) {
                    ret = base + ret;
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

    /**
     * Gibt alle Input fields zurück Object[0]=vars Object[1]=varsWithoutValue
     */
    private HashMap<String, InputField> getInputFields(String data) {
        HashMap<String, InputField> ret = new HashMap<String, InputField>();
        Matcher matcher = Pattern.compile("(?s)<[\\s]*(input|textarea|select)(.*?)>", Pattern.CASE_INSENSITIVE).matcher(data);
        while (matcher.find()) {
            InputField nv = getNameValue(matcher.group(2));
            if (nv != null) {
                // if (!ret.containsKey(nv[0]) || ret.get(nv[0]).equals("")) {
                ret.put(nv.getKey(), nv);
                // }
            }
        }
        return ret;
    }

    private InputField getNameValue(String data) {

        String[][] matches = new Regex(data, "(\\w+?)[ ]*=[ ]*[\"'](.*?)[\"']").getMatches();
        String[][] matches2 = new Regex(data, "(\\w+?)[ ]*=[ ]*([^ ^\"^'.]+)[ />]?").getMatches();
        InputField ret = new InputField();

        for (String[] match : matches) {
            if (match[0].equalsIgnoreCase("type")) {
                ret.setType(match[1]);
            } else if (match[0].equalsIgnoreCase("name")) {
                ret.setKey(match[1]);
            } else if (match[0].equalsIgnoreCase("value")) {
                ret.setValue(match[1]);
            } else {
                ret.setProperty(match[0], match[1]);
            }
        }

        for (String[] match : matches2) {
            if (match[0].equalsIgnoreCase("type")) {
                ret.setType(match[1]);
            } else if (match[0].equalsIgnoreCase("name")) {
                ret.setKey(match[1]);
            } else if (match[0].equalsIgnoreCase("value")) {
                ret.setValue(match[1]);
            } else {
                ret.setProperty(match[0], match[1]);
            }
        }

        if (ret.getType() != null && ret.getType().equalsIgnoreCase("file")) {
            method = METHOD_FILEPOST;
            setFiletoPostName("");

        }

        return ret;
    }

    public void put(String key, String value) {
        if (vars.containsKey(key)) {
            vars.get(key).setValue(value);
        } else {
            vars.put(key, new InputField(key, value));
        }
    }

    public void remove(String key) {
        vars.remove(key);
    }

    /**
     * Setzt die i-te Variable
     * 
     * @param i
     * @param value
     * @return
     */
    public String setVariable(int i, String value) {

        for (Iterator<String> it = vars.keySet().iterator(); it.hasNext();) {
            String key = it.next();
            if (--i < 0) {
                vars.get(key).setValue(value);

                return key;
            }
        }
        return null;

    }

    /**
     * Gibt den variablennamen der am besten zu varname passt zurück.
     * 
     * @param varname
     * @return
     */
    public String getBestVariable(String varname) {
        String best = null;
        int bestDist = Integer.MAX_VALUE;

        for (Map.Entry<String, InputField> entry : vars.entrySet()) {
            int dist = JDUtilities.getLevenshteinDistance(varname, entry.getKey());
            if (dist < bestDist) {
                best = entry.getKey();
                bestDist = dist;
            }
        }
        return best;

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
        for (Map.Entry<String, InputField> entry : vars.entrySet()) {
            ret += "var: " + entry.getValue() + "\n";
        }
        for (Map.Entry<String, String> entry : formProperties.entrySet()) {
            ret += "formProperty: " + entry.getKey() + "=" + entry.getValue() + "\n";
        }

        return ret;
    }

    /**
     * GIbt alle variablen als propertyString zurück
     * 
     * @return
     */
    public String getPropertyString() {
        StringBuilder stbuffer = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, InputField> entry : vars.entrySet()) {
            if (first) {
                first = false;
            } else {
                stbuffer.append("&");
            }
            stbuffer.append(entry.getKey());
            stbuffer.append("=");
            stbuffer.append(Encoding.urlEncode(entry.getValue().getValue()));
        }
        return stbuffer.toString();

    }

    public HashMap<String, InputField> getVars() {
        return vars;
    }

    public void setVars(HashMap<String, InputField> vars) {
        this.vars = vars;
    }

    private void setFiletoPostName(String filetoPostName) {
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

    /**
     * Gibt die gefrundene Submitvalues zurück.
     * 
     * @return
     */
    public String[] getSubmitValues() {
        return submitValues;
    }

    /**
     * Gibt alle gefundenen Submitvalues zurück
     * 
     * @param submitValues
     */
    public void setSubmitValues(String[] submitValues) {
        this.submitValues = submitValues;
    }

    public ArrayList<InputField> getInputFieldsByType(String type) {
        ArrayList<InputField> ret = new ArrayList<InputField>();
        for (Iterator<String> it = vars.keySet().iterator(); it.hasNext();) {
            String key = it.next();
            if (Regex.matches(vars.get(key).getType(), type)) {
                ret.add(vars.get(key));
            }
        }
        return ret;
    }

    /**
     * Gibt ein RegexObject bezüglich des Form htmltextes zurück
     * 
     * @param compile
     * @return
     */
    public Regex getRegex(String string) {
        return new Regex(htmlcode, string);
    }

    /**
     * Gibt ein RegexObject bezüglich des Form htmltextes zurück
     * 
     * @param compile
     * @return
     */
    public Regex getRegex(Pattern compile) {
        return new Regex(htmlcode, compile);
    }

    /**
     * Gibt zurück ob der gesuchte needle String im html Text bgefunden wurde
     * 
     * @param fileNotFound
     * @return
     */
    public boolean containsHTML(String needle) {
        return new Regex(htmlcode, needle).matches();
    }

    public class InputField extends Property {

        private static final long serialVersionUID = 7859094911920903660L;
        private String key = null;
        private String value = null;
        private String type;

        public InputField(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String toString() {
            return this.key + "(" + this.type + ")" + " = " + this.value + " [" + super.toString() + "]";
        }

        public String getType() {
            return type;
        }

        public void setKey(String string) {
            if (string != null) string = string.trim();
            this.key = string;
        }

        public void setType(String string) {
            if (string != null) string = string.trim();
            this.type = string;
        }

        public String getValue() {
            return value;
        }

        public InputField() {
            // TODO Auto-generated constructor stub
        }

        public String getKey() {
            return key;
        }

        public void setValue(String value) {
            if (value != null) value = value.trim();
            this.value = value;
        }

    }

    public HashMap<String, String> getVarsMap() {
        HashMap<String, String> ret = new HashMap<String, String>();
        for (Iterator<String> it = vars.keySet().iterator(); it.hasNext();) {

            String key = it.next();
            if (key != null) {/*
                               * namenlose Values werden nicht uebermittelt
                               * (siehe Liveheader im Firefox)
                               */
                InputField field = vars.get(key);
                ret.put(key, Encoding.urlEncode(field.getValue()));
            }

        }
        return ret;
    }

}
