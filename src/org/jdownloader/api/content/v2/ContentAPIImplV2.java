package org.jdownloader.api.content.v2;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.Icon;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.remoteapi.RemoteAPI;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.exceptions.APIFileNotFoundException;
import org.appwork.remoteapi.exceptions.InternalApiException;
import org.appwork.utils.Application;
import org.appwork.utils.Hash;
import org.appwork.utils.IO;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.logging.Log;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.DomainInfo;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.myjdownloader.client.bindings.interfaces.ContentInterface;

public class ContentAPIImplV2 implements ContentAPIV2 {
    public ContentAPIImplV2() {
        RemoteAPIController.validateInterfaces(ContentAPIV2.class, ContentInterface.class);
    }

    public void getFavIcon(RemoteAPIRequest request, RemoteAPIResponse response, String hostername) throws InternalApiException, APIFileNotFoundException {
        DomainInfo info = DomainInfo.getInstance(hostername);
        Icon favIcon = info.getFavIcon();
        if (favIcon == null) {
            throw new APIFileNotFoundException();
        }
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

    private static final Object                   LOCK         = new Object();
    private static final HashMap<Integer, String> ICON_KEY_MAP = new HashMap<Integer, String>();

    public String getIconKey(Icon icon) {
        if (icon instanceof AbstractIcon) {
            return ((AbstractIcon) icon).getKey();
        }
        synchronized (LOCK) {
            String cached = ICON_KEY_MAP.get(icon.hashCode());
            if (cached != null) {
                return "tmp." + cached;
            }
            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            try {
                ImageIO.write(IconIO.toBufferedImage(icon), "png", bao);

                byte[] data = bao.toByteArray();
                String hash = Hash.getMD5(data);

                File file = Application.getTempResource("apiIcons/" + hash + ".png");

                if (file.exists()) {
                    return "tmp." + hash;
                }

                IO.secureWrite(file, data);
                ICON_KEY_MAP.put(icon.hashCode(), hash);
                return "tmp." + hash;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public void getIcon(RemoteAPIRequest request, RemoteAPIResponse response, String key, int size) throws InternalApiException {
        OutputStream out = null;
        try {
            /* we force content type to image/png and allow caching of the image */
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CACHE_CONTROL, "public,max-age=60", false));
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE, "image/png", false));
            out = RemoteAPI.getOutputStream(response, request, RemoteAPI.gzip(request), false);
            if (key.startsWith("tmp.")) {
                String hash = key.substring(4).replaceAll("[^A-Fa-f0-9]", "");

                BufferedImage image = IconIO.getImage(Application.getRessourceURL("tmp/apiIcons/" + hash + ".png"));
                if (size > 0) {
                    if (image.getWidth() > size || image.getHeight() > size) {
                        image = IconIO.getScaledInstance(image, size, size);
                    }
                }

                ImageIO.write(image, "png", out);
            } else {
                ImageIO.write(IconIO.toBufferedImage(new AbstractIcon(key, size)), "png", out);
            }
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
