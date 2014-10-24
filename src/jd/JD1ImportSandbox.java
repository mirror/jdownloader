package jd;

import java.beans.ExceptionListener;
import java.beans.Expression;
import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.parsers.DocumentBuilderFactory;

import jd.config.DatabaseConnector;
import jd.config.SubConfiguration;
import jd.utils.JDUtilities;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.w3c.dom.Document;

public class JD1ImportSandbox {
    private static final Object[] PARAM_VAR_ARGS_EMPTY_OBJECT = new Object[] {};
    private static final Class[]  PARAM_VAR_ARGS_EMPTY        = new Class[] {};

    public static void run(String home) {
        System.out.println("RUNNING!!!!");

        // ArrayList<SubConfiguration> keys = JDUtilities.getDatabaseConnector().getSubConfigurationKeys();
        //
        // for (SubConfiguration config : keys) {
        // System.out.println(config);
        // }
        try {
            Field JD_HOME = JDUtilities.class.getDeclaredField("JD_HOME");
            JD_HOME.setAccessible(true);
            JD_HOME.set(null, new File(home).getParentFile());
            ArrayList<SubConfiguration> subConfigs;

            subConfigs = (ArrayList<SubConfiguration>) DatabaseConnector.class.getMethod("getSubConfigurationKeys", PARAM_VAR_ARGS_EMPTY).invoke(JDUtilities.getDatabaseConnector(), PARAM_VAR_ARGS_EMPTY_OBJECT);

            // List<FilePackage> links = (List<FilePackage>) DatabaseConnector.class.getMethod("getLinks",
            // PARAM_VAR_ARGS_EMPTY).invoke(JDUtilities.getDatabaseConnector(), PARAM_VAR_ARGS_EMPTY_OBJECT);
            // ArrayList<FilePackageStorable> packages = new ArrayList<FilePackageStorable>();
            // for (FilePackage fp : links) {
            // packages.add(new FilePackageStorable(fp));
            // }
            // File linklist = getFile("cfg/jd1import/linklist", "ejs");
            // linklist.getParentFile().mkdirs();
            // String json = JSonStorage.serializeToJson(packages);
            // ;
            // IO.writeToFile(linklist, Crypto.encrypt(json, JSonStorage.KEY));

            HashSet<String> ignore = new HashSet<String>();
            ignore.add("AccountController");
            ignore.add("update");
            ignore.add("apckage");
            for (SubConfiguration config : subConfigs) {
                if (ignore.contains(config.toString())) {
                    continue;
                }
                Map<String, Object> properties = (Map<String, Object>) SubConfiguration.class.getMethod("getProperties", PARAM_VAR_ARGS_EMPTY).invoke(config, PARAM_VAR_ARGS_EMPTY_OBJECT);
                String json = toJson(properties);
                File file = getFile("cfg/jd1import/sub_" + config.toString(), "json");
                file.getParentFile().mkdirs();
                // String json = JSonStorage.toString(properties);
                IO.writeStringToFile(file, json);
                System.out.println(config);
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    public static String toJson(Map<String, Object> properties) throws UnsupportedEncodingException, Exception {
        try {
            HashMap<String, Object> ret = new HashMap<String, Object>();

            for (Entry<String, Object> es : properties.entrySet()) {
                if (es.getValue() != null) {
                    try {
                        JSonStorage.canStore(es.getValue().getClass(), true);
                        ret.put(es.getKey(), es.getValue());
                    } catch (Exception e) {
                        try {
                            ret.put(es.getKey(), toXML(es.getValue()));
                        } catch (Exception e1) {
                            ret.put(es.getKey(), "String:" + es.getValue().toString());
                        }
                    }
                }

            }
            return JSonStorage.serializeToJson(ret);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static String toXML(final Object value) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final AtomicBoolean b = new AtomicBoolean(false);
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(bos)) {
            @Override
            public void writeExpression(Expression oldExp) {
                super.writeExpression(oldExp);
                try {
                    if (oldExp.getValue() != value) {
                        b.set(true);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        };

        encoder.setExceptionListener(new ExceptionListener() {

            @Override
            public void exceptionThrown(Exception e) {
                e.printStackTrace();
            }
        });
        encoder.writeObject(value);
        encoder.close();

        String xml = new String(bos.toByteArray(), "UTF-8");
        Document doc;

        doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(bos.toByteArray()));

        if (doc.getChildNodes().item(0).getChildNodes().getLength() == 1) {
            throw new Exception("Empty");
        }

        return xml;
    }

    private static File getFile(String pre, String ext) {
        int i = 1;
        File file = Application.getResource(pre + "_" + i + "." + ext);
        while (file.exists()) {
            i++;
            file = Application.getResource(pre + "_" + i + "." + ext);
        }
        return file;
    }
}
