package com.faforever.client.player;


import com.faforever.client.game.Game;

public class PlayerBuilder {

  private final Player player;

  private PlayerBuilder(String username) {
    player = new Player(username);
  }

  public static PlayerBuilder create(String username) {
    return new PlayerBuilder(username);
  }

  public PlayerBuilder defaultValues() {
    return id(1)
      .socialStatus(SocialStatus.OTHER)
      .clan("e")
      .country("US");
  }

  public PlayerBuilder clan(String clan) {
    player.setClanTag(clan);
    return this;
  }

  public PlayerBuilder id(int id) {
    player.setId(id);
    return this;
  }

  public Player get() {
    return player;
  }

  public PlayerBuilder socialStatus(SocialStatus socialStatus) {
    player.setSocialStatus(socialStatus);
    return this;
  }


  public PlayerBuilder game(Game game) {
    player.setGame(game);
    return this;
  }

  public PlayerBuilder country(String country) {
    player.setCountry(country);
    return this;
  }
}
