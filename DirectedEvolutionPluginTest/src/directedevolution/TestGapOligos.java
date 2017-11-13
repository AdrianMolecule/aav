package directedevolution;

import com.biomatters.geneious.publicapi.implementations.sequence.DefaultNucleotideSequence;
import com.biomatters.geneious.publicapi.plugin.TestGeneious;
import com.google.common.collect.Lists;
import directedevolution.utils.DirectedEvolutionTestUtils;
import javafx.util.Pair;
import org.junit.Before;
import org.junit.Test;
import testutils.TestUtils;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

@SuppressWarnings("ImplicitNumericConversion")
public class TestGapOligos {

    @Test
    public void testFindMatchingOligoNoAnyOverlap() throws Exception {
        String[] oligoStringsAndGap = {
                "                     ---",
                "         qwertyuiop1234567890123xghjklzxcvbnm",
        };
        /////////////////////GAP
        Pair<Gap, List<OligoModel>> pair = extractOligosAndGap(oligoStringsAndGap);
        Gap gap = pair.getKey();
        List<OligoModel> oligos = pair.getValue();
        //
        OligoModel oligoModelExpected = oligos.get(0);
        DirectedEvolutionOperationOptions defaultOptions = (DirectedEvolutionOperationOptions) TestUtils.getDefaultOptions(new DirectedEvolutionOperationOptions());
        GenerateOligosOperation generateOligosOperation = new GenerateOligosOperation();
        generateOligosOperation.setOptions(defaultOptions);
        ConcatenatedOligoModel matchingOligo = generateOligosOperation.findMatchingConcatenatedOligo(oligos, gap);
        assertThat("start index", matchingOligo.getStartIndex(), is(equalTo(oligoModelExpected.getStartIndex())));
        assertThat("issue for startOverlap size", matchingOligo.getStartOverlapSize(), is(equalTo(oligoModelExpected.getStartOverlapSize())));
        assertThat("issue for endOverlap size", matchingOligo.getEndOverlapSize(), is(equalTo(oligoModelExpected.getEndOverlapSize())));
        assertThat("issue for getEndIndexPlusOne size", matchingOligo.getEndIndexPlusOne(), is(equalTo(oligoModelExpected.getStartIndex() + oligoModelExpected.getSize())));
    }

    @Test
    public void testFindMatchingOligoChooseTheLowerOne() throws Exception {
        String[] oligoStringsAndGap = {
                "                     ---",
                "qwetyuiop1234yuiop",
                "             yuiop1234567890qwertyuiop",
                "                            qwertyuiop1234567890xghjklzxcvbnm",
        };
        /////////////////////GAP
        Pair<Gap, List<OligoModel>> pair = extractOligosAndGap(oligoStringsAndGap);
        Gap gap = pair.getKey();
        List<OligoModel> oligos = pair.getValue();
        //
        OligoModel oligoModelExpected = oligos.get(1);
        DirectedEvolutionOperationOptions defaultOptions =  (DirectedEvolutionOperationOptions) TestUtils.getDefaultOptions(new DirectedEvolutionOperationOptions());
        int minOverlap = defaultOptions.getMinOverlap();
        GenerateOligosOperation generateOligosOperation = new GenerateOligosOperation();
        generateOligosOperation.setOptions(defaultOptions);
        ConcatenatedOligoModel matchingOligo = generateOligosOperation.findMatchingConcatenatedOligo(oligos, gap);
        assertThat("start index", matchingOligo.getStartIndex(), is(equalTo(oligoModelExpected.getStartIndex())));
        assertThat("issue for startOverlap size", matchingOligo.getStartOverlapSize(), is(equalTo(oligoModelExpected.getStartOverlapSize())));
        assertThat("issue for endOverlap size", matchingOligo.getEndOverlapSize(), is(equalTo(oligoModelExpected.getEndOverlapSize())));
        assertThat("issue for getEndIndexPlusOne size", matchingOligo.getEndIndexPlusOne(), is(equalTo(oligoModelExpected.getStartIndex() + oligoModelExpected.getSize())));
    }

    @Test
    public void testFindMatchingOligo3() throws Exception {
        String[] oligoStringsAndGap = {
                "                     ---",
                "         qwertyuiop1234567890qwertyuiop",
                "                                qwertyuiop1234567890xghjklzxcvbnm",
        };
        /////////////////////GAP
        Pair<Gap, List<OligoModel>> pair = extractOligosAndGap(oligoStringsAndGap);
        Gap gap = pair.getKey();
        List<OligoModel> oligos = pair.getValue();
        //
        OligoModel oligoModelExpected = oligos.get(0);
        DirectedEvolutionOperationOptions defaultOptions =  (DirectedEvolutionOperationOptions) TestUtils.getDefaultOptions(new DirectedEvolutionOperationOptions());
        int minOverlap = defaultOptions.getMinOverlap();
        GenerateOligosOperation generateOligosOperation = new GenerateOligosOperation();
        generateOligosOperation.setOptions(defaultOptions);
        ConcatenatedOligoModel matchingOligo = generateOligosOperation.findMatchingConcatenatedOligo(oligos, gap);
        assertThat("start index", matchingOligo.getStartIndex(), is(equalTo(oligoModelExpected.getStartIndex())));
        assertThat("issue for startOverlap size", matchingOligo.getStartOverlapSize(), is(equalTo(oligoModelExpected.getStartOverlapSize())));
        assertThat("issue for endOverlap size", matchingOligo.getEndOverlapSize(), is(equalTo(oligoModelExpected.getEndOverlapSize())));
        assertThat("issue for getEndIndexPlusOne size", matchingOligo.getEndIndexPlusOne(), is(equalTo(oligoModelExpected.getStartIndex() + oligoModelExpected.getSize())));
    }

