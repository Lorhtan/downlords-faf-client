package com.faforever.client.preferences;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.preferences.jackson.BooleanPropertyAdapter;
import com.faforever.client.preferences.jackson.DoublePropertyAdapter;
import com.faforever.client.preferences.jackson.FloatPropertyAdapter;
import com.faforever.client.preferences.jackson.IntegerPropertyAdapter;
import com.faforever.client.preferences.jackson.ListPropertyAdapter;
import com.faforever.client.preferences.jackson.LongPropertyAdapter;
import com.faforever.client.preferences.jackson.MapPropertyAdapter;
import com.faforever.client.preferences.jackson.ObservableListAdapter;
import com.faforever.client.preferences.jackson.ObservableMapAdapter;
import com.faforever.client.preferences.jackson.ObservableSetAdapter;
import com.faforever.client.preferences.jackson.SetPropertyAdapter;
import com.faforever.client.preferences.jackson.StringPropertyAdapter;
import com.faforever.client.update.ClientConfiguration;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.annotations.VisibleForTesting;
import com.sun.jna.platform.win32.Shell32Util;
import com.sun.jna.platform.win32.ShlObj;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.scene.paint.Color;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

@Lazy
@Service
@Slf4j
public class PreferencesService implements InitializingBean {

  public static final String SUPREME_COMMANDER_EXE = "SupremeCommander.exe";
  public static final String FORGED_ALLIANCE_EXE = "ForgedAlliance.exe";

  /**
   * Points to the FAF data directory where log files, config files and others are held. The returned value varies
   * depending on the operating system.
   */
  private static final Path FAF_DATA_DIRECTORY;
  private static final long STORE_DELAY = 1000;
  private static final Charset CHARSET = StandardCharsets.UTF_8;
  private static final String PREFS_FILE_NAME = "client.prefs";
  private static final String APP_DATA_SUB_FOLDER = "Forged Alliance Forever";
  private static final String USER_HOME_SUB_FOLDER = ".faforever";
  private static final String REPLAYS_SUB_FOLDER = "replays";
  private static final String CORRUPTED_REPLAYS_SUB_FOLDER = "corrupt";
  private static final String CACHE_SUB_FOLDER = "cache";
  private static final String CACHE_STYLESHEETS_SUB_FOLDER = Paths.get(CACHE_SUB_FOLDER, "stylesheets").toString();
  private static final Path CACHE_DIRECTORY;

  static {
    if (org.bridj.Platform.isWindows()) {
      FAF_DATA_DIRECTORY = Paths.get(Shell32Util.getFolderPath(ShlObj.CSIDL_COMMON_APPDATA), "FAForever");
    } else {
      FAF_DATA_DIRECTORY = Paths.get(System.getProperty("user.home")).resolve(USER_HOME_SUB_FOLDER);
    }
    CACHE_DIRECTORY = FAF_DATA_DIRECTORY.resolve(CACHE_SUB_FOLDER);

    System.setProperty("logging.file", PreferencesService.FAF_DATA_DIRECTORY
      .resolve("logs")
      .resolve("downlords-faf-client.log")
      .toString());

    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();

    log.debug("Logger initialized");
  }

  private final Path preferencesFilePath;
  /**
   * @see #storeInBackground()
   */
  private final Timer timer;
  private final Collection<WeakReference<PreferenceUpdateListener>> updateListeners;
  private final ClientProperties clientProperties;
  private final ObjectMapper objectMapper;

  private Preferences preferences;
  private TimerTask storeInBackgroundTask;

  @VisibleForTesting
  PreferencesService(ClientProperties clientProperties, Path preferencesFilePath) {
    this.clientProperties = clientProperties;
    this.preferencesFilePath = preferencesFilePath;

    updateListeners = new ArrayList<>();
    timer = new Timer("PrefTimer", true);
    objectMapper = new ObjectMapper();
    objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    objectMapper.registerModule(jacksonModule());
  }

  @Autowired
  public PreferencesService(ClientProperties clientProperties) {
    this(clientProperties, getPreferencesDirectory().resolve(PREFS_FILE_NAME));
  }

