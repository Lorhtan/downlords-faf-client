package com.faforever.client.map;

import com.faforever.client.asset.AssetService;
import com.faforever.client.config.CacheNames;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Vault;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.ProgrammingError;
import com.google.common.net.UrlEscapers;
import javafx.scene.image.Image;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.nio.file.Paths;
import java.util.Locale;

import static com.github.nocatch.NoCatch.noCatch;

@Lazy
@Service
// TODO unit test
public class MapPreviewService {

  private final AssetService assetService;
  private final UiService uiService;
  private final ClientProperties clientProperties;

  public MapPreviewService(AssetService assetService, UiService uiService, ClientProperties clientProperties) {
    this.assetService = assetService;
    this.uiService = uiService;
    this.clientProperties = clientProperties;
  }

  private static URL getPreviewUrl(String mapName, String baseUrl, PreviewSize previewSize) {
    return noCatch(() -> new URL(String.format(baseUrl, previewSize.folderName, UrlEscapers.urlFragmentEscaper().escape(mapName).toLowerCase(Locale.US))));
  }

  /**
   * Loads the preview of a map or returns a "unknown map" image.
   */
  @Cacheable(CacheNames.MAP_PREVIEW)
  public Image loadPreview(FaMap map, PreviewSize previewSize) {
    URL url;
    switch (previewSize) {
      case SMALL:
        url = map.getSmallThumbnailUrl();
        break;
      case LARGE:
        url = map.getLargeThumbnailUrl();
        break;
      default:
        throw new ProgrammingError("Uncovered preview size: " + previewSize);
    }
    return loadPreview(url, previewSize);
  }

  @Cacheable(CacheNames.MAP_PREVIEW)
  public Image loadPreview(URL url, PreviewSize previewSize) {
    return assetService.loadAndCacheImage(url, Paths.get("maps").resolve(previewSize.folderName),
      () -> uiService.getThemeImage(UiService.UNKNOWN_MAP_IMAGE));
  }

  @Cacheable(value = CacheNames.MAP_PREVIEW, unless = "#result == null")
  public Image loadPreview(String mapName, PreviewSize previewSize) {
    String mapPreviewUrlFormat = clientProperties.getVault().getMapPreviewUrlFormat();
    return loadPreview(getPreviewUrl(mapName, mapPreviewUrlFormat, previewSize), previewSize);
  }
}
