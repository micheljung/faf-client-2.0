package com.faforever.client.remote;

import com.faforever.client.api.TokenService;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.legacy.UidService;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.player.Player;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.domain.Avatar;
import com.faforever.client.remote.domain.MatchmakingState;
import com.faforever.client.remote.domain.PeriodType;
import com.faforever.client.remote.domain.inbound.InboundMessage;
import com.faforever.client.remote.domain.inbound.faf.GameLaunchMessage;
import com.faforever.client.remote.domain.inbound.faf.IceServersMessage.IceServer;
import com.faforever.client.remote.domain.inbound.faf.LoginMessage;
import com.faforever.client.remote.domain.outbound.gpg.GpgOutboundMessage;
import com.faforever.client.teammatchmaking.MatchmakingQueue;
import com.faforever.client.update.Version;
import com.faforever.commons.api.dto.Faction;
import com.faforever.commons.lobby.FafLobbyClient;
import com.faforever.commons.lobby.FafLobbyClient.Config;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.GameVisibility;
import com.faforever.commons.lobby.MatchmakerState;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.github.nocatch.NoCatch.noCatch;

@Service
@Slf4j
@RequiredArgsConstructor
@Primary
public class ReactiveFafServerAccessor implements FafServerAccessor, InitializingBean {

  private final HashMap<Class<? extends InboundMessage>, Collection<Consumer<InboundMessage>>> messageListeners = new HashMap<>();

  private final ObjectProperty<ConnectionState> connectionState = new SimpleObjectProperty<>(ConnectionState.DISCONNECTED);

  private final ObjectMapper objectMapper;
  private final ClientProperties clientProperties;
  private final PreferencesService preferencesService;
  private final UidService uidService;
  private final TokenService tokenService;

  private FafLobbyClient lobbyClient;

  @Override
  public void afterPropertiesSet() throws Exception {

  }

  @Override
  public <T extends InboundMessage> void addOnMessageListener(Class<T> type, Consumer<T> listener) {
    if (!messageListeners.containsKey(type)) {
      messageListeners.put(type, new LinkedList<>());
    }
    messageListeners.get(type).add((Consumer<InboundMessage>) listener);
  }

  @Override
  public <T extends InboundMessage> void removeOnMessageListener(Class<T> type, Consumer<T> listener) {
    messageListeners.getOrDefault(type, List.of()).remove(listener);
  }

  @Override
  public ConnectionState getConnectionState() {
    return connectionState.get();
  }

  @Override
  public ReadOnlyObjectProperty<ConnectionState> connectionStateProperty() {
    return connectionState;
  }

  @Override
  public CompletableFuture<LoginMessage> connectAndLogIn() {
    objectMapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE).disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    connectionState.setValue(ConnectionState.CONNECTING);

    FafLobbyClient.Config config = new Config(
        Version.getCurrentVersion(),
        "downlords-faf-client",
        clientProperties.getServer().getHost(),
        clientProperties.getServer().getPort() + 1,
        sessionId -> noCatch(() -> uidService.generate(String.valueOf(sessionId), preferencesService.getFafDataDirectory().resolve("uid.log"))),
        1024 * 1024,
        false
    );
    lobbyClient = new FafLobbyClient(config, objectMapper);

    // Emulate the legacy server messages for backwards compatibility
    lobbyClient.getEvents()
        .flatMap(message -> {
          try {
            return Mono.just(objectMapper.convertValue(message, InboundMessage.class));
          } catch (Exception e) {
            log.error("Failed to map jackson message: {}", message);
            // swallow the error and keep the Flux alive
            return Mono.empty();
          }
        })
        .doOnNext(legacyLobbyMessage ->
            messageListeners.getOrDefault(legacyLobbyMessage.getClass(), Collections.emptyList())
                .forEach(consumer -> consumer.accept(legacyLobbyMessage)))
        .onErrorResume(throwable -> {
          log.error("Failed processing lobby event", throwable);
          // swallow the error and keep the Flux alive
          return Mono.empty();
        })
        .subscribe();

