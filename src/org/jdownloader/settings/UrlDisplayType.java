package org.jdownloader.settings;

import org.jdownloader.translate._JDT;

public enum UrlDisplayType {
    REFERRER {
        public String getTranslatedName() {
            return _JDT._.UrlDisplayType_REFERRER();
        }
    },
    ORIGIN {
        public String getTranslatedName() {
            return _JDT._.UrlDisplayType_ORIGIN();
        }
    },
    CONTAINER {
        public String getTranslatedName() {
            return _JDT._.UrlDisplayType_CONTAINER();
        }
    },
    CONTENT {
        public String getTranslatedName() {
            return _JDT._.UrlDisplayType_CONTENT();
        }
    };

    public String getTranslatedName() {
        return name();
    }
}
