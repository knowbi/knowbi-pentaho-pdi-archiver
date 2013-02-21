package org.pentaho.di.ui.trans.steps.archiver;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.Const;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.steps.archiver.ArchiverGeneration;
import org.pentaho.di.trans.steps.archiver.ArchiverMeta;
import org.pentaho.di.trans.steps.archiver.CompressionType;
import org.pentaho.di.trans.steps.archiver.WaitingUnit;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

public class ArchiverDialog extends BaseStepDialog implements StepDialogInterface {

  private static Class<?> PKG = ArchiverDialog.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

  private Label        wlStepname;
  private Text         wStepname;
  private FormData     fdlStepname, fdStepname;
  
  private Label        wlFields;
  private TableView    wFields;
  private FormData     fdlFields, fdFields;

  private ArchiverMeta meta;
  private boolean changed;
  
  public ArchiverDialog(Shell parent, Object baseStepMeta, TransMeta transMeta, String stepname) {
    super(parent, (BaseStepMeta)baseStepMeta, transMeta, stepname);
    
    meta = (ArchiverMeta)baseStepMeta;
    changed = meta.hasChanged();
  }
  
  @Override
  public String open() {
    Shell parent = getParent();
    Display display = parent.getDisplay();

    shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX);
    props.setLook(shell);
    setShellImage(shell, meta);

    ModifyListener lsMod = new ModifyListener() 
    {
        public void modifyText(ModifyEvent e) 
        {
            changed = true;
        }
    };

    FormLayout formLayout = new FormLayout ();
    formLayout.marginWidth  = Const.FORM_MARGIN;
    formLayout.marginHeight = Const.FORM_MARGIN;

    shell.setLayout(formLayout);
    shell.setText(BaseMessages.getString(PKG, "ArchiverDialog.DialogTitle"));
    
    int middle = props.getMiddlePct();
    int margin = Const.MARGIN;

    // Stepname line
    wlStepname=new Label(shell, SWT.RIGHT);
    wlStepname.setText(BaseMessages.getString(PKG, "System.Label.StepName"));
    props.setLook(wlStepname);
    fdlStepname=new FormData();
    fdlStepname.left = new FormAttachment(0, 0);
    fdlStepname.right= new FormAttachment(middle, -margin);
    fdlStepname.top  = new FormAttachment(0, margin);
    wlStepname.setLayoutData(fdlStepname);
    wStepname=new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    wStepname.setText(stepname);
    props.setLook(wStepname);
    wStepname.addModifyListener(lsMod);
    fdStepname=new FormData();
    fdStepname.left = new FormAttachment(middle, 0);
    fdStepname.top  = new FormAttachment(0, margin);
    fdStepname.right= new FormAttachment(100, 0);
    wStepname.setLayoutData(fdStepname);
    
    wlFields=new Label(shell, SWT.NONE);
    wlFields.setText(BaseMessages.getString(PKG, "ArchiverDialog.Generations.Label"));
    props.setLook(wlFields);
    fdlFields=new FormData();
    fdlFields.left = new FormAttachment(0, 0);
    fdlFields.top  = new FormAttachment(wStepname, margin);
    wlFields.setLayoutData(fdlFields);
    
    int nrGenerations=meta.getGenerations().size();
    if (nrGenerations==0) {
      nrGenerations=1;
    }
    
    ColumnInfo[] colinf=new ColumnInfo[] {
        new ColumnInfo(BaseMessages.getString(PKG, "ArchiverDialog.SourceFolder.Column"), ColumnInfo.COLUMN_TYPE_TEXT, false),
        new ColumnInfo(BaseMessages.getString(PKG, "ArchiverDialog.SourceRegex.Column"), ColumnInfo.COLUMN_TYPE_TEXT, false),
        new ColumnInfo(BaseMessages.getString(PKG, "ArchiverDialog.TargetFolder.Column"), ColumnInfo.COLUMN_TYPE_TEXT, false),
        new ColumnInfo(BaseMessages.getString(PKG, "ArchiverDialog.WaitTime.Column"), ColumnInfo.COLUMN_TYPE_TEXT, false),
        new ColumnInfo(BaseMessages.getString(PKG, "ArchiverDialog.WaitUnit.Column"), ColumnInfo.COLUMN_TYPE_CCOMBO, WaitingUnit.getDescriptions(), false),
        new ColumnInfo(BaseMessages.getString(PKG, "ArchiverDialog.KeepNumerator.Column"), ColumnInfo.COLUMN_TYPE_TEXT, false),
        new ColumnInfo(BaseMessages.getString(PKG, "ArchiverDialog.KeepDenominator.Column"), ColumnInfo.COLUMN_TYPE_TEXT, false),
        new ColumnInfo(BaseMessages.getString(PKG, "ArchiverDialog.CompressionType.Column"), ColumnInfo.COLUMN_TYPE_CCOMBO, CompressionType.getDescriptions(), false),
        new ColumnInfo(BaseMessages.getString(PKG, "ArchiverDialog.ArchiveBase.Column"), ColumnInfo.COLUMN_TYPE_TEXT, false),
        new ColumnInfo(BaseMessages.getString(PKG, "ArchiverDialog.RemoveOriginal.Column"), ColumnInfo.COLUMN_TYPE_CCOMBO, new String[] { "Y", "N" }, false),
      };
    colinf[0].setUsingVariables(true);
    colinf[1].setUsingVariables(true);
    colinf[2].setUsingVariables(true);
    colinf[3].setUsingVariables(true);
    colinf[5].setUsingVariables(true);
    colinf[6].setUsingVariables(true);
    colinf[8].setUsingVariables(true);

