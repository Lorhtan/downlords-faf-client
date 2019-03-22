package com.faforever.client.integration.v2.client;

import com.faforever.client.game.GameStateReport;
import com.faforever.client.game.HostGameRequest;
import com.faforever.client.game.IceMessage;
import com.faforever.client.game.JoinGameClientMessage;
import com.faforever.client.game.RestoreGameSessionRequest;
import com.faforever.client.game.relay.GpgMessage;
import com.faforever.client.mapstruct.MapStructConfig;
import com.faforever.client.player.UpdateSocialRelationClientMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.supcomhub.server.protocol.v2.dto.client.CancelMatchSearchClientMessage;
import org.supcomhub.server.protocol.v2.dto.client.GameStateClientMessage;
import org.supcomhub.server.protocol.v2.dto.client.HostGameClientMessage;
import org.supcomhub.server.protocol.v2.dto.client.IceClientMessage;
import org.supcomhub.server.protocol.v2.dto.client.ListIceServersClientMessage;
import org.supcomhub.server.protocol.v2.dto.client.RestoreGameSessionClientMessage;
import org.supcomhub.server.protocol.v2.dto.client.SearchMatchClientMessage;
import org.supcomhub.server.protocol.v2.dto.client.SelectAvatarClientMessage;

@Mapper(config = MapStructConfig.class)
public interface V2ClientMessageMapper {

  CancelMatchSearchClientMessage map(com.faforever.client.matchmaking.CancelMatchSearchClientMessage message);

  GameStateClientMessage map(GameStateReport message);

  IceClientMessage map(IceMessage message);

  org.supcomhub.server.protocol.v2.dto.client.JoinGameClientMessage map(JoinGameClientMessage message);

  GenericGpgClientMessage map(GpgMessage message);

  RestoreGameSessionClientMessage map(RestoreGameSessionRequest message);

  SearchMatchClientMessage map(com.faforever.client.matchmaking.SearchMatchClientMessage message);

  SelectAvatarClientMessage map(com.faforever.client.avatar.SelectAvatarClientMessage message);

  @Mapping(source = "featuredMod.id", target = "mod")
  @Mapping(source = "mapName", target = "map")
  @Mapping(source = "gameVisibility", target = "visibility")
  HostGameClientMessage map(HostGameRequest message);

  org.supcomhub.server.protocol.v2.dto.client.UpdateSocialRelationClientMessage map(UpdateSocialRelationClientMessage message);

  ListIceServersClientMessage map(com.faforever.client.game.relay.ice.ListIceServersClientMessage message);

}
