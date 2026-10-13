package com.bocfintech.allstar.dialect;

import com.bocfintech.allstar.dialect.model.ColumnDefinition;
import com.bocfintech.allstar.dialect.model.IndexDefinition;
import com.bocfintech.allstar.dialect.model.TableDefinition;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * PostgreSQL 家族（PostgreSQL / GaussDB）共用的方言支持逻辑。
 *
 * <p>PostgreSQL 与 GaussDB 同源，其首列查询、表清单查询、中立模型读取与建表语句生成逻辑一致，
 * 集中在此处以避免重复实现。
 */
final class PostgresFamilySupport {

    /** PostgreSQL 家族兼容分组标识 */
    static final String GROUP = "POSTGRES";

    private PostgresFamilySupport() {
    }

    /**
     * 获取"取第一个字段"的 SQL（无主键时作为排序依据）
     */
    static String getFirstColumnSql(String databaseName, String tableName) {
        return "SELECT a.attname AS column_name " +
                "FROM pg_catalog.pg_attribute a " +
                "JOIN pg_catalog.pg_class c ON c.oid = a.attrelid " +
                "JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace " +
                "WHERE n.nspname = 'public' AND c.relname = '" + tableName + "' " +
                "AND a.attnum > 0 AND NOT a.attisdropped " +
                "ORDER BY a.attnum LIMIT 1";
    }

    /**
     * 获取表清单 SQL（含真实表大小与表注释）
     */
    static String getTableInfoListSql() {
        return "SELECT c.relname AS TABLE_NAME, " +
                "COALESCE(d.description, '') AS TABLE_COMMENT, " +
                "ROUND(pg_total_relation_size(c.oid) / 1024.0 / 1024.0, 2) AS DATA_LENGTH_MB " +
                "FROM pg_catalog.pg_class c " +
                "JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace " +
                "LEFT JOIN pg_catalog.pg_description d ON d.objoid = c.oid AND d.objsubid = 0 " +
                "WHERE n.nspname = 'public' AND c.relkind = 'r' " +
                "AND c.relname NOT LIKE 'pg_%' " +
                "ORDER BY c.relname";
    }

    /**
     * 通过 JDBC 元数据读取表结构为中立模型（schema 固定为 public）
     */
    static TableDefinition readTableDefinition(Connection conn, String databaseName, String tableName) throws SQLException {
        TableDefinition def = new TableDefinition();
        def.setTableName(tableName);
        def.setTableComment(queryTableComment(conn, tableName));

        // 主键
        List<String> pk = new ArrayList<>();
        try (ResultSet rs = conn.getMetaData().getPrimaryKeys(null, "public", tableName)) {
            while (rs.next()) {
                pk.add(rs.getString("COLUMN_NAME"));
            }
        }
        def.setPrimaryKeys(pk);

        // 索引（排除主键索引）
        String pkIndexName = queryPrimaryIndexName(conn, tableName);
        Map<String, IndexDefinition> indexMap = new LinkedHashMap<>();
        try (ResultSet rs = conn.getMetaData().getIndexInfo(null, "public", tableName, false, false)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                String columnName = rs.getString("COLUMN_NAME");
                if (indexName == null || columnName == null) {
                    continue;
                }
                if (pkIndexName != null && pkIndexName.equals(indexName)) {
                    continue;
                }
                boolean unique = !rs.getBoolean("NON_UNIQUE");
                IndexDefinition idx = indexMap.get(indexName);
                if (idx == null) {
                    idx = new IndexDefinition();
                    idx.setName(indexName);
                    idx.setUnique(unique);
                    indexMap.put(indexName, idx);
                }
                idx.getColumns().add(columnName);
            }
        } catch (SQLException ignore) {
            // 某些对象不支持索引元数据，忽略
        }
        def.setIndexes(new ArrayList<>(indexMap.values()));

