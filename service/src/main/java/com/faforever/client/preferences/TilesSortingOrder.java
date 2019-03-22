package com.faforever.client.preferences;

import com.faforever.client.game.Game;
import javafx.scene.Node;
import lombok.Getter;

import java.util.Comparator;
import java.util.Locale;

public enum TilesSortingOrder {
  PLAYER_DES(Comparator.comparingInt(o -> playersInGame(((Game)o.getUserData()))), true, "tiles.comparator.playersDescending"),
  PLAYER_ASC(Comparator.comparingInt(o -> playersInGame(((Game)o.getUserData()))), false, "tiles.comparator.playersAscending"),
  NAME_DES(Comparator.comparing(o -> ((Game) o.getUserData()).getTitle().toLowerCase(Locale.US)), true, "tiles.comparator.nameDescending"),
  NAME_ASC(Comparator.comparing(o -> ((Game) o.getUserData()).getTitle().toLowerCase(Locale.US)), false, "tiles.comparator.nameAscending");

  @Getter
  private final Comparator<Node> comparator;

  @Getter
  private final String displayNameKey;

  TilesSortingOrder(Comparator<Node> comparator, boolean reversed, String displayNameKey) {
    this.displayNameKey = displayNameKey;
    this.comparator = reversed ? comparator.reversed() : comparator;
  }

  private static int playersInGame(Game game) {
    return game.getNumPlayers();
  }
}
