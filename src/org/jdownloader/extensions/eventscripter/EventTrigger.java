package org.jdownloader.extensions.eventscripter;

import java.util.HashMap;
import java.util.Map.Entry;

import org.appwork.storage.SimpleMapper;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.annotations.LabelInterface;
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

    };

    public String getAPIDescription() {
        return T._.none_trigger();
    }

    public HashMap<String, Object> getTestProperties() {
        return new HashMap<String, Object>();
    }

}
