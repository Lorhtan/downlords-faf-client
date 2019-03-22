package com.faforever.client.user;

import lombok.Value;

@Value
public class LoginSuccessEvent {
  String username;
  String displayName;
  String password;
  int userId;
}