    return lobbyClient.connectAndLogin(tokenService.getRefreshedTokenValue())
        .map(success -> noCatch(() -> objectMapper.readValue(objectMapper.writeValueAsString(success), LoginMessage.class)))
        .doOnNext(loginMessage -> connectionState.setValue(ConnectionState.CONNECTED))
        .toFuture();
  }

  @Override
  public CompletableFuture<GameLaunchMessage> requestHostGame(NewGameInfo newGameInfo) {
    return lobbyClient.requestHostGame(
        newGameInfo.getTitle(),
        newGameInfo.getMap(),
        newGameInfo.getFeaturedMod().getTechnicalName(),
        GameVisibility.valueOf(newGameInfo.getGameVisibility().name()),
        newGameInfo.getPassword(),
        newGameInfo.getRatingMin(),
        newGameInfo.getRatingMax(),
        newGameInfo.getEnforceRatingRange()
    )
        .map(launchResponse -> noCatch(() -> objectMapper.readValue(objectMapper.writeValueAsString(launchResponse), GameLaunchMessage.class)))
        .toFuture();
  }

  @Override
  public CompletableFuture<GameLaunchMessage> requestJoinGame(int gameId, String password) {
    return lobbyClient.requestJoinGame(gameId, password)
        .map(launchResponse -> noCatch(() -> objectMapper.readValue(objectMapper.writeValueAsString(launchResponse), GameLaunchMessage.class)))
        .toFuture();
  }

  @Override
  public void disconnect() {
    lobbyClient.disconnect();
    connectionState.setValue(ConnectionState.DISCONNECTED);
  }

  @Override
  public void reconnect() {
    lobbyClient.connectAndLogin(tokenService.getRefreshedTokenValue()).subscribe();
  }

  @Override
  public void addFriend(int playerId) {
    lobbyClient.addFriend(playerId);
  }

  @Override
  public void addFoe(int playerId) {
    lobbyClient.addFoe(playerId);
  }

  @Override
  public void requestMatchmakerInfo() {
    lobbyClient.requestMatchmakerInfo();
  }

  @Override
  public CompletableFuture<GameLaunchMessage> startSearchMatchmaker() {
    return lobbyClient.getEvents()
        .filter(event -> event instanceof GameLaunchResponse)
        .next()
        .cast(GameLaunchResponse.class)
        .map(launchResponse -> noCatch(() -> objectMapper.readValue(objectMapper.writeValueAsString(launchResponse), GameLaunchMessage.class)))
        .toFuture();
  }

  @Override
  public void stopSearchMatchmaker() {
    // Not implemented
  }

  @Override
  public void sendGpgMessage(GpgOutboundMessage message) {
    lobbyClient.sendGpgGameMessage(
        new com.faforever.commons.lobby.GpgGameOutboundMessage(
            message.getCommand(),
            message.getArgs()
        )
    );
  }

  @Override
  public void removeFriend(int playerId) {
    lobbyClient.removeFriend(playerId);
  }

  @Override
  public void removeFoe(int playerId) {
    lobbyClient.removeFoe(playerId);
  }

  @Override
  public void selectAvatar(URL url) {
    // Not implemented, use API instead
  }

  @Override
  public CompletableFuture<List<Avatar>> getAvailableAvatars() {
    return CompletableFuture.completedFuture(List.of());
  }

  @Override
  public void banPlayer(int playerId, int duration, PeriodType periodType, String reason) {
    // Not implemented, use API instead
  }

  @Override
  public void closePlayersGame(int playerId) {
    lobbyClient.closePlayerGame(playerId);
  }

  @Override
  public void closePlayersLobby(int playerId) {
    lobbyClient.closePlayerLobby(playerId);
  }

  @Override
  public void broadcastMessage(String message) {
    lobbyClient.broadcastMessage(message);
  }

  @Override
  public CompletableFuture<List<IceServer>> getIceServers() {
    return lobbyClient.getIceServers()
        .map(iceServers -> iceServers.stream()
            .map(iceServer ->
                new IceServer(
                    iceServer.getUrl(),
                    (List<String>) iceServer.getUrls(),
                    iceServer.getUsername(),
                    iceServer.getCredentialType(),
                    iceServer.getCredential()
            ))
            .collect(Collectors.toList())
        )
        .toFuture();
  }

  @Override
  public void restoreGameSession(int id) {
    // Not implemented
  }

  @Override
  public void ping() {
    // Not implemented
  }

  @Override
  public void gameMatchmaking(MatchmakingQueue queue, MatchmakingState state) {
    lobbyClient.gameMatchmaking(queue.getTechnicalName(), MatchmakerState.valueOf(state.name()));
  }

  @Override
  public void inviteToParty(Player recipient) {
    lobbyClient.inviteToParty(recipient.getId());
  }

  @Override
  public void acceptPartyInvite(Player sender) {
    lobbyClient.acceptPartyInvite(sender.getId());
  }

  @Override
  public void kickPlayerFromParty(Player kickedPlayer) {
    lobbyClient.kickPlayerFromParty(kickedPlayer.getId());
  }

  @Override
  public void readyParty() {
    lobbyClient.readyParty();
  }

  @Override
  public void unreadyParty() {
    lobbyClient.unreadyParty();
  }

  @Override
  public void leaveParty() {
    lobbyClient.leaveParty();
  }

  @Override
  public void setPartyFactions(List<Faction> factions) {
    lobbyClient.setPartyFactions(
        factions.stream()
            .map(faction -> com.faforever.commons.lobby.Faction.valueOf(faction.name()))
            .collect(Collectors.toSet())
    );
  }

}
