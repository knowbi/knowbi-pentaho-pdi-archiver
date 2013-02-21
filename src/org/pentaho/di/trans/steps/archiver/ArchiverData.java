package org.pentaho.di.trans.steps.archiver;

import java.util.List;

import org.apache.commons.vfs.FileObject;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

public class ArchiverData extends BaseStepData implements StepDataInterface {
  
  public RowMetaInterface outputRowMeta;
  public int fileNr;
  public List<FileObject> files;

  public ArchiverData() {
    super();
  }
}
