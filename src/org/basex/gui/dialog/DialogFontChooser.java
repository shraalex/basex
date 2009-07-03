package org.basex.gui.dialog;

import static org.basex.Text.*;
import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import org.basex.gui.GUI;
import org.basex.gui.GUIConstants;
import org.basex.gui.GUIProp;
import org.basex.gui.layout.BaseXBack;
import org.basex.gui.layout.BaseXCombo;
import org.basex.gui.layout.BaseXLabel;
import org.basex.gui.layout.BaseXListChooser;
import org.basex.gui.layout.TableLayout;

/**
 * Dialog window for changing the used fonts.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-09, ISC License
 * @author Christian Gruen
 */
public final class DialogFontChooser extends Dialog {
  /** Font name chooser. */
  private BaseXListChooser font;
  /** Font name chooser. */
  private BaseXListChooser font2;
  /** Font type chooser. */
  private BaseXListChooser type;
  /** Font size chooser. */
  private BaseXListChooser size;
  /** Anti-Aliasing mode. */
  private BaseXCombo aalias;

  /**
   * Default Constructor.
   * @param main reference to the main window
   */
  public DialogFontChooser(final GUI main) {
    super(main, FONTTITLE, false);

    final String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().
      getAvailableFontFamilyNames();

    BaseXBack p = new BaseXBack();
    p.setLayout(new TableLayout(2, 4, 6, 6));

    font = new BaseXListChooser(this, fonts, HELPFONT);
    font.setSize(150, 112);
    p.add(font);
    font2 = new BaseXListChooser(this, fonts, HELPFONT);
    font2.setSize(150, 112);
    p.add(font2);
    type = new BaseXListChooser(this, FONTTYPES, HELPFONT);
    type.setSize(80, 112);
    p.add(type);
    size = new BaseXListChooser(this, FTSZ, HELPFONT);
    size.setSize(50, 112);
    p.add(size);

    font.setValue(GUIProp.font);
    font2.setValue(GUIProp.monofont);
    type.setValue(FONTTYPES[GUIProp.fonttype]);
    size.setValue(Integer.toString(GUIProp.fontsize));

    final BaseXBack pp = new BaseXBack();
    pp.setLayout(new TableLayout(1, 2, 5, 5));
    pp.add(new BaseXLabel(FAALIAS));

    final String[] combo = fullAlias() ? GUIConstants.FONTALIAS :
      new String[] { GUIConstants.FONTALIAS[0], GUIConstants.FONTALIAS[1] };
    aalias = new BaseXCombo(combo, HELPFALIAS, this);
    aalias.setSelectedIndex(GUIProp.fontalias);

    pp.add(aalias);
    p.add(pp);
    set(p, BorderLayout.CENTER);

    finish(GUIProp.fontsloc);
    font.focus();
  }

  /**
   * Checks if the Java version supports all anti-aliasing variants.
   * @return result of check
   */
  public static boolean fullAlias() {
    // Check out Java 1.6 rendering; if not available, use default rendering
    try {
      RenderingHints.class.getField("VALUE_TEXT_ANTIALIAS_GASP").get(null);
      return true;
    } catch(final Exception e) {
      return false;
    }
  }


  @Override
  public void action(final String cmd) {
    GUIProp.font = font.getValue();
    GUIProp.monofont = font2.getValue();
    GUIProp.fonttype = type.getIndex();
    GUIProp.fontsize = size.getNum();
    GUIProp.fontalias = aalias.getSelectedIndex();
    GUIConstants.initFonts();
    gui.notify.layout();
  }
}
