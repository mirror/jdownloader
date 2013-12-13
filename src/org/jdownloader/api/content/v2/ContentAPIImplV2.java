package org.jdownloader.api.content.v2;

import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;
import javax.swing.Icon;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.remoteapi.RemoteAPI;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.exceptions.APIFileNotFoundException;
import org.appwork.remoteapi.exceptions.InternalApiException;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.logging.Log;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.DomainInfo;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.myjdownloader.client.bindings.interfaces.ContentInterface;

public class ContentAPIImplV2 implements ContentAPIV2 {
    public ContentAPIImplV2() {
        RemoteAPIController.validateInterfaces(ContentAPIV2.class, ContentInterface.class);
    }

    public void getFavIcon(RemoteAPIRequest request, RemoteAPIResponse response, String hostername) throws InternalApiException, APIFileNotFoundException {
        DomainInfo info = DomainInfo.getInstance(hostername);
        Icon favIcon = info.getFavIcon();
        if (favIcon == null) throw new APIFileNotFoundException();
        OutputStream out = null;
        try {
            /* we force content type to image/png and allow caching of the image */
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CACHE_CONTROL, "public,max-age=60", false));
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE, "image/png", false));
            out = RemoteAPI.getOutputStream(response, request, RemoteAPI.gzip(request), false);
            ImageIO.write(IconIO.toBufferedImage(favIcon), "png", out);
        } catch (IOException e) {
            Log.exception(e);
            throw new InternalApiException(e);

        } finally {
            try {
                out.close();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public void getFileIcon(RemoteAPIRequest request, RemoteAPIResponse response, String extension) throws InternalApiException {
        OutputStream out = null;
        try {
            /* we force content type to image/png and allow caching of the image */
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CACHE_CONTROL, "public,max-age=60", false));
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE, "image/png", false));
            out = RemoteAPI.getOutputStream(response, request, RemoteAPI.gzip(request), false);
            ImageIO.write(IconIO.toBufferedImage(CrossSystem.getMime().getFileIcon(extension, 16, 16)), "png", out);
        } catch (IOException e) {
            Log.exception(e);
            throw new InternalApiException(e);
        } finally {
            try {
                out.close();
            } catch (final Throwable e) {
            }
        }
    }
}
