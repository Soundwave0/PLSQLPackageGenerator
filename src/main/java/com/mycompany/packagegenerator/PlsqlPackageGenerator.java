/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.packagegenerator;

/**
 *
 * @author kosugek
 */
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import com.mycompany.packagegenerator.SqlFileParser.TableInfo;
import com.mycompany.packagegenerator.SqlFileParser.ColumnInfo;
import com.mycompany.packagegenerator.SqlFileParser.ConstraintInfo;

public class PlsqlPackageGenerator {

    private final TableInfo tableInfo;
    private final String schemaName = "PCDB"; 
    private final String packageName;
    public static String outputSQL;

    public PlsqlPackageGenerator(TableInfo tableInfo) {
        this.tableInfo = tableInfo;
        this.packageName = "p_" + tableInfo.getTableName().toLowerCase().replace(".", "_");
    }

    public void generatePackage(String outputFilePath) {
        outputSQL = "";
        StringBuilder plsql = new StringBuilder();
        List<ColumnInfo> uniqueColumn = SqlFileParser.getUniqueColumns(tableInfo);
        List<ColumnInfo> pkColumn = SqlFileParser.getPKColumns(tableInfo);
        // Package definition
        plsql.append("CREATE OR REPLACE PACKAGE ").append(schemaName).append(".").append(packageName).append(" AS\n\n");

        plsql.append(generateAddFunctionSignature()).append("\n");
        plsql.append(generateModifyFunctionSignature()).append("\n");
        plsql.append(generateRemoveFunctionSignature()).append("\n");
        plsql.append(generateExistsFunctionSignature()).append("\n");
        plsql.append(generateGetFunctionSignature()).append("\n");
        for (ColumnInfo column : uniqueColumn) {
            plsql.append(generateRemoveFunctionUniqueSignature(column)).append("\n");
            plsql.append(generateGetFunctionUniqueSignature(column)).append("\n");
            plsql.append(generateFindFunctionUniqueToPKSignature(column,pkColumn)).append("\n");
        }
        plsql.append("END ").append(packageName).append(";\n/\n");

        plsql.append("CREATE OR REPLACE PACKAGE BODY ").append(schemaName).append(".").append(packageName).append(" AS\n\n");
        // row_equals function
        // plsql.append(generateRowEqualsFunction()).append("\n");
        plsql.append(generateAddFunction()).append("\n");
        plsql.append(generateModifyFunction()).append("\n");
        plsql.append(generateRemoveFunction()).append("\n");
        plsql.append(generateExistsFunction()).append("\n");
        plsql.append(generateGetFunction()).append("\n");
        plsql.append("END ").append(packageName).append(";\n");
        for (ColumnInfo column : uniqueColumn) {
            plsql.append(generateRemoveFunctionUnique(column)).append("\n");
            plsql.append(generateGetFunctionUnique(column)).append("\n");
            plsql.append(generateFindFunctionUniqueToPK(column,pkColumn)).append("\n");
        }
        // Write to file
        outputSQL = plsql.toString();
        try (FileWriter writer = new FileWriter(outputFilePath)) {
            writer.write(plsql.toString());
        } catch (IOException e) {
            System.err.println("Error writing to file: " + outputFilePath + "\n" + e.getMessage());
        }
    }

    private String generateRowEqualsFunctionSignature() {
        StringBuilder func = new StringBuilder();
        func.append("  FUNCTION row_equals(first_row  ").append(schemaName).append(".")
                .append(tableInfo.getTableName()).append("%ROWTYPE,\n")
                .append("                     second_row ").append(schemaName).append(".")
                .append(tableInfo.getTableName()).append("%ROWTYPE)\n")
                .append("    RETURN BOOLEAN;\n");
        return func.toString();
    }

