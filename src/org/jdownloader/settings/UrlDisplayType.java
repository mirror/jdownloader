package org.jdownloader.settings;

import org.jdownloader.translate._JDT;

public enum UrlDisplayType {
    // order is important., it is the default order
    CUSTOM {
        public String getTranslatedName() {
            return _JDT.T.UrlDisplayType_CUSTOM();
        }

        public String getTranslatedDescription() {
            return _JDT.T.UrlDisplayType_CUSTOM_description();
        }
    },
    REFERRER {
        public String getTranslatedName() {
            return _JDT.T.UrlDisplayType_REFERRER();
        }

        public String getTranslatedDescription() {
            return _JDT.T.UrlDisplayType_REFERRER_description();
        }
    },
    ORIGIN {
        public String getTranslatedName() {
            return _JDT.T.UrlDisplayType_ORIGIN();
        }

        public String getTranslatedDescription() {
            return _JDT.T.UrlDisplayType_ORIGIN_description();
        }
    },
    CONTAINER {
        public String getTranslatedName() {
            return _JDT.T.UrlDisplayType_CONTAINER();
        }

        public String getTranslatedDescription() {
            return _JDT.T.UrlDisplayType_CONTAINER_description();
        }
    },
    CONTENT {
        public String getTranslatedName() {
            return _JDT.T.UrlDisplayType_CONTENT();
        }

        public String getTranslatedDescription() {
            return _JDT.T.UrlDisplayType_CONTENT_description();
        }
    };

    public String getTranslatedName() {
        return name();
    }

    public String getTranslatedDescription() {
        return null;
    }
}
