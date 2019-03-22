package com.faforever.client.remote;

import com.faforever.client.achievements.Achievement;
import com.faforever.client.achievements.PlayerAchievement;
import com.faforever.client.api.ApiDtoMapper;
import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.avatar.Avatar;
import com.faforever.client.avatar.event.AvatarChangedEvent;
import com.faforever.client.clan.Clan;
import com.faforever.client.config.CacheNames;
import com.faforever.client.coop.CoopMission;
import com.faforever.client.coop.CoopResult;
import com.faforever.client.event.PlayerEvent;
import com.faforever.client.game.Faction;
import com.faforever.client.game.GameEndedMessage;
import com.faforever.client.game.HostGameRequest;
import com.faforever.client.game.IceMessage;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.game.relay.GpgGameMessage;
import com.faforever.client.game.relay.ice.IceServerList;
import com.faforever.client.leaderboard.LeaderboardEntry;
import com.faforever.client.map.FaMap;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.mod.FeaturedModFile;
import com.faforever.client.mod.ModVersion;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.player.Player;
import com.faforever.client.rating.RatingHistoryDataPoint;
import com.faforever.client.replay.Replay;
import com.faforever.client.review.Review;
import com.faforever.client.review.ReviewsSummary;
import com.faforever.client.tournament.Tournament;
import com.faforever.client.user.AccountDetailsServerMessage;
import com.faforever.client.vault.SearchConfig;
import com.faforever.client.vault.SortConfig;
import com.faforever.commons.io.ByteCountListener;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.ReadOnlyObjectProperty;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.supcomhub.api.dto.Account;
import org.supcomhub.api.dto.Game;
import org.supcomhub.api.dto.GameParticipant;
import org.supcomhub.api.dto.GameReview;
import org.supcomhub.api.dto.MapVersion;
import org.supcomhub.api.dto.MapVersionReview;
import org.supcomhub.api.dto.Mod;
import org.supcomhub.api.dto.ModVersionReview;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/** Provides access to FAF services, either via server or API. */
@Lazy
@Service
public class FafService {

  private final FafServerAccessor fafServerAccessor;
  private final FafApiAccessor fafApiAccessor;
  private final EventBus eventBus;
  private final ApiDtoMapper apiDtoMapper;

  public FafService(FafServerAccessor fafServerAccessor, FafApiAccessor fafApiAccessor, EventBus eventBus, ApiDtoMapper apiDtoMapper) {
    this.fafServerAccessor = fafServerAccessor;
    this.fafApiAccessor = fafApiAccessor;
    this.eventBus = eventBus;
    this.apiDtoMapper = apiDtoMapper;
  }

  public <T extends ServerMessage> void addOnMessageListener(Class<T> type, Consumer<T> listener) {
    fafServerAccessor.addOnMessageListener(type, listener);
  }

  @SuppressWarnings("unchecked")
  public <T extends ServerMessage> void removeOnMessageListener(Class<T> type, Consumer<T> listener) {
    fafServerAccessor.removeOnMessageListener(type, listener);
  }

  public void requestHostGame(HostGameRequest hostGameRequest) {
    fafServerAccessor.requestHostGame(hostGameRequest);
  }

  public ReadOnlyObjectProperty<ConnectionState> connectionStateProperty() {
    return fafServerAccessor.connectionStateProperty();
  }

  public void requestJoinGame(int gameId, String password) {
    fafServerAccessor.requestJoinGame(gameId, password);
  }

  public void startSearchLadder1v1(Faction faction) {
    fafServerAccessor.startSearchLadder1v1(faction);
  }

  public void stopSearchingRanked() {
    fafServerAccessor.stopSearchingRanked();
  }

  public void sendGpgGameMessage(GpgGameMessage message) {
    fafServerAccessor.sendGpgMessage(message);
  }

  public CompletableFuture<AccountDetailsServerMessage> connectAndLogIn(String username, String password) {
    return fafServerAccessor.connectAndLogIn(username, password);
  }

  public void disconnect() {
    fafServerAccessor.disconnect();
  }

  public void addFriend(Player player) {
    fafServerAccessor.addFriend(player.getId());
  }

