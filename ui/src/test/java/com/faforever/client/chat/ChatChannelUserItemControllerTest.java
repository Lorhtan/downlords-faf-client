package com.faforever.client.chat;

import com.faforever.client.avatar.AvatarImageService;
import com.faforever.client.clan.Clan;
import com.faforever.client.clan.ClanService;
import com.faforever.client.clan.ClanTooltipController;
import com.faforever.client.fx.MouseEvents;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.Game;
import com.faforever.client.game.GameState;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.google.common.eventbus.EventBus;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.net.URL;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChatChannelUserItemControllerTest extends AbstractPlainJavaFxTest {

  private ChatUserItemController instance;
  @Mock
  private AvatarImageService avatarImageService;
  @Mock
  private CountryFlagService countryFlagService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;
  @Mock
  private EventBus eventBus;
  @Mock
  private ClanService clanService;
  @Mock
  private PlayerService playerService;
  @Mock
  private PlatformService platformService;
  @Mock
  private TimeService timeService;
  private ClanTooltipController clanTooltipControllerMock;
  private Clan testClan;
  private Player clanLeader;

  @Before
  public void setUp() throws Exception {
    Preferences preferences = new Preferences();
    when(preferencesService.getPreferences()).thenReturn(preferences);

    when(i18n.get(eq("user.status.hosting"), any())).thenReturn("Hosting");
    when(i18n.get(eq("user.status.waiting"), any())).thenReturn("Waiting");
    when(i18n.get(eq("user.status.playing"), any())).thenReturn("Playing");
    when(i18n.get(eq("clan.messageLeader"))).thenReturn("Message clan leader");
    when(i18n.get(eq("clan.visitPage"))).thenReturn("Visit clan website");

    testClan = new Clan();
    testClan.setTag("e");

    clanLeader = PlayerBuilder.create("ClanLeader")
      .defaultValues()
      .id(222)
      .clan(testClan.getTag())
      .get();

    testClan.setLeader(clanLeader);

    when(clanService.getClanByTag(anyString())).thenReturn(CompletableFuture.completedFuture(Optional.of(testClan)));
    when(countryFlagService.loadCountryFlag("US")).thenReturn(Optional.of(mock(Image.class)));
    when(playerService.isOnline(anyInt())).thenReturn(false);
    clanTooltipControllerMock = mock(ClanTooltipController.class);
    when(uiService.loadFxml("theme/chat/clan_tooltip.fxml")).thenReturn(clanTooltipControllerMock);
    when(clanTooltipControllerMock.getRoot()).thenReturn(new Pane());

    instance = new ChatUserItemController(
      preferencesService,
      avatarImageService,
      countryFlagService,
      i18n,
      uiService,
      eventBus,
      clanService,
      playerService,
      platformService,
      timeService
    );
    loadFxml("theme/chat/chat_user_item.fxml", param -> instance);
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.chatUserItemRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testSetChatUser() throws Exception {
    Player player = new Player("junit");
    player.setCountry("US");
    player.setClanTag("e");
    player.setAvatarUrl(new URL("http://example.com/avatar.png"));
    player.setAvatarTooltip("dog");

    when(playerService.getCurrentPlayer()).thenReturn(Optional.of(player));

    ChatChannelUser chatChannelUser = new ChatChannelUser("junit", null, false);
    chatChannelUser.setPlayer(player);
    instance.setChatUser(chatChannelUser);
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.clanMenu.getText(), is("[e]"));
    assertThat(instance.clanMenu.isVisible(), is(true));
    verify(countryFlagService).loadCountryFlag("US");
    assertThat(instance.countryTooltip, CoreMatchers.notNullValue());
    assertThat(instance.avatarTooltip, CoreMatchers.notNullValue());
    assertThat(instance.userTooltip, CoreMatchers.notNullValue());
    verify(clanTooltipControllerMock).setClan(testClan);
  }

  @Test
  public void testGetPlayer() {
    ChatChannelUser chatUser = new ChatChannelUser("junit", null, false);
    instance.setChatUser(chatUser);

    assertThat(instance.getChatUser(), is(chatUser));
  }

  @Test
  public void testOpenGameSetsStatusToWaiting() {
    Player player = new Player("junit");
    ChatChannelUser chatUser = new ChatChannelUser("junit", null, false);
    chatUser.setPlayer(player);

    instance.setChatUser(chatUser);
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.statusLabel.getText(), is(""));

    Game game = new Game();
    game.setState(GameState.OPEN);
    player.setGame(game);
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.statusLabel.getText(), is("Waiting"));
  }

  @Test
  public void testHostedGameSetsStatusToHosting() {
    Player player = new Player("junit");
    ChatChannelUser chatUser = new ChatChannelUser("junit", null, false);
    chatUser.setPlayer(player);

    instance.setChatUser(chatUser);
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.statusLabel.getText(), is(""));

    Game game = new Game();
    game.setHostName("junit");
    game.setState(GameState.OPEN);

    player.setGame(game);
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.statusLabel.getText(), is("Hosting"));
  }

  @Test
  public void testActiveGameSetsStatusToPlaying() {
    Player player = new Player("junit");

    ChatChannelUser chatUser = new ChatChannelUser("junit", null, false);
    chatUser.setPlayer(player);

    instance.setChatUser(chatUser);
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.statusLabel.getText(), is(""));

    Game game = new Game();
    game.setHostName("junit");
    game.setState(GameState.PLAYING);

    player.setGame(game);
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.statusLabel.getText(), is("Playing"));
  }

  @Test
  public void testNullGameSetsStatusToNothing() {
    Player player = new Player("junit");

    ChatChannelUser chatUser = new ChatChannelUser("junit", null, false);
    chatUser.setPlayer(player);

    instance.setChatUser(chatUser);

    Game game = new Game();
    game.setHostName("junit");
    game.setState(GameState.PLAYING);

    player.setGame(game);
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.statusLabel.getText(), is("Playing"));
    player.setGame(null);
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.statusLabel.getText(), is(""));
  }

  @Test
  public void testSingleClickDoesNotInitiatePrivateChat() {
    instance.onItemClicked(MouseEvents.generateClick(MouseButton.PRIMARY, 1));

    verify(eventBus, never()).post(CoreMatchers.any(InitiatePrivateChatEvent.class));
  }

  @Test
  public void testDoubleClickInitiatesPrivateChat() {
    ChatChannelUser channelUser = new ChatChannelUser("junit", null, false);
    instance.setChatUser(channelUser);

    WaitForAsyncUtils.waitForFxEvents();

    instance.onItemClicked(MouseEvents.generateClick(MouseButton.PRIMARY, 2));

    ArgumentCaptor<InitiatePrivateChatEvent> captor = ArgumentCaptor.forClass(InitiatePrivateChatEvent.class);
    verify(eventBus).post(captor.capture());

    assertThat(captor.getValue().getUsername(), is("junit"));
  }

  @Test
  public void testOnContextMenuRequested() {
    WaitForAsyncUtils.asyncFx(() -> getRoot().getChildren().setAll(instance.chatUserItemRoot));

    ChatChannelUser chatUser = new ChatChannelUser("junit", null, false);
    instance.setChatUser(chatUser);
    WaitForAsyncUtils.waitForFxEvents();

    ChatUserContextMenuController contextMenuController = mock(ChatUserContextMenuController.class);
    ContextMenu contextMenu = mock(ContextMenu.class);
    when(contextMenuController.getContextMenu()).thenReturn(contextMenu);
    when(uiService.loadFxml("theme/chat/chat_user_context_menu.fxml")).thenReturn(contextMenuController);

    ContextMenuEvent event = mock(ContextMenuEvent.class);
    instance.onContextMenuRequested(event);

    verify(uiService).loadFxml("theme/chat/chat_user_context_menu.fxml");
    verify(contextMenuController).setChatUser(chatUser);
    verify(contextMenu).show(any(Pane.class), anyDouble(), anyDouble());
  }

  @Test
  public void testContactClanLeaderNotShowing() {
    Player player = PlayerBuilder.create("junit")
      .defaultValues()
      .id(2)
      .clan("e")
      .get();
    instance.setChatUser(ChatChannelUserBuilder.create("junit").defaultValues().setPlayer(player).get());
    when(playerService.getCurrentPlayer()).thenReturn(Optional.of(player));
    WaitForAsyncUtils.waitForFxEvents();

    ObservableList<MenuItem> items = instance.clanMenu.getItems();
    assertThat(items.size(), is(1));
    boolean containsMessageItem = items.stream().anyMatch((item) -> "Message clan leader".equals(item.getText()));
    assertThat(containsMessageItem, is(false));
  }

  @Test
  public void testContactClanLeaderShowingForSameClan() {
    Player player = PlayerBuilder.create("junit")
      .id(1431)
      .defaultValues()
      .clan(testClan.getTag())
      .get();

    instance.setChatUser(
      ChatChannelUserBuilder.create(clanLeader.getDisplayName())
        .defaultValues()
        .setPlayer(clanLeader).get()
    );
    when(playerService.isOnline(testClan.getLeader().getId())).thenReturn(true);
    when(playerService.getCurrentPlayer()).thenReturn(Optional.of(player));
    WaitForAsyncUtils.waitForFxEvents();

    ObservableList<MenuItem> items = instance.clanMenu.getItems();
    assertThat(items.size(), is(2));
    boolean containsMessageItem = items.stream().anyMatch((item) -> "Message clan leader".equals(item.getText()));
    assertThat(containsMessageItem, is(true));
  }

  @Test
  public void testContactClanLeaderShowingForOtherClan() {
    Player player = PlayerBuilder.create("junit")
      .id(1431)
      .defaultValues()
      .clan("xyz")
      .get();

    instance.setChatUser(
      ChatChannelUserBuilder.create(clanLeader.getDisplayName())
        .defaultValues()
        .setPlayer(clanLeader).get()
    );
    when(playerService.isOnline(clanLeader.getId())).thenReturn(true);
    when(playerService.getCurrentPlayer()).thenReturn(Optional.of(player));
    WaitForAsyncUtils.waitForFxEvents();

    ObservableList<MenuItem> items = instance.clanMenu.getItems();
    assertThat(items.size(), is(2));
    boolean containsMessageItem = items.stream().anyMatch((item) -> "Message clan leader".equals(item.getText()));
    assertThat(containsMessageItem, is(true));
  }


  @Test
  public void testSetVisible() {
    instance.setVisible(true);
    assertThat(instance.chatUserItemRoot.isVisible(), is(true));
    assertThat(instance.chatUserItemRoot.isManaged(), is(true));

    instance.setVisible(false);
    assertThat(instance.chatUserItemRoot.isVisible(), is(false));
    assertThat(instance.chatUserItemRoot.isManaged(), is(false));

    instance.setVisible(true);
    assertThat(instance.chatUserItemRoot.isVisible(), is(true));
    assertThat(instance.chatUserItemRoot.isManaged(), is(true));
  }
}
