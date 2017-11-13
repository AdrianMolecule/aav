import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;
import com.biomatters.geneious.publicapi.implementations.DefaultAlignmentDocument;
import com.biomatters.geneious.publicapi.implementations.sequence.DefaultNucleotideSequence;
import com.biomatters.geneious.publicapi.plugin.PluginUtilities;
import com.biomatters.geneious.publicapi.plugin.TestGeneious;
import com.biomatters.geneious.publicapi.utilities.Interval;
import javafx.util.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
//see https://assets.geneious.com/developer/geneious/javadoc/latest/index.html

@SuppressWarnings({"ImplicitNumericConversion", "HardcodedFileSeparator", "ClassHasNoToStringMethod"})
public class MainTest {

    @Test
    public void testGenerateOptimizedNucleotideSequenceX() throws Exception {
        List<String> seqs = Arrays.asList(
                "CGTCCC",
                "CGCCCC",
                "TCCCCC",
                "TTTCCC",
                "CGTCCC",
                "CGGCCC",
                "TCTCCC",
                "TCACCC",
                "AGACCC",
                "CGACCC");
        DefaultAlignmentDocument defaultAlignmentDocument = new DefaultAlignmentDocument();
        AtomicInteger i=new AtomicInteger(0);
        seqs.forEach(s -> {
            defaultAlignmentDocument.addSequence(new DefaultNucleotideSequence("Test Sequence"+i.getAndIncrement(), s));
        });
    }

    @Before
    public void setup() throws Exception {
        TestGeneious.initialize();
    }
}