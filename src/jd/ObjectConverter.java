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
        ret[1]=ret[1].replace(" ", "   ");
        return ret[1].trim();
    }

    public Object toObject(String in) throws Exception {
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
