package org.basex.gui.view.table;

import static org.basex.Text.*;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.swing.SwingUtilities;
import org.basex.core.Context;
import org.basex.core.proc.XQuery;
import org.basex.data.Data;
import org.basex.data.Nodes;
import org.basex.gui.GUIProp;
import org.basex.gui.GUIConstants;
import org.basex.gui.layout.BaseXBar;
import org.basex.gui.layout.BaseXPopup;
import org.basex.gui.view.View;
import org.basex.gui.view.ViewData;
import org.basex.gui.view.ViewNotifier;
import org.basex.util.IntList;
import org.basex.util.Performance;
import org.basex.util.Token;

/**
 * This view creates a flat table view on the database contents.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-09, ISC License
 * @author Christian Gruen
 */
public final class TableView extends View implements Runnable {
  /** Zoom table. */
  static final double[] ZOOM = {
    1, .99, .98, .97, 1, 1.03, 1.05, .9, .8, .6, .35, .18, .13, .09, .05, .03
  };
  /** Current zoom step. */
  private int zoomstep;
  /** Table data. */
  final TableData tdata;

  /** Table header. */
  private TableHeader header;
  /** Table content area. */
  private TableContent content;
  /** Table scrollbar. */
  private BaseXBar scroll;

  /**
   * Default constructor.
   * @param man view manager
   */
  public TableView(final ViewNotifier man) {
    super(man, HELPTABLE);
    tdata = new TableData(gui.context);
    setLayout(new BorderLayout());
    header = new TableHeader(this);
    add(header, BorderLayout.NORTH);
    scroll = new BaseXBar(this);
    content = new TableContent(tdata, scroll);
    add(content, BorderLayout.CENTER);
    new BaseXPopup(this, GUIConstants.POPUP);
  }

  @Override
  public void refreshInit() {
    tdata.rootRows = null;
    tdata.rows = null;

    final Data data = gui.context.data();
    if(!GUIProp.showtable || data == null) return;
    tdata.init(data);
    refreshContext(true, false);
  }

  @Override
  public void refreshContext(final boolean more, final boolean quick) {
    if(!GUIProp.showtable || tdata.cols.length == 0) return;

    tdata.context(false);
    scroll.pos(0);
    if(tdata.rows == null) return;

    if(quick) {
      scroll.height(tdata.rows.size * tdata.rowH(1));
      findFocus();
      repaint();
    } else {
      if(!more) tdata.resetFilter();
      gui.updating = true;
      new Thread(this).start();
    }
  }

  @Override
  public void refreshFocus() {
    if(!GUIProp.showtable || tdata.rows == null) return;
    repaint();
  }

  @Override
  public void refreshMark() {
    if(!GUIProp.showtable || tdata.rows == null) return;

    final Context context = gui.context;
    final Nodes marked = context.marked();
    if(marked.size() != 0) {
      final int p = tdata.getRoot(context.data(), marked.nodes[0]);
      if(p != -1) setPos(p);
    }
    repaint();
  }

  @Override
  public void refreshLayout() {
    if(!GUIProp.showtable || tdata.rows == null) return;

    scroll.height(tdata.rows.size * tdata.rowH(1));
    refreshContext(false, true);
  }

  @Override
  public void refreshUpdate() {
    refreshContext(false, true);
  }

  @Override
  public void paintComponent(final Graphics g) {
    super.paintComponent(g);
    if(tdata.rows == null && GUIProp.showtable) refreshInit();
  }

  /**
   * Starts a context switch animation.
   */
  public void run() {
    zoomstep = ZOOM.length;
    while(--zoomstep >= 0) {
      scroll.height(tdata.rows.size * tdata.rowH(ZOOM[zoomstep]));
      repaint();
      Performance.sleep(25);
    }
    gui.updating = false;
    findFocus();
  }

  /**
   * Sets scrollbar position for the specified pre value.
   * @param pre pre value
   */
  private void setPos(final int pre) {
    final int off = getOff(pre);
    if(off == -1) return;
    final int h = getHeight() - header.getHeight() - 2 * tdata.rowH;
    final int y = (off - 1) * tdata.rowH;
    final int s = scroll.pos();
    if(y < s || y > s + h) scroll.pos(y);
  }

  /**
   * Returns list offset for specified pre value.
   * @param pre pre value
   * @return offset
   */
  private int getOff(final int pre) {
    for(int n = 0; n < tdata.rows.size; n++) {
      if(tdata.rows.list[n] == pre) return n;
    }
    return -1;
  }