  private Module jacksonModule() {
    SimpleModule module = new SimpleModule();
    module.addSerializer(StringProperty.class, StringPropertyAdapter.SERIALIZER);
    module.addSerializer(IntegerProperty.class, IntegerPropertyAdapter.SERIALIZER);
    module.addSerializer(DoubleProperty.class, DoublePropertyAdapter.SERIALIZER);
    module.addSerializer(LongProperty.class, LongPropertyAdapter.SERIALIZER);
    module.addSerializer(FloatProperty.class, FloatPropertyAdapter.SERIALIZER);
    module.addSerializer(BooleanProperty.class, BooleanPropertyAdapter.SERIALIZER);
    module.addSerializer(MapProperty.class, MapPropertyAdapter.SERIALIZER);
    module.addSerializer(SetProperty.class, SetPropertyAdapter.SERIALIZER);
    module.addSerializer(ListProperty.class, ListPropertyAdapter.SERIALIZER);

    module.addDeserializer(StringProperty.class, StringPropertyAdapter.DESERIALIZER);
    module.addDeserializer(IntegerProperty.class, IntegerPropertyAdapter.DESERIALIZER);
    module.addDeserializer(DoubleProperty.class, DoublePropertyAdapter.DESERIALIZER);
    module.addDeserializer(LongProperty.class, LongPropertyAdapter.DESERIALIZER);
    module.addDeserializer(FloatProperty.class, FloatPropertyAdapter.DESERIALIZER);
    module.addDeserializer(BooleanProperty.class, BooleanPropertyAdapter.DESERIALIZER);
    module.addDeserializer(MapProperty.class, MapPropertyAdapter.DESERIALIZER);
    module.addDeserializer(ObservableMap.class, ObservableMapAdapter.DESERIALIZER);
    module.addDeserializer(SetProperty.class, SetPropertyAdapter.DESERIALIZER);
    module.addDeserializer(ObservableSet.class, ObservableSetAdapter.DESERIALIZER);
    module.addDeserializer(ListProperty.class, ListPropertyAdapter.DESERIALIZER);
    module.addDeserializer(ObservableList.class, ObservableListAdapter.DESERIALIZER);

    module.addSerializer(Color.class, ColorTypeAdapter.SERIALIZER);
    module.addDeserializer(Color.class, ColorTypeAdapter.DESERIALIZER);
    return module;
  }

  @VisibleForTesting
  static Path getPreferencesDirectory() {
    if (org.bridj.Platform.isWindows()) {
      return Paths.get(System.getenv("APPDATA")).resolve(APP_DATA_SUB_FOLDER);
    }
    return Paths.get(System.getProperty("user.home")).resolve(USER_HOME_SUB_FOLDER);
  }

  public void afterPropertiesSet() throws IOException {
    if (Files.exists(preferencesFilePath)) {
      deleteFileIfEmpty(preferencesFilePath);
      if (Files.exists(preferencesFilePath)) {
        try {
          readExistingFile(preferencesFilePath);
        } catch (Exception e) {
          Files.delete(preferencesFilePath);
        }
      }
    }

    if (preferences == null) {
      preferences = new Preferences();
    }

    Path gamePrefs = preferences.getForgedAlliance().getPreferencesFile();
    if (Files.notExists(gamePrefs)) {
      log.info("Initializing game preferences file: {}", gamePrefs);
      Files.createDirectories(gamePrefs.getParent());
      Files.copy(getClass().getResourceAsStream("/fa/game.prefs"), gamePrefs);
    }
  }

  /**
   * Sometimes, old preferences values are renamed or moved. The purpose of this method is to temporarily perform such
   * migrations.
   */
  private void migratePreferences(Preferences preferences) {
    // Nothing to migrate at this time
//    storeInBackground();
  }

  public static void configureLogging() {
    // Calling this method causes the class to be initialized (static initializers) which in turn causes the logger to initialize.
  }


  /**
   * It may happen that the file is empty when the process is forcibly killed, so remove the file if that happened.
   */
  private void deleteFileIfEmpty(Path file) throws IOException {
    if (Files.size(file) == 0) {
      Files.delete(file);
    }
  }

  public Path getFafBinDirectory() {
    return getFafDataDirectory().resolve("bin");
  }

  public Path getFafDataDirectory() {
    return FAF_DATA_DIRECTORY;
  }

  public Path getPatchReposDirectory() {
    return getFafDataDirectory().resolve("repos");
  }

  private void readExistingFile(Path path) {
    if (preferences != null) {
      throw new IllegalStateException("Preferences have already been initialized");
    }

    try (Reader reader = Files.newBufferedReader(path, CHARSET)) {
      log.debug("Reading preferences file {}", preferencesFilePath.toAbsolutePath());
      preferences = objectMapper.readValue(reader, Preferences.class);
    } catch (IOException e) {
      log.warn("Preferences file {} could not be read", path.toAbsolutePath(), e);
    }

    migratePreferences(preferences);
  }