    private String generateRowEqualsFunction() {
        StringBuilder func = new StringBuilder();
        func.append("  FUNCTION row_equals(first_row  ").append(schemaName).append(".")
                .append(tableInfo.getTableName()).append("%ROWTYPE,\n")
                .append("                     second_row ").append(schemaName).append(".")
                .append(tableInfo.getTableName()).append("%ROWTYPE)\n")
                .append("    RETURN BOOLEAN IS\n")
                .append("  BEGIN\n")
                .append("    IF ");

        // Compare all columns
        List<String> comparisons = tableInfo.getColumns().stream()
                .map(col -> "first_row." + col.getName() + " = second_row." + col.getName())
                .collect(Collectors.toList());
        func.append(String.join("\n       AND ", comparisons));

        func.append(" THEN\n")
                .append("      RETURN TRUE;\n")
                .append("    ELSE\n")
                .append("      RETURN FALSE;\n")
                .append("    END IF;\n")
                .append("  EXCEPTION\n")
                .append("    WHEN OTHERS THEN\n")
                .append("      RETURN FALSE;\n")
                .append("  END row_equals;\n");

        return func.toString();
    }

    private String generateAddFunctionSignature() {
        StringBuilder func = new StringBuilder();
        func.append("  FUNCTION add(p_").append(tableInfo.getTableName().toLowerCase().replace(".", "_"))
                .append(" ").append(schemaName).append(".").append(tableInfo.getTableName()).append("%ROWTYPE,\n")
                .append("               p_user VARCHAR2)\n")
                .append("    RETURN NUMBER;");
        return func.toString();
    }

    private String generateAddFunction() {
        StringBuilder func = new StringBuilder();
        func.append("  FUNCTION add(p_").append(tableInfo.getTableName().toLowerCase().replace(".", "_"))
                .append(" ").append(schemaName).append(".").append(tableInfo.getTableName()).append("%ROWTYPE,\n")
                .append("               p_user VARCHAR2)\n")
                .append("    RETURN NUMBER IS\n")
                .append("  BEGIN\n")
                .append("    INSERT INTO ").append(schemaName).append(".").append(tableInfo.getTableName()).append("\n")
                .append("      (");

        // Column names for INSERT
        List<String> columns = tableInfo.getColumns().stream()
                .map(ColumnInfo::getName)
                .collect(Collectors.toList());
        columns.addAll(List.of("create_date", "create_user", "created_by",
                "change_date", "change_user", "changed_by"));
        func.append(String.join(",\n       ", columns)).append(")\n");

        // Values for INSERT
        func.append("    VALUES\n")
                .append("      (");
        List<String> values = tableInfo.getColumns().stream()
                .map(col -> "p_" + tableInfo.getTableName().toLowerCase().replace(".", "_") + "." + col.getName())
                .collect(Collectors.toList());
        values.addAll(List.of("SYSDATE", "p_user", "p_user", "SYSDATE", "p_user", "p_user"));
        func.append(String.join(",\n       ", values)).append(");\n");

        func.append("    RETURN SQL%ROWCOUNT;\n")
                .append("  END add;\n");

        return func.toString();
    }

    private String generateModifyFunctionSignature() {
        StringBuilder func = new StringBuilder();
        func.append("  FUNCTION modify(p_").append(tableInfo.getTableName().toLowerCase().replace(".", "_"))
                .append(" ").append(schemaName).append(".").append(tableInfo.getTableName()).append("%ROWTYPE,\n")
                .append("                  p_user VARCHAR2)\n")
                .append("    RETURN NUMBER;\n");
        return func.toString();
    }

