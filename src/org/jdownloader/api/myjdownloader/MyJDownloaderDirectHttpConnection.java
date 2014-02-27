package org.jdownloader.api.myjdownloader;

import java.io.IOException;
import java.net.Socket;

import org.appwork.utils.StringUtils;
import org.jdownloader.api.myjdownloader.api.MyJDownloaderAPI;
import org.jdownloader.myjdownloader.RequestLineParser;
import org.jdownloader.settings.staticreferences.CFG_MYJD;

public class MyJDownloaderDirectHttpConnection extends MyJDownloaderHttpConnection {
    
    public MyJDownloaderDirectHttpConnection(Socket clientConnection, MyJDownloaderAPI api) throws IOException {
        super(clientConnection, api);
    }
    
    @Override
    protected String parseRequestLine() throws IOException {
        try {
            return super.parseRequestLine();
        } catch (IOException e) {
            clientSocket.close();
            throw e;
        }
    }
    
    @Override
    protected String preProcessRequestLine(String requestLine) throws IOException {
        RequestLineParser parser = RequestLineParser.parse(requestLine.getBytes("UTF-8"));
        if (parser == null || parser.getSessionToken() == null || !StringUtils.equals(CFG_MYJD.CFG.getUniqueDeviceID(), parser.getDeviceID())) {
            clientSocket.close();
            throw new IOException("Invalid direct my.jdownloader.org request: " + requestLine);
        }
        return super.preProcessRequestLine(requestLine);
    }
}