  public void addFoe(Player player) {
    fafServerAccessor.addFoe(player.getId());
  }

  public void removeFriend(Player player) {
    fafServerAccessor.removeFriend(player.getId());
  }

  public void removeFoe(Player player) {
    fafServerAccessor.removeFoe(player.getId());
  }

  @Async
  public void notifyGameEnded() {
    fafServerAccessor.sendGpgMessage(new GameEndedMessage());
  }

  @Async
  public CompletableFuture<LeaderboardEntry> getLeaderboardEntryForPlayer(int playerId, String leaderboardName) {
    return CompletableFuture.completedFuture(fafApiAccessor.getLeaderboardEntry(playerId, leaderboardName)
      .map(apiDtoMapper::map)
      .orElse(null)
    );
  }

  @Async
  public CompletableFuture<List<ModVersion>> getMods() {
    return CompletableFuture.completedFuture(fafApiAccessor.getMods().stream()
      .map(Mod::getLatestVersion)
      .map(apiDtoMapper::map)
      .collect(toList()));
  }

  @Async
  public CompletableFuture<com.faforever.client.mod.ModVersion> getModVersion(UUID uuid) {
    return CompletableFuture.completedFuture(apiDtoMapper.map(fafApiAccessor.getModVersion(uuid)));
  }

  public void reconnect() {
    fafServerAccessor.reconnect();
  }

  @Async
  public CompletableFuture<List<FaMap>> getMostPlayedMaps(int count, int page) {
    return CompletableFuture.completedFuture(fafApiAccessor.getMostPlayedMaps(count, page).stream()
      .map(apiDtoMapper::map)
      .collect(toList()));
  }

  @Async
  public CompletableFuture<List<FaMap>> getHighestRatedMaps(int count, int page) {
    return CompletableFuture.completedFuture(fafApiAccessor.getHighestRatedMaps(count, page).stream()
      .map(apiDtoMapper::map)
      .collect(toList()));
  }

  @Async
  public CompletableFuture<List<FaMap>> getNewestMaps(int count, int page) {
    return CompletableFuture.completedFuture(fafApiAccessor.getNewestMaps(count, page).stream()
      .map(apiDtoMapper::map)
      .collect(toList()));
  }

  @Async
  public CompletableFuture<List<CoopMission>> getCoopMaps() {
    return CompletableFuture.completedFuture(fafApiAccessor.getCoopMissions().stream()
      .map(apiDtoMapper::map)
      .collect(toList()));
  }

  @Async
  public CompletableFuture<List<Avatar>> getAvailableAvatars(int playerId) {
    return CompletableFuture.completedFuture(fafApiAccessor.getAvailableAvatars(playerId, 100, 0).stream()
      .map(apiDtoMapper::map)
      .collect(Collectors.toList()));
  }

  public void selectAvatar(Avatar avatar) {
    fafServerAccessor.selectAvatar(avatar == null ? null : avatar.getId());
    eventBus.post(new AvatarChangedEvent(avatar));
  }

  @CacheEvict(CacheNames.MODS)
  public void evictModsCache() {
    // Cache eviction by annotation
  }

  @Async
  public CompletableFuture<List<CoopResult>> getCoopLeaderboard(CoopMission mission, int numberOfPlayers) {
    return CompletableFuture.completedFuture(fafApiAccessor.getCoopLeaderboard(mission.getId(), numberOfPlayers)
      .stream().map(apiDtoMapper::map).collect(toList()));
  }

  @Async
  public CompletableFuture<List<RatingHistoryDataPoint>> getRatingHistory(int playerId, KnownFeaturedMod knownFeaturedMod) {
    return CompletableFuture.completedFuture(fafApiAccessor.getGameParticipants(playerId, knownFeaturedMod)
      .parallelStream()
      .filter(gameParticipant -> gameParticipant.getFinishTime() != null
        && gameParticipant.getRankAfter() != null)
      .sorted(Comparator.comparing(GameParticipant::getFinishTime))
      .map(entry -> new RatingHistoryDataPoint(entry.getFinishTime(), entry.getRankAfter()))
      .collect(Collectors.toList())
    );
  }

