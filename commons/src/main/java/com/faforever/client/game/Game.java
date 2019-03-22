package com.faforever.client.game;

import com.faforever.client.player.Player;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Game {

  private final StringProperty hostName;
  private final StringProperty title;
  private final StringProperty mapName;
  private final StringProperty leaderboardName;
  private final StringProperty featuredMod;
  private final IntegerProperty featuredModVersion;
  private final IntegerProperty id;
  private final IntegerProperty maxPlayers;
  private final IntegerProperty minRank;
  private final IntegerProperty maxRank;
  private final BooleanProperty passwordProtected;
  private final StringProperty password;
  private final ObjectProperty<GameVisibility> visibility;
  private final ObjectProperty<GameState> state;
  private final ObjectProperty<Instant> startTime;
  private final IntegerProperty numPlayers;

  /**
   * Maps a sim mod's UID to its name.
   */
  private final MapProperty<UUID, String> simMods;
  /**
   * Maps team IDs to a list of player IDs.
   */
  private final MapProperty<Integer, List<Player>> teams;

  public Game() {
    id = new SimpleIntegerProperty();
    hostName = new SimpleStringProperty();
    title = new SimpleStringProperty();
    mapName = new SimpleStringProperty();
    leaderboardName = new SimpleStringProperty();
    featuredMod = new SimpleStringProperty();
    featuredModVersion = new SimpleIntegerProperty();
    maxPlayers = new SimpleIntegerProperty();
    minRank = new SimpleIntegerProperty(0);
    maxRank = new SimpleIntegerProperty(3000);
    passwordProtected = new SimpleBooleanProperty();
    password = new SimpleStringProperty();
    visibility = new SimpleObjectProperty<>();
    simMods = new SimpleMapProperty<>(FXCollections.observableHashMap());
    teams = new SimpleMapProperty<>(FXCollections.observableHashMap());
    state = new SimpleObjectProperty<>(GameState.INITIALIZING);
    startTime = new SimpleObjectProperty<>();
    numPlayers = new SimpleIntegerProperty();
  }

  public Game(Integer id) {
    this();
    setId(id);
  }

  public String getHostName() {
    return hostName.get();
  }

  public Game setHostName(String hostName) {
    this.hostName.set(hostName);
    return this;
  }

  public StringProperty hostNameProperty() {
    return hostName;
  }

  public String getTitle() {
    return title.get();
  }

  public Game setTitle(String title) {
    this.title.set(title);
    return this;
  }

  public StringProperty titleProperty() {
    return title;
  }

  public String getMapName() {
    return mapName.get();
  }

  public Game setMapName(String mapName) {
    this.mapName.set(mapName);
    return this;
  }

  public StringProperty mapNameProperty() {
    return mapName;
  }

  public String getLeaderboardName() {
    return leaderboardName.get();
  }

  public Game setLeaderboardName(String leaderboardName) {
    this.leaderboardName.set(leaderboardName);
    return this;
  }

  public String getFeaturedMod() {
    return featuredMod.get();
  }

  public Game setFeaturedMod(String featuredMod) {
    this.featuredMod.set(featuredMod);
    return this;
  }

  public StringProperty featuredModProperty() {
    return featuredMod;
  }

  public int getFeaturedModVersion() {
    return featuredModVersion.get();
  }

  public Game setFeaturedModVersion(int featuredModVersion) {
    this.featuredModVersion.set(featuredModVersion);
    return this;
  }

  public IntegerProperty featuredModVersionProperty() {
    return featuredModVersion;
  }

  public int getId() {
    return id.get();
  }

  public Game setId(int id) {
    this.id.set(id);
    return this;
  }

  public IntegerProperty idProperty() {
    return id;
  }

  public int getMaxPlayers() {
    return maxPlayers.get();
  }

  public Game setMaxPlayers(int maxPlayers) {
    this.maxPlayers.set(maxPlayers);
    return this;
  }

  public IntegerProperty maxPlayersProperty() {
    return maxPlayers;
  }

  public StringProperty leaderboardNameProperty() {
    return leaderboardName;
  }

  public int getMinRank() {
    return minRank.get();
  }

  public Game setMinRank(int minRank) {
    this.minRank.set(minRank);
    return this;
  }

  public IntegerProperty minRankProperty() {
    return minRank;
  }

  public int getMaxRank() {
    return maxRank.get();
  }

  public Game setMaxRank(int maxRank) {
    this.maxRank.set(maxRank);
    return this;
  }

  public GameState getState() {
    return state.get();
  }

  public Game setState(GameState state) {
    this.state.set(state);
    return this;
  }

  public ObjectProperty<GameState> stateProperty() {
    return state;
  }

  /**
   * Returns a map of simulation mod UIDs to the mod's name.
   */
  public ObservableMap<UUID, String> getSimMods() {
    return simMods.get();
  }

  public Game setSimMods(ObservableMap<UUID, String> simMods) {
    this.simMods.set(simMods);
    return this;
  }

  public MapProperty<UUID, String> simModsProperty() {
    return simMods;
  }

  /**
   * Maps team names ("1", "2", ...) to a list of player names. <strong>Make sure to synchronize on the return
   * value.</strong>
   */
  public ObservableMap<Integer, List<Player>> getTeams() {
    return teams.get();
  }

  public Game setTeams(ObservableMap<Integer, List<Player>> teams) {
    this.teams.set(teams);
    return this;
  }

  public MapProperty<Integer, List<Player>> teamsProperty() {
    return teams;
  }

  @Override
  public int hashCode() {
    return id.getValue().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Game
      && id.getValue().equals(((Game) obj).id.getValue());
  }

  public GameVisibility getVisibility() {
    return visibility.get();
  }

  public ObjectProperty<GameVisibility> visibilityProperty() {
    return visibility;
  }

  public IntegerProperty maxRankProperty() {
    return maxRank;
  }

  public boolean getPasswordProtected() {
    return passwordProtected.get();
  }

  public BooleanProperty passwordProtectedProperty() {
    return passwordProtected;
  }

  public String getPassword() {
    return password.get();
  }

  public Game setPassword(String password) {
    this.password.set(password);
    return this;
  }

  public StringProperty passwordProperty() {
    return password;
  }

  public Instant getStartTime() {
    return startTime.get();
  }

  public Game setStartTime(Instant startTime) {
    this.startTime.set(startTime);
    return this;
  }

  public ObjectProperty<Instant> startTimeProperty() {
    return startTime;
  }

  public Game setPasswordProtected(boolean passwordProtected) {
    this.passwordProtected.set(passwordProtected);
    return this;
  }

  public int getNumPlayers() {
    return numPlayers.get();
  }

  public IntegerProperty numPlayersProperty() {
    return numPlayers;
  }

  public void setNumPlayers(int numPlayers) {
    this.numPlayers.set(numPlayers);
  }

  @Override
  public String toString() {
    return "Game{" +
      "title=" + title.get() +
      ", id=" + id.get() +
      ", state=" + state.get() +
      '}';
  }
}
