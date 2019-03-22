package com.faforever.client.chat;

import com.faforever.client.achievements.Achievement;
import com.faforever.client.achievements.AchievementImageService;
import com.faforever.client.achievements.AchievementItemController;
import com.faforever.client.achievements.AchievementService;
import com.faforever.client.achievements.AchievementState;
import com.faforever.client.achievements.PlayerAchievement;
import com.faforever.client.event.EventService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.rating.RatingHistoryDataPoint;
import com.faforever.client.stats.StatisticsService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import javafx.scene.layout.HBox;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserInfoWindowControllerTest extends AbstractPlainJavaFxTest {

  private static final String PLAYER_NAME = "junit";
  private static final int PLAYER_ID = 123;

  private UserInfoWindowController instance;

  @Mock
  private CountryFlagService countryFlagService;
  @Mock
  private I18n i18n;
  @Mock
  private StatisticsService statisticsService;
  @Mock
  private AchievementService achievementService;
  @Mock
  private AchievementImageService achievementImageService;
  @Mock
  private EventService eventService;
  @Mock
  private UiService uiService;
  @Mock
  private AchievementItemController achievementItemController;
  @Mock
  private TimeService timeService;
  @Mock
  private PlayerService playerService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private LeaderboardService leaderboardService;

  @Before
  public void setUp() throws Exception {
    instance = new UserInfoWindowController(statisticsService, countryFlagService, achievementService, achievementImageService, eventService,
      i18n, uiService, timeService,
      notificationService, playerService, leaderboardService);

    when(uiService.loadFxml("theme/achievement_item.fxml")).thenReturn(achievementItemController);
    when(achievementItemController.getRoot()).thenReturn(new HBox());
    when(playerService.getPlayersByIds(any())).thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));

    when(statisticsService.getRatingHistory(any(), eq(PLAYER_ID))).thenReturn(CompletableFuture.completedFuture(Arrays.asList(
      new RatingHistoryDataPoint(OffsetDateTime.now(), 5),
      new RatingHistoryDataPoint(OffsetDateTime.now().plus(1, ChronoUnit.DAYS), 5)
    )));

    loadFxml("theme/user_info_window.fxml", clazz -> instance);
  }

  @Test
  public void testSetPlayerInfoBeanNoAchievementUnlocked() throws Exception {
    Achievement achievement = new Achievement();
    achievement.setId("123");
    when(achievementService.getAchievements()).thenReturn(CompletableFuture.completedFuture(singletonList(
      achievement
    )));
    when(uiService.loadFxml("theme/achievement_item.fxml")).thenReturn(achievementItemController);

    PlayerAchievement playerAchievement = new PlayerAchievement();
    playerAchievement.setAchievement(achievement);
    playerAchievement.setState(AchievementState.REVEALED);
    when(achievementService.getPlayerAchievements(PLAYER_ID)).thenReturn(CompletableFuture.completedFuture(
      singletonList(playerAchievement)
    ));

    when(eventService.getPlayerEvents(PLAYER_ID)).thenReturn(CompletableFuture.completedFuture(new HashMap<>()));

    Player player = new Player(PLAYER_NAME);
    player.setId(PLAYER_ID);
    player.getRating().put("global", 5);
    player.getRating().put("ladder1v1", 4);
    instance.setPlayer(player);

    verify(achievementService).getAchievements();
    verify(achievementService).getPlayerAchievements(PLAYER_ID);
    verify(eventService).getPlayerEvents(PLAYER_ID);

    assertThat(instance.mostRecentAchievementPane.isVisible(), is(false));
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.userInfoRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testSetPlayerInfoBean() throws Exception {
    Achievement achievement1 = new Achievement();
    achievement1.setId("foo-bar");

    Achievement achievement2 = new Achievement();

    when(achievementService.getAchievements()).thenReturn(CompletableFuture.completedFuture(asList(
      achievement1,
      achievement2
    )));
    when(uiService.loadFxml("theme/achievement_item.fxml")).thenReturn(achievementItemController);

    PlayerAchievement playerAchievement1 = new PlayerAchievement();
    playerAchievement1.setAchievement(achievement1);
    playerAchievement1.setState(AchievementState.UNLOCKED);

    PlayerAchievement playerAchievement2 = new PlayerAchievement();

    when(achievementService.getPlayerAchievements(PLAYER_ID)).thenReturn(CompletableFuture.completedFuture(asList(
      playerAchievement1,
      playerAchievement2
    )));
    when(eventService.getPlayerEvents(PLAYER_ID)).thenReturn(CompletableFuture.completedFuture(new HashMap<>()));

    Player player = new Player(PLAYER_NAME);
    player.setId(PLAYER_ID);
    player.getRating().put("global", 5);
    player.getRating().put("ladder1v1", 4);

    instance.setPlayer(player);

    verify(achievementService).getAchievements();
    verify(achievementService).getPlayerAchievements(PLAYER_ID);
    verify(eventService).getPlayerEvents(PLAYER_ID);

    assertThat(instance.mostRecentAchievementPane.isVisible(), is(true));
  }
}
