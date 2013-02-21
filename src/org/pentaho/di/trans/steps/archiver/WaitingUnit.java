package org.pentaho.di.trans.steps.archiver;

public enum WaitingUnit {
  
  MILLISECONDS(1L, "milliseconds"),
  SECONDS(1000L, "seconds"),
  MINUTES(60*1000L, "minutes"),
  HOURS(60*60*1000L, "hours"),
  DAYS(24*60*1000L, "days"),
  WEEKS(7*24*60*1000L, "weeks"),
  ;
  
  private long unitTimeInMs;
  private String description;
  
  private WaitingUnit(long waitingTimeInMs, String description) {
    this.unitTimeInMs = waitingTimeInMs;
    this.description = description;
  }
  
  public String getCode() {
    return name();
  }
  
  public long getUnitTimeInMs() {
    return unitTimeInMs;
  }
  
  public String getDescription() {
    return description;
  }

  public static String[] getDescriptions() {
    String[] strings = new String[values().length];
    for (int i=0;i<strings.length;i++) {
      strings[i] = values()[i].getDescription();
    }
    return strings;
  }
  
  public static WaitingUnit getWaitingUnitForDescription(String description) {
    for (WaitingUnit unit : values()) {
      if (unit.getDescription().equalsIgnoreCase(description)) {
        return unit;
      }
    }
    return WaitingUnit.MILLISECONDS;
  }

  public static WaitingUnit getWaitingUnitForCode(String code) {
    try {
      return WaitingUnit.valueOf(code);
    } catch(Exception e) {
      return WaitingUnit.MILLISECONDS;
    }
  }
}
