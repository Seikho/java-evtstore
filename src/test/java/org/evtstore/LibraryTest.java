/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package org.evtstore;

import org.junit.Test;
import static org.junit.Assert.*;

import org.evtstore.domain.ex.DomainExample;
import org.evtstore.domain.ex.cmd.DoOne;

public class LibraryTest {
    public static DomainExample domain = new DomainExample("test1");

    @Test
    public void appendEvent() {
        var actual = domain.execute("one", new DoOne(42));
        assertEquals((Integer) 1, actual.version);
        assertEquals((Integer) 42, actual.one);
    }

    @Test
    public void appendAnother() {
        var agg = domain.getAggregate("one");
        assertEquals((Integer) 1, agg.version);
        var actual = domain.execute("one", new DoOne(42));
        assertEquals((Integer) 2, actual.version);
        assertEquals((Integer) 84, actual.one);
    }
}
