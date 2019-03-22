package com.faforever.client.player;

import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.game.Game;
import com.faforever.client.game.GameState;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.util.ProgrammingError;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyMapProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;

import java.net.URL;
import java.time.Instant;
import java.util.Objects;

import static com.faforever.client.player.SocialStatus.OTHER;

/**
 * Represents a player with displayName, clan, country, friend/foe flag and so on.
 */
public class Player {

  private final IntegerProperty id;
  private final StringProperty displayName;
  private final StringProperty clanTag;
  private final StringProperty country;
  private final ObjectProperty<URL> avatarUrl;
  private final StringProperty avatarTooltip;
  private final ObjectProperty<SocialStatus> socialStatus;
  /** Key is the leaderboard name. */
  private final MapProperty<String, Integer> rating;
  private final ObjectProperty<Game> game;
  private final ObjectProperty<PlayerStatus> status;
  private final ObservableSet<ChatChannelUser> chatChannelUsers;
  private final IntegerProperty numberOfGames;
  private final ObjectProperty<Instant> idleSince;
  private final ObservableList<NameRecord> names;

  public Player() {
    id = new SimpleIntegerProperty();
    displayName = new SimpleStringProperty();
    clanTag = new SimpleStringProperty();
    country = new SimpleStringProperty();
    avatarUrl = new SimpleObjectProperty<>();
    avatarTooltip = new SimpleStringProperty();
    rating = new SimpleMapProperty<>(FXCollections.observableHashMap());
    status = new SimpleObjectProperty<>(PlayerStatus.IDLE);
    chatChannelUsers = FXCollections.observableSet();
    game = new SimpleObjectProperty<>();
    numberOfGames = new SimpleIntegerProperty();
    socialStatus = new SimpleObjectProperty<>(OTHER);
    idleSince = new SimpleObjectProperty<>(Instant.now());
    names = FXCollections.observableArrayList();
  }

  public Player(String displayName) {
    this();
    this.displayName.set(displayName);
  }

  public ObservableList<NameRecord> getNames() {
    return names;
  }

  public SocialStatus getSocialStatus() {
    return socialStatus.get();
  }

  public void setSocialStatus(SocialStatus socialStatus) {
    this.socialStatus.set(socialStatus);
  }

  public ObjectProperty<SocialStatus> socialStatusProperty() {
    return socialStatus;
  }

  public int getId() {
    return id.get();
  }

  public void setId(int id) {
    this.id.set(id);
  }

  public IntegerProperty idProperty() {
    return id;
  }

  public int getNumberOfGames() {
    return numberOfGames.get();
  }

  public void setNumberOfGames(int numberOfGames) {
    this.numberOfGames.set(numberOfGames);
  }

  public IntegerProperty numberOfGamesProperty() {
    return numberOfGames;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id.get(), displayName.get());
  }

  @Override
  public boolean equals(Object obj) {
    return obj != null
      && (obj.getClass() == Player.class)
      && (getId() == ((Player) obj).getId() && getId() != 0 ||
      getDisplayName().equalsIgnoreCase(((Player) obj).getDisplayName()));
  }

  public String getDisplayName() {
    return displayName.get();
  }

  public void setDisplayName(String displayName) {
    this.displayName.set(displayName);
  }

  public StringProperty displayNameProperty() {
    return displayName;
  }

  public String getClanTag() {
    return clanTag.get();
  }

  public void setClanTag(String clanTag) {
    this.clanTag.set(clanTag);
  }

  public StringProperty clanTagProperty() {
    return clanTag;
  }

  public String getCountry() {
    return country.get();
  }

  public void setCountry(String country) {
    this.country.set(country);
  }

  public StringProperty countryProperty() {
    return country;
  }

  public URL getAvatarUrl() {
    return avatarUrl.get();
  }

  public void setAvatarUrl(URL avatarUrl) {
    this.avatarUrl.set(avatarUrl);
  }

  public ObjectProperty<URL> avatarUrlProperty() {
    return avatarUrl;
  }

  public String getAvatarTooltip() {
    return avatarTooltip.get();
  }

  public void setAvatarTooltip(String avatarTooltip) {
    this.avatarTooltip.set(avatarTooltip);
  }

  public StringProperty avatarTooltipProperty() {
    return avatarTooltip;
  }

  public PlayerStatus getStatus() {
    return status.get();
  }

  public ReadOnlyObjectProperty<PlayerStatus> statusProperty() {
    return status;
  }

  public Game getGame() {
    return game.get();
  }

  public void setGame(Game game) {
    this.game.set(game);
    if (game == null) {
      status.unbind();
      status.set(PlayerStatus.IDLE);
    } else {
      this.status.bind(Bindings.createObjectBinding(() -> {
        switch (getGame().getState()) {
          case INITIALIZING:
            return PlayerStatus.LOBBYING;
          case OPEN:
            if (getGame().getHostName() != null && getGame().getHostName().equalsIgnoreCase(displayName.get())) {
              return PlayerStatus.HOSTING;
            }
            return PlayerStatus.LOBBYING;
          case PLAYING:
            return PlayerStatus.PLAYING;
          case ENDED:
            return PlayerStatus.IDLE;
          case CLOSED:
            return PlayerStatus.IDLE;
          default:
            throw new ProgrammingError("Uncovered: " + getGame().getState());
        }
      }, game.stateProperty()));
    }
  }

  public ObjectProperty<Game> gameProperty() {
    return game;
  }

  public Instant getIdleSince() {
    return idleSince.get();
  }

  public void setIdleSince(Instant idleSince) {
    this.idleSince.set(idleSince);
  }

  public ObjectProperty<Instant> idleSinceProperty() {
    return idleSince;
  }

  public ObservableSet<ChatChannelUser> getChatChannelUsers() {
    return chatChannelUsers;
  }

  public ObservableMap<String, Integer> getRating() {
    return rating.get();
  }

  public ReadOnlyMapProperty<String, Integer> ratingProperty() {
    return rating;
  }
}