  @Async
  public CompletableFuture<List<FeaturedMod>> getFeaturedMods() {
    return CompletableFuture.completedFuture(fafApiAccessor.getFeaturedMods().stream()
      .sorted(Comparator.comparingInt(org.supcomhub.api.dto.FeaturedMod::getOrder))
      .map(apiDtoMapper::map)
      .collect(Collectors.toList()));
  }

  @Async
  public CompletableFuture<List<FeaturedModFile>> getFeaturedModFiles(FeaturedMod featuredMod, Integer version) {
    return CompletableFuture.completedFuture(fafApiAccessor.getFeaturedModFiles(featuredMod.getId(), version)
      .stream().map(apiDtoMapper::map).collect(Collectors.toList()));
  }

  @Async
  public CompletableFuture<List<LeaderboardEntry>> getLeaderboard(String leaderboardName) {
    return CompletableFuture.completedFuture(fafApiAccessor.getLeaderboard(leaderboardName).parallelStream()
      .map(apiDtoMapper::map)
      .collect(toList()));
  }

  @Async
  public CompletableFuture<List<Replay>> getNewestReplays(int topElementCount, int page) {
    return CompletableFuture.completedFuture(fafApiAccessor.getNewestReplays(topElementCount, page)
      .parallelStream()
      .map(apiDtoMapper::map)
      .collect(toList()));
  }

  @Async
  public CompletableFuture<List<Replay>> getHighestRatedReplays(int topElementCount, int page) {
    return CompletableFuture.completedFuture(fafApiAccessor.getHighestRatedReplays(topElementCount, page)
      .parallelStream()
      .map(apiDtoMapper::map)
      .collect(toList()));
  }

  public void uploadMod(Path modFile, ByteCountListener byteListener) {
    fafApiAccessor.uploadMod(modFile, byteListener);
  }

  @Async
  public CompletableFuture<List<PlayerAchievement>> getPlayerAchievements(int playerId) {
    return CompletableFuture.completedFuture(fafApiAccessor.getPlayerAchievements(playerId)
      .stream().map(apiDtoMapper::map).collect(Collectors.toList()));
  }

  @Async
  public CompletableFuture<List<Achievement>> getAchievements() {
    return CompletableFuture.completedFuture(fafApiAccessor.getAchievements().stream()
      .map(apiDtoMapper::map).collect(Collectors.toList()));
  }

  @Async
  public CompletableFuture<Achievement> getAchievement(String achievementId) {
    return CompletableFuture.completedFuture(apiDtoMapper.map(fafApiAccessor.getAchievement(achievementId)));
  }

  @Async
  public CompletableFuture<List<Replay>> findReplaysByQuery(String query, int maxResults, int page, SortConfig sortConfig) {
    return CompletableFuture.completedFuture(fafApiAccessor.findReplaysByQuery(query, maxResults, page, sortConfig)
      .parallelStream()
      .map(apiDtoMapper::map)
      .collect(toList()));
  }

  @Async
  public CompletableFuture<List<FaMap>> findMapsByQuery(SearchConfig query, int page, int count) {
    return CompletableFuture.completedFuture(fafApiAccessor.findMapsByQuery(query, page, count)
      .parallelStream()
      .map(apiDtoMapper::map)
      .collect(toList()));
  }

  public CompletableFuture<Optional<FaMap>> findMapByFolderName(String folderName) {
    return CompletableFuture.completedFuture(fafApiAccessor.findMapByFolderName(folderName)
      .map(apiDtoMapper::map));
  }

  public CompletableFuture<List<Player>> getPlayersByIds(Collection<Integer> playerIds) {
    return CompletableFuture.completedFuture(fafApiAccessor.getPlayersByIds(playerIds).stream()
      .map(apiDtoMapper::map)
      .collect(toList()));
  }

  @Async
  public CompletableFuture<Void> saveGameReview(Review review, int gameId) {
    GameReview gameReview = (GameReview) new GameReview()
      .setScore(review.getScore().shortValue())
      .setText(review.getText());

    if (review.getId() == null) {
      Assert.notNull(review.getReviewer(), "Player ID must be set");
      GameReview updatedReview = fafApiAccessor.createGameReview(
        (GameReview) gameReview
          .setGame((Game) new Game().setId(String.valueOf(gameId)))
          .setReviewer((Account) new Account().setId(String.valueOf(review.getReviewer().getId())))
      );
      review.setId(updatedReview.getId());
    } else {
      fafApiAccessor.updateGameReview((GameReview) gameReview.setId(review.getId()));
    }
    return CompletableFuture.completedFuture(null);
  }

