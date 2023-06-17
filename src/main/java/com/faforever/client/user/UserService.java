package com.faforever.client.user;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.api.SessionExpiredEvent;
import com.faforever.client.api.TokenService;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Oauth;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.user.event.LogOutRequestEvent;
import com.faforever.client.user.event.LoggedOutEvent;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.faforever.commons.api.dto.MeResult;
import com.faforever.commons.lobby.Player;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.hash.Hashing;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Base64.Encoder;

@Lazy
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements InitializingBean {

  private static final Encoder BASE64_ENCODER = Base64.getUrlEncoder().withoutPadding();

  private final ReadOnlyObjectWrapper<MeResult> ownUser = new ReadOnlyObjectWrapper<>();
  private final ReadOnlyObjectWrapper<Player> ownPlayer = new ReadOnlyObjectWrapper<>();

  private final ClientProperties clientProperties;
  private final FafServerAccessor fafServerAccessor;
  private final FafApiAccessor fafApiAccessor;
  private final EventBus eventBus;
  private final TokenService tokenService;
  private final NotificationService notificationService;
  private final LoginPrefs loginPrefs;

  public String getHydraUrl(String state, String codeVerifier, URI redirectUri) {
    Oauth oauth = clientProperties.getOauth();
    String codeChallenge = BASE64_ENCODER.encodeToString(Hashing.sha256()
        .hashString(codeVerifier, StandardCharsets.US_ASCII)
        .asBytes());
    return String.format("%s/oauth2/auth?response_type=code&client_id=%s&state=%s&redirect_uri=%s&scope=%s&code_challenge_method=S256&code_challenge=%s", oauth.getBaseUrl(), oauth.getClientId(), state, redirectUri.toASCIIString(), oauth.getScopes(), codeChallenge);
  }

  public Mono<Void> login(String code, String codeVerifier, URI redirectUri) {
    log.info("Logging in with authorization code");
    return tokenService.loginWithAuthorizationCode(code, codeVerifier, redirectUri).then(loginToServices());
  }

  public Mono<Void> loginWithRefreshToken() {
    log.info("Logging in with refresh token");
    return tokenService.loginWithRefreshToken().then(loginToServices());
  }

  private Mono<Void> loginToServices() {
    return Mono.zip(loginToApi(), loginToLobbyServer()).doOnNext(TupleUtils.consumer((meResult, mePlayer) -> {
      if (Integer.parseInt(meResult.getUserId()) != mePlayer.getId()) {
        throw new IllegalStateException("Different player logged into server and api");
      }

      ownUser.set(meResult);
      ownPlayer.set(mePlayer);
    })).doOnError(throwable -> resetUserState()).then(Mono.fromRunnable(() -> eventBus.post(new LoginSuccessEvent())));
  }

  private Mono<MeResult> loginToApi() {
    return Mono.fromRunnable(fafApiAccessor::authorize).then(Mono.defer(fafApiAccessor::getMe));
  }

  private Mono<Player> loginToLobbyServer() {
    ConnectionState lobbyConnectionState = fafServerAccessor.getConnectionState();
    if (lobbyConnectionState == ConnectionState.CONNECTED) {
      return Mono.just(ownPlayer.get());
    }
    return fafServerAccessor.connectAndLogIn();
  }

  public String getUsername() {
    return getOwnUser().getUserName();
  }

  public Integer getUserId() {
    return Integer.parseInt(getOwnUser().getUserId());
  }

  private void logOut() {
    log.info("Logging out");
    resetUserState();
    eventBus.post(new LoggedOutEvent());
  }

  private void resetUserState() {
    loginPrefs.setRefreshToken(null);
    fafApiAccessor.reset();
    fafServerAccessor.disconnect();
    ownUser.set(null);
    ownPlayer.set(null);
  }

  @Subscribe
  public void onSessionExpired(SessionExpiredEvent sessionExpiredEvent) {
    notificationService.addImmediateInfoNotification("session.expired.message");
  }

  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);
  }

  @Subscribe
  public void onLogoutRequestEvent(LogOutRequestEvent event) {
    logOut();
  }

  public MeResult getOwnUser() {
    return ownUser.get();
  }

  public Player getOwnPlayer() {
    return ownPlayer.get();
  }

  public ReadOnlyObjectProperty<Player> ownPlayerProperty() {
    return ownPlayer.getReadOnlyProperty();
  }

  public ConnectionState getConnectionState() {
    return fafServerAccessor.getConnectionState();
  }

  public ReadOnlyObjectProperty<ConnectionState> connectionStateProperty() {
    return fafServerAccessor.connectionStateProperty();
  }

  public void reconnectToLobby() {
    fafServerAccessor.reconnect();
  }
}
