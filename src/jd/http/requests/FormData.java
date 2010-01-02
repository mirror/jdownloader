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

package jd.http.requests;

import java.io.File;

public class FormData {

    private final Type type;
    private String name;
    private final String value;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public byte[] getData() {
        return data;
    }

    public File getFile() {
        return file;
    }

    private byte[] data;
    private File file;
    private String mime;

    public FormData(final String name, final String value) {
        this.type = Type.VARIABLE;
        this.name = name;
        this.value = value;
    }

    public FormData(final String name, final String filename, final byte[] data) {
        this(name, filename, null, data);
    }

    public FormData(final String name, final String filename, final String mime, final byte[] data) {
        this.mime = (mime == null) ? "application/octet-stream" : mime;
        this.type = Type.DATA;
        this.name = name;
        this.value = filename;
        this.data = data;
    }

    public FormData(final String name, final String filename, final File file) {
        this(name, filename, null, file);

    }

    public FormData(final String name, final String filename, final String mime, final File file) {
        this.mime = (mime == null) ? "application/octet-stream" : mime;
        this.type = Type.FILE;
        this.name = name;
        this.value = filename;
        this.file = file;
    }

    public static enum Type {
        VARIABLE, FILE, DATA
    }

    public Type getType() {
        return type;
    }

    public String getDataType() {
        return this.mime;
    }

}
