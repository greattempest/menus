package com.tempest.menus.controller;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/materials")
public class MaterialsController {

  private static final String TABLE_NAME = "tb_materials";

  private final JdbcTemplate jdbcTemplate;

  public MaterialsController(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @GetMapping
  public Map<String, Object> getMaterials() {
    TableMetadata table = getTableMetadata();
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("columns", table.columns());
    response.put("primaryKey", table.primaryKey());
    response.put("rows", jdbcTemplate.queryForList(
        "SELECT * FROM " + quote(TABLE_NAME) + " WHERE " + quote("delete_flag") + " = 0"));
    return response;
  }

  @PostMapping
  public ResponseEntity<?> createMaterial(@RequestBody Map<String, Object> values) {
    TableMetadata table = getTableMetadata();
    List<ColumnMetadata> columns = new ArrayList<>(writableColumns(values, table, Set.of()));
    if (columns.isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of("message", "请至少填写一个字段"));
    }

    ColumnMetadata deleteFlag = table.columns().stream()
        .filter(column -> column.name().equalsIgnoreCase("delete_flag"))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(TABLE_NAME + " 缺少 delete_flag 字段"));
    if (columns.stream().noneMatch(column -> column.name().equals(deleteFlag.name()))) {
      columns.add(deleteFlag);
      values = new LinkedHashMap<>(values);
      values.put(deleteFlag.name(), 0);
    }

    String names = columns.stream().map(column -> quote(column.name())).collect(Collectors.joining(", "));
    String placeholders = columns.stream().map(column -> "?").collect(Collectors.joining(", "));
    Object[] parameters = new Object[columns.size()];
    for (int index = 0; index < columns.size(); index++) {
      parameters[index] = values.get(columns.get(index).name());
    }
    jdbcTemplate.update("INSERT INTO " + quote(TABLE_NAME) + " (" + names + ") VALUES (" + placeholders + ")",
        parameters);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> updateMaterial(@PathVariable String id, @RequestBody Map<String, Object> values) {
    TableMetadata table = getTableMetadata();
    String primaryKey = requirePrimaryKey(table);
    List<ColumnMetadata> columns = writableColumns(values, table, Set.of(primaryKey));
    if (columns.isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of("message", "请至少填写一个可修改字段"));
    }

    String assignments = columns.stream()
        .map(column -> quote(column.name()) + " = ?")
        .collect(Collectors.joining(", "));
    Object[] parameters = new Object[columns.size() + 1];
    for (int index = 0; index < columns.size(); index++) {
      parameters[index] = values.get(columns.get(index).name());
    }
    parameters[columns.size()] = id;

    int updated = jdbcTemplate.update(
        "UPDATE " + quote(TABLE_NAME) + " SET " + assignments + " WHERE " + quote(primaryKey) + " = ?",
        parameters);
    return updated == 0 ? ResponseEntity.notFound().build() : ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteMaterial(@PathVariable String id) {
    String primaryKey = requirePrimaryKey(getTableMetadata());
    int deleted = jdbcTemplate.update(
        "UPDATE " + quote(TABLE_NAME) + " SET " + quote("delete_flag") + " = 1"
            + " WHERE " + quote(primaryKey) + " = ? AND " + quote("delete_flag") + " = 0",
        id);
    return deleted == 0 ? ResponseEntity.notFound().build() : ResponseEntity.noContent().build();
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> handleInvalidMaterialRequest(IllegalArgumentException exception) {
    return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
  }

  private List<ColumnMetadata> writableColumns(
      Map<String, Object> values, TableMetadata table, Set<String> excludedColumns) {
    if (values.keySet().stream().anyMatch(MaterialsController::isProtectedColumn)) {
      throw new IllegalArgumentException("id 和 delete_flag 字段不能通过表单修改");
    }

    Set<String> columnNames = table.columns().stream()
        .map(ColumnMetadata::name)
        .collect(Collectors.toSet());
    if (!columnNames.containsAll(values.keySet())) {
      throw new IllegalArgumentException("请求包含不存在的食材字段");
    }

    return table.columns().stream()
        .filter(column -> values.containsKey(column.name()))
        .filter(column -> !column.autoIncrement())
        .filter(column -> !isProtectedColumn(column.name()))
        .filter(column -> !excludedColumns.contains(column.name()))
        .toList();
  }

  private static boolean isProtectedColumn(String name) {
    return name.equalsIgnoreCase("id") || name.equalsIgnoreCase("delete_flag");
  }

  private String requirePrimaryKey(TableMetadata table) {
    if (table.primaryKey() == null) {
      throw new IllegalStateException(TABLE_NAME + " 必须有单列主键才能修改或删除");
    }
    return table.primaryKey();
  }

  private TableMetadata getTableMetadata() {
    return jdbcTemplate.execute((ConnectionCallback<TableMetadata>) connection -> {
      DatabaseMetaData metadata = connection.getMetaData();
      String catalog = connection.getCatalog();
      List<ColumnMetadata> columns = new ArrayList<>();
      try (ResultSet result = metadata.getColumns(catalog, null, TABLE_NAME, null)) {
        while (result.next()) {
          columns.add(new ColumnMetadata(
              result.getString("COLUMN_NAME"),
              result.getString("TYPE_NAME"),
              result.getInt("NULLABLE") == DatabaseMetaData.columnNullable,
              "YES".equalsIgnoreCase(result.getString("IS_AUTOINCREMENT")),
              false));
        }
      }

      if (columns.isEmpty()) {
        throw new EmptyResultDataAccessException("找不到数据表 " + TABLE_NAME, 1);
      }

      List<String> primaryKeys = new ArrayList<>();
      try (ResultSet result = metadata.getPrimaryKeys(catalog, null, TABLE_NAME)) {
        while (result.next()) {
          primaryKeys.add(result.getString("COLUMN_NAME"));
        }
      }
      if (primaryKeys.size() == 1) {
        String primaryKey = primaryKeys.get(0);
        columns.replaceAll(column -> column.name().equals(primaryKey)
            ? new ColumnMetadata(
                column.name(), column.type(), column.nullable(), column.autoIncrement(), true)
            : column);
        return new TableMetadata(List.copyOf(columns), primaryKey);
      }
      return new TableMetadata(List.copyOf(columns), null);
    });
  }

  private String quote(String identifier) {
    return "`" + identifier.replace("`", "``") + "`";
  }

  private record ColumnMetadata(
      String name, String type, boolean nullable, boolean autoIncrement, boolean primaryKey) {
  }

  private record TableMetadata(List<ColumnMetadata> columns, String primaryKey) {
  }
}
