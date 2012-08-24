package org.jdownloader.extensions.streaming.mediaarchive;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;

import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.zip.ZipIOReader;
import org.appwork.utils.zip.ZipIOWriter;
import org.jdownloader.logging.LogController;

public abstract class MediaListController<T extends MediaItem> {

    private MediaListEventSender eventSender;
    private List<T>              list;
    private LogSource            logger;

    public MediaListEventSender getEventSender() {
        return eventSender;
    }

    public void remove(List<T> items) {

        synchronized (list) {
            list.removeAll(items);
        }
        fireContentChanged();
        save();
    }

    protected MediaListController() {
        eventSender = new MediaListEventSender();
        logger = LogController.getInstance().getLogger(getClass().getName());
        list = load();
        if (list == null) list = new ArrayList<T>();

    }

    protected List<T> load() {

        File path = Application.getResource("cfg/medialibrary/" + getType().getSimpleName() + ".zip");

        HashMap<Integer, T> map = new HashMap<Integer, T>();
        if (path != null && path.exists()) {
            ZipIOReader zip = null;
            try {
                zip = new ZipIOReader(path);

                for (ZipEntry entry : zip.getZipFiles()) {
                    if (entry.getName().matches("^\\d+$")) {

                        int index = Integer.parseInt(entry.getName());
                        InputStream is = null;
                        try {
                            is = zip.getInputStream(entry);
                            byte[] bytes = IO.readStream((int) entry.getSize(), is);
                            String json = new String(bytes, "UTF-8");

                            T o = jsonToObject(json);
                            bytes = null;

                            if (o != null) {
                                map.put(index, o);
                            }
                        } finally {
                            try {
                                is.close();
                            } catch (final Throwable e) {
                            }
                        }
                    }
                }
                /* sort positions */
                java.util.List<Integer> positions = new ArrayList<Integer>(map.keySet());
                Collections.sort(positions);
                /* build final ArrayList of Items */
                java.util.List<T> ret2 = new ArrayList<T>(positions.size());
                for (Integer position : positions) {
                    ret2.add(map.get(position));
                }

                map = null;
                positions = null;
                return ret2;
            } catch (final Throwable e) {
                logger.log(e);
            } finally {
                try {
                    zip.close();
                } catch (final Throwable e) {
                }
            }
        }
        return null;

    }

    public List<T> getList() {
        synchronized (list) {
            return new ArrayList<T>(list);
        }
    }

    public void add(T node) {
        synchronized (list) {
            list.add(node);
        }
        fireContentChanged();
        save();
    }

    private boolean save() {
        try {
            synchronized (list) {
                File path = Application.getResource("cfg/medialibrary/" + getType().getSimpleName() + ".zip");
                File tmp = Application.getResource("cfg/medialibrary/" + getType().getSimpleName() + ".zip.tmp");
                // daily backup file. at least until this is stable
                File backup = Application.getResource("cfg/medialibrary/" + getType().getSimpleName() + ".zip.backup." + (System.currentTimeMillis() / (1000l * 24 * 60 * 60)));

                tmp.getParentFile().mkdirs();

                ZipIOWriter zip = new ZipIOWriter(tmp, true);
                int index = 0;
                String format = "%02d";
                if (list.size() >= 10) {
                    format = String.format("%%0%dd", (int) Math.log10(list.size()) + 1);
                }

                for (T mi : list) {
                    String json = objectToJson(mi);
                    if (!StringUtils.isEmpty(json)) {
                        byte[] bytes = json.getBytes("UTF-8");
                        zip.addByteArry(bytes, true, "", String.format(format, (index++)));
                    }
                }

                zip.close();

                backup.delete();
                path.renameTo(backup);
                tmp.renameTo(path);
                return true;
            }
        } catch (Throwable e) {

            logger.log(e);
            return false;
        }
    }

    protected Class<T> getType() {
        final Type superClass = this.getClass().getGenericSuperclass();
        if (superClass instanceof Class) { throw new IllegalArgumentException("Wrong TypeRef Construct"); }
        return (Class<T>) ((ParameterizedType) superClass).getActualTypeArguments()[0];
    }

    protected abstract String objectToJson(T mi);

    protected abstract T jsonToObject(String json);

    private void fireContentChanged() {
        getEventSender().fireEvent(new MediaListEvent(this, MediaListEvent.Type.CONTENT_CHANGED));
    }

}
