package org.jdownloader.api;

import java.io.File;

import org.appwork.utils.Hash;
import org.appwork.utils.net.httpserver.HttpServerController;

public class HttpServer extends HttpServerController {

    private static HttpServer INSTANCE = new HttpServer();

    public static HttpServer getInstance() {
        return INSTANCE;
    }

    private HttpServer() {
    }

    public static void main(String[] args) {
        System.out.println(Hash.getMD5(new File("C:\\Users\\Thomas\\Desktop\\i4jdel0.exe")));
    }
}
