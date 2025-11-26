package org.criticizer.dao.helper;

import org.criticizer.exceptions.data.DatabaseException;
import org.criticizer.exceptions.data.ItemAlreadyExistsException;
import org.criticizer.exceptions.data.ItemInUseException;
import org.criticizer.exceptions.data.ResourceNotFoundException;
import org.criticizer.util.DataSourceProvider;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Utility class for managing database transactions in a consistent manner.
 * Provides a method to execute database operations within a transaction
 */
public class DaoHelperService {

    /**
     * Executes a database operation within a transaction, handling commit, rollback, and connection management.
     */
    public void executeInTransaction(TransactionOperation operation, Logger logger) {
        Connection conn = null;
        boolean origAutoCommit = true;
        try {
            conn = DataSourceProvider.getDataSource().getConnection();
            origAutoCommit = conn.getAutoCommit();

            if (origAutoCommit) {
                conn.setAutoCommit(false);
                logger.debug("Transaction started (auto-commit disabled)");
            }

            operation.execute(conn);

            conn.commit();
            logger.debug("Transaction committed successfully");

        } catch (SQLException e) {
            logger.error("SQL error during transaction, attempting rollback", e);
            rollbackTransaction(conn, logger);
            throw new DatabaseException("database operation", e);

        } catch (ItemAlreadyExistsException | ResourceNotFoundException | ItemInUseException e) {
            logger.warn("Business logic validation failed, rolling back: {}", e.getUserMessage());
            rollbackTransaction(conn, logger);
            throw e;

        } catch (RuntimeException e) {
            logger.error("Unexpected error during transaction, rolling back", e);
            rollbackTransaction(conn, logger);

            if (e instanceof DatabaseException) {
                throw e;
            }
            throw new DatabaseException("database operation", e);

        } finally {
            closeConnection(conn, origAutoCommit, logger);
        }
    }

    /**
     * Attempts to rollback a transaction
     */
    private void rollbackTransaction(Connection conn, Logger logger) {
        if (conn != null) {
            try {
                conn.rollback();
                logger.info("Transaction rolled back");
            } catch (SQLException ex) {
                logger.error("Failed to rollback transaction", ex);
            }
        }
    }

    /**
     * Closes the connection and restores auto-commit
     */
    private void closeConnection(Connection conn, boolean originalAutoCommit, Logger logger) {
        if (conn != null) {
            try {
                conn.setAutoCommit(originalAutoCommit);
                logger.debug("AutoCommit restored to: {}", originalAutoCommit);
            } catch (SQLException e) {
                logger.error("Error restoring autoCommit to {} for connection. " +
                        "Connection may be in invalid state!", originalAutoCommit, e);
            } finally {
                try {
                    conn.close();
                    logger.debug("Connection closed");
                } catch (SQLException e) {
                    logger.error("Error closing connection", e);
                }
            }
        }
    }

    @FunctionalInterface
    public interface TransactionOperation {
        /**
         * Executes a database operation within a transaction.
         */
        void execute(Connection conn) throws SQLException;
    }
}