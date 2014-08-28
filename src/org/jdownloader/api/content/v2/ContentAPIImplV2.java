package org.jdownloader.api.content.v2;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.Icon;

import jd.plugins.FavitIcon;

import org.appwork.exceptions.WTFException;
import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.remoteapi.RemoteAPI;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.exceptions.APIFileNotFoundException;
import org.appwork.remoteapi.exceptions.InternalApiException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.SimpleMapper;
import org.appwork.storage.StorageException;
import org.appwork.storage.TypeRef;
import org.appwork.swing.components.ExtMergedIcon;
import org.appwork.swing.components.IDIcon;
import org.appwork.utils.Hash;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.logging.Log;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.DomainInfo;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.gui.views.components.MergedIcon;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.BadgeIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.myjdownloader.client.bindings.interfaces.ContentInterface;
import org.jdownloader.myjdownloader.client.json.IconDescriptor;

public class ContentAPIImplV2 implements ContentAPIV2 {
    private SimpleMapper                    mapper;
    private HashMap<String, IconDescriptor> descriptorMap;

    public ContentAPIImplV2() {
        RemoteAPIController.validateInterfaces(ContentAPIV2.class, ContentInterface.class);
        mapper = new SimpleMapper();

        descriptorMap = new HashMap<String, IconDescriptor>();

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

    public String getIconKey(Icon icon) {
        if (icon instanceof IDIcon) {
            Object id = ((IDIcon) icon).getIdentifier();

            IconDescriptor ret = JSonStorage.convert(id, new TypeRef<IconDescriptor>() {
            });

            String s = JSonStorage.serializeToJson(ret);

            try {
                String cr32 = Hash.getMD5(s.getBytes("UTF-8"));
                s = "kc." + cr32;
                synchronized (descriptorMap) {
                    descriptorMap.put(s, ret);
                }
                return s;
            } catch (Throwable e) {
                throw new WTFException(e);
            }

        }

        return null;
    }

    @Override
    public IconDescriptor getIconDescription(String key) throws InternalApiException {

        if (key.startsWith("kc.")) {
            synchronized (descriptorMap) {
                return descriptorMap.get(key);
            }

        }
        return null;

    }

    @Override
    public void getIcon(RemoteAPIRequest request, RemoteAPIResponse response, String key, int size) throws InternalApiException, APIFileNotFoundException {
        OutputStream out = null;
        try {
            /* we force content type to image/png and allow caching of the image */
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CACHE_CONTROL, "public,max-age=60", false));
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE, "image/png", false));
            if (key.startsWith("{")) {
                IconDescriptor desc = JSonStorage.restoreFromString(key, new TypeRef<IconDescriptor>() {
                });
                if (desc == null) {
                    throw new APIFileNotFoundException();
                }
                BufferedImage ico = IconIO.toBufferedImage(createIcon(desc, size));
                out = RemoteAPI.getOutputStream(response, request, RemoteAPI.gzip(request), false);
                ImageIO.write(ico, "png", out);
            } else if (key.startsWith("kc.")) {
                IconDescriptor desc = getIconDescription(key);
                if (desc == null) {
                    throw new APIFileNotFoundException();
                }
                BufferedImage ico = IconIO.toBufferedImage(createIcon(desc, size));
                out = RemoteAPI.getOutputStream(response, request, RemoteAPI.gzip(request), false);
                ImageIO.write(ico, "png", out);
            } else {
                BufferedImage ico = IconIO.toBufferedImage(new AbstractIcon(key, size));
                out = RemoteAPI.getOutputStream(response, request, RemoteAPI.gzip(request), false);
                ImageIO.write(ico, "png", out);
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

    private Icon createIcon(IconDescriptor desc, int size) throws InternalApiException, StorageException {
        if (desc.getCls() == null && desc.getKey() != null) {
            return NewTheme.I().getIcon(desc.getKey(), size);
        }
        if ("ColMerge".equals(desc.getCls())) {
            Icon[] icons = new Icon[desc.getRsc().size()];
            for (int i = 0; i < desc.getRsc().size(); i++) {
                icons[i] = createIcon(desc.getRsc().get(i), size);
            }
            return new MergedIcon(icons);
        } else if ("Badge".equals(desc.getCls())) {
            if (desc.getRsc().get(0).getCls() == null && desc.getRsc().get(1).getCls() == null) {
                return new BadgeIcon(desc.getRsc().get(0).getKey(), desc.getRsc().get(1).getKey(), size);
            } else {
                return new BadgeIcon(createIcon(desc.getRsc().get(0), size), createIcon(desc.getRsc().get(1), size / 2), 0, 0);
            }
        } else if ("Merge".equals(desc.getCls())) {
            ExtMergedIcon ret = new ExtMergedIcon();

            int orgWidth = ((Number) desc.getPrps().get("width")).intValue();
            int orgHeight = ((Number) desc.getPrps().get("height")).intValue();

            double hfaktor = size / (double) (Math.max(orgHeight, orgWidth));
            for (int i = 0; i < desc.getRsc().size(); i++) {
                int x = 0;
                int y = 0;
                HashMap<String, Object> props = desc.getRsc().get(i).getPrps();

                int orgIconWidth = ((Number) props.get("width")).intValue();
                int orgIconHeight = ((Number) props.get("height")).intValue();
                int newIconSize = (int) (Math.max(orgIconWidth, orgIconHeight) * hfaktor);
                if (props != null) {

                    if (props.containsKey("x")) {
                        x = (int) (((Number) props.get("x")).intValue() * hfaktor);
                    }

                    if (props.containsKey("y")) {
                        y = (int) (((Number) props.get("y")).intValue() * hfaktor);
                    }
                }
                ret.add(createIcon(desc.getRsc().get(i), newIconSize), x, y);
            }
            ret.crop(size, size);

            return ret;

        } else if ("Favit".equals(desc.getCls())) {
            return new FavitIcon(createIcon(desc.getRsc().get(1), size), DomainInfo.getInstance(desc.getRsc().get(0).getKey()));
        }
        throw new InternalApiException(new Exception("Cannot paint " + JSonStorage.serializeToJson(desc)));
    }
}
