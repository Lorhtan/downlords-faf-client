package com.faforever.client.game;

/**
 * The state of a game as stored on the server.
 */
public enum GameState {
  /** The game has been created but the host's game process has not yet started. */
  INITIALIZING,
  /** The game has been created and the host's game process has been started. */
  OPEN,
  /** The game has been launched. */
  PLAYING,
  /** The game simulation ended but not all players closed their game process yet. */
  ENDED,
  /** All players closed their game process. */
  CLOSED
}
