package com.faforever.client.news;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Website;
import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.SimpleChangeListener;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.main.event.NavigateEvent;
import javafx.concurrent.Worker;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebView;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class NewsController extends AbstractViewController<Node> {
  private final WebViewConfigurer webViewConfigurer;
  private final ClientProperties clientProperties;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  public Pane newsRoot;
  public WebView newsWebView;
  public Control loadingIndicator;
  private final SimpleChangeListener<Boolean> loadingIndicatorListener = newValue
      -> loadingIndicator.getParent().getChildrenUnmodifiable().stream()
      .filter(node -> node != loadingIndicator)
      .forEach(node -> node.setVisible(!newValue));
  ;

  @Override
  public void initialize() {
    newsWebView.setContextMenuEnabled(false);
    webViewConfigurer.configureWebView(newsWebView);

    loadingIndicator.managedProperty().bind(loadingIndicator.visibleProperty());
    loadingIndicator.visibleProperty().addListener(loadingIndicatorListener);
    loadingIndicatorListener.changed(loadingIndicator.visibleProperty(), null, true);

    loadingIndicator.getParent().getChildrenUnmodifiable()
        .forEach(node -> node.managedProperty().bind(node.visibleProperty()));
    newsWebView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
      if (newState == Worker.State.SUCCEEDED) {
        onLoadingStop();
      }
    });
  }

  private void onLoadingStart() {
    fxApplicationThreadExecutor.execute(() -> loadingIndicator.setVisible(true));
  }

  private void onLoadingStop() {
    fxApplicationThreadExecutor.execute(() -> loadingIndicator.setVisible(false));
  }

  @Override
  protected void onDisplay(NavigateEvent navigateEvent) {
    onLoadingStart();
    loadNews();
  }

  private void loadNews() {
    fxApplicationThreadExecutor.execute(() -> {
      Website website = clientProperties.getWebsite();
      newsWebView.getEngine().load(website.getNewsHubUrl());
    });
  }

  public Node getRoot() {
    return newsRoot;
  }

}
