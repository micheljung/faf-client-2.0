package com.faforever.client.chat;

import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;

@Component
@Lazy
public class ChatNavigation {

  private final LinkedHashSet<ChatTab> currentTabs = new LinkedHashSet<>();

  public boolean addTabIfMissing(String tabId) {
    return currentTabs.add(new ChatTab(tabId));
  }

  public void removeTab(String tabId) {
    currentTabs.removeIf(chatTab -> chatTab.getId().equals(tabId));
  }

  @Nullable
  public String getLastOpenedTabId() {
    return currentTabs.stream().filter(ChatTab::isSelected).findFirst().map(ChatTab::getId).orElse(null);
  }

  public void setLastOpenedTabId(String tabId) {
    currentTabs.forEach(tab -> tab.setSelected(tab.getId().equals(tabId)));
  }

  public void clear() {
    currentTabs.clear();
  }
}
