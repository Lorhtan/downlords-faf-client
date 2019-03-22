package com.faforever.client.api;

import com.faforever.client.SpringProfiles;
import com.faforever.client.config.CacheNames;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Api;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.io.CountingFileSystemResource;
import com.faforever.client.user.LoggedOutEvent;
import com.faforever.client.user.LoginSuccessEvent;
import com.faforever.client.vault.SearchConfig;
import com.faforever.client.vault.SortConfig;
import com.faforever.commons.io.ByteCountListener;
import com.github.rutledgepaulv.qbuilders.builders.QBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.visitors.RSQLVisitor;
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.supcomhub.api.dto.Account;
import org.supcomhub.api.dto.Achievement;
import org.supcomhub.api.dto.Avatar;
import org.supcomhub.api.dto.AvatarAssignment;
import org.supcomhub.api.dto.Clan;
import org.supcomhub.api.dto.CoopMission;
import org.supcomhub.api.dto.CoopResult;
import org.supcomhub.api.dto.FeaturedModFile;
import org.supcomhub.api.dto.Game;
import org.supcomhub.api.dto.GameParticipant;
import org.supcomhub.api.dto.GameReview;
import org.supcomhub.api.dto.GameReviewSummary;
import org.supcomhub.api.dto.LadderMap;
import org.supcomhub.api.dto.LeaderboardEntry;
import org.supcomhub.api.dto.Map;
import org.supcomhub.api.dto.MapReview;
import org.supcomhub.api.dto.MapVersion;
import org.supcomhub.api.dto.MapVersionReview;
import org.supcomhub.api.dto.Mod;
import org.supcomhub.api.dto.ModReview;
import org.supcomhub.api.dto.ModReviewSummary;
import org.supcomhub.api.dto.ModVersion;
import org.supcomhub.api.dto.ModVersionReview;
import org.supcomhub.api.dto.PlayerAchievement;
import org.supcomhub.api.dto.PlayerEvent;
import org.supcomhub.api.dto.challonge.Tournament;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

@Slf4j
@Component
@Profile("!" + SpringProfiles.PROFILE_OFFLINE)
public class FafApiAccessorImpl implements FafApiAccessor, InitializingBean {

  private static final String MAP_ENDPOINT = "/data/map";
  private static final String TOURNAMENT_LIST_ENDPOINT = "/challonge/v1/tournaments.json";
  private static final String REPLAY_INCLUDES = "featuredMod,playerStats,playerStats.player,reviews,reviews.player,mapVersion,mapVersion.map,mapVersion.reviews,reviewsSummary";
  private static final String COOP_RESULT_INCLUDES = "game.playerStats.player";
  private static final String COOP_MISSION_INCLUDES = "map";
  private static final String PLAYER_INCLUDES = "globalRating,ladder1v1Rating,names";
  private static final String MOD_ENDPOINT = "/data/mod";
  private static final String OAUTH_TOKEN_PATH = "/oauth/token";

  private final EventBus eventBus;
  private final RestTemplateBuilder restTemplateBuilder;
  private final ClientProperties clientProperties;
  private final HttpComponentsClientHttpRequestFactory requestFactory;

  private CountDownLatch authorizedLatch;
  private RestOperations restOperations;


  public FafApiAccessorImpl(EventBus eventBus, RestTemplateBuilder restTemplateBuilder,
                            ClientProperties clientProperties, JsonApiMessageConverter jsonApiMessageConverter,
                            JsonApiErrorHandler jsonApiErrorHandler) {
    this.eventBus = eventBus;
    this.clientProperties = clientProperties;
    authorizedLatch = new CountDownLatch(1);

    requestFactory = new HttpComponentsClientHttpRequestFactory();
    this.restTemplateBuilder = restTemplateBuilder
      .requestFactory(() -> requestFactory)
      .additionalMessageConverters(jsonApiMessageConverter)
      .errorHandler(jsonApiErrorHandler);
  }

  private static String rsql(Condition<?> eq) {
    return eq.query(new RSQLVisitor());
  }

