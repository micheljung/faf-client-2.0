package com.faforever.client.replay;

import com.faforever.client.domain.api.FeaturedMod;
import com.faforever.client.domain.api.Replay;
import com.faforever.client.featuredmod.FeaturedModService;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.OpenOnlineReplayVaultEvent;
import com.faforever.client.main.event.ShowReplayEvent;
import com.faforever.client.main.event.ShowUserReplaysEvent;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.VaultPrefs;
import com.faforever.client.preferences.ReplaySearchPrefs;
import com.faforever.client.query.CategoryFilterController;
import com.faforever.client.query.SearchablePropertyMappings;
import com.faforever.client.query.TextFilterController;
import com.faforever.client.query.RangeFilterController;
import com.faforever.client.query.DateRangeFilterController;
import com.faforever.client.query.ToggleFilterController;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.theme.UiService;
import com.faforever.client.vault.VaultEntityController;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.commons.api.dto.Game;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import javafx.beans.binding.Bindings;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.faforever.client.filter.ChatUserFilterController.MAX_RATING;
import static com.faforever.client.filter.ChatUserFilterController.MIN_RATING;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class OnlineReplayVaultController extends VaultEntityController<Replay> {

  private static final int TOP_ELEMENT_COUNT = 6;

  private final FeaturedModService featuredModService;
  private final LeaderboardService leaderboardService;
  private final ReplayService replayService;

  private int playerId;
  private ReplayDetailController replayDetailController;

  public OnlineReplayVaultController(FeaturedModService featuredModService, LeaderboardService leaderboardService,
                                     ReplayService replayService, UiService uiService,
                                     NotificationService notificationService, I18n i18n,
                                     ReportingService reportingService, VaultPrefs vaultPrefs,
                                     FxApplicationThreadExecutor fxApplicationThreadExecutor) {
    super(uiService, notificationService, i18n, reportingService, vaultPrefs, fxApplicationThreadExecutor);
    this.leaderboardService = leaderboardService;
    this.replayService = replayService;
    this.featuredModService = featuredModService;
  }

  @Override
  protected void onInitialize() {
    super.onInitialize();
    uploadButton.setVisible(false);
  }

  @Override
  protected void onDisplayDetails(Replay replay) {
    JavaFxUtil.assertApplicationThread();
    replayDetailController.setReplay(replay);
    replayDetailController.getRoot().setVisible(true);
    replayDetailController.getRoot().requestFocus();
  }

  @Override
  protected void setSupplier(SearchConfig searchConfig) {
    switch (searchType) {
      case SEARCH -> currentSupplier = replayService.findByQueryWithPageCount(searchConfig, pageSize, pagination.getCurrentPageIndex() + 1);
      case OWN -> currentSupplier = replayService.getOwnReplaysWithPageCount(pageSize, pagination.getCurrentPageIndex() + 1);
      case NEWEST -> currentSupplier = replayService.getNewestReplaysWithPageCount(pageSize, pagination.getCurrentPageIndex() + 1);
      case HIGHEST_RATED -> currentSupplier = replayService.getHighestRatedReplaysWithPageCount(pageSize, pagination.getCurrentPageIndex() + 1);
      case PLAYER -> currentSupplier = replayService.getReplaysForPlayerWithPageCount(playerId, pageSize, pagination.getCurrentPageIndex() + 1);
    }
  }

  @Override
  protected ReplayCardController createEntityCard() {
    ReplayCardController controller = uiService.loadFxml("theme/vault/replay/replay_card.fxml");
    controller.setOnOpenDetailListener(this::onDisplayDetails);
    return controller;
  }

  @Override
  protected List<ShowRoomCategory<Replay>> getShowRoomCategories() {
    return List.of(
        new ShowRoomCategory<>(() -> replayService.getOwnReplaysWithPageCount(TOP_ELEMENT_COUNT, 1), SearchType.OWN,
                               "vault.replays.ownReplays"),
        new ShowRoomCategory<>(() -> replayService.getNewestReplaysWithPageCount(TOP_ELEMENT_COUNT, 1),
                               SearchType.NEWEST,
                               "vault.replays.newest"),
        new ShowRoomCategory<>(() -> replayService.getHighestRatedReplaysWithPageCount(TOP_ELEMENT_COUNT, 1),
                               SearchType.HIGHEST_RATED, "vault.replays.highestRated")
    );
  }

  @Override
  public void onUploadButtonClicked() {
    // do nothing
  }

  @Override
  protected void onManageVaultButtonClicked() {
    // do nothing
  }

  @Override
  protected Node getDetailView() {
    replayDetailController = uiService.loadFxml("theme/vault/replay/replay_detail.fxml");
    return replayDetailController.getRoot();
  }

  @Override
  protected void initSearchController() {
    searchController.setRootType(Game.class);
    searchController.setSearchableProperties(SearchablePropertyMappings.GAME_PROPERTY_MAPPING);
    searchController.setSortConfig(vaultPrefs.onlineReplaySortConfigProperty());
    searchController.setOnlyShowLastYearCheckBoxVisible(true);
    searchController.setVaultRoot(vaultRoot);
    searchController.setSavedQueries(vaultPrefs.getSavedReplayQueries());

    ReplaySearchPrefs replaySearchPrefs = vaultPrefs.getReplaySearch();
    TextFilterController textFilterController = searchController.addTextFilter("playerStats.player.login", i18n.get("game.player.username"), true);
    textFilterController.textFieldProperty().bindBidirectional(replaySearchPrefs.playerNameFieldProperty());
    textFilterController = searchController.addTextFilter("mapVersion.map.displayName", i18n.get("game.map.displayName"), false);
    textFilterController.textFieldProperty().bindBidirectional(replaySearchPrefs.mapNameFieldProperty());
    textFilterController = searchController.addTextFilter("mapVersion.map.author.login", i18n.get("game.map.author"), false);
    textFilterController.textFieldProperty().bindBidirectional(replaySearchPrefs.mapAuthorFieldProperty());
    textFilterController = searchController.addTextFilter("name", i18n.get("game.title"), false);
    textFilterController.textFieldProperty().bindBidirectional(replaySearchPrefs.titleFieldProperty());
    textFilterController = searchController.addTextFilter("id", i18n.get("game.id"), true);
    textFilterController.textFieldProperty().bindBidirectional(replaySearchPrefs.replayIDFieldProperty());

    CategoryFilterController featuredModFilterController = searchController.addCategoryFilter("featuredMod.displayName",
        i18n.get("featuredMod.displayName"), List.of());

    featuredModService.getFeaturedMods().map(FeaturedMod::displayName)
                      .collectList()
                      .publishOn(fxApplicationThreadExecutor.asScheduler())
                      .subscribe((mods) -> {
                        featuredModFilterController.setItems(mods);
                        replaySearchPrefs.featuredModFilterProperty().get().stream().forEach((item) -> featuredModFilterController.checkItem(item));
                        replaySearchPrefs.featuredModFilterProperty().bind(Bindings.createObjectBinding(() -> featuredModFilterController.getCheckedItems()));
                      });

    CategoryFilterController leaderboardFilterController = searchController.addCategoryFilter(
        "playerStats.ratingChanges.leaderboard.id",
        i18n.get("leaderboard.displayName"), Map.of());

    leaderboardService.getLeaderboards()
                      .collect(Collectors.toMap(
                          leaderboard -> i18n.getOrDefault(leaderboard.technicalName(), leaderboard.nameKey()),
                          leaderboard -> String.valueOf(leaderboard.id())))
                      .publishOn(fxApplicationThreadExecutor.asScheduler())
                      .subscribe((leaderboards) -> {
                        leaderboardFilterController.setItems(leaderboards);
                        replaySearchPrefs.leaderboardFilterProperty().get().stream().forEach((item) -> leaderboardFilterController.checkItem(item));
                        replaySearchPrefs.leaderboardFilterProperty().bind(Bindings.createObjectBinding(() -> leaderboardFilterController.getCheckedItems()));
                      });

    //TODO: Use rating rather than estimated mean with an assumed deviation of 300 when that is available
    RangeFilterController rangeFilterController = searchController.addRangeFilter("playerStats.ratingChanges.meanBefore", i18n.get("game.rating"),
        MIN_RATING, MAX_RATING, 10, 4, 0, value -> value + 300);
    rangeFilterController.lowValueProperty().bindBidirectional(replaySearchPrefs.ratingMinProperty());
    rangeFilterController.highValueProperty().bindBidirectional(replaySearchPrefs.ratingMaxProperty());

    rangeFilterController = searchController.addRangeFilter("reviewsSummary.averageScore", i18n.get("reviews.averageScore"),0, 5, 10, 4, 1);
    rangeFilterController.lowValueProperty().bindBidirectional(replaySearchPrefs.averageReviewScoresMinProperty());
    rangeFilterController.highValueProperty().bindBidirectional(replaySearchPrefs.averageReviewScoresMaxProperty());

    DateRangeFilterController dateRangerFilterController = searchController.addDateRangeFilter("endTime", i18n.get("game.date"), 1);
    dateRangerFilterController.beforeDateProperty().bindBidirectional(replaySearchPrefs.gameBeforeDateProperty());
    dateRangerFilterController.afterDateProperty().bindBidirectional(replaySearchPrefs.gameAfterDateProperty());

    rangeFilterController = searchController.addRangeFilter("replayTicks", i18n.get("game.duration"), 0, 60, 12, 4, 0, value -> (int) (value * 60 * 10));
    rangeFilterController.lowValueProperty().bindBidirectional(replaySearchPrefs.gameDurationMinProperty());
    rangeFilterController.highValueProperty().bindBidirectional(replaySearchPrefs.gameDurationMaxProperty());

    ToggleFilterController toggleFilterController = searchController.addToggleFilter("validity", i18n.get("game.onlyRanked"), "VALID");
    toggleFilterController.selectedProperty().bindBidirectional(replaySearchPrefs.onlyRankedProperty());
  }

  @Override
  protected Class<? extends NavigateEvent> getDefaultNavigateEvent() {
    return OpenOnlineReplayVaultEvent.class;
  }

  @Override
  protected void handleSpecialNavigateEvent(NavigateEvent navigateEvent) {
    if (navigateEvent instanceof ShowReplayEvent showReplayEvent) {
      onShowReplayEvent(showReplayEvent);
    } else if (navigateEvent instanceof ShowUserReplaysEvent showUserReplaysEvent) {
      onShowUserReplaysEvent(showUserReplaysEvent);
    } else {
      log.warn("No such NavigateEvent for this Controller: {}", navigateEvent.getClass());
    }
  }

  private void onShowReplayEvent(ShowReplayEvent event) {
    int replayId = event.getReplayId();
    if (state.get() == State.UNINITIALIZED) {
      ChangeListener<State> stateChangeListener = new ChangeListener<>() {
        @Override
        public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue) {
          if (newValue != State.SHOWROOM) {
            return;
          }
          showReplayWithID(replayId);
          state.removeListener(this);
        }
      };
      state.addListener(stateChangeListener);
      //We have to wait for the Show Room to load otherwise it will not be loaded and it looks strange
      loadShowRooms();
    }

    showReplayWithID(replayId);
  }

  private void showReplayWithID(int replayId) {
    replayService.findById(replayId)
                 .switchIfEmpty(Mono.fromRunnable(
                     () -> notificationService.addImmediateWarnNotification("replay.replayNotFoundText", replayId)))
                 .publishOn(fxApplicationThreadExecutor.asScheduler())
                 .subscribe(this::onDisplayDetails,
                            throwable -> log.error("Error while loading replay {}", replayId, throwable));
  }

  private void onShowUserReplaysEvent(ShowUserReplaysEvent event) {
    enterSearchingState();
    searchType = SearchType.PLAYER;
    playerId = event.getPlayerId();
    displayFromSupplier(() -> replayService.getReplaysForPlayerWithPageCount(playerId, pageSize, 1), true);
  }
}
