package com.faforever.client.user;

import com.faforever.client.remote.ServerMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * Message sent from the server to the client containing details about the logged in account. This message is sent by
 * the server after successful login.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountDetailsServerMessage implements ServerMessage {

  private int id;
  private String displayName;
  private Map<String, Integer> ranks;
  private int numberOfGames;
  private Avatar avatar;
  private String clanTag;

  @Getter
  @Setter
  public static class Avatar {
    private String url;
    private String description;
  }
}
