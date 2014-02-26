package org.jdownloader.api.myjdownloader;

import java.io.IOException;
import java.net.Socket;

import org.jdownloader.api.myjdownloader.api.MyJDownloaderAPI;

public class MyJDownloaderDirectHttpConnection extends MyJDownloaderHttpConnection {
    
    public MyJDownloaderDirectHttpConnection(Socket clientConnection, MyJDownloaderAPI api) throws IOException {
        super(clientConnection, api);
    }
    
}