        // 列
        List<ColumnDefinition> columns = new ArrayList<>();
        try (ResultSet rs = conn.getMetaData().getColumns(null, "public", tableName, null)) {
            while (rs.next()) {
                ColumnDefinition c = new ColumnDefinition();
                String colName = rs.getString("COLUMN_NAME");
                c.setName(colName);
                c.setNullable(!"NO".equalsIgnoreCase(rs.getString("IS_NULLABLE")));
                c.setDefaultValue(rs.getString("COLUMN_DEF"));
                c.setComment(rs.getString("REMARKS"));
                c.setAutoIncrement("YES".equalsIgnoreCase(rs.getString("IS_AUTOINCREMENT")));
                c.setPrimaryKey(pk.contains(colName));
                mapPgColumnType(c, rs.getString("TYPE_NAME"),
                        rs.getInt("COLUMN_SIZE"), rs.getInt("DECIMAL_DIGITS"));
                columns.add(c);
            }
        }
        def.setColumns(columns);
        return def;
    }

    /**
     * 依据中立模型生成 PostgreSQL 建表语句（可能包含多条：建表、表/列注释、索引）
     */
    static List<String> buildCreateTableStatements(TableDefinition def) {
        List<String> statements = new ArrayList<>();

        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE \"").append(def.getTableName()).append("\" (\n");
        List<String> parts = new ArrayList<>();
        for (ColumnDefinition c : def.getColumns()) {
            parts.add("  " + buildColumnDefinition(c));
        }
        if (def.getPrimaryKeys() != null && !def.getPrimaryKeys().isEmpty()) {
            parts.add("  PRIMARY KEY (" + def.getPrimaryKeys().stream()
                    .map(PostgresFamilySupport::quoteIdent).collect(Collectors.joining(", ")) + ")");
        }
        sb.append(String.join(",\n", parts));
        sb.append("\n)");
        statements.add(sb.toString());

        if (def.getTableComment() != null && !def.getTableComment().trim().isEmpty()) {
            statements.add("COMMENT ON TABLE \"" + def.getTableName() + "\" IS '"
                    + escape(def.getTableComment()) + "'");
        }
        for (ColumnDefinition c : def.getColumns()) {
            if (c.getComment() != null && !c.getComment().trim().isEmpty()) {
                statements.add("COMMENT ON COLUMN \"" + def.getTableName() + "\".\""
                        + c.getName() + "\" IS '" + escape(c.getComment()) + "'");
            }
        }

        if (def.getIndexes() != null) {
            for (IndexDefinition idx : def.getIndexes()) {
                if (idx.getColumns() == null || idx.getColumns().isEmpty()) {
                    continue;
                }
                String cols = idx.getColumns().stream()
                        .map(PostgresFamilySupport::quoteIdent).collect(Collectors.joining(", "));
                String prefix = idx.isUnique() ? "CREATE UNIQUE INDEX " : "CREATE INDEX ";
                statements.add(prefix + "\"" + idx.getName() + "\" ON \"" + def.getTableName()
                        + "\" (" + cols + ")");
            }
        }
        return statements;
    }

    // ==================== 私有方法 ====================

    private static String buildColumnDefinition(ColumnDefinition c) {
        StringBuilder sb = new StringBuilder();
        sb.append(quoteIdent(c.getName())).append(" ").append(toPgType(c));
        if (!c.isNullable()) {
            sb.append(" NOT NULL");
        }
        if (c.isAutoIncrement()) {
            sb.append(" GENERATED BY DEFAULT AS IDENTITY");
        } else {
            String def = normalizePgDefault(c.getDefaultValue());
            if (def != null) {
                sb.append(" DEFAULT ").append(def);
            }
        }
        return sb.toString();
    }

    private static String toPgType(ColumnDefinition c) {
        String t = c.getTypeName() == null ? "TEXT" : c.getTypeName().toUpperCase(Locale.ROOT);
        switch (t) {
            case "BOOLEAN": return "boolean";
            case "SMALLINT": return "smallint";
            case "INTEGER": return "integer";
            case "BIGINT": return "bigint";
            case "NUMERIC": return buildNumeric(c);
            case "REAL": return "real";
            case "DOUBLE": return "double precision";
            case "VARCHAR": return "varchar(" + positive(c.getLength(), 255) + ")";
            case "CHAR": return "char(" + positive(c.getLength(), 1) + ")";
            case "BYTEA": return "bytea";
            case "DATE": return "date";
            case "TIME": return "time";
            case "TIMESTAMP": return "timestamp";
            case "JSONB": return "jsonb";
            case "BIT": return "bit(1)";
            case "UUID": return "uuid";
            case "TEXT":
            default: return "text";
        }
    }

    private static String buildNumeric(ColumnDefinition c) {
        if (c.getScale() != null && c.getScale() > 0) {
            return "numeric(" + positive(c.getLength(), 10) + "," + c.getScale() + ")";
        }
        if (c.getLength() != null && c.getLength() > 0) {
            return "numeric(" + c.getLength() + ")";
        }
        return "numeric";
    }

    private static int positive(Integer v, int fallback) {
        return (v != null && v > 0) ? v : fallback;
    }

    /**
     * 将（通常来自 MySQL 的）默认值归一化为 PostgreSQL 可接受的写法
     */
    private static String normalizePgDefault(String raw) {
        if (raw == null) {
            return null;
        }
        String d = raw.trim();
        if (d.isEmpty() || "null".equalsIgnoreCase(d)) {
            return null;
        }
        String lower = d.toLowerCase(Locale.ROOT);
        // MySQL 零值日期，PostgreSQL 不支持
        if (lower.startsWith("'0000-00-00")) {
            return null;
        }
        // MySQL 位字面量 b'0' / b'1'
        if (lower.startsWith("b'") && d.endsWith("'")) {
            return d.substring(2, d.length() - 1).equals("1") ? "true" : "false";
        }
        if (lower.equals("current_timestamp") || lower.endsWith("()")) {
            return "CURRENT_TIMESTAMP";
        }
        if (d.startsWith("'") && d.endsWith("'")) {
            return d;
        }
        if (d.matches("[-+]?\\d+(\\.\\d+)?")) {
            return d;
        }
        return "'" + escape(d) + "'";
    }

    private static void mapPgColumnType(ColumnDefinition c, String rawType, int size, int scale) {
        String type = rawType == null ? "" : rawType.toLowerCase(Locale.ROOT);
        switch (type) {
            case "varchar":
                c.setTypeName("VARCHAR");
                if (size > 0) c.setLength(size);
                break;
            case "bpchar":
            case "char":
                c.setTypeName("CHAR");
                if (size > 0) c.setLength(size);
                break;
            case "text":
                c.setTypeName("TEXT");
                break;
            case "int2":
                c.setTypeName("SMALLINT");
                break;
            case "int4":
                c.setTypeName("INTEGER");
                break;
            case "int8":
                c.setTypeName("BIGINT");
                break;
            case "numeric":
            case "decimal":
                c.setTypeName("NUMERIC");
                if (size > 0) c.setLength(size);
                if (scale > 0) c.setScale(scale);
                break;
            case "float4":
                c.setTypeName("REAL");
                break;
            case "float8":
                c.setTypeName("DOUBLE");
                break;
            case "bool":
            case "boolean":
                c.setTypeName("BOOLEAN");
                break;
            case "date":
                c.setTypeName("DATE");
                break;
            case "time":
            case "timetz":
                c.setTypeName("TIME");
                break;
            case "timestamp":
            case "timestamptz":
                c.setTypeName("TIMESTAMP");
                break;
            case "json":
            case "jsonb":
                c.setTypeName("JSONB");
                break;
            case "bytea":
                c.setTypeName("BYTEA");
                break;
            case "uuid":
                c.setTypeName("UUID");
                break;
            case "bit":
            case "varbit":
                c.setTypeName("BIT");
                break;
            default:
                c.setTypeName("TEXT");
        }
    }

    private static String queryTableComment(Connection conn, String tableName) {
        String sql = "SELECT COALESCE(d.description, '') FROM pg_catalog.pg_class c " +
                "JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace " +
                "LEFT JOIN pg_catalog.pg_description d ON d.objoid = c.oid AND d.objsubid = 0 " +
                "WHERE n.nspname = 'public' AND c.relname = ? AND c.relkind = 'r'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (SQLException ignore) {
            // 忽略
        }
        return null;
    }

    private static String queryPrimaryIndexName(Connection conn, String tableName) {
        String sql = "SELECT i.relname FROM pg_catalog.pg_index ix " +
                "JOIN pg_catalog.pg_class i ON i.oid = ix.indexrelid " +
                "JOIN pg_catalog.pg_class t ON t.oid = ix.indrelid " +
                "JOIN pg_catalog.pg_namespace n ON n.oid = t.relnamespace " +
                "WHERE ix.indisprimary AND n.nspname = 'public' AND t.relname = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (SQLException ignore) {
            // 忽略
        }
        return null;
    }

    private static String quoteIdent(String name) {
        return "\"" + name + "\"";
    }

    private static String escape(String value) {
        return value == null ? null : value.replace("'", "''");
    }
}
