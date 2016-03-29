package mil.nga.dice.report;

/**
 * Cache file within a Report
 */
public class ReportCache {

    /**
     * Cache name
     */
    private String name;

    /**
     * Cache file path
     */
    private String path;

    /**
     * Cache shared flag
     */
    private boolean shared;

    /**
     * Constructor
     *
     * @param name
     * @param path
     * @param shared
     */
    public ReportCache(String name, String path, boolean shared) {
        this.name = name;
        this.path = path;
        this.shared = shared;
    }

    /**
     * Get the cache name
     *
     * @return cache name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the cache file path
     *
     * @return cache file path
     */
    public String getPath() {
        return path;
    }

    /**
     * Is the cache shared
     *
     * @return true if shared
     */
    public boolean isShared() {
        return shared;
    }

}
