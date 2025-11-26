package org.criticizer.dao.helper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Fluent query builder for constructing dynamic SQL queries.
 * Helps eliminate code duplication and ensures safe parameterized queries.
 */
public class QueryBuilder {
    private final StringBuilder query = new StringBuilder();
    private final List<Object> parameters = new ArrayList<>();
    private boolean hasWhere = false;

    public QueryBuilder select(String columns) {
        query.append("SELECT ").append(columns);
        return this;
    }

    public QueryBuilder from(String table) {
        query.append(" FROM ").append(table);
        return this;
    }

    public QueryBuilder join(String table, String condition, Object... params) {
        query.append(" JOIN ").append(table).append(" ON ").append(condition);
        parameters.addAll(Arrays.asList(params));
        return this;
    }

    public QueryBuilder where(String condition, Object... params) {
        if (!hasWhere) {
            query.append(" WHERE ");
            hasWhere = true;
        } else {
            query.append(" AND ");
        }
        query.append(condition);
        Collections.addAll(parameters, params);
        return this;
    }

    public QueryBuilder orderBy(String orderBy) {
        query.append(" ORDER BY ").append(orderBy);
        return this;
    }

    public QueryBuilder limit(int limit, int offset) {
        query.append(" LIMIT ? OFFSET ?");
        parameters.add(limit);
        parameters.add(offset);
        return this;
    }

    public String build() {
        return query.toString();
    }

    public PreparedStatement prepareStatement(Connection conn) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(query.toString());
        for (int i = 0; i < parameters.size(); i++) {
            stmt.setObject(i + 1, parameters.get(i));
        }
        return stmt;
    }

    @Override
    public String toString() {
        return query.toString();
    }
}