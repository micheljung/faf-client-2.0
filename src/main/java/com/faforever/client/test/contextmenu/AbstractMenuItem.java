package com.faforever.client.test.contextmenu;

import com.faforever.client.i18n.I18n;
import javafx.scene.control.MenuItem;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationContext;

public abstract class AbstractMenuItem<T> extends MenuItem {

  @Getter
  @Setter()
  private T object;

  private ApplicationContext context;

  public AbstractMenuItem() {
    setOnAction(event -> onClicked(object));
  }

  public final void setContext(ApplicationContext context) {
    this.context = context;

    startItemInitialization();
  }

  private void startItemInitialization() {
    setText(getItemText(getBean(I18n.class)));
    setVisible(getVisible());
  }

  protected abstract void onClicked(T object);

  protected abstract String getItemText(I18n i18n);

  protected boolean getVisible() {
    return true; // by-default;
  }

  protected final <S> S getBean(Class<S> clazz) {
    return context.getBean(clazz);
  }
}
