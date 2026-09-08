import java.sql.*;
public class ShowMenusSchema {
  public static void main(String[] args) throws Exception {
    String url = "jdbc:mysql://47.100.186.68:3306/menus?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    String user = "menus";
    String pass = "Repeatafterme@2026";
    try (Connection c = DriverManager.getConnection(url, user, pass)) {
      DatabaseMetaData md = c.getMetaData();
      try (ResultSet rs = md.getColumns("menus", null, "tb_menus", null)) {
        while (rs.next()) {
          System.out.println(rs.getString("COLUMN_NAME") + " | " + rs.getString("TYPE_NAME") + " | " + rs.getInt("COLUMN_SIZE"));
        }
      }
      try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT * FROM tb_menus LIMIT 5")) {
        ResultSetMetaData rmd = rs.getMetaData();
        int cols = rmd.getColumnCount();
        System.out.println("ROWS:");
        while (rs.next()) {
          for (int i = 1; i <= cols; i++) {
            System.out.print(rmd.getColumnName(i) + "=" + rs.getObject(i) + (i < cols ? ", " : ""));
          }
          System.out.println();
        }
      }
    }
  }
}
