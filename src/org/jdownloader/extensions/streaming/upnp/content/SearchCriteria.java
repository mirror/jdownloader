package org.jdownloader.extensions.streaming.upnp.content;

/**
 * Search Criterion
 */
public class SearchCriteria {
    // Search Type
    private SearchType searchType;

    public SearchType getSearchType() {
        return searchType;
    }

    public void setSearchType(SearchType searchType) {
        this.searchType = searchType;
    }

    /**
     * Parse Search Criterion from String TODO: Not Completed
     * 
     * @param searchCriterionString
     *            Search Criterion String
     * @return Search Criterion
     */
    public static SearchCriteria create(String searchCriterionString) {
        SearchCriteria searchCriterion = new SearchCriteria();
        if (searchCriterionString != null) {
            String[] factors = searchCriterionString.split("(and|or)");
            for (String factor : factors) {
                factor = factor.trim();
                String[] subFactors = factor.split("\\s", 3);
                if (subFactors != null && subFactors.length == 3) {
                    if ("upnp:class".equalsIgnoreCase(subFactors[0]) && ("=".equalsIgnoreCase(subFactors[1]) || "derivedfrom".equalsIgnoreCase(subFactors[1]))) {
                        if ("\"object.item.imageItem\"".equalsIgnoreCase(subFactors[2]) || "\"object.item.imageItem.photo\"".equalsIgnoreCase(subFactors[2])) {
                            searchCriterion.setSearchType(SearchCriteria.SearchType.SEARCH_IMAGE);
                        } else if ("\"object.item.videoItem\"".equalsIgnoreCase(subFactors[2])) {
                            searchCriterion.setSearchType(SearchCriteria.SearchType.SEARCH_VIDEO);
                        } else if ("\"object.container.playlistContainer\"".equalsIgnoreCase(subFactors[2])) {
                            searchCriterion.setSearchType(SearchCriteria.SearchType.SEARCH_PLAYLIST);
                        } else {
                            searchCriterion.setSearchType(SearchCriteria.SearchType.SEARCH_UNKNOWN);
                        }
                    }
                }
            }
        }
        return searchCriterion;
    }

    /**
     * Search Type TODO: Not Completed
     */
    public enum SearchType {
        SEARCH_IMAGE,
        SEARCH_VIDEO,
        SEARCH_PLAYLIST,
        SEARCH_UNKNOWN
    }
}
