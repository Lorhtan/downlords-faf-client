package com.faforever.client.player;


import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.OffsetDateTime;

public final class NameRecord {
  private final StringProperty name;
  private final ObjectProperty<OffsetDateTime> changeDate;

  public NameRecord() {
    this.name = new SimpleStringProperty();
    this.changeDate = new SimpleObjectProperty<>();
  }

  public String getName() {
    return name.get();
  }

  public void setName(String name) {
    this.name.set(name);
  }

  public StringProperty nameProperty() {
    return name;
  }

  public OffsetDateTime getChangeDate() {
    return changeDate.get();
  }

  public void setChangeDate(OffsetDateTime changeDate) {
    this.changeDate.set(changeDate);
  }

  public ObjectProperty<OffsetDateTime> changeDateProperty() {
    return changeDate;
  }
}
