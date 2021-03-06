package org.basex.query.expr;

import org.basex.query.util.*;
import org.basex.query.*;
import org.junit.*;

/**
 * Tests for XQuery Maps.
 *
 * @author BaseX Team 2005-13, BSD License
 * @author Leo Woerteler
 */
public final class MapTest extends AdvancedQueryTest {
  /** A map as key should lead to FOTY0013. */
  @Test public void mapAsKeyTest() {
    error("declare variable $m := map { 'a': 'b' };" +
          "declare variable $q := map { $m: 'a' };" +
          "$q", Err.FIATOM);
  }

  /** Tests the the new syntax for map literals (see GH-755). */
  @Test public void jsonSyntax() {
    query("(<x><y/></x> / map {'test':y, 42:'asdf'})('test')", "<y/>");
  }
}
