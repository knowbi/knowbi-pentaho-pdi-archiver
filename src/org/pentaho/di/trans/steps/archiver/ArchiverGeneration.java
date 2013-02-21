package org.pentaho.di.trans.steps.archiver;

import java.util.regex.Pattern;

import org.apache.commons.vfs.AllFileSelector;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSelectInfo;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileType;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.w3c.dom.Node;

public class ArchiverGeneration {
  
  private static Class<?> PKG = ArchiverGeneration.class;
  
  public static String XML_TAG = "archive-generation"; 
  
  /** The field from which this generation sources */
  private String sourceFolder;
  
  /** Which file to select from the source folder, empty means all files */
  private String sourceRegex;
  
  /** The field in which this generation lives, empty means delete files */
  private String targetFolder;
  
  /** The time a file needs to stay in the source folder before it gets copied over to the target */
  private String waitTime;
  
  /** The waiting time unit */
  private WaitingUnit waitUnit;
  
  /** The keep numerator in combination with the denominator determines how many files are kept while archiving.  For example 3/4 keeps 3 out of every 4 files. 3 is the numerator, 4 is denominator */
  private String keepNumerator;
  
  /** The keep denominator in combination with the numerator determines how many files are kept while archiving.  For example 3/4 keeps 3 out of every 4 files. 3 is the numerator, 4 is denominator */
  private String keepDenominator;

  /** The compression type describes how the archived files need to be treated. */
  private CompressionType compressionType;
  
  /** The base name of the archiving (compression types ZIP and TARGZ) file to use */
  private String archiveBaseName;
  
  /** Flag to indicate whether or not we need to remove the original file */
  private boolean removingOriginal;
  
  public ArchiverGeneration(String sourceFolder, String sourceRegex, String targetFolder, String waitTime,
      WaitingUnit waitUnit, String keepNumerator, String keepDenominator, CompressionType compressionType, String archiveBaseName, 
      boolean removingOriginal) {
    super();
    this.sourceFolder = sourceFolder;
    this.sourceRegex = sourceRegex;
    this.targetFolder = targetFolder;
    this.waitTime = waitTime;
    this.waitUnit = waitUnit;
    this.keepNumerator = keepNumerator;
    this.keepDenominator = keepDenominator;
    this.compressionType = compressionType;
    this.archiveBaseName = archiveBaseName;
    this.removingOriginal = removingOriginal;
  }

  public ArchiverGeneration(Node node) {
    sourceFolder = XMLHandler.getTagValue(node, "source_folder");
    sourceRegex = XMLHandler.getTagValue(node, "source_regex");
    targetFolder = XMLHandler.getTagValue(node, "target_folder");
    waitTime = XMLHandler.getTagValue(node, "wait_time");
    waitUnit = WaitingUnit.getWaitingUnitForCode(XMLHandler.getTagValue(node, "wait_unit"));
    keepNumerator = XMLHandler.getTagValue(node, "keep_numerator");
    keepDenominator = XMLHandler.getTagValue(node, "keep_denominator");
    compressionType = CompressionType.getCompressionTypeForCode(XMLHandler.getTagValue(node, "compression_type"));
    archiveBaseName = XMLHandler.getTagValue(node, "archive_base");
    removingOriginal = "Y".equalsIgnoreCase(XMLHandler.getTagValue(node, "remove_original"));
  }
   
  public ArchiverGeneration(Repository rep, ObjectId id_step, int i) throws KettleException {
    sourceFolder = rep.getStepAttributeString(id_step, i, "source_folder");
    sourceRegex = rep.getStepAttributeString(id_step, i, "source_regex");
    targetFolder = rep.getStepAttributeString(id_step, i, "target_folder");
    waitTime = rep.getStepAttributeString(id_step, i, "wait_time");
    waitUnit = WaitingUnit.getWaitingUnitForCode(rep.getStepAttributeString(id_step, i, "wait_unit"));
    keepNumerator = rep.getStepAttributeString(id_step, i, "keep_numerator");
    keepDenominator = rep.getStepAttributeString(id_step, i, "keep_denominator");
    compressionType = CompressionType.getCompressionTypeForCode(rep.getStepAttributeString(id_step, i, "compression_type"));
    archiveBaseName = rep.getStepAttributeString(id_step, i, "archive_base");
    removingOriginal = rep.getStepAttributeBoolean(id_step, i, "remove_original");
  }

  public String getXML() {
    StringBuilder xml = new StringBuilder();
    
    xml.append(XMLHandler.openTag(XML_TAG));
    xml.append(XMLHandler.addTagValue("source_folder", sourceFolder));
    xml.append(XMLHandler.addTagValue("source_regex", sourceRegex));
    xml.append(XMLHandler.addTagValue("target_folder", targetFolder));
    xml.append(XMLHandler.addTagValue("wait_time", waitTime));
    xml.append(XMLHandler.addTagValue("wait_unit", waitUnit!=null ? waitUnit.getCode() : null));
    xml.append(XMLHandler.addTagValue("keep_numerator", keepNumerator));
    xml.append(XMLHandler.addTagValue("keep_denominator", keepDenominator));
    xml.append(XMLHandler.addTagValue("compression_type", compressionType!=null ? compressionType.getCode() : null));
    xml.append(XMLHandler.addTagValue("archive_base", archiveBaseName));
    xml.append(XMLHandler.addTagValue("remove_original", removingOriginal));
    
    xml.append(XMLHandler.closeTag(XML_TAG));
    
    return xml.toString();
  }

