package org.jdownloader.plugins.components.archiveorg;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "archive.org", type = Type.CRAWLER)
public interface ArchiveOrgConfig extends PluginConfigInterface {
    final String                    text_FileCrawlerCrawlOnlyOriginalVersions                    = "File crawler: Download only original versions of files?";
    final String                    text_FileCrawlerCrawlArchiveView                             = "File crawler: Include archive view?";
    final String                    text_FileCrawlerCrawlMetadataFiles                           = "File crawler: Include metadata files (typically .xml & .sqlite files)?";
    final String                    text_PlaylistFilenameScheme                                  = "Playlist filename scheme";
    final String                    text_BookImageQuality                                        = "Book image quality (0 = highest, 10 = lowest)";
    final String                    text_BookCrawlMode                                           = "Book crawl mode";
    final String                    text_PlaylistCrawlMode                                       = "'/details/...' crawler: Audio/video playlist crawl mode";
    final String                    text_MarkNonViewableBookPagesAsOfflineIfNoAccountIsAvailable = "Mark non viewable book pages as offline if no account is available?";
    final String                    text_SearchTermCrawlerMaxResultsLimit                        = "Search term crawler: Limit max results [0 = disable this crawler]";
    public static final TRANSLATION TRANSLATION                                                  = new TRANSLATION();

    public static class TRANSLATION {
        public String getFileCrawlerCrawlOnlyOriginalVersions_label() {
            return text_FileCrawlerCrawlOnlyOriginalVersions;
        }

        public String getFileCrawlerCrawlArchiveView_label() {
            return text_FileCrawlerCrawlArchiveView;
        }

        public String getFileCrawlerCrawlMetadataFiles_label() {
            return text_FileCrawlerCrawlMetadataFiles;
        }

        public String getPlaylistFilenameScheme_label() {
            return text_PlaylistFilenameScheme;
        }

        public String getBookImageQuality_label() {
            return text_BookImageQuality;
        }

        public String getBookCrawlMode_label() {
            return text_BookCrawlMode;
        }

        public String getMarkNonViewableBookPagesAsOfflineIfNoAccountIsAvailable_label() {
            return text_MarkNonViewableBookPagesAsOfflineIfNoAccountIsAvailable;
        }

        public String getPlaylistCrawlMode_label() {
            return text_PlaylistCrawlMode;
        }

        public String getSearchTermCrawlerMaxResultsLimit_label() {
            return text_SearchTermCrawlerMaxResultsLimit;
        }
    }

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry(text_FileCrawlerCrawlOnlyOriginalVersions)
    @Order(10)
    boolean isFileCrawlerCrawlOnlyOriginalVersions();

    void setFileCrawlerCrawlOnlyOriginalVersions(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry(text_FileCrawlerCrawlArchiveView)
    @Order(20)
    boolean isFileCrawlerCrawlArchiveView();

    void setFileCrawlerCrawlArchiveView(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry(text_FileCrawlerCrawlMetadataFiles)
    @Order(25)
    boolean isFileCrawlerCrawlMetadataFiles();

    void setFileCrawlerCrawlMetadataFiles(boolean b);

    public static enum PlaylistFilenameScheme implements LabelInterface {
        PLAYLIST_TITLE_WITH_TRACK_NUMBER {
            @Override
            public String getLabel() {
                return "Like in playlist: <TrackNumber>.<title> - <artist>.<fileExt>";
            }
        },
        ORIGINAL_FILENAME {
            @Override
            public String getLabel() {
                return "Original / serverside filenames";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("PLAYLIST_TITLE_WITH_TRACK_NUMBER")
    @Order(26)
    @DescriptionForConfigEntry(text_BookCrawlMode)
    PlaylistFilenameScheme getPlaylistFilenameScheme();

    void setPlaylistFilenameScheme(final PlaylistFilenameScheme scheme);

    @AboutConfig
    @SpinnerValidator(min = 0, max = 10, step = 1)
    @DefaultIntValue(0)
    @DescriptionForConfigEntry(text_BookImageQuality)
    @Order(30)
    int getBookImageQuality();

    void setBookImageQuality(int i);

    public static enum BookCrawlMode implements LabelInterface {
        PREFER_ORIGINAL {
            @Override
            public String getLabel() {
                return "Original files if possible else loose book pages";
            }
        },
        ORIGINAL_AND_LOOSE_PAGES {
            @Override
            public String getLabel() {
                return "Original files if possible and loose book pages";
            }
        },
        LOOSE_PAGES {
            @Override
            public String getLabel() {
                return "Only loose book pages";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("PREFER_ORIGINAL")
    @Order(40)
    @DescriptionForConfigEntry(text_BookCrawlMode)
    BookCrawlMode getBookCrawlMode();

    void setBookCrawlMode(final BookCrawlMode bookCrawlerMode);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_MarkNonViewableBookPagesAsOfflineIfNoAccountIsAvailable)
    @Order(41)
    boolean isMarkNonViewableBookPagesAsOfflineIfNoAccountIsAvailable();

    void setMarkNonViewableBookPagesAsOfflineIfNoAccountIsAvailable(boolean b);

    public static enum PlaylistCrawlMode implements LabelInterface {
        PLAYLIST_ONLY {
            @Override
            public String getLabel() {
                return "Playlist only";
            }
        },
        PLAYLIST_AND_FILES {
            @Override
            public String getLabel() {
                return "Playlist and files";
            }
        },
        FILES_ONLY {
            @Override
            public String getLabel() {
                return "Files only";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("PLAYLIST_ONLY")
    @Order(50)
    @DescriptionForConfigEntry(text_PlaylistCrawlMode)
    PlaylistCrawlMode getPlaylistCrawlMode();

    void setPlaylistCrawlMode(final PlaylistCrawlMode bookCrawlerMode);

    @AboutConfig
    @SpinnerValidator(min = 0, max = 100000, step = 100)
    @DefaultIntValue(100)
    @DescriptionForConfigEntry(text_SearchTermCrawlerMaxResultsLimit)
    @Order(60)
    int getSearchTermCrawlerMaxResultsLimit();

    void setSearchTermCrawlerMaxResultsLimit(int i);
}