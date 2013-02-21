package org.pentaho.di.trans.steps.archiver;

public enum CompressionType {
  NONE("", "No compression"),
  GZIP(".gz", "GZip files"),
  BZIP(".bz2", "BZip2 files"),
  ZIP(".zip", "ZIP Archive"),
  TAR(".tar", "TAR Archive"),
  TARGZIP(".tgz", "GZipped TAR Archive"),
  ;
  
  private String extension;
  private String description;
  
  private CompressionType(String extension, String description) {
    this.extension = extension;
    this.description = description;
  }
  
  public String getCode() {
    return name();
  }

  public String getExtension() {
    return extension;
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
  
  public static CompressionType getCompressionTypeForDescription(String description) {
    for (CompressionType compressionType : values()) {
      if (compressionType.getDescription().equalsIgnoreCase(description)) {
        return compressionType;
      }
    }
    return CompressionType.NONE;
  }

  public static CompressionType getCompressionTypeForCode(String code) {
    try {
      return CompressionType.valueOf(code);
    } catch(Exception e) {
      return CompressionType.NONE;
    }
  }
}
