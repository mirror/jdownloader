package org.jdownloader.extensions.eventscripter;

import java.util.HashMap;
import java.util.Map.Entry;

import jd.controlling.downloadcontroller.DownloadController;

import org.appwork.storage.SimpleMapper;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.utils.Application;
import org.jdownloader.api.downloads.v2.DownloadLinkAPIStorableV2;
import org.jdownloader.api.downloads.v2.FilePackageAPIStorableV2;

public enum EventTrigger implements LabelInterface {
    ON_DOWNLOAD_CONTROLLER_START {
        @Override
        public String getLabel() {
            return T._.ON_DOWNLOAD_CONTROLLER_START();
        }

        public HashMap<String, Object> getTestProperties() {
            HashMap<String, Object> ret = new HashMap<String, Object>();
            ret.put("link", new DownloadLinkAPIStorableV2());
            ret.put("package", new FilePackageAPIStorableV2());
            return ret;
        }

        public String getAPIDescription() {
            StringBuilder sb = new StringBuilder();
            sb.append(T._.properties_for_eventtrigger(getLabel())).append("\r\n");
            sb.append(T._.downloadLink()).append("\r\n");
            SimpleMapper mapper = new SimpleMapper();
            for (Entry<String, Object> es : mapper.convert(new DownloadLinkAPIStorableV2(), TypeRef.HASHMAP).entrySet()) {
                sb.append("link." + es.getKey() + ";").append("\r\n");
            }
            sb.append(T._.filepackage()).append("\r\n");
            for (Entry<String, Object> es : mapper.convert(new FilePackageAPIStorableV2(), TypeRef.HASHMAP).entrySet()) {
                sb.append("package." + es.getKey() + ";").append("\r\n");
            }

            return sb.toString();
        }
    },
    ON_DOWNLOAD_CONTROLLER_STOPPED {
        @Override
        public String getLabel() {
            return T._.ON_DOWNLOAD_CONTROLLER_STOPPED();
        }

        public HashMap<String, Object> getTestProperties() {
            return ON_DOWNLOAD_CONTROLLER_START.getTestProperties();
        }

        public String getAPIDescription() {
            return ON_DOWNLOAD_CONTROLLER_START.getAPIDescription();
        }

    },

    ON_JDOWNLOADER_STARTED {
        @Override
        public String getLabel() {
            return T._.ON_JDOWNLOADER_STARTED();
        }

        public HashMap<String, Object> getTestProperties() {
            return NONE.getTestProperties();
        }

        public String getAPIDescription() {
            return NONE.getAPIDescription();
        }

    },
    NONE {
        @Override
        public String getLabel() {
            return T._.NONE();
        }

    },
    ON_OUTGOING_REMOTE_API_EVENT {
        @Override
        public String getLabel() {
            return T._.ON_OUTGOING_REMOTE_API_EVENT();
        }

        public HashMap<String, Object> getTestProperties() {
            HashMap<String, Object> props = new HashMap<String, Object>();
            props.put("eventID", "RUNNING");
            props.put("eventData", null);
            props.put("eventPublisher", "downloadwatchdog");
            return props;
        }

        public String getAPIDescription() {
            StringBuilder sb = new StringBuilder();
            sb.append(T._.properties_for_eventtrigger(getLabel())).append("\r\n");
            sb.append("var myString=eventID;/*NEW|EXPIRED|...*/").append("\r\n");
            sb.append("var myString=eventPublisher; /*downloadwatchdog|dialogs|captchas|...*/").append("\r\n");
            sb.append("var myObject=eventData;/*additional data like the dialog id in case of dialog events*/").append("\r\n");

            return sb.toString();
        }
    },
    ON_NEW_FILE {
        @Override
        public String getLabel() {
            return T._.ON_NEW_FILE();
        }

        public HashMap<String, Object> getTestProperties() {
            HashMap<String, Object> props = new HashMap<String, Object>();

            props.put("files", new String[] { Application.getResource("license.txt").getAbsolutePath() });
            props.put("caller", DownloadController.class.getName());

            return props;
        }

        public String getAPIDescription() {
            StringBuilder sb = new StringBuilder();
            sb.append(T._.properties_for_eventtrigger(getLabel())).append("\r\n");
            sb.append("var myStringArray=files;").append("\r\n");
            sb.append("var myString=caller; /*Who created the files*/").append("\r\n");

            return sb.toString();
        }
    };

    public String getAPIDescription() {
        return T._.none_trigger();
    }

    public HashMap<String, Object> getTestProperties() {
        return new HashMap<String, Object>();
    }

}