  @Async
  public CompletableFuture<Void> saveModVersionReview(Review review, String modVersionId) {
    ModVersionReview modVersionReview = (ModVersionReview) new ModVersionReview()
      .setScore(review.getScore().shortValue())
      .setText(review.getText());

    if (review.getId() == null) {
      Assert.notNull(review.getReviewer(), "Player ID must be set");
      ModVersionReview updatedReview = fafApiAccessor.createModVersionReview(
        (ModVersionReview) modVersionReview
          .setModVersion((org.supcomhub.api.dto.ModVersion) new org.supcomhub.api.dto.ModVersion().setId(String.valueOf(modVersionId)))
          .setReviewer((Account) new Account().setId(String.valueOf(review.getReviewer().getId())))
          .setId(String.valueOf(review.getId()))
      );
      review.setId(updatedReview.getId());
    } else {
      fafApiAccessor.updateModVersionReview((ModVersionReview) modVersionReview.setId(review.getId()));
    }
    return CompletableFuture.completedFuture(null);
  }

  @Async
  public CompletableFuture<Void> saveMapVersionReview(Review review, String mapVersionId) {
    MapVersionReview mapVersionReview = (MapVersionReview) new MapVersionReview()
      .setScore(review.getScore().shortValue())
      .setText(review.getText());

    if (review.getId() == null) {
      Assert.notNull(review.getReviewer(), "Player ID must be set");
      MapVersionReview updatedReview = fafApiAccessor.createMapVersionReview(
        (MapVersionReview) mapVersionReview
          .setMapVersion((MapVersion) new MapVersion().setId(mapVersionId))
          .setReviewer((Account) new Account().setId(String.valueOf(review.getReviewer().getId())))
          .setId(String.valueOf(review.getId()))
      );
      review.setId(updatedReview.getId());
    } else {
      fafApiAccessor.updateMapVersionReview((MapVersionReview) mapVersionReview.setId(review.getId()));
    }

    return CompletableFuture.completedFuture(null);
  }

  @Async
  public CompletableFuture<Optional<Replay>> getLastGameOnMap(int playerId, String mapVersionId) {
    return CompletableFuture.completedFuture(fafApiAccessor.getLastGamesOnMap(playerId, mapVersionId, 1).stream()
      .map(apiDtoMapper::map)
      .findFirst());
  }

  @Async
  public CompletableFuture<Void> deleteGameReview(Review review) {
    fafApiAccessor.deleteGameReview(review.getId());
    return CompletableFuture.completedFuture(null);
  }

  @Async
  public CompletableFuture<Void> deleteMapVersionReview(Review review) {
    fafApiAccessor.deleteMapVersionReview(review.getId());
    return CompletableFuture.completedFuture(null);
  }

  @Async
  public CompletableFuture<Void> deleteModVersionReview(Review review) {
    fafApiAccessor.deleteModVersionReview(review.getId());
    return CompletableFuture.completedFuture(null);
  }

  public CompletableFuture<Optional<Replay>> findReplayById(int id) {
    return CompletableFuture.completedFuture(fafApiAccessor.findReplayById(id)
      .map(apiDtoMapper::map));
  }

  public CompletableFuture<IceServerList> getIceServers() {
    return fafServerAccessor.getIceServers();
  }

  public void restoreGameSession(int id) {
    fafServerAccessor.restoreGameSession(id);
  }

  @Async
  public CompletableFuture<List<ModVersion>> findModsByQuery(SearchConfig query, int page, int count) {
    return CompletableFuture.completedFuture(fafApiAccessor.findModsByQuery(query, page, count)
      .parallelStream()
      .map(Mod::getLatestVersion)
      .map(apiDtoMapper::map)
      .collect(toList()));
  }

