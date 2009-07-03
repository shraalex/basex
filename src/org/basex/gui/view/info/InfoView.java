package org.basex.gui.view.info;

import static org.basex.Text.*;
import static org.basex.gui.GUIConstants.*;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import org.basex.core.Prop;
import org.basex.gui.GUIConstants;
import org.basex.gui.GUIProp;
import org.basex.gui.GUIConstants.Fill;
import org.basex.gui.layout.BaseXBack;
import org.basex.gui.layout.BaseXLabel;
import org.basex.gui.layout.BaseXLayout;
import org.basex.gui.layout.BaseXText;
import org.basex.gui.view.View;
import org.basex.gui.view.ViewNotifier;
import org.basex.util.IntList;
import org.basex.util.Performance;
import org.basex.util.StringList;
import org.basex.util.Token;
import org.basex.util.TokenBuilder;

/**
 * This view displays query information.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-09, ISC License
 * @author Christian Gruen
 */
public final class InfoView extends View {
  /** Header label. */
  private final BaseXLabel header;
  /** Timer label. */
  private final BaseXLabel timer;
  /** Timer label. */
  private final BaseXBack north;
  /** Text Area. */
  private final BaseXText area;

  /** Focused bar. */
  private int focus = -1;
  /** Query statistics. */
  private IntList stat = new IntList();
  /** Query statistics strings. */
  private StringList compile;
  /** Query statistics strings. */
  private StringList strings;
  /** Query statistics strings. */
  private StringList evaluate;
  /** Query plan. */
  private StringList plan;
  /** Query. */
  private String query = "";
  /** Compiled Query. */
  private String result = "";
  /** Panel Width. */
  private int w;
  /** Panel Height. */
  private int h;
  /** Bar widths. */
  private int bw;
  /** Bar size. */
  private int bs;

  /**
   * Default constructor.
   * @param man view manager
   * @param help help text
   */
  public InfoView(final ViewNotifier man, final byte[] help) {
    super(man, help);
    setMode(Fill.UP);
    setBorder(8, 8, 8, 8);
    setLayout(new BorderLayout());

    north = new BaseXBack(Fill.NONE);
    north.setLayout(new BorderLayout());
    header = new BaseXLabel(INFOTIT);
    north.add(header, BorderLayout.NORTH);
    north.add(header, BorderLayout.NORTH);
    timer = new BaseXLabel(" ", true, false);
    north.add(timer, BorderLayout.SOUTH);
    add(north, BorderLayout.NORTH);

    area = new BaseXText(gui, help, false);
    add(area, BorderLayout.CENTER);
    refreshLayout();
  }

  @Override
  public void refreshInit() { }

  @Override
  public void refreshFocus() { }

  @Override
  public void refreshMark() { }

  @Override
  public void refreshContext(final boolean more, final boolean quick) { }

  @Override
  public void refreshUpdate() { }

  @Override
  public void refreshLayout() {
    header.setFont(GUIConstants.lfont);
    timer.setFont(GUIConstants.font);
    area.setFont(GUIConstants.font);
  }

