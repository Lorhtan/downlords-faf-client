package com.faforever.client.integration.v2.server;

import com.faforever.client.chat.ChatChannelsServerMessage;
import com.faforever.client.game.GameInfoServerMessage;
import com.faforever.client.game.GameInfosServerMessage;
import com.faforever.client.game.GameResultMessage;
import com.faforever.client.game.HostGameMessage;
import com.faforever.client.game.StartGameProcessServerMessage;
import com.faforever.client.game.relay.GpgMessage;
import com.faforever.client.game.relay.ice.IceServersServerMessage;
import com.faforever.client.matchmaking.MatchMakerInfoServerMessage;
import com.faforever.client.notification.InfoServerMessage;
import com.faforever.client.player.PlayerServerMessage;
import com.faforever.client.player.PlayersServerMessage;
import com.faforever.client.player.SocialRelationsServerMessage;
import com.faforever.client.remote.ErrorServerMessage;
import com.faforever.client.remote.UpdatedAchievementsServerMessage;
import com.faforever.client.user.AccountDetailsServerMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.supcomhub.server.protocol.v2.dto.server.V2GpgServerMessage;

import java.net.MalformedURLException;
import java.net.URL;

@Mapper(componentModel = "spring")
public interface V2ServerMessageMapper {

  GpgMessage map(V2GpgServerMessage source);

  ChatChannelsServerMessage map(org.supcomhub.server.protocol.v2.dto.server.ChatChannelsServerMessage source);

  @Mapping(source = "mod.name", target = "mod")
  @Mapping(source = "mod.version", target = "modVersion")
  GameInfoServerMessage map(org.supcomhub.server.protocol.v2.dto.server.GameInfoServerMessage source);

  IceServersServerMessage map(org.supcomhub.server.protocol.v2.dto.server.IceServersServerMessage source);

  InfoServerMessage map(org.supcomhub.server.protocol.v2.dto.server.InfoServerMessage source);

  AccountDetailsServerMessage map(org.supcomhub.server.protocol.v2.dto.server.AccountDetailsServerMessage source);

  MatchMakerInfoServerMessage map(org.supcomhub.server.protocol.v2.dto.server.MatchMakerInfoServerMessage source);

  SocialRelationsServerMessage map(org.supcomhub.server.protocol.v2.dto.server.SocialRelationsServerMessage source);

  StartGameProcessServerMessage map(org.supcomhub.server.protocol.v2.dto.server.StartGameProcessServerMessage source);

  UpdatedAchievementsServerMessage map(org.supcomhub.server.protocol.v2.dto.server.UpdatedAchievementsServerMessage source);

  PlayersServerMessage map(org.supcomhub.server.protocol.v2.dto.server.PlayersServerMessage source);

  PlayerServerMessage map(org.supcomhub.server.protocol.v2.dto.server.PlayerServerMessage source);

  HostGameMessage map(org.supcomhub.server.protocol.v2.dto.server.HostGameServerMessage source);

  GameInfosServerMessage map(org.supcomhub.server.protocol.v2.dto.server.GameInfosServerMessage source);

  GameResultMessage map(org.supcomhub.server.protocol.v2.dto.server.GameResultMessage source);

  ErrorServerMessage map(org.supcomhub.server.protocol.v2.dto.server.ErrorServerMessage source);

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
