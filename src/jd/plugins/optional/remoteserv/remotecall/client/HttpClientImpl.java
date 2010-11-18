package jd.plugins.optional.remoteserv.remotecall.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import jd.plugins.optional.remoteserv.remotecall.HttpClient;

public class HttpClientImpl implements HttpClient {
    public String post(final URL url, final String data) throws IOException {
        HttpURLConnection connection = null;
        OutputStreamWriter writer = null;
        BufferedReader reader = null;
        OutputStream outputStream = null;
        InputStreamReader isr = null;
        try {

            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setDoOutput(true);

            connection.setRequestProperty("Connection", "Close");

            connection.connect();

            outputStream = connection.getOutputStream();
            writer = new OutputStreamWriter(outputStream);
            writer.write(data);
            writer.flush();
            reader = new BufferedReader(isr = new InputStreamReader(connection.getInputStream()));
            final StringBuilder sb = new StringBuilder();
            String str;
            while ((str = reader.readLine()) != null) {
                sb.append(str);
                sb.append("\r\n");
            }

            return sb.toString();

        } finally {
            try {
                reader.close();
            } catch (final Throwable e) {
            }
            try {
                isr.close();
            } catch (final Throwable e) {
            }
            try {
                writer.close();
            } catch (final Throwable e) {
            }
            try {
                outputStream.close();
            } catch (final Throwable e) {
            }
            try {
                connection.disconnect();
            } catch (final Throwable e) {
            }

        }
    }
}