  /**
   * Processes the query info.
   * @param inf info string
   * @param ok success flag
   */
  public void setInfo(final byte[] inf, final boolean ok) {
    final IntList il = new IntList();
    final StringList sl = new StringList();
    final StringList cmp = new StringList();
    final StringList eval = new StringList();
    final StringList pln = new StringList();
    String err = "";

    final String[] split = Token.string(inf).split(Prop.NL);
    for(int i = 0; i < split.length; i++) {
      final String line = split[i];
      final int s = line.indexOf(':');
      if(line.startsWith(QUERYPARSE) || line.startsWith(QUERYCOMPILE) ||
          line.startsWith(QUERYEVALUATE) || line.startsWith(QUERYPRINT) ||
          line.startsWith(QUERYTOTAL)) {
        final int t = line.indexOf(" ms");
        sl.add(line.substring(0, s).trim());
        il.add((int) (Double.parseDouble(line.substring(s + 1, t)) * 100));
      } else if(line.startsWith(QUERYSTRING)) {
        query = line.substring(s + 1).trim();
      } else if(line.startsWith(QUERYPLAN)) {
        while(split[++i].length() != 0) pln.add(split[i]);
        --i;
      } else if(line.startsWith(QUERYCOMP)) {
        while(!split[++i].contains(QUERYRESULT)) cmp.add(split[i]);
        result = split[i].substring(split[i].indexOf(':') + 1).trim();
      } else if(line.startsWith(QUERYEVAL)) {
        while(split[++i].startsWith(QUERYSEP)) eval.add(split[i]);
        --i;
      } else if(!ok) {
        err += line;
      }
    }

    final TokenBuilder tb = new TokenBuilder();
    String tm = INFONO;

    stat = il;
    strings = sl;
    compile = cmp;
    evaluate = eval;
    plan = pln;

    if(sl.size != 0) {
      add(tb, QUERYQU, query);
      add(tb, QUERYCOMP, compile);
      if(compile.size != 0) add(tb, QUERYRESULT, result);
      add(tb, QUERYPLAN, plan);
      add(tb, QUERYEVAL, evaluate);
      add(tb, QUERYTIME, strings);
      tm = strings.list[il.size - 1] + ": " + Performance.getTimer(
          stat.list[il.size - 1] * 10000L * Prop.runs, Prop.runs);
    } else if(!ok) {
      add(tb, "Error:  ", err);
      tm = "";
    }

    area.setText(tb.finish());
    timer.setText(tm);
    repaint();
  }

  /**
   * Adds the specified strings..
   * @param tb token builder
   * @param head string header
   * @param list list reference
   */
  private void add(final TokenBuilder tb, final String head,
      final StringList list) {

    if(list.size == 0) return;
    tb.add((byte) 0x02);
    tb.add(head);
    tb.add((byte) 0x03);
    tb.add((byte) 0x0A);
    for(int i = 0; i < list.size; i++) {
      String line = list.list[i];
      if(list == strings) line = " " + QUERYSEP + line + ":  " +
        Performance.getTimer(stat.list[i] * 10000L * Prop.runs, Prop.runs);
      tb.add(line);
      tb.add((byte) 0x0A);
    }
    tb.add((byte) 0x0B);
  }

  /**
   * Adds a string.
   * @param tb token builder
   * @param head string header
   * @param txt text
   */
  private void add(final TokenBuilder tb, final String head, final String txt) {
    if(txt.length() == 0) return;
    tb.add((byte) 0x02);
    tb.add(head);
    tb.add((byte) 0x03);
    tb.add(txt);
    tb.add((byte) 0x0A);
    tb.add((byte) 0x0B);
  }

  @Override
  public void mouseMoved(final MouseEvent e) {
    final int l = stat.size;
    if(l == 0) return;

    focus = -1;
    if(e.getY() < h) {
      for(int i = 0; i < l; i++) {
        final int bx = w - bw + bs * i;
        if(e.getX() >= bx && e.getX() < bx + bs) focus = i;
      }
    }

    final int f = focus == -1 ? l - 1 : focus;
    timer.setText(f == -1 ? "" : " " + strings.list[f] + ": " +
        Performance.getTimer(stat.list[f] * 10000L * Prop.runs, Prop.runs));
    repaint();
  }

  @Override
  public void paintComponent(final Graphics g) {
    super.paintComponent(g);
    final int l = stat.size;
    if(l == 0) return;
    BaseXLayout.antiAlias(g);

    h = north.getHeight();
    w = getWidth() - 8;
    bw = GUIProp.fontsize * 2 + w / 10;
    bs = bw / l;

    // find maximum value
    int m = 0;
    for(int i = 0; i < l; i++) m = Math.max(m, stat.list[i]);

    // draw focused bar
    final int by = 10;
    final int bh = h - by;

    for(int i = 0; i < l; i++) {
      if(i != focus) continue;
      final int bx = w - bw + bs * i;
      g.setColor(color4);
      g.fillRect(bx, by, bs + 1, bh);
    }

    // draw all bars
    for(int i = 0; i < l; i++) {
      final int bx = w - bw + bs * i;
      g.setColor(COLORS[(i == focus ? 3 : 2) + i * 2]);
      final int p = Math.max(1, stat.list[i] * bh / m);
      g.fillRect(bx, by + bh - p, bs, p);
      g.setColor(COLORS[8]);
      g.drawRect(bx, by + bh - p, bs, p - 1);
    }
  }
}
