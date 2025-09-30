/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.packagegenerator;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.alter.AlterExpression;
import net.sf.jsqlparser.statement.alter.AlterOperation;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.ForeignKeyIndex;
import net.sf.jsqlparser.statement.create.table.Index;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class SqlFileParser {

    private List<TableInfo> tables;

    public SqlFileParser() {
        this.tables = new ArrayList<>();
    }

    public void parse(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            StringBuilder sqlContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sqlContent.append(line).append("\n");
            }

            // Preprocess SQL to remove Oracle-specific clauses
            String cleanedSql = preprocessSql(sqlContent.toString());

            // Split SQL content into individual statements
            String[] statements = cleanedSql.split(";\\s*");
            for (String stmt : statements) {
                if (stmt.trim().isEmpty()) {
                    continue;
                }

                try {
                    Statement statement = CCJSqlParserUtil.parse(stmt);
                    if (statement instanceof CreateTable) {
                        processCreateTable((CreateTable) statement);
                    } else if (statement instanceof Alter) {
                        processAlterTable((Alter) statement);
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing statement: " + stmt + "\n" + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading file: " + filePath + "\n" + e.getMessage());
        }
    }

    // Preprocess SQL to remove Oracle-specific clauses
    private String preprocessSql(String sql) {
        // Remove TABLESPACE, STORAGE, and related clauses
        String cleaned = sql.replaceAll("(?i)\\s*TABLESPACE\\s+\\w+(\\s+PCTFREE\\s+\\d+)?(\\s+INITRANS\\s+\\d+)?(\\s+MAXTRANS\\s+\\d+)?(\\s+STORAGE\\s*\\(.*?\\))?(\\s+USING\\s+INDEX)?", "");
        // Remove comments
        cleaned = cleaned.replaceAll("(?m)^\\s*--.*$", "");
        return cleaned.trim();
    }

    // Process CREATE TABLE statement
    private void processCreateTable(CreateTable createTable) {
        String tableName = createTable.getTable().getName();
        TableInfo tableInfo = findOrCreateTable(tableName);

        Map<String, ColumnInfo> columnMap = new HashMap<>();
        for (ColumnDefinition column : createTable.getColumnDefinitions()) {
            String colName = column.getColumnName();
            String colType = column.getColDataType().toString();
            boolean isPrimaryKey = column.getColumnSpecs() != null
                    && column.getColumnSpecs().contains("PRIMARY")
                    && column.getColumnSpecs().contains("KEY");
            boolean isUnique = column.getColumnSpecs() != null
                    && column.getColumnSpecs().contains("UNIQUE");
            ColumnInfo columnInfo = new ColumnInfo(colName, colType, isPrimaryKey, isUnique, false);
            tableInfo.addColumn(columnInfo);
            columnMap.put(colName, columnInfo);
        }

        if (createTable.getIndexes() != null) {
            for (Index index : createTable.getIndexes()) {
                List<String> columns = index.getColumnsNames();
                String type = index.getType();
                if ("PRIMARY KEY".equalsIgnoreCase(type)) {
                    for (String colName : columns) {
                        ColumnInfo col = columnMap.get(colName);
                        if (col != null) {
                            col.setPrimaryKey(true);
                        }
                    }
                } else if ("UNIQUE".equalsIgnoreCase(type)) {
                    for (String colName : columns) {
                        ColumnInfo col = columnMap.get(colName);
                        if (col != null) {
                            col.setUnique(true);
                        }
                    }
                } else if ("FOREIGN KEY".equalsIgnoreCase(type)) {
                    ForeignKeyIndex fk = (ForeignKeyIndex) index;
                    for (String colName : columns) {
                        ColumnInfo col = columnMap.get(colName);
                        if (col != null) {
                            col.setForeignKey(true);
                        }
                    }
                    tableInfo.addConstraint(new ConstraintInfo(type, columns,
                            fk.getTable().getName(), fk.getReferencedColumnNames()));
                }
            }
        }
    }

    // Process ALTER TABLE statement
    private void processAlterTable(Alter alter) {
        String tableName = alter.getTable().getName();
        TableInfo tableInfo = findOrCreateTable(tableName);

        if (alter.getAlterExpressions() != null) {
            for (AlterExpression expr : alter.getAlterExpressions()) {
                if (expr.getOperation() == AlterOperation.ADD) {
                    Index index = expr.getIndex();
                    if (index != null) {
                        String constraintType = index.getType();
                        List<String> columns = index.getColumnsNames();
                        String referencedTable = null;
                        List<String> referencedColumns = null;

                        if ("PRIMARY KEY".equalsIgnoreCase(constraintType)) {
                            for (String colName : columns) {
                                ColumnInfo col = findOrCreateColumn(tableInfo, colName, "UNKNOWN");
                                col.setPrimaryKey(true);
                            }
                        } else if ("UNIQUE".equalsIgnoreCase(constraintType)) {
                            for (String colName : columns) {
                                ColumnInfo col = findOrCreateColumn(tableInfo, colName, "UNKNOWN");
                                col.setUnique(true);
                            }
                        } else if ("FOREIGN KEY".equalsIgnoreCase(constraintType)) {
                            ForeignKeyIndex fk = (ForeignKeyIndex) index;
                            referencedTable = fk.getTable().getName();
                            referencedColumns = fk.getReferencedColumnNames();
                            for (String colName : columns) {
                                ColumnInfo col = findOrCreateColumn(tableInfo, colName, "UNKNOWN");
                                col.setForeignKey(true);
                            }
                        }
                        tableInfo.addConstraint(new ConstraintInfo(constraintType, columns,
                                referencedTable, referencedColumns));
                    }
                }
            }
        }
    }

    // Find or create TableInfo
    private TableInfo findOrCreateTable(String tableName) {
        for (TableInfo table : tables) {
            if (table.getTableName().equalsIgnoreCase(tableName)) {
                return table;
            }
        }
        TableInfo newTable = new TableInfo(tableName);
        tables.add(newTable);
        return newTable;
    }

    // Find or create ColumnInfo
    private ColumnInfo findOrCreateColumn(TableInfo tableInfo, String colName, String defaultType) {
        for (ColumnInfo col : tableInfo.getColumns()) {
            if (col.getName().equalsIgnoreCase(colName)) {
                return col;
            }
        }
        ColumnInfo newCol = new ColumnInfo(colName, defaultType, false, false, false);
        tableInfo.addColumn(newCol);
        return newCol;
    }

    public static List<ColumnInfo> getUniqueColumns(TableInfo table) {
        List<ColumnInfo> uniqueColumns = new ArrayList<>();
        for (ColumnInfo column : table.getColumns()) {
            if (column.isUnique) {
                uniqueColumns.add(column);
            }
        }
        return uniqueColumns;
    }
    public static List<ColumnInfo> getPKColumns(TableInfo table) {
        List<ColumnInfo> PKColumns = new ArrayList<>();
        for (ColumnInfo column : table.getColumns()) {
            if (column.isPrimaryKey) {
                PKColumns.add(column);
            }
        }
        return PKColumns;
    }

    public List<TableInfo> getTables() {
        return tables;
    }

    // Inner class for column information
    public static class ColumnInfo {

        private String name;
        private String dataType;
        private boolean isPrimaryKey;
        private boolean isUnique;
        private boolean isForeignKey;

        public ColumnInfo(String name, String dataType, boolean isPrimaryKey,
                boolean isUnique, boolean isForeignKey) {
            this.name = name;
            this.dataType = dataType;
            this.isPrimaryKey = isPrimaryKey;
            this.isUnique = isUnique;
            this.isForeignKey = isForeignKey;
        }

        public String getName() {
            return name;
        }

        public String getDataType() {
            return dataType;
        }

        public boolean isPrimaryKey() {
            return isPrimaryKey;
        }

        public boolean isUnique() {
            return isUnique;
        }

        public boolean isForeignKey() {
            return isForeignKey;
        }

        public void setPrimaryKey(boolean isPrimaryKey) {
            this.isPrimaryKey = isPrimaryKey;
        }

        public void setUnique(boolean isUnique) {
            this.isUnique = isUnique;
        }

        public void setForeignKey(boolean isForeignKey) {
            this.isForeignKey = isForeignKey;
        }

        @Override
        public String toString() {
            return String.format("%s (%s)%s%s%s", name, dataType,
                    isPrimaryKey ? " [PK]" : "",
                    isUnique ? " [UNIQUE]" : "",
                    isForeignKey ? " [FK]" : "");
        }
    }

    // Inner class for table information
    public static class TableInfo {

        public String tableName;
        public List<ColumnInfo> columns;
        public List<ConstraintInfo> constraints;

        public TableInfo(String tableName) {
            this.tableName = tableName;
            this.columns = new ArrayList<>();
            this.constraints = new ArrayList<>();
        }

        public void addColumn(ColumnInfo column) {
            columns.add(column);
        }

        public void addConstraint(ConstraintInfo constraint) {
            constraints.add(constraint);
        }

        public String getTableName() {
            return tableName;
        }

        public List<ColumnInfo> getColumns() {
            return columns;
        }

        public List<ConstraintInfo> getConstraints() {
            return constraints;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Table: ").append(tableName).append("\n");
            sb.append("Columns:\n");
            for (ColumnInfo col : columns) {
                sb.append("  - ").append(col).append("\n");
            }
            sb.append("Constraints:\n");
            for (ConstraintInfo con : constraints) {
                sb.append("  - ").append(con).append("\n");
            }
            return sb.toString();
        }
    }

    // Inner class for constraint information
    public static class ConstraintInfo {

        private String type;
        private List<String> columns;
        private String referencedTable;
        private List<String> referencedColumns;

        public ConstraintInfo(String type, List<String> columns,
                String referencedTable, List<String> referencedColumns) {
            this.type = type;
            this.columns = new ArrayList<>(columns);
            this.referencedTable = referencedTable;
            this.referencedColumns = referencedColumns != null
                    ? new ArrayList<>(referencedColumns) : new ArrayList<>();
        }

        @Override
        public String toString() {
            String base = type + " on " + columns;
            if (referencedTable != null) {
                base += " references " + referencedTable + referencedColumns;
            }
            return base;
        }
    }

    public static void main(String[] args) {
        SqlFileParser parser = new SqlFileParser();
        parser.parse("schema.sql");
        for (TableInfo table : parser.getTables()) {
            System.out.println(table);
        }
    }
}