    private String generateModifyFunction() {
        StringBuilder func = new StringBuilder();
        func.append("  FUNCTION modify(p_").append(tableInfo.getTableName().toLowerCase().replace(".", "_"))
                .append(" ").append(schemaName).append(".").append(tableInfo.getTableName()).append("%ROWTYPE,\n")
                .append("                  p_user VARCHAR2)\n")
                .append("    RETURN NUMBER IS\n")
                .append("  BEGIN\n")
                .append("    UPDATE ").append(schemaName).append(".").append(tableInfo.getTableName()).append("\n")
                .append("       SET ");

        // Non-PK columns for UPDATE
        List<String> updates = tableInfo.getColumns().stream()
                .filter(col -> !col.isPrimaryKey())
                .map(col -> col.getName() + " = p_" + tableInfo.getTableName().toLowerCase().replace(".", "_") + "." + col.getName())
                .collect(Collectors.toList());
        updates.addAll(List.of("change_date = SYSDATE", "change_user = p_user", "changed_by = p_user"));
        func.append(String.join(",\n           ", updates));

        // WHERE clause with PK columns
        List<String> pkColumns = tableInfo.getColumns().stream()
                .filter(ColumnInfo::isPrimaryKey)
                .map(col -> col.getName() + " = p_" + tableInfo.getTableName().toLowerCase().replace(".", "_") + "." + col.getName())
                .collect(Collectors.toList());
        if (!pkColumns.isEmpty()) {
            func.append("\n     WHERE ").append(String.join("\n       AND ", pkColumns));
        }

        func.append(";\n")
                .append("    RETURN SQL%ROWCOUNT;\n")
                .append("  END modify;\n");

        return func.toString();
    }

    private String generateRemoveFunctionSignature() {
        StringBuilder func = new StringBuilder();
        List<ColumnInfo> pkColumns = tableInfo.getColumns().stream()
                .filter(ColumnInfo::isPrimaryKey)
                .collect(Collectors.toList());

        // Function signature with PK columns
        func.append("  FUNCTION remove(");
        List<String> params = pkColumns.stream()
                .map(col -> "p_" + col.getName().toLowerCase() + " " + schemaName + "."
                + tableInfo.getTableName() + "." + col.getName() + "%TYPE")
                .collect(Collectors.toList());
        params.add("p_user VARCHAR2");
        func.append(String.join(",\n                  ", params))
                .append(")\n")
                .append("    RETURN NUMBER;\n");
        return func.toString();
    }

    private String generateRemoveFunction() {
        StringBuilder func = new StringBuilder();
        List<ColumnInfo> pkColumns = tableInfo.getColumns().stream()
                .filter(ColumnInfo::isPrimaryKey)
                .collect(Collectors.toList());

        // Function signature with PK columns
        func.append("  FUNCTION remove(");
        List<String> params = pkColumns.stream()
                .map(col -> "p_" + col.getName().toLowerCase() + " " + schemaName + "."
                + tableInfo.getTableName() + "." + col.getName() + "%TYPE")
                .collect(Collectors.toList());
        params.add("p_user VARCHAR2");
        func.append(String.join(",\n                  ", params))
                .append(")\n")
                .append("    RETURN NUMBER IS\n")
                .append("  BEGIN\n")
                .append("    DELETE FROM ").append(schemaName).append(".").append(tableInfo.getTableName()).append("\n");

        // WHERE clause with PK columns
        List<String> conditions = pkColumns.stream()
                .map(col -> col.getName() + " = p_" + col.getName().toLowerCase())
                .collect(Collectors.toList());
        if (!conditions.isEmpty()) {
            func.append("     WHERE ").append(String.join("\n       AND ", conditions));
        }

        func.append(";\n")
                .append("    RETURN SQL%ROWCOUNT;\n")
                .append("  END remove;\n");

        return func.toString();
    }

    private String generateExistsFunctionSignature() {
        StringBuilder func = new StringBuilder();
        List<ColumnInfo> pkColumns = tableInfo.getColumns().stream()
                .filter(ColumnInfo::isPrimaryKey)
                .collect(Collectors.toList());

        // Function signature with PK columns
        func.append("  FUNCTION exists(");
        List<String> params = pkColumns.stream()
                .map(col -> "p_" + col.getName().toLowerCase() + " " + schemaName + "."
                + tableInfo.getTableName() + "." + col.getName() + "%TYPE")
                .collect(Collectors.toList());
        func.append(String.join(",\n                  ", params))
                .append(")\n")
                .append("    RETURN BOOLEAN;\n");
        return func.toString();
    }

