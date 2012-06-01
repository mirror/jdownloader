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

package org.jdownloader.extensions.langfileeditor;

import org.appwork.utils.Regex;

public class KeyInfo implements Comparable<KeyInfo> {

    private final String key;

    private String       source                 = "";

    private String       language               = "";

    private String       english                = "";

    private int          sourceParameterCount   = -1;

    private int          languageParameterCount = -1;

    public KeyInfo(String key, String source, String language, String english) {
        this.key = key;
        this.setSource(source);
        this.setEnglish(english);
        this.setLanguage(language);
    }

    public String getKey() {
        return this.key;
    }

    public String getLanguage() {
        return this.language;
    }

    public String getSource() {
        return this.source;
    }

    /**
     * Returns the english value
     * 
     * @return
     */
    public String getEnglish() {
        return this.english;
    }

    public boolean hasWrongParameterCount() {
        return this.sourceParameterCount != -1 && this.languageParameterCount != -1 && this.sourceParameterCount != this.languageParameterCount;
    }

    public void setLanguage(String language) {
        if (language != null) {
            this.language = language;
            this.languageParameterCount = language.equals("") ? -1 : new Regex(language, "\\%s").count();
        }
    }

    public void setSource(String source) {
        if (source != null) {
            this.source = source;
            this.sourceParameterCount = source.equals("") ? -1 : new Regex(source, "\\%s").count();
        }
    }

    public void setEnglish(String english) {
        if (english != null) this.english = english;
    }

    public boolean isMissing() {
        return this.getLanguage().equals("") && !this.getSource().equals("");
    }

    public boolean isOld() {
        return !this.getLanguage().equals("") && this.getSource().equals("");
    }

    public int compareTo(KeyInfo o) {
        return this.getKey().compareToIgnoreCase(o.getKey());
    }

    @Override
    public String toString() {
        return this.getKey() + " = " + this.getLanguage();
    }

}
