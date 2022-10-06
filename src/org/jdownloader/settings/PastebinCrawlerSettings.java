package org.jdownloader.settings;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;

public interface PastebinCrawlerSettings extends ConfigInterface {
    final String text_PastebinPlaintextCrawlMode = "Pastebin crawlers: Define when pastebin text should be added as separate .txt file";

    public static class TRANSLATION {
        public String getPastebinPlaintextCrawlMode_label() {
            return text_PastebinPlaintextCrawlMode;
        }
    }

    public static enum PastebinCrawlMode implements LabelInterface {
        ALWAYS {
            @Override
            public String getLabel() {
                return "Always";
            }
        },
        ONLY_IF_NO_HTTP_URLS_WERE_FOUND {
            @Override
            public String getLabel() {
                return "Only if no http URLs were found";
            }
        },
        NEVER {
            @Override
            public String getLabel() {
                return "Never";
            }
        };
    }

    public static enum PastebinPlaintextCrawlMode implements LabelInterface {
        GLOBAL {
            @Override
            public String getLabel() {
                return "Use global mode";
            }

            @Override
            public PastebinCrawlMode getCrawlMode() {
                // lookup to global PastebinCrawlMode mode
                return PastebinCrawlMode.ALWAYS;
            }
        },
        ALWAYS {
            @Override
            public String getLabel() {
                return "Always";
            }

            @Override
            public PastebinCrawlMode getCrawlMode() {
                return PastebinCrawlMode.ALWAYS;
            }
        },
        ONLY_IF_NO_HTTP_URLS_WERE_FOUND {
            @Override
            public String getLabel() {
                return "Only if no http URLs were found";
            }

            @Override
            public PastebinCrawlMode getCrawlMode() {
                return PastebinCrawlMode.ONLY_IF_NO_HTTP_URLS_WERE_FOUND;
            }
        },
        NEVER {
            @Override
            public String getLabel() {
                return "Never";
            }

            @Override
            public PastebinCrawlMode getCrawlMode() {
                return PastebinCrawlMode.NEVER;
            }
        };
        public abstract PastebinCrawlMode getCrawlMode();
    }

    @AboutConfig
    @DefaultEnumValue("GLOBAL")
    @DescriptionForConfigEntry(text_PastebinPlaintextCrawlMode)
    PastebinPlaintextCrawlMode getPastebinPlaintextCrawlMode();

    void setPastebinPlaintextCrawlMode(final PastebinPlaintextCrawlMode mode);
}