    private Pair<Gap, List<OligoModel>> extractOligosAndGap(String[] oligoStringsAndGap) throws Exception {
        if (oligoStringsAndGap.length == 0) {
            throw new Exception("oligoStringsAndGap should have at least one dummy oligo string");
        }
        Gap gap = null;
        List<OligoModel> oligoModels = Lists.newArrayList();
        for (String s : oligoStringsAndGap) {
            Pattern p = Pattern.compile("(\\s*)([a-z]*)\\d*(-*)\\d*([1-z]*)\\s*.*");
            Matcher m = p.matcher(s);
            if (!m.matches()) {
                throw new Exception("the format fo the test string appears incorrect");
            }
            int oligoStartIndex = m.group(1).length();
            int startOverlap = m.group(2).length();
            if (m.group(3).length() > 0) {
                gap = new Gap(m.start(3), m.group(3).length(), new DefaultNucleotideSequence("ignored name", "ignored"), 77);
                continue;
            }
            int endOverlap = m.group(4).length();
            OligoModel oligoModel = new OligoModel(s.trim(), oligoStartIndex, startOverlap, OligoModel.ProblemCode.NO_PROBLEM);
            oligoModel.setEndOverlapSize(endOverlap);
            oligoModels.add(oligoModel);
        }
        return new Pair<>(gap, oligoModels);
    }

    @Test
    public void testFindGapsList() {
        DefaultNucleotideSequence sequence1 = new DefaultNucleotideSequence("Test Sequence", "---ATGC----AATAA");
        DirectedEvolutionTestUtils.compareGapLists(Stream.of(new Gap(0, 3, sequence1, 77), new Gap(7, 4, sequence1, 77)).collect(Collectors.toList()), GenerateOligosOperation.findGapsList(sequence1, 77));
        //
        DefaultNucleotideSequence sequence2 = new DefaultNucleotideSequence("Test Sequence", "---ATGC----AATA-");
        DirectedEvolutionTestUtils.compareGapLists(Stream.of(new Gap(0, 3, sequence2, 77), new Gap(7, 4, sequence2, 77), new Gap(15, 1, sequence2, 77)).collect(Collectors.toList()), GenerateOligosOperation.findGapsList(sequence2, 77));
        //
        DefaultNucleotideSequence sequence3 = new DefaultNucleotideSequence("Test Sequence", "ATGCAATA");
        DirectedEvolutionTestUtils.compareGapLists(Lists.newArrayList(), GenerateOligosOperation.findGapsList(sequence3, 77));
        //
        DefaultNucleotideSequence sequence4 = new DefaultNucleotideSequence("Test Sequence", "-ATGCAATA");
        DirectedEvolutionTestUtils.compareGapLists(Stream.of(new Gap(0, 1, sequence4, 77)).collect(Collectors.toList()), GenerateOligosOperation.findGapsList(sequence4, 77));
    }

    @Test //just one Gap with no overlap issue
    public void testOneGapOligo() throws Exception {
        String oldDocFile = "Translation alignment of 2 sequences with 1 Gap Oligo REFERENCE.geneious";
        String newDocFile = "Translation alignment of 2 sequences With 1 Gap Olig NEW.geneious";
        TestUtils.compareDocuments(new File(TestUtils.RELATIVE_TEST_DATA_DIR + oldDocFile), new File(TestUtils.RELATIVE_TEST_DATA_DIR + newDocFile));
    }

    @Test //just 8 Gap with multiple to the right
    public void test8GapsOligo() throws Exception {
        String oldDocFile = "Translation alignment of 2 sequences 1 and 5 with 8 Gap Oligo REFERENCE.geneious";
        String newDocFile = "Translation alignment of 2 sequences 1 and 5 with 8 Gap Oligo NEW.geneious";
        TestUtils.compareDocuments(new File(TestUtils.RELATIVE_TEST_DATA_DIR + oldDocFile), new File(TestUtils.RELATIVE_TEST_DATA_DIR + newDocFile));
    }

    @Before
    public void setup() throws Exception {
        TestGeneious.initialize();
    }
}
