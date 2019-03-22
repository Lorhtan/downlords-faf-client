package com.faforever.client.map;

import com.faforever.client.asset.AssetService;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.theme.UiService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class MapPreviewServiceTest {

  private MapPreviewService instance;

  @Mock
  private AssetService assetService;
  @Mock
  private UiService uiService;
  private ClientProperties clientProperties;

  @Before
  public void setUp() throws Exception {
    clientProperties = new ClientProperties();
    instance = new MapPreviewService(assetService, uiService, clientProperties);
  }

  @Test
  public void testLoadPreview() {
    clientProperties.getVault().setMapPreviewUrlFormat("http://example.com/%s");
    for (PreviewSize previewSize : PreviewSize.values()) {
      Path cacheSubDir = Paths.get("maps").resolve(previewSize.folderName);
      instance.loadPreview("preview", previewSize);
      verify(assetService).loadAndCacheImage(any(URL.class), eq(cacheSubDir), any());
    }
  }

}
