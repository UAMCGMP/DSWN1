package br.anhembi.gamecollection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Database {
  private static Connection connection;

  public static Connection connect() {
    if (connection == null) {
      try {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:gamecollection.db");
      } catch (ClassNotFoundException | SQLException e) {
        e.printStackTrace();
      }
    }
    return connection;
  }

  public static void closeConnection() {
    if (connection != null) {
      try {
        connection.close();
        connection = null;
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }

  public static ResultSet executeQuery(String query, Object... params) {
    Connection conn = connect();
    ResultSet result = null;
    try {
      PreparedStatement statement = conn.prepareStatement(query);
      for (int i = 0; i < params.length; i++) {
        statement.setObject(i + 1, params[i]);
      }
      result = statement.executeQuery();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return result;
  }

  public static int executeUpdate(String query, Object... params) {
    Connection conn = connect();
    int affectedRows = 0;
    try {
      PreparedStatement statement = conn.prepareStatement(query);
      for (int i = 0; i < params.length; i++) {
        statement.setObject(i + 1, params[i]);
      }
      affectedRows = statement.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return affectedRows;
  }
}