  private static <T extends QBuilder<T>> QBuilder<T> qBuilder() {
    return new QBuilder<>();
  }

  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);
  }

  @Subscribe
  public void onLoggedOutEvent(LoggedOutEvent event) {
    authorizedLatch = new CountDownLatch(1);
    restOperations = null;
  }

  @Subscribe
  public void onLoginSuccessEvent(LoginSuccessEvent event) {
    authorize(event.getUserId(), event.getUsername(), event.getPassword());
  }

  @Override
  public List<PlayerAchievement> getPlayerAchievements(int playerId) {
    return getAll("/data/playerAchievement", ImmutableMap.of(
      "filter", rsql(qBuilder().intNum("player.id").eq(playerId))
    ));
  }

  @Override
  public List<PlayerEvent> getPlayerEvents(int playerId) {
    return getAll("/data/playerEvent", ImmutableMap.of(
      "filter", rsql(qBuilder().intNum("player.id").eq(playerId))
    ));
  }

  @Override
  @Cacheable(CacheNames.ACHIEVEMENTS)
  public List<Achievement> getAchievements() {
    return getAll("/data/achievement", ImmutableMap.of(
      "sort", "order"
    ));
  }

  @Override
  @Cacheable(CacheNames.ACHIEVEMENTS)
  public Achievement getAchievement(String achievementId) {
    return getOne("/data/achievement/" + achievementId, Achievement.class);
  }

  @Override
  @Cacheable(CacheNames.MODS)
  public List<Mod> getMods() {
    return getAll("/data/mod", ImmutableMap.of(
      "include", "latestVersion,latestVersion.reviewsSummary"));
  }

  @Override
  @Cacheable(CacheNames.FEATURED_MODS)
  public List<org.supcomhub.api.dto.FeaturedMod> getFeaturedMods() {
    return getMany("/data/featuredMod", 1000, ImmutableMap.of());
  }

  @Cacheable(CacheNames.LEADERBOARD)
  @SneakyThrows
  public List<LeaderboardEntry> getLeaderboard(String leaderboardName) {
    return getAll("/data/leaderboardEntry", ImmutableMap.of(
      "filter", rsql(qBuilder()
        .string("leaderboard.technicalName").eq(leaderboardName)
      ),
      "sort", "-rank",
      "include", "account.id,account.displayName"
    ));
  }

  @Override
  public Optional<LeaderboardEntry> getLeaderboardEntry(int playerId, String leaderboardName) {
    List<LeaderboardEntry> all = getAll("/data/leaderboardEntry", ImmutableMap.of(
      "filter", rsql(qBuilder()
        .string("leaderboard.technicalName").eq(leaderboardName)
        .and().intNum("account.id").eq(playerId)
      ),
      "include", "account.id,account.displayName"
    ));
    return all.stream().findFirst();
  }

  @Override
  @Cacheable(CacheNames.RATING_HISTORY)
  public List<GameParticipant> getGameParticipants(int playerId, KnownFeaturedMod knownFeaturedMod) {
    return getAll("/data/gameParticipant", ImmutableMap.of(
      "filter", rsql(qBuilder()
        .intNum("player.id").eq(playerId)
        .and()
        .string("game.featuredMod.technicalName").eq(knownFeaturedMod.getTechnicalName())
      )));
  }

  @Override
  @Cacheable(CacheNames.MAPS)
  public List<org.supcomhub.api.dto.Map> getMostPlayedMaps(int count, int page) {
    // FIXME not yet provided by the new API
    return Collections.emptyList();
//    return this.<MapStatistics>getPage("/data/mapStatistics", count, page, ImmutableMap.of(
//      "include", "map,map.statistics,map.latestVersion,map.author,map.versions.reviews,map.versions.reviews.player",
//      "sort", "-plays")).stream()
//      .map(MapStatistics::getMap)
//      .collect(Collectors.toList());
  }

  @Override
  public List<org.supcomhub.api.dto.Map> getHighestRatedMaps(int count, int page) {
    // FIXME not yet provided by the new API
    return Collections.emptyList();
//    return this.<MapStatistics>getPage("/data/mapStatistics", count, page, ImmutableMap.of(
//      "include", "map.statistics,map,map.latestVersion,map.author,map.versions.reviews,map.versions.reviews.player,map.latestVersion.reviewsSummary",
//      "sort", "-map.latestVersion.reviewsSummary.lowerBound")).stream()
//      .map(MapStatistics::getMap)
//      .collect(Collectors.toList());
  }

  @Override
  public List<org.supcomhub.api.dto.Map> getNewestMaps(int count, int page) {
    return getPage(MAP_ENDPOINT, count, page, ImmutableMap.of(
      "include", "statistics,latestVersion,author,versions.reviews,versions.reviews.player",
      "sort", "-updateTime",
      "filter", "latestVersion.hidden==\"false\""
    ));
  }

  @Override
  public List<Game> getLastGamesOnMap(int playerId, String mapVersionId, int count) {
    return getMany("/data/game", count, ImmutableMap.of(
      "filter", rsql(qBuilder()
        .string("mapVersion.id").eq(mapVersionId)
        .and()
        .intNum("playerStats.player.id").eq(playerId)),
      "sort", "-endTime"
    ));
  }

  @Override
  public void uploadMod(Path file, ByteCountListener listener) {
    MultiValueMap<String, Object> multipartContent = createFileMultipart(file, listener);
    post("/mods/upload", multipartContent, false);
  }

  @Override
  public void uploadMap(Path file, boolean isRanked, ByteCountListener listener) {
    MultiValueMap<String, Object> multipartContent = createFileMultipart(file, listener);
    multipartContent.add("metadata", ImmutableMap.of("isRanked", isRanked));
    post("/maps/upload", multipartContent, false);
  }

  @Override
  public void changePassword(String username, String currentPasswordHash, String newPasswordHash) {
    java.util.Map<String, String> body = ImmutableMap.of(
      "currentPassword", currentPasswordHash,
      "newPassword", newPasswordHash
    );

    post("/users/changePassword", body, true);
  }

  @Override
  public ModVersion getModVersion(UUID uid) {
    return (ModVersion) getMany("/data/modVersion", 1,
      ImmutableMap.of("filter", rsql(qBuilder().string("uid").eq(uid.toString())), "include", "mod,mod.latestVersion,mod.versions,mod.uploader")
    ).get(0);
  }

  @Override
  @Cacheable(CacheNames.FEATURED_MOD_FILES)
  public List<FeaturedModFile> getFeaturedModFiles(String featuredModId, Integer version) {
    String endpoint = String.format("/featuredMods/%s/files/%s", featuredModId,
      Optional.ofNullable(version).map(String::valueOf).orElse("latest"));
    return getMany(endpoint, 10_000, ImmutableMap.of());
  }

  @Override
  public List<Game> getNewestReplays(int count, int page) {
    return getPage("/data/game", count, page, ImmutableMap.of(
      "sort", "-endTime",
      "include", REPLAY_INCLUDES,
      "filter", "endTime=isnull=false"
    ));
  }

  @Override
  public List<Game> getHighestRatedReplays(int count, int page) {
    return getPage("/data/game", count, page, ImmutableMap.of(
      "sort", "-reviewsSummary.lowerBound",
      "include", REPLAY_INCLUDES,
      "filter", "endTime=isnull=false"
    ));
  }

  @Override
  public List<Game> findReplaysByQuery(String query, int maxResults, int page, SortConfig sortConfig) {
    return getPage("/data/game", maxResults, page, ImmutableMap.of(
      "filter", "(" + query + ");endTime=isnull=false",
      "include", REPLAY_INCLUDES,
      "sort", sortConfig.toQuery()
    ));
  }

  @Override
  public Optional<MapVersion> findMapByFolderName(String folderName) {
    List<MapVersion> maps = getMany("/data/mapVersion", 1, ImmutableMap.of(
      "filter", String.format("filename==\"*%s*\"", folderName),
      "include", "map,map.statistics,reviews"));
    if (maps.isEmpty()) {
      return Optional.empty();
    }
    return Optional.ofNullable(maps.get(0));
  }

  @Override
  public List<Account> getPlayersByIds(Collection<Integer> playerIds) {
    List<String> ids = playerIds.stream().map(String::valueOf).collect(Collectors.toList());

    return getMany("/data/player", playerIds.size(), ImmutableMap.of(
      "include", PLAYER_INCLUDES,
      "filter", rsql(qBuilder().string("id").in(ids))
    ));
  }

  @Override
  public GameReview createGameReview(GameReview review) {
    return post("/data/game/" + review.getGame().getId() + "/reviews", review, GameReview.class);
  }

  @Override
  public void updateGameReview(GameReview review) {
    patch("/data/gameReview/" + review.getId(), review, Void.class);
  }

  @Override
  public ModVersionReview createModVersionReview(ModVersionReview review) {
    return post("/data/modVersion/" + review.getModVersion().getId() + "/reviews", review, ModVersionReview.class);
  }

  @Override
  public void updateModVersionReview(ModVersionReview review) {
    patch("/data/modVersionReview/" + review.getId(), review, Void.class);
  }

  @Override
  public MapVersionReview createMapVersionReview(MapVersionReview review) {
    return post("/data/mapVersion/" + review.getMapVersion().getId() + "/reviews", review, MapVersionReview.class);
  }

  @Override
  public void updateMapVersionReview(MapVersionReview review) {
    patch("/data/mapVersionReview/" + review.getId(), review, Void.class);
  }

  @Override
  public void deleteGameReview(String id) {
    delete("/data/gameReview/" + id);
  }

  @Override
  public void deleteMapVersionReview(String id) {
    delete("/data/mapVersionReview/" + id);
  }

  @Override
  public List<Mod> findModsByQuery(SearchConfig searchConfig, int page, int count) {
    MultiValueMap<String, String> parameterMap = new LinkedMultiValueMap<>();
    if (searchConfig.hasQuery()) {
      parameterMap.add("filter", searchConfig.getSearchQuery() + ";latestVersion.hidden==\"false\"");
    }
    parameterMap.add("include", "latestVersion,latestVersion.reviews,latestVersion.reviews.player,latestVersion.reviewsSummary");
    parameterMap.add("sort", searchConfig.getSortConfig().toQuery());
    return getPage(MOD_ENDPOINT, count, page, parameterMap);
  }

  @Override
  public void deleteModVersionReview(String id) {
    delete("/data/modVersionReview/" + id);
  }

  @Override
  public Optional<Game> findReplayById(int id) {
    return Optional.ofNullable(getOne("/data/game/" + id, Game.class, ImmutableMap.of("include", REPLAY_INCLUDES)));
  }

  @Override
  public List<LadderMap> getLadder1v1Maps(int count, int page) {
    return getPage("/data/ladder1v1Map", count, page, ImmutableMap.of(
      "include", "mapVersion,mapVersion.map,mapVersion.map.latestVersion,mapVersion.map.latestVersion.reviews,mapVersion.map.author,mapVersion.map.statistics"));
  }

  @Override
  public List<MapVersion> getOwnedMaps(int playerId, int loadMoreCount, int page) {
    return getPage("/data/mapVersion", loadMoreCount, page, ImmutableMap.of(
      "include", "map,map.latestVersion,map.latestVersion.reviews,map.author,map.statistics",
      "filter", rsql(qBuilder().string("map.author.id").eq(String.valueOf(playerId)))
    ));
  }

  @Override
  public void updateMapVersion(String id, MapVersion mapVersion) {
    patch(String.format("/data/mapVersion/%s", id), mapVersion, Void.class);
  }

  @Override
  public List<Avatar> getAvailableAvatars(int playerId, int loadMoreCount, int page) {
    return this.<AvatarAssignment>getPage("/data/avatarAssignment", loadMoreCount, page, ImmutableMap.of(
      "include", "avatar",
      "filter", rsql(qBuilder().string("player.id").eq(String.valueOf(playerId)))
    )).stream()
      .map(AvatarAssignment::getAvatar)
      .collect(Collectors.toList());
  }

  @Override
  public List<MapReview> findNonEmptyMapReviews(String id, int count, int page) {
    // FIXME filter by ID
    return getPage("/data/mapReview", count, page, ImmutableMap.of());
  }

  @Override
  public List<GameReview> findNonEmptyGameReviews(int id, int count, int page) {
    // FIXME filter by ID
    return getPage("/data/gameReview", count, page, ImmutableMap.of());
  }

  @Override
  public GameReviewSummary findGameReviewSummary(int id) {
    return getOne("/data/" + GameReviewSummary.TYPE_NAME + "/" + id, GameReviewSummary.class);
  }

  @Override
  public MapReview findOwnMapReview(String mapId) {
    return getOne("/data/" + MapReview.TYPE_NAME, MapReview.class);
  }

  @Override
  public List<ModReview> findNonEmptyModReviews(String id, int count, int page) {
    return getPage("/data/" + ModReview.TYPE_NAME, count, page, ImmutableMap.of(
      "filter", rsql(qBuilder().string("text").ne(""))
    ));
  }

  @Override
  public ModReviewSummary findModReviewSummary(String id) {
    return getOne("/data/" + ModReviewSummary.TYPE_NAME + "/" + id, ModReviewSummary.class);
  }

  @Override
  @Cacheable(CacheNames.CLAN)
  public Optional<Clan> getClanByTag(String tag) {
    List<Clan> clans = getMany("/data/clan", 1, ImmutableMap.of(
      "include", "leader,founder,memberships,memberships.player",
      "filter", rsql(qBuilder().string("tag").eq(tag))
    ));
    if (clans.isEmpty()) {
      return Optional.empty();
    }
    return Optional.ofNullable(clans.get(0));
  }

  @Override
  public List<Map> findMapsByQuery(SearchConfig searchConfig, int page, int count) {
    MultiValueMap<String, String> parameterMap = new LinkedMultiValueMap<>();
    if (searchConfig.hasQuery()) {
      parameterMap.add("filter", searchConfig.getSearchQuery() + ";latestVersion.hidden==\"false\"");
    }
    parameterMap.add("include", "latestVersion,latestVersion.reviews,latestVersion.reviews.player,author,statistics,latestVersion.reviewsSummary");
    parameterMap.add("sort", searchConfig.getSortConfig().toQuery());
    return getPage(MAP_ENDPOINT, count, page, parameterMap);
  }

  @Override
  public Optional<MapVersion> findMapVersionById(String id) {
    // FIXME: that is not gonna work this way
    // FIXME: filter hidden maps
    return Optional.ofNullable(getOne(MAP_ENDPOINT + "/" + id, MapVersion.class));
  }

  @Override
  @Cacheable(CacheNames.COOP_MAPS)
  public List<CoopMission> getCoopMissions() {
    return this.getAll("/data/coopMission", ImmutableMap.of(
      "include", COOP_MISSION_INCLUDES
    ));
  }

  @Override
  @Cacheable(CacheNames.COOP_LEADERBOARD)
  public List<CoopResult> getCoopLeaderboard(String missionId, int numberOfPlayers) {
    return getMany("/data/coopResult", 1000, ImmutableMap.of(
      "filter", rsql(qBuilder().intNum("playerCount").eq(numberOfPlayers)
        .and().string("mission").eq(missionId)),
      "include", COOP_RESULT_INCLUDES,
      "sort", "duration"
    ));
  }

  @Override
  @SneakyThrows
  public List<Tournament> getAllTournaments() {
    return Arrays.asList(restOperations.getForObject(TOURNAMENT_LIST_ENDPOINT, Tournament[].class));
  }

  @Override
  @SneakyThrows
  public void authorize(int playerId, String username, String password) {
    Api apiProperties = clientProperties.getApi();

    ResourceOwnerPasswordResourceDetails details = new ResourceOwnerPasswordResourceDetails();
    details.setClientId(apiProperties.getClientId());
    details.setClientSecret(apiProperties.getClientSecret());
    details.setClientAuthenticationScheme(AuthenticationScheme.header);
    details.setAccessTokenUri(apiProperties.getBaseUrl() + OAUTH_TOKEN_PATH);
    details.setUsername(username);
    details.setPassword(password);

    restOperations = restTemplateBuilder
      // Base URL can be changed in login window
      .rootUri(apiProperties.getBaseUrl())
      .configure(new OAuth2RestTemplate(details));

    authorizedLatch.countDown();
  }

  @NotNull
  private MultiValueMap<String, Object> createFileMultipart(Path file, ByteCountListener listener) {
    MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
    form.add("file", new CountingFileSystemResource(file, listener));
    return form;
  }

  @SneakyThrows
  private void post(String endpointPath, Object request, boolean bufferRequestBody) {
    authorizedLatch.await();
    requestFactory.setBufferRequestBody(bufferRequestBody);

    try {
      // Don't use Void.class here, otherwise Spring won't even try to deserialize error messages in the body
      restOperations.postForEntity(endpointPath, request, String.class);
    } finally {
      requestFactory.setBufferRequestBody(true);
    }
  }

  @SneakyThrows
  private <T> T post(String endpointPath, Object request, Class<T> type) {
    authorizedLatch.await();
    ResponseEntity<T> entity = restOperations.postForEntity(endpointPath, request, type);
    return entity.getBody();
  }

  @SneakyThrows
  private <T> T patch(String endpointPath, Object request, Class<T> type) {
    authorizedLatch.await();
    return restOperations.patchForObject(endpointPath, request, type);
  }

  private void delete(String endpointPath) {
    restOperations.delete(endpointPath);
  }

  @SneakyThrows
  private <T> T getOne(String endpointPath, Class<T> type) {
    return restOperations.getForObject(endpointPath, type, Collections.emptyMap());
  }

  @SneakyThrows
  private <T> T getOne(String endpointPath, Class<T> type, java.util.Map<String, Serializable> params) {
    java.util.Map<String, List<String>> multiValues = params.entrySet().stream()
      .collect(Collectors.toMap(Entry::getKey, entry -> Collections.singletonList(String.valueOf(entry.getValue()))));

    UriComponents uriComponents = UriComponentsBuilder.fromPath(endpointPath)
      .queryParams(CollectionUtils.toMultiValueMap(multiValues))
      .build();

    authorizedLatch.await();
    return getOne(uriComponents.toUriString(), type);
  }

  private <T> List<T> getAll(String endpointPath) {
    return getAll(endpointPath, Collections.emptyMap());
  }

  private <T> List<T> getAll(String endpointPath, java.util.Map<String, Serializable> params) {
    return getMany(endpointPath, clientProperties.getApi().getMaxPageSize(), params);
  }

  @SneakyThrows
  private <T> List<T> getMany(String endpointPath, int count, java.util.Map<String, Serializable> params) {
    List<T> result = new LinkedList<>();
    List<T> current = null;
    int page = 1;
    int maxPageSize = clientProperties.getApi().getMaxPageSize();
    while ((current == null || current.size() >= maxPageSize) && result.size() < count) {
      current = getPage(endpointPath, count, page++, params);
      result.addAll(current);
    }
    return result;
  }

  private <T> List<T> getPage(String endpointPath, int pageSize, int page, java.util.Map<String, Serializable> params) {
    java.util.Map<String, List<String>> multiValues = params.entrySet().stream()
      .collect(Collectors.toMap(Entry::getKey, entry -> Collections.singletonList(String.valueOf(entry.getValue()))));

    return getPage(endpointPath, pageSize, page, CollectionUtils.toMultiValueMap(multiValues));
  }

  @SuppressWarnings("unchecked")
  @SneakyThrows
  private <T> List<T> getPage(String endpointPath, int pageSize, int page, MultiValueMap<String, String> params) {
    UriComponents uriComponents = UriComponentsBuilder.fromPath(endpointPath)
      .queryParams(params)
      .replaceQueryParam("page[size]", pageSize)
      .replaceQueryParam("page[number]", page)
      .build();

    authorizedLatch.await();
    return (List<T>) restOperations.getForObject(uriComponents.toUriString(), List.class);
  }
}
