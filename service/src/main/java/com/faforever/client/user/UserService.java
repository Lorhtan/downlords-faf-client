package com.faforever.client.user;

import com.faforever.client.login.LoginFailedException;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.ErrorServerMessage;
import com.faforever.client.remote.FafService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.TaskService;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;

@Lazy
@Service
public class UserService implements InitializingBean {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final StringProperty displayName;

  private final FafService fafService;
  private final PreferencesService preferencesService;
  private final EventBus eventBus;
  private final ApplicationContext applicationContext;
  private final TaskService taskService;

  private String password;
  private Integer userId;
  private CompletableFuture<Void> loginFuture;


  public UserService(FafService fafService, PreferencesService preferencesService, EventBus eventBus, ApplicationContext applicationContext, TaskService taskService) {
    displayName = new SimpleStringProperty();
    this.fafService = fafService;
    this.preferencesService = preferencesService;
    this.eventBus = eventBus;
    this.applicationContext = applicationContext;
    this.taskService = taskService;
  }


  public CompletableFuture<Void> login(String username, String password, boolean autoLogin) {
    this.password = password;

    preferencesService.getPreferences().getLogin()
        .setUsername(username)
        .setPassword(password)
        .setAutoLogin(autoLogin);
    preferencesService.storeInBackground();

    loginFuture = fafService.connectAndLogIn(username, password)
        .thenAccept(accountDetails -> {
          userId = accountDetails.getId();

          // Because of different case (upper/lower)
          String displayName = accountDetails.getDisplayName();
          UserService.this.displayName.set(displayName);

          preferencesService.getPreferences().getLogin().setUsername(displayName);
          preferencesService.storeInBackground();

          eventBus.post(new LoginSuccessEvent(username, displayName, password, userId));
        })
        .whenComplete((aVoid, throwable) -> {
          if (throwable != null) {
            logger.warn("Error during login", throwable);
            fafService.disconnect();
          }
          loginFuture = null;
        });
    return loginFuture;
  }


  public String getDisplayName() {
    return displayName.get();
  }


  public String getPassword() {
    return password;
  }

  public Integer getUserId() {
    return userId;
  }


  public void cancelLogin() {
    if (loginFuture != null) {
      loginFuture.toCompletableFuture().cancel(true);
      loginFuture = null;
      fafService.disconnect();
    }
  }

  private void onLoginError(ErrorServerMessage noticeMessage) {
    if (loginFuture != null) {
      loginFuture.toCompletableFuture().completeExceptionally(new LoginFailedException(noticeMessage.getText()));
      loginFuture = null;
      fafService.disconnect();
    }
  }

  public void logOut() {
    logger.info("Logging out");
    fafService.disconnect();
    eventBus.post(new LoggedOutEvent());
    preferencesService.getPreferences().getLogin().setAutoLogin(false);
  }


  public CompletableTask<Void> changePassword(String currentPassword, String newPassword) {
    ChangePasswordTask changePasswordTask = applicationContext.getBean(ChangePasswordTask.class);
    changePasswordTask.setUsername(displayName.get());
    changePasswordTask.setCurrentPassword(currentPassword);
    changePasswordTask.setNewPassword(newPassword);

    return taskService.submitTask(changePasswordTask);
  }

  @Override
  public void afterPropertiesSet() {
    fafService.addOnMessageListener(AccountDetailsServerMessage.class, loginInfo -> userId = loginInfo.getId());
    fafService.addOnMessageListener(ErrorServerMessage.class, this::onLoginError);
    eventBus.register(this);
  }

  @Subscribe
  public void onLogoutRequestEvent(LogOutRequestEvent event) {
    logOut();
  }
}
