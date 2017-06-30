package org.jdownloader.api.myjdownloader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpserver.EmptyRequestException;
import org.jdownloader.api.myjdownloader.api.MyJDownloaderAPI;
import org.jdownloader.myjdownloader.RequestLineParser;
import org.jdownloader.settings.staticreferences.CFG_MYJD;

public class MyJDownloaderDirectHttpConnection extends MyJDownloaderHttpConnection {
    public MyJDownloaderDirectHttpConnection(Socket clientConnection, MyJDownloaderAPI api) throws IOException {
        super(clientConnection, api);
    }

    public MyJDownloaderDirectHttpConnection(final Socket clientSocket, final InputStream is, final OutputStream os, MyJDownloaderAPI api) throws IOException {
        super(clientSocket, is, os, api);
    }

    @Override
    protected String preProcessRequestLine(String requestLine) throws IOException {
        if (StringUtils.isEmpty(requestLine)) {
            throw new EmptyRequestException();
        }
        final RequestLineParser parser = RequestLineParser.parse(requestLine.getBytes("UTF-8"));
        if (parser == null || parser.getSessionToken() == null || !StringUtils.equals(CFG_MYJD.CFG.getUniqueDeviceIDV2(), parser.getDeviceID())) {
            throw new InvalidMyJDownloaderRequest();
        }
        return super.preProcessRequestLine(requestLine);
    }
}