  @Override
  public void mouseMoved(final MouseEvent e) {
    super.mouseMoved(e);
    if(!GUIProp.showtable || tdata.rows == null) return;

    tdata.mouseX = e.getX();
    tdata.mouseY = e.getY();
    findFocus();
  }

  /**
   * Finds the current focus.
   */
  private void findFocus() {
    final int y = tdata.mouseY - header.getHeight() + scroll.pos();
    final int l = y / tdata.rowH;
    final boolean valid = y >= 0 && l < tdata.rows.size;

    if(valid) {
      final int pre = tdata.rows.list[l];
      final Context context = gui.context;
      final TableIterator it = new TableIterator(context.data(), tdata);
      final int c = tdata.column(getWidth() - BaseXBar.SIZE, tdata.mouseX);
      it.init(pre);
      while(it.more()) {
        if(it.col == c) {
          gui.notify.focus(it.pre, this);
          content.repaint();
          break;
        }
      }
    }
    final String str = content.focusedString;
    final Data data = gui.context.data();
    gui.cursor(valid && (str != null &&
      str.length() <= Token.MAXLEN || data.fs != null && tdata.mouseX < 20) ?
      GUIConstants.CURSORHAND : GUIConstants.CURSORARROW);
  }

  @Override
  public void mouseExited(final MouseEvent e) {
    gui.cursor(GUIConstants.CURSORARROW);
    gui.notify.focus(-1, null);
  }

  @Override
  public void mousePressed(final MouseEvent e) {
    super.mousePressed(e);
    final Context context = gui.context;
    final Data data = context.data();
    if(tdata.rows == null || data.fs != null && tdata.mouseX < 20) return;

    if(e.getY() < header.getHeight()) return;

    final int pre = gui.focused;
    if(SwingUtilities.isLeftMouseButton(e)) {
      if(e.getClickCount() == 1) {
        final String str = content.focusedString;
        if(str == null || str.length() > Token.MAXLEN) return;
        if(!e.isShiftDown()) tdata.resetFilter();
        final int c = tdata.column(getWidth() - BaseXBar.SIZE, e.getX());
        tdata.cols[c].filter = str;
        query();
        //repaint();
      } else {
        Nodes nodes = context.marked();
        if(getCursor() == GUIConstants.CURSORARROW) {
          nodes = new Nodes(tdata.getRoot(nodes.data, pre), nodes.data);
        }
        gui.notify.context(nodes, false, null);
      }
    } else {
      if(pre != -1) {
        final TableIterator it = new TableIterator(context.data(), tdata);
        final int c = tdata.column(getWidth() - BaseXBar.SIZE, e.getX());
        it.init(pre);
        while(it.more()) {
          if(it.col == c) {
            gui.notify.mark(new Nodes(it.pre, context.data()), null);
            return;
          }
        }
      }
    }
  }

  /**
   * Performs a table query.
   */
  void query() {
    final String query = tdata.find();
    if(query != null) gui.execute(new XQuery(query));
  }

  @Override
  public void mouseClicked(final MouseEvent e) {
    final Data data = gui.context.data();
    if(data.fs != null && tdata.mouseX < 20) {
      data.fs.launch(ViewData.parent(data, gui.focused));
    }
  }

  @Override
  public void mouseWheelMoved(final MouseWheelEvent e) {
    if(tdata.rows == null) return;

    scroll.pos(scroll.pos() + e.getUnitsToScroll() * tdata.rowH);
    mouseMoved(e);
    repaint();
  }

  @Override
  public void keyPressed(final KeyEvent e) {
    super.keyPressed(e);
    if(tdata.rows == null) return;

    final int key = e.getKeyCode();

    final int lines = (getHeight() - header.getHeight()) / tdata.rowH;
    final int oldPre = tdata.getRoot(gui.context.data(), gui.focused);
    int pre = oldPre;

    final IntList rows = tdata.rows;
    if(key == KeyEvent.VK_HOME) {
      pre = rows.list[0];
    } else if(key == KeyEvent.VK_END) {
      pre = rows.list[rows.size - 1];
    } else if(key == KeyEvent.VK_UP) {
      pre = rows.list[Math.max(0, getOff(pre) - 1)];
    } else if(key == KeyEvent.VK_DOWN) {
      pre = rows.list[Math.min(rows.size - 1, getOff(pre) + 1)];
    } else if(key == KeyEvent.VK_PAGE_UP) {
      pre = rows.list[Math.max(0, getOff(pre) - lines)];
    } else if(key == KeyEvent.VK_PAGE_DOWN) {
      pre = rows.list[Math.min(rows.size - 1, getOff(pre) + lines)];
    }

    if(pre != oldPre) {
      setPos(pre);
      gui.notify.focus(pre, null);
    }
  }
}