  @Async
  public CompletableFuture<List<FaMap>> getLadder1v1Maps(int count, int page) {
    List<FaMap> maps = fafApiAccessor.getLadder1v1Maps(count, page).stream()
      .map(apiDtoMapper::map)
      .collect(toList());
    return CompletableFuture.completedFuture(maps);
  }

  @Async
  public CompletableFuture<Optional<Clan>> getClanByTag(String tag) {
    return CompletableFuture.completedFuture(fafApiAccessor.getClanByTag(tag)
      .map(apiDtoMapper::map));
  }

  public Optional<FaMap> findMapById(String id) {
    return fafApiAccessor.findMapVersionById(id)
      .map(apiDtoMapper::map);
  }

  public void sendIceMessage(int remotePlayerId, Object message) {
    fafServerAccessor.sendGpgMessage(new IceMessage(remotePlayerId, message));
  }

  @Async
  public CompletableFuture<List<Tournament>> getAllTournaments() {
    return CompletableFuture.completedFuture(fafApiAccessor.getAllTournaments()
      .stream()
      .map(apiDtoMapper::map)
      .collect(toList()));
  }


  @Async
  public CompletableFuture<List<FaMap>> getOwnedMaps(int playerId, int loadMoreCount, int page) {
    List<MapVersion> maps = fafApiAccessor.getOwnedMaps(playerId, loadMoreCount, page);
    return CompletableFuture.completedFuture(maps.stream().map(apiDtoMapper::map).collect(toList()));
  }

  @Async
  public CompletableFuture<Void> hideMapVersion(FaMap map) {
    String id = map.getId();
    MapVersion mapVersion = new MapVersion();
    mapVersion.setHidden(true);
    mapVersion.setId(map.getId());
    fafApiAccessor.updateMapVersion(id, mapVersion);
    return CompletableFuture.completedFuture(null);
  }

  @Async
  public CompletableFuture<Void> unrankMapVersion(FaMap map) {
    String id = map.getId();
    MapVersion mapVersion = new MapVersion();
    mapVersion.setRanked(false);
    mapVersion.setId(map.getId());
    fafApiAccessor.updateMapVersion(id, mapVersion);
    return CompletableFuture.completedFuture(null);
  }

  @Async
  public CompletableFuture<List<Review>> findNonEmptyMapReviews(String id, int count, int page) {
    return CompletableFuture.completedFuture(fafApiAccessor.findNonEmptyMapReviews(id, count, page).stream()
      .map(apiDtoMapper::map).collect(toList()));
  }

  @Async
  public CompletableFuture<List<Review>> findNonEmptyGameReviews(int id, int count, int page) {
    return CompletableFuture.completedFuture(fafApiAccessor.findNonEmptyGameReviews(id, count, page).stream()
      .map(apiDtoMapper::map).collect(toList()));
  }

  @Async
  public CompletableFuture<Optional<ReviewsSummary>> findGameReviewsSummary(int id) {
    return CompletableFuture.completedFuture(Optional.ofNullable(
      apiDtoMapper.map(fafApiAccessor.findGameReviewSummary(id))
    ));
  }

  @Async
  public CompletableFuture<Optional<Review>> findOwnMapReview(String id) {
    return CompletableFuture.completedFuture(Optional.ofNullable(
      apiDtoMapper.map(fafApiAccessor.findOwnMapReview(id))
    ));
  }

  public CompletableFuture<List<Review>> findNonEmptyModReviews(String id, int count, int page) {
    return CompletableFuture.completedFuture(fafApiAccessor.findNonEmptyModReviews(id, count, page).stream()
      .map(apiDtoMapper::map).collect(toList()));
  }

  public CompletableFuture<Optional<ReviewsSummary>> findModReviewsSummary(String id) {
    return CompletableFuture.completedFuture(Optional.ofNullable(
      apiDtoMapper.map(fafApiAccessor.findModReviewSummary(id))
    ));
  }

  @Async
  public CompletableFuture<Map<String, PlayerEvent>> getPlayerEvents(int playerId) {
    return CompletableFuture.completedFuture(fafApiAccessor.getPlayerEvents(playerId).stream()
      .collect(toMap(playerEvent -> playerEvent.getEvent().getId(), apiDtoMapper::map)));
  }
}
