package com.faforever.client.api;

import com.faforever.client.achievements.Achievement;
import com.faforever.client.achievements.PlayerAchievement;
import com.faforever.client.avatar.Avatar;
import com.faforever.client.clan.Clan;
import com.faforever.client.coop.CoopMission;
import com.faforever.client.coop.CoopResult;
import com.faforever.client.event.PlayerEvent;
import com.faforever.client.leaderboard.LeaderboardEntry;
import com.faforever.client.map.FaMap;
import com.faforever.client.mapstruct.MapStructConfig;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.mod.FeaturedModFile;
import com.faforever.client.mod.Mod;
import com.faforever.client.mod.ModVersion;
import com.faforever.client.player.NameRecord;
import com.faforever.client.player.Player;
import com.faforever.client.replay.Replay;
import com.faforever.client.review.Review;
import com.faforever.client.review.ReviewsSummary;
import com.faforever.client.tournament.Tournament;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.supcomhub.api.dto.Account;
import org.supcomhub.api.dto.ClanMembership;
import org.supcomhub.api.dto.GameReview;
import org.supcomhub.api.dto.GameReviewSummary;
import org.supcomhub.api.dto.LadderMap;
import org.supcomhub.api.dto.MapReview;
import org.supcomhub.api.dto.MapVersion;
import org.supcomhub.api.dto.MapVersionReview;
import org.supcomhub.api.dto.ModReview;
import org.supcomhub.api.dto.ModReviewSummary;
import org.supcomhub.api.dto.ModVersionReview;
import org.supcomhub.api.dto.ReviewScoreCount;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(config = MapStructConfig.class, uses = {
  GameParticipantsMapper.class,
  FxMapper.class
})
public interface ApiDtoMapper {
  Avatar map(org.supcomhub.api.dto.Avatar dto);

  Achievement map(org.supcomhub.api.dto.Achievement dto);

  @Mapping(target = "socialStatus", ignore = true)
  @Mapping(target = "numberOfGames", ignore = true)
  @Mapping(target = "clanTag", ignore = true)
  @Mapping(target = "country", ignore = true)
  @Mapping(target = "avatarUrl", ignore = true)
  @Mapping(target = "avatarTooltip", ignore = true)
  @Mapping(target = "game", ignore = true)
  @Mapping(target = "idleSince", ignore = true)
  @Mapping(target = "names", ignore = true)
  @Mapping(target = "chatChannelUsers", ignore = true)
  @Mapping(target = "rating", ignore = true)
//TODO: implement lazy fetching
  Player map(Account dto);


  @Mapping(target = "selected", ignore = true)
  @Mapping(target = "selectable", ignore = true)
  @Mapping(target = "likes", ignore = true)
  @Mapping(target = "played", ignore = true)
  @Mapping(target = "comments", ignore = true)
  @Mapping(target = "reviewsSummary", ignore = true)
  @Mapping(target = "modType", ignore = true)
  @Mapping(target = "mountInfos", ignore = true)
  @Mapping(target = "hookDirectories", ignore = true)
  @Mapping(target = "uploader", source = "mod.uploader.displayName")
  @Mapping(target = "imagePath", ignore = true)
  @Mapping(target = "displayName", source = "mod.displayName")
  ModVersion map(org.supcomhub.api.dto.ModVersion modVersion);

  @Mapping(target = "version", source = "map.version")
  @Mapping(target = "mapFolderName", source = "map.folderName")
  CoopMission map(org.supcomhub.api.dto.CoopMission coopMission);

  @Mapping(target = "gameId", source = "game.id")
  CoopResult map(org.supcomhub.api.dto.CoopResult coopResult);

  FeaturedModFile map(org.supcomhub.api.dto.FeaturedModFile featuredModFile);

  @Mapping(target = "title", source = "name")
  @Mapping(target = "replayFile", ignore = true)
  @Mapping(target = "teams", source = "playerStats")
  @Mapping(target = "chatMessages", ignore = true)
  @Mapping(target = "gameOptions", ignore = true)
  @Mapping(target = "teamPlayerStats", ignore = true)
  @Mapping(target = "views", ignore = true)
  @Mapping(target = "map", source = "mapVersion")
  Replay map(org.supcomhub.api.dto.Game game);

