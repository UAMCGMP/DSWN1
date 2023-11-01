package br.anhembi.gamecollection;

import java.sql.Timestamp;

public class Game {
  private int id;
  private Timestamp createdAt;
  private String title;
  private String platform;

  public Game(int id, Timestamp createdAt, String title, String platform) {
    this.id = id;
    this.createdAt = createdAt;
    this.title = title;
    this.platform = platform;
  }

  public int getId() {
    return id;
  }

  public Timestamp getCreatedAt() {
    return createdAt;
  }

  public String getTitle() {
    return title;
  }

  public String getPlatform() {
    return platform;
  }
}
