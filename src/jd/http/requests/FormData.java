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

    private Type type;
    private String name;
    private String value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
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

    public FormData(String name, String value) {
        this.type = Type.VARIABLE;
        this.name = name;
        this.value = value;
    }

    public FormData(String name, String filename, byte[] data) {
        this(name, filename, null, data);
    }

    public FormData(String name, String filename, String mime, byte[] data) {
        if (mime == null) mime = "application/octet-stream";
        this.mime = mime;
        this.type = Type.DATA;
        this.name = name;
        this.value = filename;
        this.data = data;
    }

    public FormData(String name, String filename, File file) {
        this(name, filename, null, file);

    }

    public FormData(String name, String filename, String mime, File file) {
        if (mime == null) mime = "application/octet-stream";
        this.mime = mime;
        this.type = Type.FILE;
        this.name = name;
        this.value = filename;
        this.file = file;
    }

    public static enum Type {
        VARIABLE, FILE, DATA
    }

    public Type getType() {
        // TODO Auto-generated method stub
        return type;
    }

    public String getDataType() {
        // TODO Auto-generated method stub
        return this.mime;
    }

}
