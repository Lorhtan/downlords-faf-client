package com.faforever.client.play;

import com.faforever.client.game.Game;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.preferences.TilesSortingOrder;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.CountDownLatch;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GamesTilesContainerControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  private UiService uiService;
  @Mock
  private PreferencesService preferencesService;

  private GamesTilesContainerController instance;
  private Preferences preferences;

  @Before
  public void setUp() throws Exception {
    instance = new GamesTilesContainerController(uiService, preferencesService);

    when(uiService.loadFxml("theme/play/game_card.fxml")).thenAnswer(invocation -> {
      GameTileController controller = mock(GameTileController.class);
      when(controller.getRoot()).thenReturn(new Pane());
      return controller;
    });
    preferences = new Preferences();
    when(preferencesService.getPreferences()).thenReturn(preferences);

    loadFxml("theme/play/games_tiles_container.fxml", clazz -> instance);
  }

  @Test
  public void testCreateTiledFlowPaneWithEmptyList() throws Exception {
    ObservableList<Game> observableList = FXCollections.observableArrayList();

    instance.createTiledFlowPane(observableList, new ComboBox<>());

    assertThat(instance.tiledFlowPane.getChildren(), empty());
  }

  @Test
  public void testCreateTiledFlowPaneWithPopulatedList() throws Exception {
    ObservableList<Game> observableList = FXCollections.observableArrayList();
    observableList.add(new Game());

    instance.createTiledFlowPane(observableList, new ComboBox<>());

    assertThat(instance.tiledFlowPane.getChildren(), hasSize(1));
  }

  @Test
  public void testCreateTiledFlowPaneWithPostInstantiatedGameInfoBean() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    instance.tiledFlowPane.getChildren().addListener((Observable observable) -> latch.countDown());

    ObservableList<Game> observableList = FXCollections.observableArrayList();

    instance.createTiledFlowPane(observableList, new ComboBox<>());
    observableList.add(new Game());

    latch.await();
    assertThat(instance.tiledFlowPane.getChildren(), hasSize(1));
  }

  @Test
  public void testCreateTiledFlowPaneWithPopulatedListAndPostInstantiatedGameInfoBean() throws Exception {
    CountDownLatch latch = new CountDownLatch(2);
    ObservableList<Node> children = instance.tiledFlowPane.getChildren();
    children.addListener((Observable observable) -> latch.countDown());

    ObservableList<Game> observableList = FXCollections.observableArrayList();

    observableList.add(new Game());
    instance.createTiledFlowPane(observableList, new ComboBox<>());
    observableList.add(new Game());

    latch.await();

    WaitForAsyncUtils.waitForFxEvents();

    assertThat(children, hasSize(2));
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), instanceOf(Node.class));
  }

  @Test
  public void testSorting() {
    ObservableList<Game> observableList = FXCollections.observableArrayList();
    Game game1 = new Game();
    game1.setTitle("abc");
    game1.setId(234);
    game1.setNumPlayers(5);

    Game game2 = new Game();
    game2.setTitle("xyz");
    game2.setId(123);
    game2.setNumPlayers(6);

    observableList.addAll(game1, game2);

    preferences.setGameTileSortingOrder(TilesSortingOrder.PLAYER_DES);

    instance.createTiledFlowPane(observableList, new ComboBox<>());
    assertEquals(instance.uidToGameCard.get(game2.getId()), instance.tiledFlowPane.getChildren().get(0));
  }
}
