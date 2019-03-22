package com.faforever.client.play;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.game.Game;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.preferences.TilesSortingOrder;
import com.faforever.client.theme.UiService;
import com.google.common.annotations.VisibleForTesting;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class GamesTilesContainerController implements Controller<Node> {

  private final UiService uiService;
  private final ListChangeListener<Game> gameListChangeListener;
  private final WeakListChangeListener<Game> weakGameListChangeListener;
  private final PreferencesService preferencesService;
  private final ChangeListener<? super TilesSortingOrder> sortingListener;
  public FlowPane tiledFlowPane;
  public ScrollPane tiledScrollPane;
  @VisibleForTesting
  Map<Integer, Node> uidToGameCard;
  private ObjectProperty<Game> selectedGame;
  private Comparator<Node> appliedComparator;


  public GamesTilesContainerController(UiService uiService, PreferencesService preferencesService) {
    this.uiService = uiService;
    this.preferencesService = preferencesService;
    selectedGame = new SimpleObjectProperty<>();

    sortingListener = (observable, oldValue, newValue) -> {
      if (newValue == null) {
        return;
      }
      preferencesService.getPreferences().setGameTileSortingOrder(newValue);
      preferencesService.storeInBackground();
      appliedComparator = newValue.getComparator();
      sortNodes();
    };

    gameListChangeListener = change -> Platform.runLater(() -> {
      synchronized (change) {
        while (change.next()) {
          change.getRemoved().forEach(gameInfoBean -> tiledFlowPane.getChildren().remove(uidToGameCard.remove(gameInfoBean.getId())));
          change.getAddedSubList().forEach(GamesTilesContainerController.this::addGameCard);
        }
        sortNodes();
      }
    });
    weakGameListChangeListener = new WeakListChangeListener<>(gameListChangeListener);
  }

  private void sortNodes() {
    ObservableList<Node> sortedChildren = tiledFlowPane.getChildren().sorted(appliedComparator);
    tiledFlowPane.getChildren().setAll(sortedChildren);
  }

  public void initialize() {
    JavaFxUtil.fixScrollSpeed(tiledScrollPane);
  }

  ReadOnlyObjectProperty<Game> selectedGameProperty() {
    return this.selectedGame;
  }

  @VisibleForTesting
  void createTiledFlowPane(ObservableList<Game> games, ComboBox<TilesSortingOrder> choseSortingTypeChoiceBox) {
    initializeChoiceBox(choseSortingTypeChoiceBox);
    uidToGameCard = new HashMap<>();
    games.forEach(this::addGameCard);
    games.removeListener(weakGameListChangeListener);
    JavaFxUtil.addListener(games, weakGameListChangeListener);
    selectFirstGame();
    sortNodes();
  }

  private void initializeChoiceBox(ComboBox<TilesSortingOrder> sortingTypeChoiceBox) {
    sortingTypeChoiceBox.setVisible(true);
    sortingTypeChoiceBox.getItems().addAll(TilesSortingOrder.values());
    sortingTypeChoiceBox.getSelectionModel().selectedItemProperty().addListener(new WeakChangeListener<>(sortingListener));
    sortingTypeChoiceBox.getSelectionModel().select(preferencesService.getPreferences().getGameTileSortingOrder());
  }

  private void selectFirstGame() {
    ObservableList<Node> cards = tiledFlowPane.getChildren();
    if (!cards.isEmpty()) {
      selectedGame.set((Game) cards.get(0).getUserData());
    }
  }

  private void addGameCard(Game game) {
    GameTileController gameTileController = uiService.loadFxml("theme/play/game_card.fxml");
    gameTileController.setGame(game);
    gameTileController.setOnSelectedListener(selection -> selectedGame.set(selection));

    Node root = gameTileController.getRoot();
    root.setUserData(game);
    tiledFlowPane.getChildren().add(root);
    uidToGameCard.put(game.getId(), root);
  }

  public Node getRoot() {
    return tiledScrollPane;
  }

}
