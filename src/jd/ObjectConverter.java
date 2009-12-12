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

package jd;

import java.beans.ExceptionListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import jd.parser.Regex;

public class ObjectConverter {

    protected Exception exception;
    private String pre;
    private String post;

    public String toString(Object obj) throws Exception {

        ByteArrayOutputStream ba;
        DataOutputStream out = new DataOutputStream(ba = new ByteArrayOutputStream());
        XMLEncoder xmlEncoder = new XMLEncoder(out);

        xmlEncoder.setExceptionListener(new ExceptionListener() {

            public void exceptionThrown(Exception e) {
                exception = e;
            }
        });

        xmlEncoder.writeObject(obj);
        xmlEncoder.close();
        out.close();
        if (exception != null) throw exception;

        String[] ret = new Regex(new String(ba.toByteArray()), "(<java .*?>)(.*?)(</java>)").getRow(0);
        this.pre = ret[0];
        this.post = ret[2];
        ret[1] = ret[1].replace(" ", "   ");
        return ret[1].trim();
    }

    public Object toObject(String in) throws Exception {
        if (pre == null || post == null) {
            // dummy
            toString(new Object());
        }
        Object objectLoaded = null;
        String str = (pre + in + post);
        ByteArrayInputStream ba = new ByteArrayInputStream(str.getBytes());
        XMLDecoder xmlDecoder = new XMLDecoder(ba);
        xmlDecoder.setExceptionListener(new ExceptionListener() {

            public void exceptionThrown(Exception e) {
                exception = e;

            }
        });
        objectLoaded = xmlDecoder.readObject();
        xmlDecoder.close();
        ba.close();
        if (exception != null) throw exception;
        return objectLoaded;
    }

}
