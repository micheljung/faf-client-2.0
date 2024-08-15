package com.faforever.client.preferences;

import com.faforever.client.vault.search.SearchController.SortConfig;
import com.faforever.client.vault.search.SearchController.SortOrder;
import com.fasterxml.jackson.annotation.JsonMerge;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import lombok.Getter;

public class VaultPrefs {
  private final ObjectProperty<SortConfig> onlineReplaySortConfig = new SimpleObjectProperty<>(
      new SortConfig("startTime", SortOrder.DESC));
  private final ObjectProperty<SortConfig> mapSortConfig = new SimpleObjectProperty<>(
      new SortConfig("gamesPlayed", SortOrder.DESC));
  private final ObjectProperty<SortConfig> modVaultConfig = new SimpleObjectProperty<>(
      new SortConfig("latestVersion.createTime", SortOrder.DESC));
  private final MapProperty<String, String> savedReplayQueries = new SimpleMapProperty<>(
      FXCollections.observableHashMap());
  private final MapProperty<String, String> savedMapQueries = new SimpleMapProperty<>(
      FXCollections.observableHashMap());
  private final MapProperty<String, String> savedModQueries = new SimpleMapProperty<>(
      FXCollections.observableHashMap());
  @JsonMerge
  @Getter
  private final ReplaySearchPrefs replaySearch = new ReplaySearchPrefs();
  @JsonMerge
  @Getter
  private final LiveReplaySearchPrefs liveReplaySearch = new LiveReplaySearchPrefs();
  @JsonMerge
  @Getter
  private final MapSearchPrefs mapSearch = new MapSearchPrefs();
  @JsonMerge
  @Getter
  private final ModSearchPrefs modSearch = new ModSearchPrefs();
  
  public SortConfig getOnlineReplaySortConfig() {
    return onlineReplaySortConfig.get();
  }

  public void setOnlineReplaySortConfig(SortConfig onlineReplaySortConfig) {
    this.onlineReplaySortConfig.set(onlineReplaySortConfig);
  }

  public ObjectProperty<SortConfig> onlineReplaySortConfigProperty() {
    return onlineReplaySortConfig;
  }

  public SortConfig getMapSortConfig() {
    return mapSortConfig.get();
  }

  public void setMapSortConfig(SortConfig mapSortConfig) {
    this.mapSortConfig.set(mapSortConfig);
  }

  public ObjectProperty<SortConfig> mapSortConfigProperty() {
    return mapSortConfig;
  }

  public SortConfig getModVaultConfig() {
    return modVaultConfig.get();
  }

  public void setModVaultConfig(SortConfig modVaultConfig) {
    this.modVaultConfig.set(modVaultConfig);
  }

  public ObjectProperty<SortConfig> modVaultConfigProperty() {
    return modVaultConfig;
  }

  public ObservableMap<String, String> getSavedReplayQueries() {
    return savedReplayQueries.get();
  }

  public void setSavedReplayQueries(ObservableMap<String, String> savedReplayQueries) {
    this.savedReplayQueries.set(savedReplayQueries);
  }

  public MapProperty<String, String> savedReplayQueriesProperty() {
    return savedReplayQueries;
  }

  public ObservableMap<String, String> getSavedMapQueries() {
    return savedMapQueries.get();
  }

  public void setSavedMapQueries(ObservableMap<String, String> savedMapQueries) {
    this.savedMapQueries.set(savedMapQueries);
  }

  public MapProperty<String, String> savedMapQueriesProperty() {
    return savedMapQueries;
  }

  public ObservableMap<String, String> getSavedModQueries() {
    return savedModQueries.get();
  }

  public void setSavedModQueries(ObservableMap<String, String> savedModQueries) {
    this.savedModQueries.set(savedModQueries);
  }

  public MapProperty<String, String> savedModQueriesProperty() {
    return savedModQueries;
  }
}
