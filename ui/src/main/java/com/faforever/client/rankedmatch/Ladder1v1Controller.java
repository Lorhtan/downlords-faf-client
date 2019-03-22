package com.faforever.client.rankedmatch;

import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.game.Faction;
import com.faforever.client.game.GameService;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.leaderboard.RatingStat;
import com.faforever.client.map.ShowLadderMapsEvent;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.PlayerUpdatedEvent;
import com.faforever.client.preferences.MissingGamePathEvent;
import com.faforever.client.preferences.PreferenceUpdateListener;
import com.faforever.client.preferences.PreferencesService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Component
@Lazy
// TODO clean up this class
public class Ladder1v1Controller extends AbstractViewController<Node> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final PseudoClass NOTIFICATION_HIGHLIGHTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("highlighted-bar");

  private final Random random;
  private final GameService gameService;
  private final PreferencesService preferencesService;
  private final PlayerService playerService;
  private final LeaderboardService leaderboardService;
  private final I18n i18n;
  public CategoryAxis ratingDistributionXAxis;
  public NumberAxis ratingDistributionYAxis;
  public BarChart<String, Integer> ratingDistributionChart;
  public Label rankHintLabel;
  public Label searchingForOpponentLabel;
  public Label rankLabel;
  public ProgressIndicator searchProgressIndicator;
  public ProgressIndicator rankProgressIndicator;
  public ToggleButton aeonButton;
  public ToggleButton uefButton;
  public ToggleButton cybranButton;
  public ToggleButton seraphimButton;
  public Button cancelButton;
  public Button playButton;
  public ScrollPane ladder1v1Root;
  public Label gamesPlayedLabel;
  public Label rankingLabel;
  public Label winLossRationLabel;
  public Label rankingOutOfLabel;
  @VisibleForTesting
  HashMap<Faction, ToggleButton> factionsToButtons;
  private EventBus eventBus;
  private Text youLabel;
  // Kept as a field in order to prevent garbage collection
  private PreferenceUpdateListener preferenceUpdateListener;


  public Ladder1v1Controller(GameService gameService,
                             PreferencesService preferencesService,
                             PlayerService playerService,
                             LeaderboardService leaderboardService,
                             I18n i18n,
                             EventBus eventBus) {
    this.gameService = gameService;
    this.preferencesService = preferencesService;
    this.playerService = playerService;
    this.leaderboardService = leaderboardService;
    this.i18n = i18n;
    this.eventBus = eventBus;

    random = new Random();

    youLabel = new Text(i18n.get("ranked1v1.you"));
    youLabel.setId("1v1-you-text");
  }

  @Override
  public void initialize() {
    super.initialize();
    cancelButton.managedProperty().bind(cancelButton.visibleProperty());
    playButton.managedProperty().bind(playButton.visibleProperty());
    rankLabel.managedProperty().bind(rankLabel.visibleProperty());
    rankProgressIndicator.managedProperty().bind(rankProgressIndicator.visibleProperty());

    factionsToButtons = new HashMap<>();
    factionsToButtons.put(Faction.AEON, aeonButton);
    factionsToButtons.put(Faction.UEF, uefButton);
    factionsToButtons.put(Faction.CYBRAN, cybranButton);
    factionsToButtons.put(Faction.SERAPHIM, seraphimButton);

    setSearching(false);

    JavaFxUtil.addListener(gameService.searching1v1Property(), (observable, oldValue, newValue) -> setSearching(newValue));

    ObservableList<Faction> factions = preferencesService.getPreferences().getLadder1v1().getFactions();
    for (Faction faction : EnumSet.of(Faction.AEON, Faction.CYBRAN, Faction.UEF, Faction.SERAPHIM)) {
      factionsToButtons.get(faction).setSelected(factions.contains(faction));
    }
    playButton.setDisable(factionsToButtons.values().stream().noneMatch(ToggleButton::isSelected));

    preferenceUpdateListener = preferences -> {
      if (preferencesService.getPreferences().getForgedAlliance().getPath() == null) {
        onCancelButtonClicked();
      }
    };
    preferencesService.addUpdateListener(new WeakReference<>(preferenceUpdateListener));

    JavaFxUtil.addListener(playerService.currentPlayerProperty(), (observable, oldValue, newValue) -> Platform.runLater(() -> setCurrentPlayer(newValue)));
    playerService.getCurrentPlayer().ifPresent(this::setCurrentPlayer);
  }

  @VisibleForTesting
  void setSearching(boolean searching) {
    cancelButton.setVisible(searching);
    playButton.setVisible(!searching);
    searchProgressIndicator.setVisible(searching);
    searchingForOpponentLabel.setVisible(searching);
    setFactionButtonsDisabled(searching);
  }

  private void setFactionButtonsDisabled(boolean disabled) {
    factionsToButtons.values().forEach(button -> button.setDisable(disabled));
  }

  public void onCancelButtonClicked() {
    gameService.stopSearchLadder1v1();
    setSearching(false);
  }

  @Override
  public Node getRoot() {
    return ladder1v1Root;
  }

  public void onPlayButtonClicked() {
    if (preferencesService.getPreferences().getForgedAlliance().getPath() == null) {
      eventBus.post(new MissingGamePathEvent(true));
      return;
    }

    setFactionButtonsDisabled(true);

    ObservableList<Faction> factions = preferencesService.getPreferences().getLadder1v1().getFactions();

    Faction randomFaction = factions.get(random.nextInt(factions.size()));
    gameService.startSearchLadder1v1(randomFaction);
  }

  public void onFactionButtonClicked() {
    List<Faction> factions = factionsToButtons.entrySet().stream()
      .filter(entry -> entry.getValue().isSelected())
      .map(Map.Entry::getKey)
      .collect(Collectors.toList());

    preferencesService.getPreferences().getLadder1v1().getFactions().setAll(factions);
    preferencesService.storeInBackground();

    playButton.setDisable(factions.isEmpty());
  }

  @EventListener
  public void onPlayerUpdated(PlayerUpdatedEvent event) {
    update(event.getPlayer());
  }

  private void setCurrentPlayer(Player player) {
    update(player);
  }

  private void update(Player player) {
    updateRating(player);
    updateOtherValues(player);
  }

  private void updateRating(Player player) {
    int rank = player.getRating().getOrDefault(KnownFeaturedMod.LADDER_1V1.getTechnicalName(), 0);

    if (rank == 0) {
      // FIXME make this configurable, or better, read from server once available
      rankProgressIndicator.setProgress(player.getNumberOfGames() / 10f);
      rankProgressIndicator.setVisible(true);
      rankLabel.setVisible(false);
      rankHintLabel.setText(i18n.get("ranked1v1.ratingProgress.stillLearning"));
    } else {
      rankProgressIndicator.setVisible(false);
      rankLabel.setVisible(true);
      // TODO display skill class graphic instead
      rankLabel.setText(i18n.number(rank));
      rankHintLabel.setVisible(false);
      updateRankHint(rank);
    }

    leaderboardService.getLeaderboardStats(KnownFeaturedMod.LADDER_1V1.getTechnicalName())
      .thenAccept(ranked1v1Stats -> {
        ranked1v1Stats.sort(Comparator.comparingInt(RatingStat::getRating));
        int totalPlayers = 0;
        for (RatingStat entry : ranked1v1Stats) {
          totalPlayers += entry.getTotalCount();
        }
        plotRatingDistributions(ranked1v1Stats, player);
        String rankingOutOfText = i18n.get("ranked1v1.rankingOutOf", totalPlayers);
        Platform.runLater(() -> rankingOutOfLabel.setText(rankingOutOfText));
      })
      .exceptionally(throwable -> {
        logger.warn("Could not plot rating distribution", throwable);
        return null;
      });
  }

  private void updateOtherValues(Player currentPlayer) {
    leaderboardService.getEntryForPlayer(currentPlayer.getId(), KnownFeaturedMod.LADDER_1V1.getTechnicalName()).thenAccept(leaderboardEntryBean -> Platform.runLater(() -> {
      rankingLabel.setText(i18n.get("ranked1v1.rankingFormat", leaderboardEntryBean.getPosition()));
      gamesPlayedLabel.setText(String.format("%d", leaderboardEntryBean.getTotalGames()));
      winLossRationLabel.setText(i18n.get("percentage", leaderboardEntryBean.getWinLossRatio() * 100));
    })).exceptionally(throwable -> {
      // Debug instead of warn, since it's fairly common that players don't have a leaderboard entry which causes a 404
      logger.debug("Leaderboard entry could not be read for current player: " + currentPlayer.getDisplayName(), throwable);
      return null;
    });
  }

  private void updateRankHint(int rating) {
    // TODO remove/rethink rating hint
//    if (rating < environment.getProperty("rating.low", int.class)) {
//      ratingHintLabel.setText(i18n.valueOf("ranked1v1.ratingHint.low"));
//    } else if (rating < environment.getProperty("rating.moderate", int.class)) {
//      ratingHintLabel.setText(i18n.valueOf("ranked1v1.ratingHint.moderate"));
//    } else if (rating < environment.getProperty("rating.good", int.class)) {
//      ratingHintLabel.setText(i18n.valueOf("ranked1v1.ratingHint.good"));
//    } else if (rating < environment.getProperty("rating.high", int.class)) {
//      ratingHintLabel.setText(i18n.valueOf("ranked1v1.ratingHint.high"));
//    } else if (rating < environment.getProperty("rating.top", int.class)) {
//      ratingHintLabel.setText(i18n.valueOf("ranked1v1.ratingHint.top"));
//    }
  }

  private void plotRatingDistributions(List<RatingStat> ratingStats, Player player) {
    XYChart.Series<String, Integer> series = new XYChart.Series<>();
    series.setName(i18n.get("ranked1v1.players", LeaderboardService.MINIMUM_GAMES_PLAYED_TO_BE_SHOWN));
    int currentPlayerRank = player.getRating().get(KnownFeaturedMod.LADDER_1V1.getTechnicalName());

    series.getData().addAll(ratingStats.stream()
      .sorted(Comparator.comparingInt(RatingStat::getRating))
      .map(item -> {
        int rating = item.getRating();
        XYChart.Data<String, Integer> data = new XYChart.Data<>(i18n.number(rating), item.getCountWithEnoughGamesPlayed());
        if (rating == currentPlayerRank) {
          data.nodeProperty().addListener((observable, oldValue, newValue) -> {
            newValue.pseudoClassStateChanged(NOTIFICATION_HIGHLIGHTED_PSEUDO_CLASS, true);
            addNodeOnTopOfBar(data, youLabel);
          });
        }
        return data;
      })
      .collect(Collectors.toList()));

    Platform.runLater(() -> ratingDistributionChart.getData().setAll(series));
  }

  private void addNodeOnTopOfBar(XYChart.Data<String, Integer> data, Node nodeToAdd) {
    final Node node = data.getNode();
    node.parentProperty().addListener((ov, oldParent, parent) -> {
      if (parent == null) {
        return;
      }
      Group parentGroup = (Group) parent;
      ObservableList<Node> children = parentGroup.getChildren();
      if (!children.contains(nodeToAdd)) {
        children.add(nodeToAdd);
      }
    });

    JavaFxUtil.addListener(node.boundsInParentProperty(), (ov, oldBounds, bounds) -> {
      nodeToAdd.setLayoutX(Math.round(bounds.getMinX() + bounds.getWidth() / 2 - nodeToAdd.prefWidth(-1) / 2));
      nodeToAdd.setLayoutY(Math.round(bounds.getMinY() - nodeToAdd.prefHeight(-1) * 0.5));
    });
  }

  public void showLadderMaps(ActionEvent actionEvent) {
    eventBus.post(new ShowLadderMapsEvent());
  }
}
