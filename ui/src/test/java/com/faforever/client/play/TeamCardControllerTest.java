package com.faforever.client.play;

import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.i18n.I18n;
import com.faforever.client.play.PlayerCardTooltipController;
import com.faforever.client.play.TeamCardController;
import com.faforever.client.player.Player;
import com.faforever.client.replay.Replay.PlayerStats;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.control.Label;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TeamCardControllerTest extends AbstractPlainJavaFxTest {
  private static final String LEADERBOARD_NAME = KnownFeaturedMod.LADDER_1V1.getTechnicalName();
  private TeamCardController instance;

  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;
  @Mock
  private PlayerCardTooltipController playerCardTooltipController;

  private ArrayList<Player> playerList;
  private ObservableMap<String, List<PlayerStats>> teams;
  private PlayerStats playerStats;

  @Before
  public void setUp() throws IOException {
    instance = new TeamCardController(uiService, i18n);
    playerList = new ArrayList<>();
    Player player = new Player();
    player.setId(1);
    player.getRating().put(LEADERBOARD_NAME, 1);

    playerList.add(player);
    teams = FXCollections.observableHashMap();

    when(uiService.loadFxml("theme/player_card_tooltip.fxml")).thenReturn(playerCardTooltipController);
    when(playerCardTooltipController.getRoot()).thenReturn(new Label());

    playerStats = new PlayerStats(1, 5, 5);
    teams.put("2", Collections.singletonList(playerStats));

    loadFxml("theme/team_card.fxml", param -> instance);
  }

  @Test
  public void setPlayersInTeam() throws Exception {
    instance.setPlayersInTeam(2, playerList, LEADERBOARD_NAME);
    verify(i18n).get("game.tooltip.teamTitle", 1, 1);
  }

}
