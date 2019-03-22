package com.faforever.client.mod;

import org.supcomhub.api.dto.ApiException;

public class ModUploadFailedException extends RuntimeException {
  public ModUploadFailedException(ApiException e) {
    super(e.getLocalizedMessage());
  }
}