    private String generateExistsFunction() {
        StringBuilder func = new StringBuilder();
        List<ColumnInfo> pkColumns = tableInfo.getColumns().stream()
                .filter(ColumnInfo::isPrimaryKey)
                .collect(Collectors.toList());

        // Function signature with PK columns
        func.append("  FUNCTION exists(");
        List<String> params = pkColumns.stream()
                .map(col -> "p_" + col.getName().toLowerCase() + " " + schemaName + "."
                + tableInfo.getTableName() + "." + col.getName() + "%TYPE")
                .collect(Collectors.toList());
        func.append(String.join(",\n                  ", params))
                .append(")\n")
                .append("    RETURN BOOLEAN IS\n")
                .append("    v_count NUMBER;\n")
                .append("  BEGIN\n")
                .append("    SELECT COUNT(*)\n")
                .append("      INTO v_count\n")
                .append("      FROM ").append(schemaName).append(".").append(tableInfo.getTableName()).append(" table_alias\n");

        // WHERE clause with PK columns
        List<String> conditions = pkColumns.stream()
                .map(col -> "table_alias." + col.getName() + " = p_" + col.getName().toLowerCase())
                .collect(Collectors.toList());
        if (!conditions.isEmpty()) {
            func.append("     WHERE ").append(String.join("\n       AND ", conditions))
                    .append("\n       AND rownum = 1");
        }

        func.append(";\n")
                .append("    RETURN v_count > 0;\n")
                .append("  END exists;\n");

        return func.toString();
    }

    private String generateGetFunctionSignature() {
        StringBuilder func = new StringBuilder();
        List<ColumnInfo> pkColumns = tableInfo.getColumns().stream()
                .filter(ColumnInfo::isPrimaryKey)
                .collect(Collectors.toList());

        // Function signature with PK columns
        func.append("  FUNCTION get(");
        List<String> params = pkColumns.stream()
                .map(col -> "p_" + col.getName().toLowerCase() + " " + schemaName + "."
                + tableInfo.getTableName() + "." + col.getName() + "%TYPE")
                .collect(Collectors.toList());
        func.append(String.join(",\n               ", params))
                .append(")\n")
                .append("    RETURN ").append(schemaName).append(".").append(tableInfo.getTableName()).append("%ROWTYPE;\n");
        return func.toString();
    }

    private String generateGetFunction() {
        StringBuilder func = new StringBuilder();
        List<ColumnInfo> pkColumns = tableInfo.getColumns().stream()
                .filter(ColumnInfo::isPrimaryKey)
                .collect(Collectors.toList());

        // Function signature with PK columns
        func.append("  FUNCTION get(");
        List<String> params = pkColumns.stream()
                .map(col -> "p_" + col.getName().toLowerCase() + " " + schemaName + "."
                + tableInfo.getTableName() + "." + col.getName() + "%TYPE")
                .collect(Collectors.toList());
        func.append(String.join(",\n               ", params))
                .append(")\n")
                .append("    RETURN ").append(schemaName).append(".").append(tableInfo.getTableName()).append("%ROWTYPE IS\n")
                .append("    v_result ").append(schemaName).append(".").append(tableInfo.getTableName()).append("%ROWTYPE;\n")
                .append("  BEGIN\n")
                .append("    SELECT *\n")
                .append("      INTO v_result\n")
                .append("      FROM ").append(schemaName).append(".").append(tableInfo.getTableName()).append(" table_alias\n");

        // WHERE clause with PK columns
        List<String> conditions = pkColumns.stream()
                .map(col -> "table_alias." + col.getName() + " = p_" + col.getName().toLowerCase())
                .collect(Collectors.toList());
        if (!conditions.isEmpty()) {
            func.append("     WHERE ").append(String.join("\n       AND ", conditions))
                    .append("\n       AND rownum = 1");
        }

        func.append(";\n")
                .append("    RETURN v_result;\n")
                .append("  END get;\n");

        return func.toString();
    }