  public Preferences getPreferences() {
    return preferences;
  }

  public void store() {
    Path parent = preferencesFilePath.getParent();
    try {
      if (!Files.exists(parent)) {
        Files.createDirectories(parent);
      }
    } catch (IOException e) {
      log.warn("Could not create directory {}", parent.toAbsolutePath(), e);
      return;
    }

    try (Writer writer = Files.newBufferedWriter(preferencesFilePath, CHARSET)) {
      log.debug("Writing preferences file {}", preferencesFilePath.toAbsolutePath());
      objectMapper.writeValue(writer, preferences);
    } catch (IOException e) {
      log.warn("Preferences file {} could not be written", preferencesFilePath.toAbsolutePath(), e);
    }
  }

  /**
   * Stores the preferences in background, with a delay of {@link #STORE_DELAY}. Each subsequent call to this method
   * during that delay causes the delay to be reset. This ensures that the prefs file is written only once if multiple
   * calls occur within a short time.
   */
  public void storeInBackground() {
    if (storeInBackgroundTask != null) {
      storeInBackgroundTask.cancel();
    }

    storeInBackgroundTask = new TimerTask() {
      @Override
      public void run() {
        store();
        ArrayList<WeakReference<PreferenceUpdateListener>> toBeRemoved = new ArrayList<>();
        for (WeakReference<PreferenceUpdateListener> updateListener : updateListeners) {
          PreferenceUpdateListener preferenceUpdateListener = updateListener.get();
          if (preferenceUpdateListener == null) {
            toBeRemoved.add(updateListener);
            continue;
          }
          preferenceUpdateListener.onPreferencesUpdated(preferences);
        }

        for (WeakReference<PreferenceUpdateListener> preferenceUpdateListenerWeakReference : toBeRemoved) {
          updateListeners.remove(preferenceUpdateListenerWeakReference);
        }
      }
    };

    timer.schedule(storeInBackgroundTask, STORE_DELAY);
  }

  /**
   * Adds a listener to be notified whenever the preferences have been updated (that is, stored to file).
   */
  public void addUpdateListener(WeakReference<PreferenceUpdateListener> listener) {
    updateListeners.add(listener);
  }

  public Path getCorruptedReplaysDirectory() {
    return getReplaysDirectory().resolve(CORRUPTED_REPLAYS_SUB_FOLDER);
  }

  public Path getReplaysDirectory() {
    return getFafDataDirectory().resolve(REPLAYS_SUB_FOLDER);
  }

  public Path getCacheDirectory() {
    return CACHE_DIRECTORY;
  }

  public Path getFafLogDirectory() {
    return getFafDataDirectory().resolve("logs");
  }

  public Path getThemesDirectory() {
    return getFafDataDirectory().resolve("themes");
  }

  public boolean isGamePathValid() {
    return isGamePathValid(preferences.getForgedAlliance().getPath().resolve("bin"));
  }

  public boolean isGamePathValid(Path binPath) {
    return binPath != null
      && (Files.isRegularFile(binPath.resolve(FORGED_ALLIANCE_EXE))
      || Files.isRegularFile(binPath.resolve(SUPREME_COMMANDER_EXE))
    );
  }

  public Path getCacheStylesheetsDirectory() {
    return getFafDataDirectory().resolve(CACHE_STYLESHEETS_SUB_FOLDER);
  }

  public Path getLanguagesDirectory() {
    return getFafDataDirectory().resolve("languages");
  }

  public CompletableFuture<ClientConfiguration> getRemotePreferences() {
    CompletableFuture<ClientConfiguration> future = new CompletableFuture<>();

    try {
      URL url = new URL(clientProperties.getClientConfigUrl());
      HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
      urlConnection.setConnectTimeout((int) clientProperties.getClientConfigConnectTimeout().toMillis());

      ClientConfiguration clientConfiguration;
      try (Reader reader = new InputStreamReader(urlConnection.getInputStream(), StandardCharsets.UTF_8)) {
        clientConfiguration = objectMapper.readValue(reader, ClientConfiguration.class);
      }
      future.complete(clientConfiguration);
    } catch (IOException e) {
      future.completeExceptionally(e);
    }

    return future;
  }
}
