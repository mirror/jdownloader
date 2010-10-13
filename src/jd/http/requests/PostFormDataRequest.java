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

package jd.http.requests;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;

import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.http.URLConnectionAdapter.METHOD;

import org.appwork.utils.net.CountingOutputStream;
import org.appwork.utils.net.NullOutputStream;

/**
 * Extending the Request class, this class is able to to HTML Formdata Posts.
 * 
 * @author coalado
 */
public class PostFormDataRequest extends Request {

    private String              boundary;
    private ArrayList<FormData> formDatas;
    private String              encodeType = "multipart/form-data";

    public String getEncodeType() {
        return encodeType;
    }

    public void setEncodeType(String encodeType) {
        this.encodeType = encodeType;
    }

    public PostFormDataRequest(String url) throws MalformedURLException {
        super(Browser.correctURL(url));
        generateBoundary();
        this.formDatas = new ArrayList<FormData>();
    }

    /**
     * send the postData of the Request. in case httpConnection is null, it
     * outputs the data to a NullOutputStream
     */
    public long postRequest(URLConnectionAdapter httpConnection) throws IOException {
        CountingOutputStream output = null;
        if (httpConnection != null && httpConnection.getOutputStream() != null) {
            output = new CountingOutputStream(httpConnection.getOutputStream());
        } else {
            output = new CountingOutputStream(new NullOutputStream());
        }
        try {
            for (int i = 0; i < this.formDatas.size(); i++) {
                write(formDatas.get(i), output);
            }
            OutputStreamWriter writer = new OutputStreamWriter(output);
            writer.write(this.boundary);
            writer.write("--\r\n");
            writer.flush();
            output.flush();
        } finally {
            if (httpConnection != null) httpConnection.postDataSend();
        }
        return output.bytesWritten();
    }

    private void write(FormData formData, OutputStream output) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(output);
        writer.write(this.boundary);
        writer.write("\r\n");
        switch (formData.getType()) {
        case VARIABLE:
            writer.write("Content-Disposition: form-data; name=\"" + formData.getName() + "\"");
            writer.write("\r\n\r\n");
            writer.write(formData.getValue() + "\r\n");
            break;
        case DATA:
            writer.write("Content-Disposition: form-data; name=\"" + formData.getName() + "\"; filename=\"" + formData.getValue() + "\"");
            writer.write("\r\nContent-Type: " + formData.getDataType() + "\r\n\r\n");
            writer.flush();
            output.write(formData.getData(), 0, formData.getData().length);
            output.flush();
            writer.write("\r\n");
            writer.flush();
            break;
        case FILE:
            writer.write("Content-Disposition: form-data; name=\"" + formData.getName() + "\"; filename=\"" + formData.getValue() + "\"");
            writer.write("\r\nContent-Type: " + formData.getDataType() + "\r\n\r\n");
            writer.flush();
            byte[] b = new byte[1024];
            InputStream in = null;
            try {
                in = new FileInputStream(formData.getFile());
                int n;
                while ((n = in.read(b)) > -1) {
                    output.write(b, 0, n);
                }
                output.flush();
                writer.write("\r\n");
                writer.flush();
            } finally {
                if (in != null) in.close();
            }
            break;
        }
        writer.flush();
        output.flush();
    }

    public void preRequest(URLConnectionAdapter httpConnection) throws IOException {
        httpConnection.setRequestMethod(METHOD.POST);
        httpConnection.setRequestProperty("Content-Type", encodeType + "; boundary=" + boundary.substring(2));
        httpConnection.setRequestProperty("Content-Length", this.postRequest(null) + "");
    }

    private void generateBoundary() {
        long range = (999999999999999l - 100000000000000l);
        long rand = (long) (Math.random() * range) + 100000000000000l;
        boundary = "---------------------" + rand;
    }

    public void addFormData(FormData fd) {
        this.formDatas.add(fd);
    }

    public String getPostDataString() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < this.formDatas.size(); i++) {
            write(formDatas.get(i), sb);
        }
        sb.append(this.boundary);
        sb.append("--\r\n");
        return sb.toString();
    }

    private void write(FormData formData, StringBuffer sb) {
        sb.append(this.boundary);
        sb.append("\r\n");
        switch (formData.getType()) {
        case VARIABLE:
            sb.append("Content-Disposition: form-data; name=\"").append(formData.getName()).append("\"");
            sb.append("\r\n\r\n");
            sb.append(formData.getValue()).append("\r\n");
            break;
        case DATA:
            sb.append("Content-Disposition: form-data; name=\"").append(formData.getName()).append("\"; filename=\"").append(formData.getValue()).append("\"");
            sb.append("\r\nContent-Type: ").append(formData.getDataType());
            sb.append("\r\n\r\n[.....").append(formData.getData().length).append(" Byte DATA....]\r\n");
            break;
        case FILE:
            sb.append("Content-Disposition: form-data; name=\"").append(formData.getName()).append("\"; filename=\"").append(formData.getValue()).append("\"");
            sb.append("\r\nContent-Type: ").append(formData.getDataType());
            sb.append("\r\n\r\n[.....").append(formData.getFile().length()).append(" FileByte DATA....]\r\n");
            break;
        }
    }

}
