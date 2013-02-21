package org.pentaho.di.trans.steps.archiver;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.provider.local.LocalFile;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleFileException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

public class Archiver extends BaseStep implements StepInterface {

  private static Class<?> PKG = Archiver.class;
 
  private ArchiverMeta meta;
  private ArchiverData data;
  
  private enum ArchiveType {
    DELETE("delete"),
    MOVE("move"),
    IGNORE("ignore"),
    ARCHIVE("ARCHIVE"),
    ;
    private String description;
    private ArchiveType(String description) {
      this.description = description;
    }
    public String getDescription() {
      return description;
    }
  }

  public Archiver(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
    super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
  }
  
  @Override
  public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
    
    meta = (ArchiverMeta) smi;
    data = (ArchiverData) sdi;
    
    data.outputRowMeta = new RowMeta();
    meta.getFields(data.outputRowMeta, getStepname(), null, null, this);
    
    ArchiverMeta meta = (ArchiverMeta)smi;
    
    try {
      for (ArchiverGeneration generation : meta.getGenerations()) {
        handleGeneration(generation);
      }
      setOutputDone();
      return false;
    } catch(Exception e) {
      throw new KettleException(e);
    }
    
  }

  private void handleGeneration(ArchiverGeneration generation) throws KettleException {
    try {
      // Reset for this generation...
      //
      data.fileNr = 0;
      data.files = new ArrayList<FileObject>();
      
      // Which files can we find for this generation folder?
      //
      FileObject[] files = generation.findFiles(this);
      for (FileObject file : files) {
        try {
          ArchiveType archiveType = archiveFile(generation, file);
          Object[] outputRow = RowDataUtil.allocateRowData(data.outputRowMeta.size());
          
          handleArchiving(generation, file, archiveType);
          
          int outIndex = 0;
          outputRow[outIndex++] = file.toString();
          outputRow[outIndex++] = archiveType.getDescription();
          putRow(data.outputRowMeta, outputRow);
        } catch(Exception e) {
          if (getStepMeta().isDoingErrorHandling()) {
            putError(getInputRowMeta(), RowDataUtil.allocateRowData(getInputRowMeta().size()), 1, e.getMessage(), file.toString(), "ARC-001");
          } else {
            throw new KettleException(BaseMessages.getString(PKG, "Archiver.Exception.UnableToArchiveFile", file.toString()), e);
          }
        }
      }
      
      if (!data.files.isEmpty()) {
        archiveFiles(generation, data.files);
      }
      
    } catch(Exception e) {
      throw new KettleException(BaseMessages.getString(PKG, "Archiver.Exception.UnableToHandleGenerationSourcingFrom", generation.getSourceFolder()), e);
    }
    
  }

  private void archiveFiles(ArchiverGeneration generation, List<FileObject> files) throws Exception {
    switch(generation.getCompressionType()) {
    case ZIP: zipFiles(generation, files); break;
    case TARGZIP: tarGzFiles(generation, files); break;
    case TAR: tarFiles(generation, files); break;
    default:
      break;
    }
  }
  
  private void zipFiles(ArchiverGeneration generation, List<FileObject> files) throws Exception {
    String targetFile = getTargetFile(generation);
    OutputStream os = null;
    ZipOutputStream zos = null;
    try {
      os = KettleVFS.getOutputStream(targetFile, false);
      zos = new ZipOutputStream(os);
      for (FileObject file : files) {
        ZipEntry e = new ZipEntry(file.getName().getBaseName());
        zos.putNextEntry(e);
        InputStream inputStream = null;
        try {
          inputStream = KettleVFS.getInputStream(file);
          IOUtils.copyLarge(inputStream, zos);
        } finally {
          zos.closeEntry();
          IOUtils.closeQuietly(inputStream);
        }
      }
      zos.finish();
      zos.close();
      
      // Only if all went well we remove the files...
      //
      // If compression went OK we delete the original file...
      //
      for (FileObject file : files) {
        deleteFile(generation, file);
      }
      
    } finally {
      IOUtils.closeQuietly(os);
    }
  }

  private void tarGzFiles(ArchiverGeneration generation, List<FileObject> files) throws Exception {
    String targetFile = getTargetFile(generation);
    OutputStream os = null;
    GZIPOutputStream gzos = null;
    TarArchiveOutputStream taos = null;

    try {
      os = KettleVFS.getOutputStream(targetFile, false);
      gzos = new GZIPOutputStream(os);
      taos = new TarArchiveOutputStream(gzos);
      taos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
      taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
      
      for (FileObject file : files) {
        if (!(file instanceof LocalFile)) {
          throw new KettleException(BaseMessages.getString(PKG, "Archiver.Exception.TarOnlySupportsLocalFiles", file.toString()));
        }
        
        String localFilename = KettleVFS.getFilename(file);
        
        TarArchiveEntry entry = new TarArchiveEntry(new File(localFilename), file.getName().getBaseName());
        taos.putArchiveEntry(entry);
        
        BufferedInputStream inputStream = null;
        try {
          inputStream = new BufferedInputStream(KettleVFS.getInputStream(file));
          IOUtils.copyLarge(inputStream, taos);
        } finally {
          taos.closeArchiveEntry();
          IOUtils.closeQuietly(inputStream);
        }
      }
      
      taos.close();
      
      // Only if all went OK we remove the files...
      //
      // If compression went OK we delete the original file...
      //
      for (FileObject file : files) {
        deleteFile(generation, file);
      }
      
    } finally {
      if (os!=null) os.close();
      IOUtils.closeQuietly(gzos);
    }
  }
  
  private void tarFiles(ArchiverGeneration generation, List<FileObject> files) throws Exception {
    String targetFile = getTargetFile(generation);
    OutputStream os = null;
    TarArchiveOutputStream taos = null;

    try {
      os = KettleVFS.getOutputStream(targetFile, false);
      taos = new TarArchiveOutputStream(os);
      taos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
      taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
      
      for (FileObject file : files) {
        
        if (!(file instanceof LocalFile)) {
          throw new KettleException(BaseMessages.getString(PKG, "Archiver.Exception.TarOnlySupportsLocalFiles", file.toString()));
        }
        
        String localFilename = KettleVFS.getFilename(file);
        
        TarArchiveEntry entry = new TarArchiveEntry(new File(localFilename), file.getName().getBaseName());
        taos.putArchiveEntry(entry);
        
        BufferedInputStream inputStream = null;
        try {
          inputStream = new BufferedInputStream(KettleVFS.getInputStream(file));
          IOUtils.copyLarge(inputStream, taos);
        } finally {
          taos.closeArchiveEntry();
          IOUtils.closeQuietly(inputStream);
        }
      }
      
      taos.close();
      
      // Only if all went OK we remove the files...
      //
      // If compression went OK we delete the original file...
      //
      for (FileObject file : files) {
        deleteFile(generation, file);
      }
      
    } finally {
      if (os!=null) os.close();
    }
    
  }



  private void handleArchiving(ArchiverGeneration generation, FileObject file, ArchiveType archiveType) throws Exception {
    
    String targetFolder = environmentSubstitute(generation.getTargetFolder());
    
    switch(archiveType) {
    case MOVE:
      moveFile(generation, file, targetFolder);
      break;
    
    case DELETE:
        deleteFile(generation, file);
      break;
      
    case ARCHIVE:
      // Just add to the files list for archiving all at once.
      //
      data.files.add(file);
      break;
    case IGNORE:
      // Don't do anything...
      break;
      
    default:
      throw new KettleException(BaseMessages.getString(PKG, "Archiver.Exception.UnhandledArchivingType", archiveType.name()));
    }
  }

  private void deleteFile(ArchiverGeneration generation, FileObject file) throws Exception {
    // Some safety please...
    //
    if (!generation.isRemovingOriginal()) return;
    
    // This file needs to be removed...
    //
    try {
      if (!file.delete()) {
        throw new KettleException(BaseMessages.getString(PKG, "Archiver.Exception.FileCouldNotBeDeleted", file.toString()));
      }
    } catch(Exception e) {
      throw new KettleException(BaseMessages.getString(PKG, "Archiver.Exception.FileCouldNotBeDeleted", file.toString()), e);
    }
  }

  private void moveFile(ArchiverGeneration generation, FileObject file, String targetFolder) throws Exception {
    
    if (generation.getCompressionType() != CompressionType.NONE) {
      compressFile(generation, file, targetFolder);
    } else  {
      // Regular move: move the file to the target folder...
      //
      FileObject targetFileObject = KettleVFS.getFileObject(targetFolder+File.separator+file.getName().getBaseName());
      try {
        file.moveTo(targetFileObject);
      } catch(Exception e) {
        throw new KettleException(BaseMessages.getString(PKG, "Archiver.Exception.FileCouldNotBeMoved", file.toString()), e);
      }
    }
  }

  private void compressFile(ArchiverGeneration generation, FileObject file, String targetFolder) throws Exception {
    CompressionType compressionType = generation.getCompressionType();
    FileObject targetFileObject = KettleVFS.getFileObject(targetFolder+File.separator+file.getName().getBaseName()+compressionType.getExtension());

    switch(compressionType) {
    case GZIP:
      {
        // GZip : move the file to the target folder but compress it first.
        //
        GZIPOutputStream gzos=null; 
        InputStream is = null; 
        try {
          gzos = new GZIPOutputStream(KettleVFS.getOutputStream(targetFileObject, false));
          is = KettleVFS.getInputStream(file);
          IOUtils.copyLarge(is, gzos);
        } finally {
          IOUtils.closeQuietly(is);
          IOUtils.closeQuietly(gzos);
        }
      }
      break;
    case BZIP:
      {
        // BZip2 : move the file to the target folder but compress it first.
        //
        BZip2CompressorOutputStream bz2os = null; 
        InputStream is = null; 
        try {
          bz2os = new BZip2CompressorOutputStream(KettleVFS.getOutputStream(targetFileObject, false));
          is = KettleVFS.getInputStream(file);
          IOUtils.copyLarge(is, bz2os);
        } finally {
          IOUtils.closeQuietly(is);
          IOUtils.closeQuietly(bz2os);
        }
      }
      break;
    default:
      break;
    }
    
    // If compression went OK we delete the original file...
    //
    deleteFile(generation, file);
  }

  private ArchiveType archiveFile(ArchiverGeneration generation, FileObject file) throws Exception {
    // Let's calculate the age of the file, see if we need to move it or delete it...
    //
    long lastModifiedTime = file.getContent().getLastModifiedTime();
    long now = getTrans().getCurrentDate().getTime();
    long actualAge = now - lastModifiedTime;
    long maxWaitAge = generation.getMaxWaitTime();
    
    if (actualAge>maxWaitAge) {
      // We need to do something with this file...
      //
      String targetFolder = environmentSubstitute(generation.getTargetFolder());
      if (Const.isEmpty(targetFolder)) {
        return ArchiveType.DELETE;
      } else {
        data.fileNr++;
        boolean archiveFile;
        
        if (generation.isRatioConfigured(this)) {
          archiveFile = generation.keepFile(this, data.fileNr);          
        } else {
          archiveFile = true;
        }
        
        if (archiveFile) {
          if (generation.isArchivedToSingleFile()) {
            return ArchiveType.MOVE;
          } else {
            return ArchiveType.ARCHIVE;
          }
        } else {
          return ArchiveType.DELETE;
        }
      }
    } else {
      return ArchiveType.IGNORE;
    }

  }
  
  
  private String getTargetFile(ArchiverGeneration generation) throws KettleFileException {
    String targetFolder = environmentSubstitute(generation.getTargetFolder());
    FileObject sourceFolder = KettleVFS.getFileObject(environmentSubstitute(generation.getSourceFolder()));
    CompressionType compressionType = generation.getCompressionType();
    String archiveBaseName;
    if (Const.isEmpty(generation.getArchiveBaseName())) {
      archiveBaseName = sourceFolder.getName().getBaseName();
    } else {
      archiveBaseName = environmentSubstitute(generation.getArchiveBaseName());
    }
    Date fileDate = getTrans().getCurrentDate();
    String dateString = new SimpleDateFormat("_yyyyMMdd_HHmmss").format(fileDate);
    
    String targetFile = targetFolder+File.separator+archiveBaseName+dateString+compressionType.getExtension();
    return targetFile;
  }


}
