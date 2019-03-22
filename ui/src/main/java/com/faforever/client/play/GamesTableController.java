package com.faforever.client.play;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.StringCell;
import com.faforever.client.game.Game;
import com.faforever.client.game.PlayerFill;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapPreviewService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import com.google.common.base.Joiner;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SortEvent;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.util.Pair;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class GamesTableController implements Controller<Node> {

  private final ObjectProperty<Game> selectedGame;
  private final MapPreviewService mapPreviewService;
  private final JoinGameHelper joinGameHelper;
  private final I18n i18n;
  private final UiService uiService;
  private final PreferencesService preferencesService;
  public TableView<Game> gamesTable;
  public TableColumn<Game, Image> mapPreviewColumn;
  public TableColumn<Game, String> gameTitleColumn;
  public TableColumn<Game, PlayerFill> playersColumn;
  public TableColumn<Game, RankRange> rankColumn;
  public TableColumn<Game, String> modsColumn;
  public TableColumn<Game, String> hostColumn;
  public TableColumn<Game, Boolean> passwordProtectionColumn;
  public TableColumn<Game, String> coopMissionName;


  public GamesTableController(MapPreviewService mapPreviewService, JoinGameHelper joinGameHelper, I18n i18n, UiService uiService, PreferencesService preferencesService) {
    this.mapPreviewService = mapPreviewService;
    this.joinGameHelper = joinGameHelper;
    this.i18n = i18n;
    this.uiService = uiService;
    this.preferencesService = preferencesService;

    this.selectedGame = new SimpleObjectProperty<>();
  }

  public ObjectProperty<Game> selectedGameProperty() {
    return selectedGame;
  }

  public Node getRoot() {
    return gamesTable;
  }

  public void initializeGameTable(ObservableList<Game> games) {
    initializeGameTable(games, null);
  }

  public void initializeGameTable(ObservableList<Game> games, Function<String, String> coopMissionNameProvider) {
    SortedList<Game> sortedList = new SortedList<>(games);
    sortedList.comparatorProperty().bind(gamesTable.comparatorProperty());
    gamesTable.setPlaceholder(new Label(i18n.get("games.noGamesAvailable")));
    gamesTable.setRowFactory(param1 -> gamesRowFactory());
    gamesTable.setItems(sortedList);

    applyLastSorting(gamesTable);
    gamesTable.setOnSort(this::onColumnSorted);

    JavaFxUtil.addListener(sortedList, (Observable observable) -> selectFirstGame());
    selectFirstGame();

    passwordProtectionColumn.setCellValueFactory(param -> param.getValue().passwordProtectedProperty());
    passwordProtectionColumn.setCellFactory(param -> passwordIndicatorColumn());
    mapPreviewColumn.setCellFactory(param -> new MapPreviewTableCell(uiService));
    mapPreviewColumn.setCellValueFactory(param -> Bindings.createObjectBinding(
      () -> mapPreviewService.loadPreview(param.getValue().getMapName(), PreviewSize.SMALL),
      param.getValue().mapNameProperty()
    ));

    gameTitleColumn.setCellValueFactory(param -> param.getValue().titleProperty());
    gameTitleColumn.setCellFactory(param -> new StringCell<>(title -> title));

    playersColumn.setCellValueFactory(param -> Bindings.createObjectBinding(
      () -> new PlayerFill(countPlayers(param.getValue()), param.getValue().getMaxPlayers()),
      // FIXME this comiles, but modifying a team won't trigger an update
      param.getValue().teamsProperty(), param.getValue().maxPlayersProperty())
    );
    playersColumn.setCellFactory(param -> playersCell());
    rankColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(new RankRange(param.getValue().getMinRank(), param.getValue().getMaxRank())));
    rankColumn.setCellFactory(param -> rankTableCell());
    hostColumn.setCellValueFactory(param -> param.getValue().hostNameProperty());
    hostColumn.setCellFactory(param -> new StringCell<>(String::toString));
    modsColumn.setCellValueFactory(this::modCell);
    modsColumn.setCellFactory(param -> new StringCell<>(String::toString));
    coopMissionName.setVisible(coopMissionNameProvider != null);

    if (coopMissionNameProvider != null) {
      coopMissionName.setCellFactory(param -> new StringCell<>(name -> name));
      coopMissionName.setCellValueFactory(param -> new SimpleObjectProperty<>(coopMissionNameProvider.apply(param.getValue().getMapName())));
    }


    gamesTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue)
      -> Platform.runLater(() -> selectedGame.set(newValue)));
  }

  private Integer countPlayers(Game game) {
    return game.getTeams().entrySet().stream()
      // TODO 0 is "observers" and 1 is "no team". Move to method and/or constants, e.g. in GameService
      .filter(teamIdToPlayers -> teamIdToPlayers.getKey() > 1)
      .mapToInt(integerListEntry -> integerListEntry.getValue().size())
      .sum();
  }

  public void setModsColumnVisibility(boolean isVisible) {
    modsColumn.setVisible(isVisible);
  }

  public void setPasswordProtectionColumnVisibility(boolean isVisible) {
    passwordProtectionColumn.setVisible(isVisible);
  }

  private void applyLastSorting(TableView<Game> gamesTable) {
    final Map<String, SortType> lookup = new HashMap<>();
    final ObservableList<TableColumn<Game, ?>> sortOrder = gamesTable.getSortOrder();
    preferencesService.getPreferences().getGameListSorting().forEach(sorting -> lookup.put(sorting.getKey(), sorting.getValue()));
    sortOrder.clear();
    gamesTable.getColumns().forEach(gameTableColumn -> {
      if (lookup.containsKey(gameTableColumn.getId())) {
        gameTableColumn.setSortType(lookup.get(gameTableColumn.getId()));
        sortOrder.add(gameTableColumn);
      }
    });
  }

  private void onColumnSorted(@NotNull SortEvent<TableView<Game>> event) {
    List<Pair<String, SortType>> sorters = event.getSource().getSortOrder()
        .stream()
        .map(column -> new Pair<>(column.getId(), column.getSortType()))
        .collect(Collectors.toList());

    preferencesService.getPreferences().getGameListSorting().setAll(sorters);
    preferencesService.storeInBackground();
  }

  @NotNull
  private ObservableValue<String> modCell(CellDataFeatures<Game, String> param) {
    int simModCount = param.getValue().getSimMods().size();
    List<String> modNames = param.getValue().getSimMods().entrySet().stream()
        .limit(2)
        .map(Entry::getValue)
        .collect(Collectors.toList());
    if (simModCount > 2) {
      return new SimpleStringProperty(i18n.get("game.mods.twoAndMore", modNames.get(0), modNames.size()));
    }
    return new SimpleStringProperty(Joiner.on(i18n.get("textSeparator")).join(modNames));
  }

  private void selectFirstGame() {
    TableView.TableViewSelectionModel<Game> selectionModel = gamesTable.getSelectionModel();
    if (selectionModel.getSelectedItem() == null && !gamesTable.getItems().isEmpty()) {
      Platform.runLater(() -> selectionModel.select(0));
    }
  }

  @NotNull
  private TableRow<Game> gamesRowFactory() {
    TableRow<Game> row = new TableRow<>();
    row.setOnMouseClicked(event -> {
      if (event.getClickCount() == 2) {
        Game game = row.getItem();
        joinGameHelper.join(game);
      }
    });
    return row;
  }

  private TableCell<Game, Boolean> passwordIndicatorColumn() {
    return new StringCell<>(
        isPasswordProtected -> isPasswordProtected ? "\uD83D\uDD12" : "",
        Pos.CENTER, UiService.CSS_CLASS_ICON);
  }

  private TableCell<Game, PlayerFill> playersCell() {
    return new StringCell<>(playerFill -> i18n.get("game.players.format",
        playerFill.getPlayers(), playerFill.getMaxPlayers()), Pos.CENTER);
  }

  private TableCell<Game, RankRange> rankTableCell() {
    return new StringCell<>(ratingRange -> {
      if (ratingRange.getMin() == null && ratingRange.getMax() == null) {
        return "";
      }

      if (ratingRange.getMin() != null && ratingRange.getMax() != null) {
        return i18n.get("game.ratingFormat.minMax", ratingRange.getMin(), ratingRange.getMax());
      }

      if (ratingRange.getMin() != null) {
        return i18n.get("game.ratingFormat.minOnly", ratingRange.getMin());
      }

      return i18n.get("game.ratingFormat.maxOnly", ratingRange.getMax());
    }, Pos.CENTER);
  }

  @Value
  private static class RankRange {
    Integer min;
    Integer max;
  }
}
