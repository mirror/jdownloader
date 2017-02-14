package org.jdownloader.api;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.appwork.exceptions.WTFException;
import org.appwork.remoteapi.DefaultDocsPageFactory;
import org.appwork.remoteapi.SessionRemoteAPI;
import org.appwork.remoteapi.Template;
import org.appwork.remoteapi.annotations.ApiDoc;
import org.appwork.storage.Storable;
import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.jdownloader.myjdownloader.client.json.DeviceErrorType;
import org.jdownloader.myjdownloader.client.json.ServerErrorType;

public class DocsPageFactoryImpl extends DefaultDocsPageFactory {
    public DocsPageFactoryImpl(SessionRemoteAPI<?> api) throws SecurityException, NoSuchMethodException {
        super(api);
    }

    @Override
    protected void addNavigation(HTMLStringBuilder nav, Template template) {
        try {
            template.put("navigation", (IO.readURLToString(DocsPageFactoryImpl.class.getResource("Docs.basics.nav.html"))) + nav.toString());
        } catch (IOException e) {
            throw new WTFException(e);
        }
    }

    @Override
    protected String getDocByField(Field fi) {
        System.out.println(fi);
        String ret = super.getDocByField(fi);
        org.jdownloader.myjdownloader.client.bindings.ApiDoc an = fi.getAnnotation(org.jdownloader.myjdownloader.client.bindings.ApiDoc.class);
        if (an != null && StringUtils.isNotEmpty(an.value())) {
            if (ret == null) {
                ret = an.value();
            } else {
                ret += "\r\n" + an.value();
            }
        }
        return ret;
    }

    @Override
    protected String getDocByMethod(Method m) {
        String ret = super.getDocByMethod(m);
        org.jdownloader.myjdownloader.client.bindings.ApiDoc an = m.getAnnotation(org.jdownloader.myjdownloader.client.bindings.ApiDoc.class);
        if (an != null && StringUtils.isNotEmpty(an.value())) {
            if (ret == null) {
                ret = an.value();
            } else {
                ret += "\r\n" + an.value();
            }
        }
        return ret;
    }

    private String replace(String readURLToString) {
        readURLToString = readURLToString.replace("<!--%%%DeviceErrorType%%%-->", createEnumList(DeviceErrorType.class));
        readURLToString = readURLToString.replace("<!--%%%ServerErrorType%%%-->", createEnumList(ServerErrorType.class));
        return readURLToString;
    }

    private String createEnumList(Class<? extends Enum> num) {
        HTMLStringBuilder content = new HTMLStringBuilder();
        content.comment("Start " + num + "");
        content.append("<ul class='enums'>");
        for (Enum<?> c : num.getEnumConstants()) {
            content.append("<li>");
            Field fi;
            try {
                fi = num.getField(c.name());
                ApiDoc docAnno = fi.getAnnotation(ApiDoc.class);
                if (docAnno != null) {
                    appendDocs(content, docAnno.value());
                }
                if (num == DeviceErrorType.class) {
                    appendDocs(content, "HTTP Response Code: " + ((DeviceErrorType) c).getCode());
                } else if (num == ServerErrorType.class) {
                    appendDocs(content, "HTTP Response Code: " + ((ServerErrorType) c).getCode());
                }
            } catch (NoSuchFieldException e) {
            } catch (Throwable e) {
                e.printStackTrace();
            }
            if (LabelInterface.class.isAssignableFrom(num)) {
                appendDocs(content, ((LabelInterface) c).getLabel());
            }
            content.append(c.name());
            content.append("</li>");
        }
        content.append("</ul>");
        content.comment("End " + num + "");
        return content.toString();
    }

    @Override
    protected void addContent(HTMLStringBuilder content, Template template) {
        try {
            template.put("content", replace(IO.readURLToString(DocsPageFactoryImpl.class.getResource("Docs.basics.html"))) + content.toString());
        } catch (IOException e) {
            throw new WTFException(e);
        }
    }

    protected void addTitle(Template template) {
        template.put("title", "My.JDownloader API Documentation");
    }

    @Override
    protected String readCSSStyle() {
        String ret = super.readCSSStyle();
        // Menu Background
        ret = ret.replace("#1b9bff", "#307782");
        // Drop Menu Darker
        ret = ret.replace("#0787eb", "#175E69");
        // Blue border
        ret = ret.replace("#0082e7", "#00444F");
        ret += "\r\n/*JD Customization*/\r\n";
        try {
            ret += IO.readURLToString(DocsPageFactoryImpl.class.getResource("Docs.style.css"));
        } catch (IOException e) {
            throw new WTFException(e);
        }
        return ret;
    }

    @Override
    protected boolean isAddObjectToHelp(java.lang.reflect.Type returnType) {
        if (Storable.class.isAssignableFrom((Class<?>) returnType) || org.jdownloader.myjdownloader.client.json.JsonFactoryInterface.class.isAssignableFrom((Class<?>) returnType)) {
            return true;
        }
        return false;
    };
}
