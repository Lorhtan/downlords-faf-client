package com.faforever.client.mod;

import com.faforever.client.config.CacheNames;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.mod.ModVersion.ModType;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.query.SearchableProperties;
import com.faforever.client.remote.FafService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.vault.SearchConfig;
import com.faforever.client.vault.SortConfig;
import com.faforever.client.vault.SortOrder;
import com.faforever.commons.mod.ModLoadException;
import com.faforever.commons.mod.ModReader;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.SneakyThrows;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.github.nocatch.NoCatch.noCatch;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;

@Lazy
@Service
public class ModService implements InitializingBean, DisposableBean {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final Pattern ACTIVE_MODS_PATTERN = Pattern.compile("active_mods\\s*=\\s*\\{.*?}", Pattern.DOTALL);
  private static final Pattern ACTIVE_MOD_PATTERN = Pattern.compile("\\['(.*?)']\\s*=\\s*(true|false)", Pattern.DOTALL);

  private final FafService fafService;
  private final PreferencesService preferencesService;
  private final TaskService taskService;
  private final ApplicationContext applicationContext;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final PlatformService platformService;
  private final ModReader modReader;
  private final ModInfoMapper modInfoMapper;

  private Path modsDirectory;
  private Map<Path, ModVersion> pathToMod;
  private ObservableList<ModVersion> installedModVersions;
  private ObservableList<ModVersion> readOnlyInstalledModVersions;
  private Thread directoryWatcherThread;


  // TODO divide and conquer
  public ModService(TaskService taskService, FafService fafService, PreferencesService preferencesService,
                    ApplicationContext applicationContext,
                    NotificationService notificationService, I18n i18n,
                    PlatformService platformService,
                    ModInfoMapper modInfoMapper
  ) {
    this.modInfoMapper = modInfoMapper;
    pathToMod = new HashMap<>();
    modReader = new ModReader();
    installedModVersions = FXCollections.observableArrayList();
    readOnlyInstalledModVersions = FXCollections.unmodifiableObservableList(installedModVersions);
    this.taskService = taskService;
    this.fafService = fafService;
    this.preferencesService = preferencesService;
    this.applicationContext = applicationContext;
    this.notificationService = notificationService;
    this.i18n = i18n;
    this.platformService = platformService;
  }

  @Override
  public void afterPropertiesSet() {
    modsDirectory = preferencesService.getPreferences().getForgedAlliance().getModsDirectory();
    JavaFxUtil.addListener(preferencesService.getPreferences().getForgedAlliance().modsDirectoryProperty(), (observable, oldValue, newValue) -> {
      if (newValue != null) {
        modsDirectory = newValue;
        onModDirectoryReady();
      }
    });

    if (modsDirectory != null) {
      onModDirectoryReady();
    }
  }

  private void onModDirectoryReady() {
    try {
      createDirectories(modsDirectory);
      directoryWatcherThread = startDirectoryWatcher(modsDirectory);
    } catch (IOException e) {
      logger.warn("Could not start mod directory watcher", e);
      // TODO notify user
    }
    loadInstalledMods();
  }

  private Thread startDirectoryWatcher(Path modsDirectory) {
    Thread thread = new Thread(() -> noCatch(() -> {
      WatchService watcher = modsDirectory.getFileSystem().newWatchService();
      modsDirectory.register(watcher, ENTRY_DELETE);

      try {
        while (!Thread.interrupted()) {
          WatchKey key = watcher.take();
          key.pollEvents().stream()
            .filter(event -> event.kind() == ENTRY_DELETE)
            .forEach(event -> removeMod(modsDirectory.resolve((Path) event.context())));
          key.reset();
        }
      } catch (InterruptedException e) {
        logger.debug("Watcher terminated ({})", e.getMessage());
      }
    }));
    thread.start();
    return thread;
  }