  public void saveRep(Repository rep, ObjectId id_transformation, ObjectId id_step, int i) throws KettleException {
    rep.saveStepAttribute(id_transformation, id_step, i, "source_folder", sourceFolder);
    rep.saveStepAttribute(id_transformation, id_step, i, "source_regex", sourceRegex);
    rep.saveStepAttribute(id_transformation, id_step, i, "target_folder", targetFolder);
    rep.saveStepAttribute(id_transformation, id_step, i, "wait_time", waitTime);
    rep.saveStepAttribute(id_transformation, id_step, i, "wait_unit", waitUnit!=null ? waitUnit.getCode() : null);
    rep.saveStepAttribute(id_transformation, id_step, i, "keep_numerator", keepNumerator);
    rep.saveStepAttribute(id_transformation, id_step, i, "keep_denominator", keepDenominator);
    rep.saveStepAttribute(id_transformation, id_step, i, "compression_type", compressionType!=null ? compressionType.getCode() : null);
    rep.saveStepAttribute(id_transformation, id_step, i, "archive_base", archiveBaseName);
    rep.saveStepAttribute(id_transformation, id_step, i, "remove_original", removingOriginal);
  }

  public FileObject[] findFiles(VariableSpace space) throws KettleException {
    try {
      final Pattern pattern;
      String realSourceRegex = space.environmentSubstitute(sourceRegex);
      if (Const.isEmpty(realSourceRegex)) {
        pattern = null;
      } else {
        pattern = Pattern.compile(realSourceRegex);
      }
      FileObject sourceFileObject = KettleVFS.getFileObject(space.environmentSubstitute(sourceFolder));
      FileObject[] files = sourceFileObject.findFiles(new AllFileSelector() {
        @Override
        public boolean includeFile(FileSelectInfo info) {
          try {
            if (info.getFile().getType().equals(FileType.FILE)) {
              if (pattern==null) {
                return true;
              } else {
                String baseName = info.getFile().getName().getBaseName();
                return pattern.matcher(baseName).matches();
              }
            }
            return false;
          } catch(FileSystemException e) {
            throw new RuntimeException(e);
          }
        }
        
        @Override
        public boolean traverseDescendents(FileSelectInfo arg0) {
          return true;
        }
      });
      
      return files;
    } catch(Exception e) {
      throw new KettleException(BaseMessages.getString(PKG, "ArchiverGeneration.ErrorListingFiles"), e);
    }
  }

  /**
   * @return The maximum waiting age (waiting time) in milliseconds.
   */
  public long getMaxWaitTime() {
    return Const.toLong(waitTime, 0L) * waitUnit.getUnitTimeInMs();
  }

  public boolean isRatioConfigured(VariableSpace space) {
    // Everything empty : not configured
    //
    if (Const.isEmpty(keepNumerator) || Const.isEmpty(keepDenominator)) {
      return false;
    }
    
    int numerator = Const.toInt(space.environmentSubstitute(keepNumerator), 0);
    int denominator = Const.toInt(space.environmentSubstitute(keepDenominator), 0);
    
    // Incorrectly configured: not configured
    //
    if (numerator==0 || denominator==0) {
      return false;
    }
    // Looks OK
    //
    return true;
  }

  public boolean keepFile(VariableSpace space, int fileNr) {
    int numerator = Const.toInt(space.environmentSubstitute(keepNumerator), 0);
    int denominator = Const.toInt(space.environmentSubstitute(keepDenominator), 0);
    
    // Calculate the modulo on the file nr, for example if we keep 5 out of 8 files (5/8), we calculate the modulo of 8
    // If then the remainder plus one is lower or equal to 5 we keep the file, otherwise we don't keep it.
    // In this example:
    //
    // 1, 2, 3, 4, 5 : keep
    // 6, 7, 8: don't keep
    //
    return ((fileNr%denominator)+1)<=numerator;
  }
  
  public boolean isArchivedToSingleFile() {
    return compressionType==null || compressionType==CompressionType.NONE || compressionType==CompressionType.GZIP  || compressionType==CompressionType.BZIP;
  }

  
  
  
  public String getArchiveBaseName() {
    return archiveBaseName;
  }

  public void setArchiveBaseName(String archiveBaseName) {
    this.archiveBaseName = archiveBaseName;
  }

  public CompressionType getCompressionType() {
    return compressionType;
  }

  public void setCompressionType(CompressionType compressionType) {
    this.compressionType = compressionType;
  }

  public String getSourceFolder() {
    return sourceFolder;
  }

  public void setSourceFolder(String sourceFolder) {
    this.sourceFolder = sourceFolder;
  }

  public String getSourceRegex() {
    return sourceRegex;
  }

  public void setSourceRegex(String sourceRegex) {
    this.sourceRegex = sourceRegex;
  }

  public String getTargetFolder() {
    return targetFolder;
  }

  public void setTargetFolder(String targetFolder) {
    this.targetFolder = targetFolder;
  }

  public String getWaitTime() {
    return waitTime;
  }

  public void setWaitTime(String waitTime) {
    this.waitTime = waitTime;
  }

  public WaitingUnit getWaitUnit() {
    return waitUnit;
  }

  public void setWaitUnit(WaitingUnit waitUnit) {
    this.waitUnit = waitUnit;
  }

  public String getKeepNumerator() {
    return keepNumerator;
  }

  public void setKeepNumerator(String keepNumerator) {
    this.keepNumerator = keepNumerator;
  }

  public String getKeepDenominator() {
    return keepDenominator;
  }

  public void setKeepDenominator(String keepDenominator) {
    this.keepDenominator = keepDenominator;
  }
  

  public boolean isRemovingOriginal() {
    return removingOriginal;
  }

  public void setRemovingOriginal(boolean removingOriginal) {
    this.removingOriginal = removingOriginal;
  }
}
