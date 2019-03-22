package com.faforever.client.map;

import org.supcomhub.api.dto.ApiException;

public class MapUploadFailedException extends RuntimeException {
  public MapUploadFailedException(ApiException e) {
    super(e.getLocalizedMessage());
  }
}
