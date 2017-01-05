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

package org.jdownloader.extensions.extraction;

import java.util.regex.Pattern;

public class Signature {

    private final String  id;

    private final Pattern signatur;

    private final String  desc;

    private final Pattern extensionSure;
    private final Pattern extensionUnsure;

    public Signature(String id, String signaturPattern, String desc, String ext) {
        this.id = id;
        this.signatur = signaturPattern != null ? Pattern.compile(signaturPattern, Pattern.CASE_INSENSITIVE) : null;
        this.extensionUnsure = null;
        this.extensionSure = ext != null ? Pattern.compile(ext, Pattern.CASE_INSENSITIVE) : null;
        this.desc = desc;

    }

    public Signature(String id, String signaturPattern, String desc, String ext, String unsureext) {
        this.id = id;
        this.signatur = signaturPattern != null ? Pattern.compile(signaturPattern, Pattern.CASE_INSENSITIVE) : null;
        this.extensionSure = ext != null ? Pattern.compile(ext, Pattern.CASE_INSENSITIVE) : null;
        this.extensionUnsure = ext != null ? Pattern.compile(unsureext, Pattern.CASE_INSENSITIVE) : null;
        this.desc = desc;
    }

    public boolean isPrecisePatternStart() {
        return signatur != null && !signatur.pattern().startsWith(".");
    }

    public String getDesc() {
        return desc;
    }

    public Pattern getExtensionSure() {
        return extensionSure;
    }

    public Pattern getExtensionUnsure() {
        return extensionUnsure;
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

}
