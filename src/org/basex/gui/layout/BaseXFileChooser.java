package org.basex.gui.layout;

import static org.basex.Text.*;
import java.awt.FileDialog;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import org.basex.BaseX;
import org.basex.gui.GUI;
import org.basex.gui.GUIProp;
import org.basex.gui.dialog.Dialog;
import org.basex.io.IO;

/**
 * Project specific File Chooser implementation.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-09, ISC License
 * @author Christian Gruen
 */
public final class BaseXFileChooser {
  /** File Dialog Mode. */
  public enum Mode {
    /** Open file. */ FOPEN,
    /** Open file or directory.  */ FDOPEN,
    /** Open directory.  */ DOPEN,
    /** Save file. */ FSAVE,
    /** Save file or directory. */ DSAVE,
  }

  /** Reference to main window. */
  private GUI gui;
  /** Swing file chooser. */
  private JFileChooser fc;
  /** Simple file dialog. */
  private FileDialog fd;

  /**
   * Default Constructor.
   * @param title dialog title
   * @param path initial path
   * @param main reference to main window
   */
  public BaseXFileChooser(final String title, final String path,
      final GUI main) {

    if(GUIProp.simplefd) {
      fd = new FileDialog(main, title);
      fd.setDirectory(new File(path).getPath());
    } else {
      fc = new JFileChooser(path);
      final File file = new File(path);
      if(!file.isDirectory()) fc.setSelectedFile(file);
      fc.setDialogTitle(title);
      gui = main;
    }
  }

  /**
   * Sets a file filter.
   * @param dsc description
   * @param suf suffixes
   */
  public void addFilter(final String dsc, final String... suf) {
    if(fc != null) fc.addChoosableFileFilter(new Filter(suf, dsc));
    else for(final String s : suf) fd.setFile("*" + s);
  }

  /**
   * Selects a file or directory.
   * @param mode type defined by {@link Mode}
   * @return resulting input reference
   */
  public IO select(final Mode mode) {
    if(fd != null) {
      if(mode == Mode.FDOPEN) fd.setFile(" ");
      fd.setMode(mode == Mode.FSAVE || mode == Mode.DSAVE ?
          FileDialog.SAVE : FileDialog.LOAD);
      fd.setVisible(true);
      if(fd.getFile() == null) return null;

      final String dir = fd.getDirectory();
      return IO.get(mode == Mode.DOPEN || mode == Mode.DSAVE ? dir :
        dir + "/" + fd.getFile());
    }

    int state = 0;
    switch(mode) {
      case FOPEN:
        state = fc.showOpenDialog(gui);
        break;
      case FDOPEN:
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        state = fc.showOpenDialog(gui);
        break;
      case FSAVE:
        state = fc.showSaveDialog(gui);
        break;
      case DOPEN:
      case DSAVE:
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        state = fc.showDialog(gui, null);
        break;
    }
    if(state != JFileChooser.APPROVE_OPTION) return null;

    final IO io = IO.get(fc.getSelectedFile().getPath());
    return mode != Mode.FSAVE || !io.exists() ||
      Dialog.confirm(gui, BaseX.info(FILEREPLACE, io.name())) ? io : null;
  }

  /**
   * Defines a file filter for XML documents.
   */
  static class Filter extends FileFilter {
    /** Suffix. */
    private String[] suf;
    /** Description. */
    private String desc;

    /**
     * Constructor.
     * @param s suffix
     * @param d description
     */
    Filter(final String[] s, final String d) {
      suf = s;
      desc = d;
    }

    @Override
    public boolean accept(final File file) {
      if(file.isDirectory()) return true;
      final String name = file.getName().toLowerCase();
      for(final String s : suf) if(name.endsWith(s)) return true;
      return false;
    }
    @Override
    public String getDescription() {
      return desc;
    }
  }
}
