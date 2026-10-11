package pw.rkd.appcat;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class CategoryCriteriaTest {
    @Test public void excludesOtherButKeepsCustomCategories() {
        var choices=CategoryCriteria.choices(List.of(new Catalog.Category("c16","Other"),new Catalog.Category("lower"," other "),new Catalog.Category("custom","Security"),new Catalog.Category("c7","Productivity")));
        assertFalse(choices.containsKey("c16"));assertFalse(choices.containsKey("lower"));assertTrue(choices.containsKey("custom"));assertTrue(choices.get("c7").contains("Not a generic category"));
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsAnEmptyChoiceSet() {CategoryCriteria.choices(List.of(new Catalog.Category("c16","Other")));}
}
