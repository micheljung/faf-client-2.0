package com.faforever.client.filter;

import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.domain.api.FeaturedMod;
import com.faforever.client.domain.server.GameInfo;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.featuredmod.FeaturedModService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.LiveReplaySearchPrefs;
import com.faforever.client.social.SocialService;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.faforever.commons.lobby.GameType;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import static com.faforever.client.builders.GameInfoBuilder.create;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class LiveGamesFilterControllerTest extends PlatformTest {

  @Mock
  private UiService uiService;
  @Mock
  private I18n i18n;
  @Mock
  private FeaturedModService featuredModService;
  @Mock
  private PlayerService playerService;
  @Mock
  private SocialService socialService;
  @Mock
  private MapGeneratorService mapGeneratorService;

  @Spy
  private LiveReplaySearchPrefs liveReplaySearchPrefs;

  @Mock
  private FilterCheckboxController<GameInfo> hideModdedGamesFilter;
  @Mock
  private FilterCheckboxController<GameInfo> hideSingleGamesFilter;
  @Mock
  private FilterCheckboxController<GameInfo> onlyGamesWithFriendsFilter;
  @Mock
  private FilterCheckboxController<GameInfo> onlyGeneratedMapsFilter;

  @Mock
  private FilterMultiCheckboxController<GameType, GameInfo> gameTypeFilter;
  @Mock
  private FilterMultiCheckboxController<FeaturedMod, GameInfo> featuredModFilter;

  @Mock
  private FilterTextFieldController<GameInfo> playerNameFilter;


  @InjectMocks
  private LiveGamesFilterController instance;

  @BeforeEach
  public void setUp() throws Exception {
    when(uiService.loadFxml(anyString())).thenReturn(
        hideModdedGamesFilter, // Sim mods
        hideSingleGamesFilter,
        onlyGamesWithFriendsFilter,
        onlyGeneratedMapsFilter,
        gameTypeFilter,
        featuredModFilter,
        playerNameFilter
    );
    when(featuredModService.getFeaturedMods()).thenReturn(Flux.empty());

    when(hideModdedGamesFilter.valueProperty()).thenReturn(new SimpleBooleanProperty());
    when(hideSingleGamesFilter.valueProperty()).thenReturn(new SimpleBooleanProperty());
    when(onlyGamesWithFriendsFilter.valueProperty()).thenReturn(new SimpleBooleanProperty());
    when(onlyGeneratedMapsFilter.valueProperty()).thenReturn(new SimpleBooleanProperty());

    when(gameTypeFilter.valueProperty()).thenReturn(new SimpleListProperty<>());
    when(featuredModFilter.valueProperty()).thenReturn(new SimpleListProperty<>());

    when(playerNameFilter.valueProperty()).thenReturn(new SimpleStringProperty());

    loadFxml("theme/filter/filter.fxml", clazz -> instance, instance);
  }

  @Test
  public void testGameTypeFilter() {
    ArgumentCaptor<BiFunction<List<GameType>, GameInfo, Boolean>> argumentCaptor = ArgumentCaptor.forClass(
        BiFunction.class);
    verify(gameTypeFilter).registerListener(argumentCaptor.capture());

    BiFunction<List<GameType>, GameInfo, Boolean> filter = argumentCaptor.getValue();

    List<GameType> emptyList = Collections.emptyList();
    assertTrue(filter.apply(emptyList, create().defaultValues().gameType(GameType.CUSTOM).get()));
    assertTrue(filter.apply(emptyList, create().defaultValues().gameType(GameType.MATCHMAKER).get()));

    List<GameType> matchmaker = List.of(GameType.MATCHMAKER);
    assertFalse(filter.apply(matchmaker, create().defaultValues().gameType(GameType.CUSTOM).get()));
    assertTrue(filter.apply(matchmaker, create().defaultValues().gameType(GameType.MATCHMAKER).get()));
  }

  @Test
  public void testPlayerNameFilter() {
    ArgumentCaptor<BiFunction<String, GameInfo, Boolean>> argumentCaptor = ArgumentCaptor.forClass(BiFunction.class);
    verify(playerNameFilter).registerListener(argumentCaptor.capture());

    PlayerInfo player1 = PlayerInfoBuilder.create().defaultValues().id(1).username("player1").get();
    PlayerInfo player2 = PlayerInfoBuilder.create().defaultValues().id(2).username("player2").get();
    PlayerInfo enemy1 = PlayerInfoBuilder.create().defaultValues().id(3).username("enemy1").get();
    PlayerInfo enemy2 = PlayerInfoBuilder.create().defaultValues().id(4).username("enemy2").get();

    when(playerService.getPlayerByIdIfOnline(1)).thenReturn(Optional.of(player1));
    when(playerService.getPlayerByIdIfOnline(2)).thenReturn(Optional.of(player2));
    when(playerService.getPlayerByIdIfOnline(3)).thenReturn(Optional.of(enemy1));
    when(playerService.getPlayerByIdIfOnline(4)).thenReturn(Optional.of(enemy2));

    GameInfo game = create().defaultValues().teams(Map.of(1, List.of(1, 2), 2, List.of(3, 4))).get();
    BiFunction<String, GameInfo, Boolean> filter = argumentCaptor.getValue();
    assertTrue(filter.apply("", game));
    assertTrue(filter.apply("player", game));
    assertTrue(filter.apply("enemy", game));
    assertFalse(filter.apply("god", game));
  }

  @Test
  public void testSingleGamesFilter() {
    ArgumentCaptor<BiFunction<Boolean, GameInfo, Boolean>> argumentCaptor = ArgumentCaptor.forClass(BiFunction.class);
    verify(hideSingleGamesFilter).registerListener(argumentCaptor.capture());

    BiFunction<Boolean, GameInfo, Boolean> filter = argumentCaptor.getValue();

    assertTrue(filter.apply(false, create().defaultValues().teams(Map.of(1, List.of(1))).get()));
    assertTrue(filter.apply(false, create().defaultValues().teams(Map.of(1, List.of(1, 2))).get()));
    assertTrue(filter.apply(true, create().defaultValues().teams(Map.of(1, List.of(1), 2, List.of(2))).get()));
    assertFalse(filter.apply(true, create().defaultValues().teams(Map.of(1, List.of(1))).get()));
  }

  @Test
  public void testGameWithFriendsFilter() {
    ArgumentCaptor<BiFunction<Boolean, GameInfo, Boolean>> argumentCaptor = ArgumentCaptor.forClass(BiFunction.class);
    verify(onlyGamesWithFriendsFilter).registerListener(argumentCaptor.capture());

    GameInfo game = create().defaultValues().get();

    BiFunction<Boolean, GameInfo, Boolean> filter = argumentCaptor.getValue();

    assertTrue(filter.apply(false, game));

    when(socialService.areFriendsInGame(game)).thenReturn(false, true);
    assertFalse(filter.apply(true, game));
    assertTrue(filter.apply(true, game));
  }

  @Test
  public void testGeneratedMapsFilter() {
    ArgumentCaptor<BiFunction<Boolean, GameInfo, Boolean>> argumentCaptor = ArgumentCaptor.forClass(BiFunction.class);
    verify(onlyGeneratedMapsFilter).registerListener(argumentCaptor.capture());

    GameInfo game = create().defaultValues().get();

    BiFunction<Boolean, GameInfo, Boolean> filter = argumentCaptor.getValue();

    assertTrue(filter.apply(false, game));

    when(mapGeneratorService.isGeneratedMap(game.getMapFolderName())).thenReturn(false, true);
    assertFalse(filter.apply(true, game));
    assertTrue(filter.apply(true, game));
  }
}
