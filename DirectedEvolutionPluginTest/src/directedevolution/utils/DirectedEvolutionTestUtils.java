package directedevolution.utils;

import com.biomatters.geneious.publicapi.plugin.Options;
import directedevolution.Gap;
import org.junit.Assert;
import testutils.TestUtils;

import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DirectedEvolutionTestUtils {


    public static void compareGapLists(List<Gap> gapStreamExpected, List<Gap> actualGapList) {
        Assert.assertEquals("Gap array size not correct, expected:" + gapStreamExpected, gapStreamExpected.size(), actualGapList.size());
        AtomicInteger atomicInteger = new AtomicInteger(0);
        gapStreamExpected.forEach(gap -> {
            Gap actual = actualGapList.get(atomicInteger.getAndIncrement());
            Assert.assertEquals(actual, gap);
        });
    }
}
