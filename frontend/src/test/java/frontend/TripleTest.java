package frontend;

import org.junit.Assert;
import org.junit.Test;

import frontend.models.Triple;

public class TripleTest {
    @Test
    public void testTripleCreation() {
        Triple<Integer, String, Double> triple = new Triple<>(1, "Test", 2.5);
        Assert.assertEquals(Integer.valueOf(1), triple.first);
        Assert.assertEquals("Test", triple.second);
        Assert.assertEquals(Double.valueOf(2.5), triple.third);
        Assert.assertEquals("(1, Test, 2.5)", triple.toString());
    }
}
