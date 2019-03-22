package com.faforever.client.integration;

import lombok.experimental.UtilityClass;

/**
 * Holds the names of all channel. A channel's name is also the name of its bean. Channels can be configured in {@link
 * com.faforever.client.config.integration.ChannelConfiguration}.
 */
@UtilityClass
public class ChannelNames {

  /**
   * Channel for messages sent from the client to the server. The payload of messages in this channel is {@link
   * com.faforever.client.remote.ClientMessage}.
   */
  public static final String SERVER_OUTBOUND = "serverOutbound";

  /**
   * Channel for messages sent from the server to the client. The payload of messages in this channel is {@link
   * com.faforever.client.remote.ServerMessage}.
   */
  public static final String SERVER_INBOUND = "serverInbound";

  /**
   * Channel for outbound messages over the standard protocol.
   */
  public static final String WEB_OUTBOUND = "webOutbound";

  /**
   * Channel for all messages that need to be processed, no matter whether they are coming from an external system or
   * from the server itself. This is required to make sure that all actions are processed in a serial order no matter
   * where they originate from.
   */
  public static final String INBOUND_DISPATCH = "inboundDispatch";

  /**
   * Channel for {@link ServerDisconnectedEvent}.
   */
  public static final String SERVER_DISCONNECTED_EVENT = "serverDisconnectedEvent";

  /**
   * Channel for raw outbound messages to be written on the web socket.
   */
  public static final String WEB_SOCKET_OUTBOUND = "webSocketOutbound";

  /** Channel to receive {@link com.faforever.client.user.AccountDetailsServerMessage}. */
  public static final String ACCOUNT_DETAILS = "accountDetailsChannel";
  /** Channel to receive {@link com.faforever.client.player.PlayersServerMessage}. */
  public static final String PLAYERS = "playersChannel";
  /** Channel to receive {@link com.faforever.client.player.PlayerServerMessage}. */
  public static final String PLAYER = "playerChannel";
  /** Channel to receive {@link com.faforever.client.game.GameInfosServerMessage}. */
  public static final String GAMES = "gamesChannel";
  /** Channel to receive {@link com.faforever.client.chat.ChatChannelsServerMessage}. */
  public static final String CHAT_CHANNELS = "chatChannelsChannel";
}