    wFields=new TableView(transMeta, shell, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI, colinf, nrGenerations, lsMod, props );

    fdFields=new FormData();
    fdFields.left  = new FormAttachment(0, 0);
    fdFields.top   = new FormAttachment(wlFields, margin);
    fdFields.right = new FormAttachment(100, 0);
    fdFields.bottom= new FormAttachment(100, -50);
    wFields.setLayoutData(fdFields);
    
    // Some buttons
    wOK=new Button(shell, SWT.PUSH);
    wOK.setText(BaseMessages.getString(PKG, "System.Button.OK"));
    wCancel=new Button(shell, SWT.PUSH);
    wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel"));

    setButtonPositions(new Button[] { wOK, wCancel }, margin, null);

    // Add listeners
    lsCancel   = new Listener() { public void handleEvent(Event e) { cancel(); } };
    lsOK       = new Listener() { public void handleEvent(Event e) { ok();     } };
    
    wCancel.addListener(SWT.Selection, lsCancel);
    wOK.addListener    (SWT.Selection, lsOK    );
    
    lsDef=new SelectionAdapter() { public void widgetDefaultSelected(SelectionEvent e) { ok(); } };
    
    wStepname.addSelectionListener( lsDef );
    
    // Detect X or ALT-F4 or something that kills this window...
    //
    shell.addShellListener( new ShellAdapter() { public void shellClosed(ShellEvent e) { cancel(); } } );

    // Set the shell size, based upon previous time...
    //
    setSize();
    
    getData();
    meta.setChanged(changed);

    shell.open();
    while (!shell.isDisposed())
    {
            if (!display.readAndDispatch()) display.sleep();
    }
    return stepname;
  }


  /**
   * Copy information from the meta-data currentMeta to the dialog fields.
   */ 
  public void getData() {
    wStepname.selectAll();
    
    for (int i=0;i<meta.getGenerations().size();i++) {
      ArchiverGeneration generation = meta.getGenerations().get(i);
      TableItem item = wFields.table.getItem(i);
      int index=1;
      item.setText( index++, Const.NVL(generation.getSourceFolder(), ""));
      item.setText( index++, Const.NVL(generation.getSourceRegex(), ""));
      item.setText( index++, Const.NVL(generation.getTargetFolder(), ""));
      item.setText( index++, Const.NVL(generation.getWaitTime(), ""));
      item.setText( index++, Const.NVL(generation.getWaitUnit().getDescription(), ""));
      item.setText( index++, Const.NVL(generation.getKeepNumerator(), ""));
      item.setText( index++, Const.NVL(generation.getKeepDenominator(), ""));
      item.setText( index++, generation.getCompressionType().getDescription());
      item.setText( index++, Const.NVL(generation.getArchiveBaseName(), ""));
      item.setText( index++, generation.isRemovingOriginal() ? "Y" : "N" );
    }
    
    wFields.setRowNums();
    wFields.optWidth(true);
  }

  private void cancel() {
      stepname=null;
      dispose();
  }

  private void ok() {
    if (Const.isEmpty(wStepname.getText())) return;

    stepname = wStepname.getText(); // return value
    
    int nrNonEmptyFields = wFields.nrNonEmpty(); 
    meta.clear();
    
    for (int i=0;i<nrNonEmptyFields;i++) {
      TableItem item = wFields.getNonEmpty(i);
      int index=1;
      String sourceFolder = item.getText(index++);
      String sourceRegex = item.getText(index++);
      String targetFolder = item.getText(index++);
      String waitTime = item.getText(index++);
      String waitUnit = item.getText(index++);
      String keepNumerator = item.getText(index++);
      String keepDenominator = item.getText(index++);
      String compressionType = item.getText(index++);
      String archiveBaseName = item.getText(index++);
      boolean removeOriginal = "Y".equalsIgnoreCase(item.getText(index++));
      
      ArchiverGeneration generation = new ArchiverGeneration(sourceFolder, sourceRegex, targetFolder, 
          waitTime, WaitingUnit.getWaitingUnitForDescription(waitUnit),
          keepNumerator, keepDenominator,
          CompressionType.getCompressionTypeForDescription(compressionType),
          archiveBaseName, removeOriginal
         );
      meta.getGenerations().add(generation);
    }
        
    dispose();
  }
}