    // add function to find the ones with unique data type and present in menu
    private String generateRemoveFunctionUnique(ColumnInfo column) {
        StringBuilder func = new StringBuilder();
        List<ColumnInfo> uniqueColumns = new ArrayList<>();
        uniqueColumns.add(column);

        // Function signature with PK columns
        func.append("  FUNCTION remove_by_")
                .append(column.getName())
                .append("(");
        List<String> params = uniqueColumns.stream()
                .map(col -> "p_" + col.getName().toLowerCase() + " " + schemaName + "."
                + tableInfo.getTableName() + "." + col.getName() + "%TYPE")
                .collect(Collectors.toList());
        params.add("p_user VARCHAR2");
        func.append(String.join(",\n                  ", params))
                .append(")\n")
                .append("    RETURN NUMBER IS\n")
                .append("  BEGIN\n")
                .append("    DELETE FROM ").append(schemaName).append(".").append(tableInfo.getTableName()).append("\n");

        // WHERE clause with PK columns
        List<String> conditions = uniqueColumns.stream()
                .map(col -> col.getName() + " = p_" + col.getName().toLowerCase())
                .collect(Collectors.toList());
        if (!conditions.isEmpty()) {
            func.append("     WHERE ").append(String.join("\n       AND ", conditions));
        }

        func.append(";\n")
                .append("    RETURN SQL%ROWCOUNT;\n")
                .append("  END remove;\n");

        return func.toString();
    }

    private String generateRemoveFunctionUniqueSignature(ColumnInfo column) {
        StringBuilder func = new StringBuilder();
        List<ColumnInfo> uniqueColumns = new ArrayList<>();
        uniqueColumns.add(column);

        // Function signature with PK columns
        func.append("  FUNCTION remove_by_")
                .append(column.getName())
                .append("(");
        List<String> params = uniqueColumns.stream()
                .map(col -> "p_" + col.getName().toLowerCase() + " " + schemaName + "."
                + tableInfo.getTableName() + "." + col.getName() + "%TYPE")
                .collect(Collectors.toList());
        params.add("p_user VARCHAR2");
        func.append(String.join(",\n                  ", params))
                .append(")\n")
                .append("    RETURN NUMBER;");
        return func.toString();
    }

    private String generateGetFunctionUniqueSignature(ColumnInfo column) {
        StringBuilder func = new StringBuilder();
        List<ColumnInfo> uniqueColumn = new ArrayList<>();
        uniqueColumn.add(column);
        // Function signature with PK columns
        func.append("  FUNCTION get_by_")
                .append(column.getName())
                .append("(");
        List<String> params = uniqueColumn.stream()
                .map(col -> "p_" + col.getName().toLowerCase() + " " + schemaName + "."
                + tableInfo.getTableName() + "." + col.getName() + "%TYPE")
                .collect(Collectors.toList());
        func.append(String.join(",\n               ", params))
                .append(")\n")
                .append("    RETURN ").append(schemaName).append(".").append(tableInfo.getTableName()).append("%ROWTYPE;\n");
        return func.toString();
    }

    private String generateGetFunctionUnique(ColumnInfo column) {
        StringBuilder func = new StringBuilder();
        List<ColumnInfo> uniqueColumn = new ArrayList<>();
        uniqueColumn.add(column);

        func.append("  FUNCTION get_by_")
                .append(column.getName())
                .append("(");

        List<String> params = uniqueColumn.stream()
                .map(col -> "p_" + col.getName().toLowerCase() + " " + schemaName + "."
                + tableInfo.getTableName() + "." + col.getName() + "%TYPE")
                .collect(Collectors.toList());
        func.append(String.join(",\n               ", params))
                .append(")\n")
                .append("    RETURN ").append(schemaName).append(".").append(tableInfo.getTableName()).append("%ROWTYPE IS\n")
                .append("    v_result ").append(schemaName).append(".").append(tableInfo.getTableName()).append("%ROWTYPE;\n")
                .append("  BEGIN\n")
                .append("    SELECT *\n")
                .append("      INTO v_result\n")
                .append("      FROM ").append(schemaName).append(".").append(tableInfo.getTableName()).append(" table_alias\n");

        List<String> conditions = uniqueColumn.stream()
                .map(col -> "table_alias." + col.getName() + " = p_" + col.getName().toLowerCase())
                .collect(Collectors.toList());
        if (!conditions.isEmpty()) {
            func.append("     WHERE ").append(String.join("\n       AND ", conditions))
                    .append("\n       AND rownum = 1");
        }

        func.append(";\n")
                .append("    RETURN v_result;\n")
                .append("  END get;\n");

        return func.toString();
    }

