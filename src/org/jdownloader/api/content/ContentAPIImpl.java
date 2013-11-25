package org.jdownloader.api.content;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;
import javax.swing.Icon;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.remoteapi.RemoteAPI;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.exceptions.InternalApiException;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.logging.Log;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.DomainInfo;

public class ContentAPIImpl implements ContentAPI {

    public void favicon(RemoteAPIRequest request, RemoteAPIResponse response, String hostername) throws FileNotFoundException, InternalApiException {
        DomainInfo info = DomainInfo.getInstance(hostername);
        Icon favIcon = info.getFavIcon();
        if (favIcon == null) throw new FileNotFoundException();
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
    public void fileIcon(RemoteAPIRequest request, RemoteAPIResponse response, String extension) throws InternalApiException {
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
