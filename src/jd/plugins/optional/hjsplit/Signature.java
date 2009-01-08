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

package jd.plugins.optional.hjsplit;

import java.util.regex.Pattern;

public class Signature {

    private String id;

    private Pattern signatur;

    private String desc;

    private Pattern extension;

    public Signature(String id, String signaturPattern, String desc, String ext) {
        this.id = id;
        this.signatur = signaturPattern != null ? Pattern.compile(signaturPattern, Pattern.CASE_INSENSITIVE) : null;
        this.extension = ext != null ? Pattern.compile(ext, Pattern.CASE_INSENSITIVE) : null;
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public Pattern getExtension() {
        return extension;
    }

    public String getId() {
        return id;
    }

    public Pattern getSignatur() {
        return signatur;
    }

    public boolean matches(String sig) {
        return signatur.matcher(sig).matches();
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public void setExtension(Pattern extension) {
        this.extension = extension;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setSignatur(Pattern signatur) {
        this.signatur = signatur;
    }

}
