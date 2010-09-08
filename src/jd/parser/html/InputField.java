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

package jd.parser.html;

import java.io.File;

import jd.config.Property;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;

public class InputField extends Property {

    private static final long serialVersionUID = 7859094911920903660L;
    private String            key              = null;
    private String            value            = null;
    private String            type             = null;

    public InputField(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public static InputField parse(String data) {
        /* first we try values with " at start/end */
        String[][] matches = new Regex(data, "[\"' ](\\w+?)[ ]*=[ ]*[\"](.*?)[\"]").getMatches();
        if (matches == null || matches.length == 0) {
            /* then we try values with ' at start/end */
            matches = new Regex(data, "[\"' ](\\w+?)[ ]*=[ ]*['](.*?)[']").getMatches();
        }
        String[][] matches2 = new Regex(data, "[\"' ](\\w+?)[ ]*=[ ]*([^>^ ^\"^']+)").getMatches();
        InputField ret = new InputField();

        for (String[] match : matches) {
            if (match[0].equalsIgnoreCase("type")) {
                ret.setType(match[1]);
            } else if (match[0].equalsIgnoreCase("name")) {
                ret.setKey(Encoding.formEncoding(match[1]));
            } else if (match[0].equalsIgnoreCase("value")) {
                ret.setValue(Encoding.formEncoding(match[1]));
            } else {
                ret.setProperty(Encoding.formEncoding(match[0]), Encoding.formEncoding(match[1]));
            }
        }

        for (String[] match : matches2) {
            if (match[0].equalsIgnoreCase("type")) {
                ret.setType(match[1]);
            } else if (match[0].equalsIgnoreCase("name")) {
                ret.setKey(Encoding.formEncoding(match[1]));
            } else if (match[0].equalsIgnoreCase("value")) {
                ret.setValue(Encoding.formEncoding(match[1]));
            } else {
                ret.setProperty(Encoding.formEncoding(match[0]), Encoding.formEncoding(match[1]));
            }
        }

        // if (ret.getType() != null && ret.getType().equalsIgnoreCase("file"))
        // {
        // // method = METHOD_FILEPOST;
        //
        // }

        return ret;
    }

    public String toString() {

        return "Field: " + this.key + "(" + this.type + ")" + " = " + this.value + " [" + super.toString() + "]";
    }

    public void setFileToPost(File file) {
        if (!type.equalsIgnoreCase("file")) throw new IllegalStateException("No file post field");
        this.value = file.getAbsolutePath();
    }

    public File getFileToPost() {
        if (!type.equalsIgnoreCase("file")) throw new IllegalStateException("No file post field");

        return new File(this.value);
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