    private String generateFindFunctionUniqueToPK(ColumnInfo unique_column, List<ColumnInfo> pk_columns) {
        StringBuilder func = new StringBuilder();

        // Function signature with unique column
        func.append("  FUNCTION find_pk_by_")
                .append(unique_column.getName().toLowerCase())
                .append("(");

        // Generate parameter for unique column
        List<String> params = new ArrayList<>();
        params.add("p_" + unique_column.getName().toLowerCase() + " " + schemaName + "."
                + tableInfo.getTableName() + "." + unique_column.getName() + "%TYPE");

        func.append(String.join(",\n               ", params))
                .append(")\n");

        // Determine return type based on whether PK is composite
        String returnType;
        if (pk_columns.size() > 1) {
            returnType = schemaName + "." + tableInfo.getTableName() + "%ROWTYPE";
        } else {
            returnType = schemaName + "." + tableInfo.getTableName() + "."
                    + pk_columns.get(0).getName() + "%TYPE";
        }

        func.append("    RETURN ").append(returnType).append(" IS\n")
                .append("    v_result ").append(returnType).append(";\n")
                .append("  BEGIN\n");

        // SELECT clause with PK columns
        List<String> selectColumns = pk_columns.stream()
                .map(col -> "table_alias." + col.getName())
                .collect(Collectors.toList());

        func.append("    SELECT ").append(String.join(", ", selectColumns)).append("\n")
                .append("      INTO v_result\n")
                .append("      FROM ").append(schemaName).append(".").append(tableInfo.getTableName())
                .append(" table_alias\n");

        // WHERE clause with unique column
        List<String> conditions = new ArrayList<>();
        conditions.add("table_alias." + unique_column.getName() + " = p_"
                + unique_column.getName().toLowerCase());

        if (!conditions.isEmpty()) {
            func.append("     WHERE ").append(String.join("\n       AND ", conditions));
        }

        func.append(";\n")
                .append("    RETURN v_result;\n")
                .append("  EXCEPTION\n")
                .append("    WHEN NO_DATA_FOUND THEN\n")
                .append("      RETURN NULL;\n")
                .append("    WHEN TOO_MANY_ROWS THEN\n")
                .append("      RAISE_APPLICATION_ERROR(-20001, 'Multiple rows found for unique constraint');\n")
                .append("  END find_pk_by_").append(unique_column.getName().toLowerCase()).append(";\n");

        return func.toString();
    }

    private String generateFindFunctionUniqueToPKSignature(ColumnInfo unique_column, List<ColumnInfo> pk_columns) {
        StringBuilder func = new StringBuilder();

        // Function signature with unique column
        func.append("  FUNCTION find_pk_by_")
                .append(unique_column.getName().toLowerCase())
                .append("(");

        // Generate parameter for unique column
        List<String> params = new ArrayList<>();
        params.add("p_" + unique_column.getName().toLowerCase() + " " + schemaName + "."
                + tableInfo.getTableName() + "." + unique_column.getName() + "%TYPE");

        func.append(String.join(",\n               ", params))
                .append(")\n");

        // Determine return type based on whether PK is composite
        String returnType;
        if (pk_columns.size() > 1) {
            returnType = schemaName + "." + tableInfo.getTableName() + "%ROWTYPE";
        } else {
            returnType = schemaName + "." + tableInfo.getTableName() + "."
                    + pk_columns.get(0).getName() + "%TYPE";
        }

        func.append("    RETURN ").append(returnType).append(";");

        return func.toString();
    }

}