  public void loadInstalledMods() {
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(modsDirectory, entry -> Files.isDirectory(entry))) {
      for (Path path : directoryStream) {
        addMod(path);
      }
    } catch (IOException e) {
      logger.warn("Mods could not be read from: " + modsDirectory, e);
    }
  }

  public ObservableList<ModVersion> getInstalledModVersions() {
    return readOnlyInstalledModVersions;
  }

  @SneakyThrows
  public CompletableFuture<Void> downloadAndInstallMod(UUID uuid) {
    return fafService.getModVersion(uuid)
      .thenCompose(mod -> downloadAndInstallMod(mod, null, null))
      .exceptionally(throwable -> {
        logger.warn("Sim mod could not be installed", throwable);
        return null;
      });
  }

  public CompletableFuture<Void> downloadAndInstallMod(URL url) {
    return downloadAndInstallMod(url, null, null);
  }

  public CompletableFuture<Void> downloadAndInstallMod(URL url, @Nullable DoubleProperty progressProperty, @Nullable StringProperty titleProperty) {
    InstallModTask task = applicationContext.getBean(InstallModTask.class);
    task.setUrl(url);
    if (progressProperty != null) {
      progressProperty.bind(task.progressProperty());
    }
    if (titleProperty != null) {
      titleProperty.bind(task.titleProperty());
    }

    return taskService.submitTask(task).getFuture()
      .thenRun(this::loadInstalledMods);
  }

  public CompletableFuture<Void> downloadAndInstallMod(ModVersion modVersion, @Nullable DoubleProperty progressProperty, StringProperty titleProperty) {
    return downloadAndInstallMod(modVersion.getDownloadUrl(), progressProperty, titleProperty);
  }

  public Set<UUID> getInstalledModUids() {
    return getInstalledModVersions().stream()
      .map(ModVersion::getUuid)
      .collect(Collectors.toSet());
  }

  public Set<UUID> getInstalledUiModsUids() {
    return getInstalledModVersions().stream()
      .filter(mod -> mod.getModType() == ModType.UI)
      .map(ModVersion::getUuid)
      .collect(Collectors.toSet());
  }

  public void enableSimMods(Set<UUID> simMods) throws IOException {
    Map<UUID, Boolean> modStates = readModStates();

    Set<UUID> installedUiMods = getInstalledUiModsUids();

    for (Entry<UUID, Boolean> entry : modStates.entrySet()) {
      UUID uid = entry.getKey();

      if (!installedUiMods.contains(uid)) {
        // Only disable it if it's a sim mod; because it has not been selected
        entry.setValue(false);
      }
    }
    for (UUID simModUid : simMods) {
      modStates.put(simModUid, true);
    }

    writeModStates(modStates);
  }

  public boolean isModInstalled(UUID uid) {
    return getInstalledModUids().contains(uid);
  }

  public CompletableFuture<Void> uninstallMod(ModVersion modVersion) {
    UninstallModTask task = applicationContext.getBean(UninstallModTask.class);
    task.setModVersion(modVersion);
    return taskService.submitTask(task).getFuture();
  }

  public Path getPathForMod(ModVersion modVersionToFind) {
    return pathToMod.entrySet().stream()
      .filter(pathModEntry -> pathModEntry.getValue().getUuid().equals(modVersionToFind.getUuid()))
      .findFirst()
      .map(Entry::getKey)
      .orElse(null);
  }

  public CompletableFuture<List<ModVersion>> getNewestMods(int count, int page) {
    return findByQuery(new SearchConfig(new SortConfig(SearchableProperties.NEWEST_MOD_KEY, SortOrder.DESC), ""), page, count);
  }

  @NotNull
  public ModVersion extractModInfo(Path path) {
    Path modInfoLua = path.resolve("mod_info.lua");
    logger.debug("Reading mod {}", path);
    if (Files.notExists(modInfoLua)) {
      throw new ModLoadException("Missing mod_info.lua in: " + path.toAbsolutePath());
    }

    try (InputStream inputStream = Files.newInputStream(modInfoLua)) {
      return extractModInfo(inputStream, path);
    } catch (Exception e) {
      throw new ModLoadException(e);
    }
  }

  @NotNull
  private ModVersion extractModInfo(InputStream inputStream, Path basePath) {
    return fromModInfo(modReader.readModInfo(inputStream, basePath), basePath);
  }

  /**
   * @param basePath path to the directory where all the mod files are, used to resolve the path of the icon file.
   */
  private ModVersion fromModInfo(com.faforever.commons.mod.Mod modInfo, Path basePath) {
    ModVersion modVersion = modInfoMapper.map(modInfo);

    Optional.ofNullable(modVersion.getIcon())
      .map(icon -> Paths.get(icon))
      .filter(iconPath -> iconPath.getNameCount() > 2)
      .ifPresent(iconPath -> modVersion.setImagePath(basePath.resolve(iconPath.subpath(2, iconPath.getNameCount()))));

    return modVersion;
  }

  public CompletableTask<Void> uploadMod(Path modPath) {
    ModUploadTask modUploadTask = applicationContext.getBean(ModUploadTask.class);
    modUploadTask.setModPath(modPath);

    return taskService.submitTask(modUploadTask);
  }

  public void evictModsCache() {
    fafService.evictModsCache();
  }

  /**
   * Returns the download size of the specified modVersion in bytes.
   */
  @SneakyThrows
  public long getModSize(ModVersion modVersion) {
    HttpURLConnection conn = null;
    try {
      conn = (HttpURLConnection) modVersion.getDownloadUrl().openConnection();
      conn.setRequestMethod(HttpMethod.HEAD.name());
      return conn.getContentLength();
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
    }
  }

  public ComparableVersion readModVersion(Path modDirectory) {
    return extractModInfo(modDirectory).getVersion();
  }

  public CompletableFuture<List<FeaturedMod>> getFeaturedMods() {
    return fafService.getFeaturedMods();
  }

  public CompletableFuture<FeaturedMod> getFeaturedMod(String featuredMod) {
    return getFeaturedMods().thenCompose(featuredModBeans -> completedFuture(featuredModBeans.stream()
      .filter(mod -> featuredMod.equals(mod.getTechnicalName()))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Not a valid featured mod: " + featuredMod))
    ));
  }

  public CompletableFuture<List<ModVersion>> findByQuery(SearchConfig searchConfig, int page, int count) {
    return fafService.findModsByQuery(searchConfig, page, count);
  }

  @CacheEvict(value = CacheNames.MODS, allEntries = true)
  public void evictCache() {
    // Nothing to see here
  }

  @Async
  public CompletableFuture<List<ModVersion>> getHighestRatedUiMods(int count, int page) {
    return fafService.findModsByQuery(new SearchConfig(new SortConfig(SearchableProperties.HIGHEST_RATED_MOD_KEY, SortOrder.DESC), "latestVersion.type==UI"), page, count);
  }

  public CompletableFuture<List<ModVersion>> getHighestRatedMods(int count, int page) {
    return fafService.findModsByQuery(new SearchConfig(new SortConfig(SearchableProperties.HIGHEST_RATED_MOD_KEY, SortOrder.DESC), ""), page, count);
  }

  public List<ModVersion> getActivatedSimAndUIMods() throws IOException {
    Map<UUID, Boolean> modStates = readModStates();
    return getInstalledModVersions().parallelStream()
      .filter(mod -> modStates.containsKey(mod.getUuid()) && modStates.get(mod.getUuid()))
      .collect(Collectors.toList());
  }

  public void overrideActivatedMods(List<ModVersion> modVersions) throws IOException {
    Map<UUID, Boolean> modStates = modVersions.parallelStream().collect(Collectors.toMap(ModVersion::getUuid, o -> true));
    writeModStates(modStates);
  }

  private Map<UUID, Boolean> readModStates() throws IOException {
    Path preferencesFile = preferencesService.getPreferences().getForgedAlliance().getPreferencesFile();
    Map<UUID, Boolean> mods = new HashMap<>();

    String preferencesContent = new String(Files.readAllBytes(preferencesFile), US_ASCII);
    Matcher matcher = ACTIVE_MODS_PATTERN.matcher(preferencesContent);
    if (matcher.find()) {
      Matcher activeModMatcher = ACTIVE_MOD_PATTERN.matcher(matcher.group(0));
      while (activeModMatcher.find()) {
        UUID modUid = UUID.fromString(activeModMatcher.group(1));
        boolean enabled = Boolean.parseBoolean(activeModMatcher.group(2));

        mods.put(modUid, enabled);
      }
    }

    return mods;
  }

  private void writeModStates(Map<UUID, Boolean> modStates) throws IOException {
    Path preferencesFile = preferencesService.getPreferences().getForgedAlliance().getPreferencesFile();
    String preferencesContent = new String(Files.readAllBytes(preferencesFile), US_ASCII);

    String currentActiveModsContent = null;
    Matcher matcher = ACTIVE_MODS_PATTERN.matcher(preferencesContent);
    if (matcher.find()) {
      currentActiveModsContent = matcher.group(0);
    }

    StringBuilder newActiveModsContentBuilder = new StringBuilder("active_mods = {");

    Iterator<Entry<UUID, Boolean>> iterator = modStates.entrySet().iterator();
    while (iterator.hasNext()) {
      Entry<UUID, Boolean> entry = iterator.next();
      if (!entry.getValue()) {
        continue;
      }

      newActiveModsContentBuilder.append("\n    ['");
      newActiveModsContentBuilder.append(entry.getKey());
      newActiveModsContentBuilder.append("'] = true");
      if (iterator.hasNext()) {
        newActiveModsContentBuilder.append(",");
      }
    }
    newActiveModsContentBuilder.append("\n}");

    if (currentActiveModsContent != null) {
      preferencesContent = preferencesContent.replace(currentActiveModsContent, newActiveModsContentBuilder);
    } else {
      preferencesContent += newActiveModsContentBuilder.toString();
    }

    Files.write(preferencesFile, preferencesContent.getBytes(US_ASCII));
  }

  private void removeMod(Path path) {
    logger.debug("Removing mod: {}", path);
    installedModVersions.remove(pathToMod.remove(path));
  }

  private void addMod(Path path) {
    logger.debug("Adding mod: {}", path);
    try {
      ModVersion modVersion = extractModInfo(path);
      pathToMod.put(path, modVersion);
      if (!installedModVersions.contains(modVersion)) {
        installedModVersions.add(modVersion);
      }
    } catch (ModLoadException e) {
      logger.debug("Corrupt mod: " + path, e);

      notificationService.addNotification(new PersistentNotification(i18n.get("corruptedMods.notification", path.getFileName()), Severity.WARN, singletonList(
        new Action(i18n.get("corruptedMods.show"), event -> platformService.reveal(path))
      )));
    }
  }

  @Override
  public void destroy() {
    Optional.ofNullable(directoryWatcherThread).ifPresent(Thread::interrupt);
  }
}
