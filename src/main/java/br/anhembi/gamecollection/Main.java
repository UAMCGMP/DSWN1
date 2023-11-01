package br.anhembi.gamecollection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.javalin.Javalin;
import io.javalin.http.Cookie;
import io.javalin.http.HttpStatus;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Main {
  private static Map<String, String> sessions = new HashMap<>();
  private static ObjectMapper objectMapper = new ObjectMapper();

  public static void main(String[] args) {
    Javalin app = Javalin.create();

    app.get("/logout", ctx -> {
      ctx.removeCookie("session");
      ctx.redirect("/", HttpStatus.TEMPORARY_REDIRECT);
    });

    app.post("/api/login", ctx -> {
      if (sessions.get(ctx.cookie("session")) != null) {
        ctx.status(400).json("Erro: Já está logado.");
        return;
      }
      JsonNode body = objectMapper.readTree(ctx.body());
      String username = body.get("username").asText();
      String password = body.get("password").asText();
      if (checkCredentials(username, password)) {
        Cookie sessionCookie = new Cookie("session", createSession(username));
        sessionCookie.setHttpOnly(true);
        ctx.cookie(sessionCookie);
        ctx.json(objectMapper.readTree("{\"status\":\"success\",\"data\":null}"));
      } else {
        ctx.status(401).json("Erro: Credenciais inválidas.");
      }
    });

    app.post("/api/register", ctx -> {
      JsonNode body = objectMapper.readTree(ctx.body());
      String username = body.get("username").asText();
      String password = body.get("password").asText();
      if (!username.matches("[a-zA-Z]{3,16}")) {
        ctx.status(400).json("Erro: O username deve conter de 3 a 16 letras maiúsculas ou minúsculas.");
        return;
      }
      if (!password.matches("[a-zA-Z0-9]{6,32}")) {
        ctx.status(400).json("Erro: A senha deve conter de 6 a 32 letras maiúsculas, minúsculas ou números.");
        return;
      }
      if (checkUsername(username)) {
        ctx.status(400).json("Erro: O username já está em uso.");
        return;
      }
      String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
      if (registerUser(username, hashedPassword)) {
        ctx.json(objectMapper.readTree("{\"status\":\"success\",\"data\":null}"));
      } else {
        ctx.status(500).json(objectMapper.readTree("{\"status\":\"error\",\"message\":\"Database Error\"}"));
      }
    });

    app.get("/api/games", ctx -> {
      JsonNode body = objectMapper.readTree(ctx.body());
      String username = body.get("username").asText();
      List<Game> games = getUserGames(username);
      if (!checkUsername(username)) {
        ctx.status(400).json("Erro: O username não existe.");
        return;
      }
      if (games != null) {
        ctx.json(games);
      } else {
        ctx.status(500).json(objectMapper.readTree("{\"status\":\"error\",\"message\":\"Database Error\"}"));
      }
    });

    app.post("/api/games", ctx -> {
      String username = sessions.get(ctx.cookie("session"));
      if (username == null) {
        ctx.status(401).json("Erro: O usuário não está logado.");
        return;
      }
      JsonNode body = objectMapper.readTree(ctx.body());
      String title = body.get("title").asText();
      String platform = body.get("platform").asText();
      if (registerGame(username, title, platform)) {
        ctx.json(objectMapper.readTree("{\"status\":\"success\",\"data\":null}"));
      } else {
        ctx.status(500).json(objectMapper.readTree("{\"status\":\"error\",\"message\":\"Database Error\"}"));
      }
    });

    app.delete("/api/games", ctx -> {
      String username = sessions.get(ctx.cookie("session"));
      if (username == null) {
        ctx.status(401).json("Erro: O usuário não está logado.");
        return;
      }
      JsonNode body = objectMapper.readTree(ctx.body());
      int id = body.get("id").asInt();
      if (deleteGame(username, id)) {
        ctx.json(objectMapper.readTree("{\"status\":\"success\",\"data\":null}"));
      } else {
        ctx.status(500).json(objectMapper.readTree("{\"status\":\"error\",\"message\":\"Database Error\"}"));
      }
    });

    app.error(404, ctx -> {
      ctx.status(404).json(objectMapper.readTree("{\"status\":\"error\",\"message\":\"Not Found\"}"));
    });

    app.error(500, ctx -> {
      ctx.status(500).json(objectMapper.readTree("{\"status\":\"error\",\"message\":\"Internal Server Error\"}"));
    });

    app.start(3000);
  }

  private static boolean checkCredentials(String username, String password) {
    ResultSet resultSet = Database.executeQuery("SELECT username, password FROM user WHERE username = ?", username);
    try {
      if (resultSet.next()) {
        String hashedPassword = resultSet.getString("password");
        return BCrypt.checkpw(password, hashedPassword);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return false;
  }

  private static boolean checkUsername(String username) {
    ResultSet resultSet = Database.executeQuery("SELECT username FROM user WHERE username = ?", username);
    try {
      return resultSet.next();
    } catch (SQLException e) {
      e.printStackTrace();
      return false;
    }
  }

  private static String createSession(String username) {
    String sessionId = UUID.randomUUID().toString();
    sessions.put(sessionId, username);
    return sessionId;
  }

  private static boolean deleteGame(String username, int id) {
    int rowsAffected = Database.executeUpdate("DELETE FROM game WHERE username = ? AND id = ?", username, id);
    return rowsAffected > 0;
  }

  private static List<Game> getUserGames(String username) {
    List<Game> games = new ArrayList<>();
    ResultSet resultSet = Database.executeQuery("SELECT id, createdAt, title, platform FROM game WHERE username = ?", username);
    try {
      while (resultSet.next()) {
        int id = resultSet.getInt("id");
        Timestamp createdAt = resultSet.getTimestamp("createdAt");
        String title = resultSet.getString("title");
        String platform = resultSet.getString("platform");
        games.add(new Game(id, createdAt, title, platform));
      }
      return games;
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

  private static boolean registerGame(String username, String title, String platform) {
    int rowsAffected = Database.executeUpdate("INSERT INTO game (username, createdAt, title, platform) VALUES (?, ?, ?, ?)", username, new Timestamp(System.currentTimeMillis()), title, platform);
    return rowsAffected > 0;
  }

  private static boolean registerUser(String username, String hashedPassword) {
    int affectedRows = Database.executeUpdate("INSERT INTO user (username, password) VALUES (?, ?)", username, hashedPassword);
    return affectedRows > 0;
  }
}
