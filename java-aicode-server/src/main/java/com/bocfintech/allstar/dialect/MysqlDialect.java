package com.bocfintech.allstar.dialect;

import com.bocfintech.allstar.dialect.model.ColumnDefinition;
import com.bocfintech.allstar.dialect.model.IndexDefinition;
import com.bocfintech.allstar.dialect.model.TableDefinition;
import com.bocfintech.allstar.entity.DbConnectionConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class MysqlDialect implements DatabaseDialect {

    private static final String GROUP = "MYSQL";

    @Override
    public String buildJdbcUrl(DbConnectionConfig config) {
        return String.format(
                "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowMultiQueries=true",
                config.getHost(), config.getPort(), config.getDatabaseName()
        );
    }

    @Override
    public String quote(String name) {
        return "`" + name + "`";
    }

    @Override
    public String getTableInfoListSql(String databaseName) {
        return "SELECT TABLE_NAME, TABLE_COMMENT, " +
                "ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS DATA_LENGTH_MB " +
                "FROM information_schema.TABLES " +
                "WHERE TABLE_SCHEMA = '" + databaseName + "' " +
                "ORDER BY TABLE_NAME";
    }

    @Override
    public String getCreateTableSql(Connection conn, String databaseName, String tableName) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE " + quote(tableName))) {
            if (rs.next()) {
                return rs.getString(2);
            }
        }
        return null;
    }

    @Override
    public String getColumnMetaSql(String databaseName, String tableName) {
        return "SELECT " +
                "COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT, " +
                "COLUMN_KEY, EXTRA, ORDINAL_POSITION, COLUMN_COMMENT " +
                "FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = '" + databaseName + "' AND TABLE_NAME = '" + tableName + "' " +
                "ORDER BY ORDINAL_POSITION";
    }

    @Override
    public String getPrimaryKeyColumnSql(String databaseName, String tableName) {
        return "SELECT COLUMN_NAME " +
                "FROM information_schema.KEY_COLUMN_USAGE " +
                "WHERE TABLE_SCHEMA = '" + databaseName + "' AND TABLE_NAME = '" + tableName + "' AND CONSTRAINT_NAME = 'PRIMARY' " +
                "ORDER BY ORDINAL_POSITION LIMIT 1";
    }

    @Override
    public String getFirstColumnSql(String databaseName, String tableName) {
        return "SELECT COLUMN_NAME FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = '" + databaseName + "' AND TABLE_NAME = '" + tableName + "' " +
                "ORDER BY ORDINAL_POSITION LIMIT 1";
    }

    @Override
    public String getCompatibilityGroup() {
        return GROUP;
    }

    @Override
    public TableDefinition readTableDefinition(Connection conn, String databaseName, String tableName) throws SQLException {
        TableDefinition def = new TableDefinition();
        def.setTableName(tableName);
        def.setTableComment(queryTableComment(conn, databaseName, tableName));

        // 主键
        List<String> pk = new ArrayList<>();
        try (ResultSet rs = conn.getMetaData().getPrimaryKeys(databaseName, null, tableName)) {
            while (rs.next()) {
                pk.add(rs.getString("COLUMN_NAME"));
            }
        }
        def.setPrimaryKeys(pk);

        // 索引（排除主键，主键在 MySQL 中索引名为 PRIMARY）
        Map<String, IndexDefinition> indexMap = new LinkedHashMap<>();
        try (ResultSet rs = conn.getMetaData().getIndexInfo(databaseName, null, tableName, false, false)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                String columnName = rs.getString("COLUMN_NAME");
                if (indexName == null || columnName == null || "PRIMARY".equalsIgnoreCase(indexName)) {
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
        try (ResultSet rs = conn.getMetaData().getColumns(databaseName, null, tableName, null)) {
            while (rs.next()) {
                ColumnDefinition c = new ColumnDefinition();
                String colName = rs.getString("COLUMN_NAME");
                c.setName(colName);
                c.setNullable(!"NO".equalsIgnoreCase(rs.getString("IS_NULLABLE")));
                c.setDefaultValue(rs.getString("COLUMN_DEF"));
                c.setComment(rs.getString("REMARKS"));
                c.setAutoIncrement("YES".equalsIgnoreCase(rs.getString("IS_AUTOINCREMENT")));
                c.setPrimaryKey(pk.contains(colName));
                mapMysqlColumnType(c, rs.getString("TYPE_NAME"),
                        rs.getInt("COLUMN_SIZE"), rs.getInt("DECIMAL_DIGITS"));
                columns.add(c);
            }
        }
        def.setColumns(columns);
        return def;
    }

    @Override
    public List<String> buildCreateTableStatements(TableDefinition def) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(quote(def.getTableName())).append(" (\n");
        List<String> parts = new ArrayList<>();
        for (ColumnDefinition c : def.getColumns()) {
            parts.add("  " + buildColumnDefinition(c));
        }
        if (def.getPrimaryKeys() != null && !def.getPrimaryKeys().isEmpty()) {
            parts.add("  PRIMARY KEY (" + def.getPrimaryKeys().stream()
                    .map(this::quote).collect(Collectors.joining(", ")) + ")");
        }
        if (def.getIndexes() != null) {
            for (IndexDefinition idx : def.getIndexes()) {
                if (idx.getColumns() == null || idx.getColumns().isEmpty()) {
                    continue;
                }
                String cols = idx.getColumns().stream().map(this::quote).collect(Collectors.joining(", "));
                parts.add((idx.isUnique() ? "  UNIQUE KEY " : "  KEY ") + quote(idx.getName()) + " (" + cols + ")");
            }
        }
        sb.append(String.join(",\n", parts));
        sb.append("\n)");
        if (def.getTableComment() != null && !def.getTableComment().trim().isEmpty()) {
            sb.append(" COMMENT='").append(escape(def.getTableComment())).append("'");
        }
        return Collections.singletonList(sb.toString());
    }

    // ==================== 私有方法 ====================

    private String buildColumnDefinition(ColumnDefinition c) {
        StringBuilder sb = new StringBuilder();
        sb.append(quote(c.getName())).append(" ").append(toMysqlType(c));
        // AUTO_INCREMENT 列必须为 NOT NULL
        boolean notNull = !c.isNullable() || c.isAutoIncrement();
        sb.append(notNull ? " NOT NULL" : " NULL");
        String def = normalizeMysqlDefault(c.getDefaultValue());
        if (def != null) {
            sb.append(" DEFAULT ").append(def);
        }
        if (c.isAutoIncrement()) {
            sb.append(" AUTO_INCREMENT");
        }
        if (c.getComment() != null && !c.getComment().trim().isEmpty()) {
            sb.append(" COMMENT '").append(escape(c.getComment())).append("'");
        }
        return sb.toString();
    }

    private String toMysqlType(ColumnDefinition c) {
        String t = c.getTypeName() == null ? "TEXT" : c.getTypeName().toUpperCase(Locale.ROOT);
        switch (t) {
            case "BOOLEAN": return "tinyint(1)";
            case "SMALLINT": return "smallint";
            case "INTEGER": return "int";
            case "BIGINT": return "bigint";
            case "NUMERIC": return buildDecimal(c);
            case "REAL": return "float";
            case "DOUBLE": return "double";
            case "VARCHAR": return "varchar(" + positive(c.getLength(), 255) + ")";
            case "CHAR": return "char(" + positive(c.getLength(), 1) + ")";
            case "BYTEA": return "longblob";
            case "DATE": return "date";
            case "TIME": return "time";
            case "TIMESTAMP": return "datetime";
            case "JSONB": return "json";
            case "BIT": return "bit(1)";
            case "UUID": return "char(36)";
            case "TEXT":
            default: return "longtext";
        }
    }

    private String buildDecimal(ColumnDefinition c) {
        if (c.getScale() != null && c.getScale() > 0) {
            return "decimal(" + positive(c.getLength(), 10) + "," + c.getScale() + ")";
        }
        if (c.getLength() != null && c.getLength() > 0) {
            return "decimal(" + c.getLength() + ")";
        }
        return "decimal(18,0)";
    }

    private int positive(Integer v, int fallback) {
        return (v != null && v > 0) ? v : fallback;
    }

    /**
     * 将（通常来自 PostgreSQL 的）默认值归一化为 MySQL 可接受的写法
     */
    private String normalizeMysqlDefault(String raw) {
        if (raw == null) {
            return null;
        }
        String d = raw.trim();
        if (d.isEmpty()) {
            return null;
        }
        String lower = d.toLowerCase(Locale.ROOT);
        // 序列自增，已由 AUTO_INCREMENT 处理
        if (lower.startsWith("nextval(")) {
            return null;
        }
        if (lower.contains("now()") || lower.equals("current_timestamp")) {
            return "CURRENT_TIMESTAMP";
        }
        if (lower.equals("true")) {
            return "1";
        }
        if (lower.equals("false")) {
            return "0";
        }
        if (lower.equals("null")) {
            return null;
        }
        if (lower.startsWith("b'") && d.endsWith("'")) {
            return d.substring(2, d.length() - 1).equals("1") ? "1" : "0";
        }
        int castIdx = d.indexOf("::");
        if (castIdx > 0) {
            d = d.substring(0, castIdx).trim();
        }
        if (d.startsWith("(") && d.endsWith(")") && d.length() > 1) {
            d = d.substring(1, d.length() - 1).trim();
        }
        if ((d.startsWith("'") && d.endsWith("'")) || (d.startsWith("\"") && d.endsWith("\""))) {
            return d;
        }
        if (d.matches("[-+]?\\d+(\\.\\d+)?")) {
            return d;
        }
        return "'" + escape(d) + "'";
    }

    private void mapMysqlColumnType(ColumnDefinition c, String rawType, int size, int scale) {
        String type = rawType == null ? "" : rawType.toUpperCase(Locale.ROOT);
        switch (type) {
            case "TINYINT":
            case "SMALLINT":
                c.setTypeName("SMALLINT");
                break;
            case "MEDIUMINT":
            case "INT":
            case "INTEGER":
                c.setTypeName("INTEGER");
                break;
            case "BIGINT":
                c.setTypeName("BIGINT");
                break;
            case "DECIMAL":
            case "NUMERIC":
                c.setTypeName("NUMERIC");
                if (size > 0) c.setLength(size);
                if (scale > 0) c.setScale(scale);
                break;
            case "FLOAT":
                c.setTypeName("REAL");
                break;
            case "DOUBLE":
            case "REAL":
                c.setTypeName("DOUBLE");
                break;
            case "BIT":
                c.setTypeName("BIT");
                break;
            case "VARCHAR":
                c.setTypeName("VARCHAR");
                if (size > 0) c.setLength(size);
                break;
            case "CHAR":
                c.setTypeName("CHAR");
                if (size > 0) c.setLength(size);
                break;
            case "TINYTEXT":
            case "TEXT":
            case "MEDIUMTEXT":
            case "LONGTEXT":
                c.setTypeName("TEXT");
                break;
            case "TINYBLOB":
            case "BLOB":
            case "MEDIUMBLOB":
            case "LONGBLOB":
            case "BINARY":
            case "VARBINARY":
                c.setTypeName("BYTEA");
                break;
            case "DATE":
                c.setTypeName("DATE");
                break;
            case "TIME":
                c.setTypeName("TIME");
                break;
            case "DATETIME":
            case "TIMESTAMP":
                c.setTypeName("TIMESTAMP");
                break;
            case "YEAR":
                c.setTypeName("INTEGER");
                break;
            case "JSON":
                c.setTypeName("JSONB");
                break;
            case "ENUM":
                c.setTypeName("VARCHAR");
                c.setLength(255);
                break;
            case "SET":
                c.setTypeName("TEXT");
                break;
            default:
                c.setTypeName("TEXT");
        }
    }

    private String queryTableComment(Connection conn, String databaseName, String tableName) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT TABLE_COMMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?")) {
            ps.setString(1, databaseName);
            ps.setString(2, tableName);
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

    private String escape(String value) {
        return value == null ? null : value.replace("'", "''");
    }
}
