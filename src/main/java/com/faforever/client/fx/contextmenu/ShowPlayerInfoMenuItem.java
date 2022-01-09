package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerInfoWindowController;
import com.faforever.client.theme.UiService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class ShowPlayerInfoMenuItem extends AbstractMenuItem<PlayerBean> {

  private final I18n i18n;
  private final UiService uiService;

  @Override
  protected void onClicked(PlayerBean player) {
    Assert.notNull(player, "No player has been set");
    PlayerInfoWindowController controller = uiService.loadFxml("theme/user_info_window.fxml");
    controller.setPlayer(player);
    controller.setOwnerWindow(getParentPopup().getOwnerWindow());
    controller.show();
  }

  @Override
  protected boolean isItemVisible(PlayerBean player) {
    return player != null;
  }

  @Override
  protected String getItemText() {
    return i18n.get("chat.userContext.userInfo");
  }
}