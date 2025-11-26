package org.criticizer.util;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe singleton provider for HikariCP DataSource.
 */
public class DataSourceProvider {

    private static final Logger log = LoggerFactory.getLogger(DataSourceProvider.class);

    private static final AtomicReference<HikariDataSource> dataSourceRef =
            new AtomicReference<>();

    private DataSourceProvider() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Initializes the DataSource.
     */
    public static void initialize(HikariDataSource dsInstanceToInitialize) {
        if (dsInstanceToInitialize == null) {
            throw new IllegalArgumentException("DataSource cannot be null");
        }

        if (!dataSourceRef.compareAndSet(null, dsInstanceToInitialize)) {
            HikariDataSource existing = dataSourceRef.get();
            if (existing != null && !existing.isClosed()) {
                log.warn("DataSource already initialized. Ignoring second initialization attempt.");

                return;
            }
            dataSourceRef.set(dsInstanceToInitialize);
        }

        log.info("DataSource initialized successfully");
    }

    /**
     * Returns the DataSource instance.
     */
    public static DataSource getDataSource() {
        HikariDataSource ds = dataSourceRef.get();

        if (ds == null) {
            throw new IllegalStateException("DataSource not initialized. " +
                    "Call initialize() first.");
        }

        if (ds.isClosed()) {
            throw new IllegalStateException("DataSource pool has been closed. " +
                    "Cannot obtain connections.");
        }

        return ds;
    }

    /**
     * Closes the DataSource if it exists and is open
     */
    public static void close() {
        HikariDataSource ds = dataSourceRef.getAndSet(null);

        if (ds != null && !ds.isClosed()) {
            log.info("Closing DataSource pool...");
            try {
                ds.close();
                log.info("DataSource closed successfully");
            } catch (Exception e) {
                log.error("Error closing DataSource", e);
            }
        } else if (ds == null) {
            log.debug("DataSource close called, but was never initialized");
        } else {
            log.debug("DataSource close called, but was already closed");
        }
    }

    /**
     * Check if DataSource is initialized and active
     */
    public static boolean isAvailable() {
        HikariDataSource ds = dataSourceRef.get();
        return ds != null && !ds.isClosed();
    }

    
}