  PlayerAchievement map(org.supcomhub.api.dto.PlayerAchievement playerAchievement);

  @Mapping(target = "smallThumbnailUrl", source = "thumbnailUrlSmall")
  @Mapping(target = "largeThumbnailUrl", source = "thumbnailUrlLarge")
  @Mapping(target = "author", source = "map.uploader.displayName")
  @Mapping(target = "numberOfPlays", ignore = true)
  @Mapping(target = "downloads", ignore = true)
  @Mapping(target = "reviewsSummary", ignore = true)
  @Mapping(target = "players", source = "maxPlayers")
  @Mapping(target = "displayName", source = "map.displayName")
  @Mapping(target = "type", source = "map.battleType")
  @Mapping(target = "size", expression = "java(com.faforever.client.map.MapSize.valueOf(mapVersion.getWidth(), mapVersion.getHeight()))")
  FaMap map(MapVersion mapVersion);

  default FaMap map(LadderMap ladderMap) {
    return map(ladderMap.getMapVersion());
  }

  @Mapping(target = "type", source = "battleType")
  @Mapping(target = "author", source = "uploader.displayName")
  @Mapping(target = "description", ignore = true)
  @Mapping(target = "numberOfPlays", ignore = true)
  @Mapping(target = "downloads", ignore = true)
  @Mapping(target = "players", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "folderName", ignore = true)
  @Mapping(target = "largeThumbnailUrl", ignore = true)
  @Mapping(target = "smallThumbnailUrl", ignore = true)
  @Mapping(target = "hidden", ignore = true)
  @Mapping(target = "ranked", ignore = true)
  @Mapping(target = "reviewsSummary", ignore = true)
  @Mapping(target = "downloadUrl", ignore = true)
  @Mapping(target = "size", ignore = true)
  FaMap map(org.supcomhub.api.dto.Map map);

  //  @Mapping(target = "totalGames", ignore = true)   TODO: add ?, was ignored before
//  @Mapping(target = "wonGames", ignore = true)
  LeaderboardEntry map(org.supcomhub.api.dto.LeaderboardEntry leaderboardEntry);

  Mod map(org.supcomhub.api.dto.Mod mod);

  FeaturedMod map(org.supcomhub.api.dto.FeaturedMod featuredMod);

  @Mapping(target = "members", source = "memberships")
  Clan map(org.supcomhub.api.dto.Clan clan);

  default Player map(ClanMembership membership) {
    return map(membership.getMember());
  }

  List<Player> mapClanMemberships(List<ClanMembership> clanMemberships);

  Tournament map(org.supcomhub.api.dto.challonge.Tournament tournament);

  NameRecord map(NameRecord nameRecord);

  Review map(MapReview dto);

  List<Review> mapModVersionReviews(List<ModVersionReview> list);

  Review map(MapVersionReview dto);

  Review map(GameReview dto);

  Review map(ModReview dto);

  ReviewsSummary map(GameReviewSummary gameReviewSummary);

  ReviewsSummary map(ModReviewSummary modReviewSummary);

  PlayerEvent map(org.supcomhub.api.dto.PlayerEvent playerEvent);

  default ComparableVersion map(short version) {
    return new ComparableVersion(String.valueOf(version));
  }

  default LocalDateTime map(OffsetDateTime dateTime) {
    return LocalDateTime.from(dateTime);
  }

  default ObservableMap<Byte, Integer> map(List<ReviewScoreCount> reviewScoreCounts) {
    return FXCollections.observableMap(reviewScoreCounts.stream()
      .collect(Collectors.toMap(ReviewScoreCount::getScore, ReviewScoreCount::getCount)));
  }

  default URL map(String url) {
    try {
      return new URL(url);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  default String map(URL url) {
    return url.toExternalForm();
  }
}
