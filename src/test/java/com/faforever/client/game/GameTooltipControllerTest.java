package com.faforever.client.game;

import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.testfx.util.WaitForAsyncUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class GameTooltipControllerTest extends UITest {

  @Mock
  private UiService uiService;
  @Mock
  private PlayerService playerService;
  
  @Mock
  private TeamCardController teamCardController;
  @InjectMocks
  private GameTooltipController instance;
  
  @BeforeEach
  public void setUp() throws Exception {
    when(uiService.loadFxml("theme/team_card.fxml")).thenReturn(teamCardController);
    when(teamCardController.getRoot()).thenReturn(new Pane());
    when(playerService.getPlayerByNameIfOnline(Mockito.anyString())).thenReturn(Optional.of(PlayerBeanBuilder.create().defaultValues().get()));

    loadFxml("theme/play/game_tooltip.fxml", clazz -> instance);
  }
  
  @Test
  public void testSetGame() {
    Map<String, String> simMods = new HashMap<>();
    Map<Integer, Set<PlayerBean>> teams = new HashMap<>();

    GameBean game = GameBeanBuilder.create().defaultValues().simMods(simMods).teams(teams).get();

    instance.setGame(game);
    instance.displayGame();
    WaitForAsyncUtils.waitForFxEvents();
    assertFalse(instance.modsPane.isVisible());
    assertThat(instance.teamsPane.getPrefColumns(), is(0));

    teams.put(1, Set.of(PlayerBeanBuilder.create().defaultValues().get()));
    instance.setGame(game);
    instance.displayGame();
    WaitForAsyncUtils.waitForFxEvents();
    assertThat(instance.teamsPane.getPrefColumns(), is(1));
    
    simMods.put("mod1", "mod1");
    instance.setGame(game);
    instance.displayGame();
    WaitForAsyncUtils.waitForFxEvents();
    assertTrue(instance.modsPane.isVisible());
  }
  
  @Test
  public void testSetGameNull() {
    instance.setGame(null);
    instance.displayGame();
    WaitForAsyncUtils.waitForFxEvents();
  }
}
