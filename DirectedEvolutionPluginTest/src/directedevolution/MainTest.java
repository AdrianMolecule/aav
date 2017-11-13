package directedevolution;

import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;
import com.biomatters.geneious.publicapi.implementations.DefaultAlignmentDocument;
import com.biomatters.geneious.publicapi.implementations.sequence.DefaultNucleotideSequence;
import com.biomatters.geneious.publicapi.plugin.PluginUtilities;
import com.biomatters.geneious.publicapi.plugin.TestGeneious;
import com.biomatters.geneious.publicapi.utilities.Interval;
import directedevolution.utils.DirectedEvolutionTestUtils;
import javafx.util.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import testutils.TestUtils;

import java.io.File;
import java.math.BigInteger;
import java.util.List;
//see https://assets.geneious.com/developer/geneious/javadoc/latest/index.html

@SuppressWarnings({"ImplicitNumericConversion", "HardcodedFileSeparator", "ClassHasNoToStringMethod"})
public class MainTest {
//    private final JUnitRuleMockery mockery = new JUnitRuleMockery() {{
//        setThreadingPolicy(new Synchroniser());
//    }};

    private DirectedEvolutionOperationOptions defaultOptions;
    private GenerateOligosOperation generateOligosOperation;

    @Test
    public void testGenerateOptimizedNucleotideSequences() throws Exception {
        //load test data
        List<AnnotatedPluginDocument> annotatedPluginDocuments = PluginUtilities.importDocuments(new File("testdata/Translation alignment of 8 sequences for test.geneious"), null);
        DefaultAlignmentDocument defaultAlignmentDocument = ((DefaultAlignmentDocument) annotatedPluginDocuments.get(0).getDocumentOrCrash());
        List<DefaultNucleotideSequence> expectedOptimizedSequences = TestUtils.loadFileSequences(new File("testdata/Optimized sequences for Translation alignment of 8 sequences Expected.fasta"));
        Pair<List<OptimizedNucleotideSequence>, List<List<GenerateOligosOperation.OptimizationAnnotationInfo>>> listListPair = generateOligosOperation.generateOptimizedSequences(defaultAlignmentDocument, -1);
        List<OptimizedNucleotideSequence> actualOptimizedSequences = listListPair.getKey();
        for (int index = 0; index < expectedOptimizedSequences.size(); index++) {
            int firstDiff = TestUtils.findFirstDiffIndex(expectedOptimizedSequences.get(index).getSequenceString(), (actualOptimizedSequences.get(index).getSequenceString()));
            if (firstDiff != -1) {
                Assert.assertEquals("Sequence string is different at index:" + firstDiff + " where actual is:\n" + actualOptimizedSequences.get(index).getSequenceString().substring(firstDiff) +
                                " and expected is:\n" + expectedOptimizedSequences.get(index).getSequenceString().substring(firstDiff),
                        expectedOptimizedSequences.get(index).getSequenceString(),
                        actualOptimizedSequences.get(index).getSequenceString());
            }
            Assert.assertEquals("Annotations are different", expectedOptimizedSequences.get(index).getSequenceAnnotations(), actualOptimizedSequences.get(index).getSequenceAnnotations());
        }
    }

    @Test //tests only the codons on the first sequence
    public void testGenerateOptimizedNucleotideSequencesWithIgnoredAAV8() throws Exception {
        /* 73-75
        TTG
        CTC
        CTG
        CTG
        CTT
        TTG
        CTG
        TTG*/
        //load test data
        List<AnnotatedPluginDocument> annotatedPluginDocuments = PluginUtilities.importDocuments(new File("testdata/Translation alignment of 8 sequences for test.geneious"), null);
        DefaultAlignmentDocument defaultAlignmentDocument = ((DefaultAlignmentDocument) annotatedPluginDocuments.get(0).getDocumentOrCrash());
        List<DefaultNucleotideSequence> expectedOptimizedSequences = TestUtils.loadFileSequences(new File("testdata/Optimized sequences for Translation alignment of 8 sequences Expected.fasta"));
        int ignoreSequenceNumber = 7;
        Pair<List<OptimizedNucleotideSequence>, List<List<GenerateOligosOperation.OptimizationAnnotationInfo>>> listListPair = generateOligosOperation.generateOptimizedSequences(defaultAlignmentDocument, ignoreSequenceNumber);
        List<OptimizedNucleotideSequence> actualOptimizedSequences = listListPair.getKey();
        for (int index = 0; index < expectedOptimizedSequences.size(); index++) {
            //String expectedCodon = "";
            String actualCodon;
            int firstDiff = TestUtils.findFirstDiffIndex(expectedOptimizedSequences.get(index).getSequenceString(), (actualOptimizedSequences.get(index).getSequenceString()));
            if (firstDiff != -1) {
                firstDiff -= firstDiff % 3;//go to codon start
                //expectedCodon = expectedOptimizedSequences.get(index).getSequenceString().substring(firstDiff, firstDiff + 3);
                actualCodon = actualOptimizedSequences.get(index).getSequenceString().substring(firstDiff, firstDiff + 3);
                //                Assert.assertEquals("Wrong Sequence " + expectedOptimizedSequences.get(index).getName() + " at sequence index:" + index + " at char:" +
                //                                firstDiff + " expected is:" + expectedCodon + " actual is:" + actualCodon + " when ignoring sequence:" + actualOptimizedSequences.get(ignoreSequenceNumber).getName(),
                //                        expectedOptimizedSequences.get(index).getSequenceString(),
                //                        actualOptimizedSequences.get(index).getSequenceString());
                //
                Assert.assertTrue("Wrong Sequence ", "CTG".equals(actualCodon));
                break;
            }
        }
    }

    @Test //tests only the codons on the first sequence
    public void testConsensusGenerateOptimizedNucleotideSequencesWithIgnoredAAV8() throws Exception {
        String oldDocFile = "Translation alignment of 8 sequences With Oligos 20 7 8 no oligos REFERENCE test.geneious";
        String newDocFile = "Translation alignment of 8 sequences With Oligos 20 7 8 no oligos NEW.geneious";
        List<AnnotatedPluginDocument> oldDocuments = PluginUtilities.importDocuments(new File(TestUtils.RELATIVE_TEST_DATA_DIR + oldDocFile), null);
        List<AnnotatedPluginDocument> newDocuments = PluginUtilities.importDocuments(new File(TestUtils.RELATIVE_TEST_DATA_DIR + newDocFile), null);
        DefaultAlignmentDocument oldDocumentOrCrash = ((DefaultAlignmentDocument) oldDocuments.get(0).getDocumentOrCrash());
        DefaultAlignmentDocument newDocumentOrCrash = ((DefaultAlignmentDocument) newDocuments.get(0).getDocumentOrCrash());
        for (int sec = 0; sec < oldDocumentOrCrash.getSequences().size(); sec++) {
            if ("Optimized Consensus".equalsIgnoreCase(oldDocumentOrCrash.getSequences().get(sec).getName())) {
                Assert.assertEquals(" Sequence index:" + sec + " named:" + oldDocumentOrCrash.getSequences().get(sec).getName() + " for old one",
                        oldDocumentOrCrash.getSequences().get(sec).getSequenceString(), newDocumentOrCrash.getSequences().get(sec).getSequenceString());
            }
        }
    }

    @Test //NO OLIGOS
    public void testPlainGeneratedOligoDocumentsWithDefaultOptionsAndNoOligos() throws Exception {
        String oldDocFile = "Translation alignment of 8 sequences With Oligos 20 7 8 no oligos REFERENCE test.geneious";
        String newDocFile = "Translation alignment of 8 sequences With Oligos 20 7 8 no oligos NEW.geneious";
        TestUtils.compareDocuments(new File(TestUtils.RELATIVE_TEST_DATA_DIR + oldDocFile), new File(TestUtils.RELATIVE_TEST_DATA_DIR + newDocFile));
    }

    @Test //20 7 8 Backward First-GENERATED OLIGOS
    public void testPlainGeneratedOligoDocumentsWithDefaultOptionsAndOligos() throws Exception {
        String oldDocFile = "Translation alignment of 8 sequences With Oligos 20 7 8 Backwards generated oligos REFERENCE test.geneious";//old one has GAG
        String newDocFile = "Translation alignment of 8 sequences With Oligos 20 7 8 Backwards NEW.geneious";
        TestUtils.compareDocuments(new File(TestUtils.RELATIVE_TEST_DATA_DIR + oldDocFile), new File(TestUtils.RELATIVE_TEST_DATA_DIR + newDocFile));
    }

    @Test //20 7 8 Forward GENERATED OLIGOS
    public void testPlainGeneratedOligoDocumentsWithForwardAndDefaultOptionsAndOligos() throws Exception {
        String oldDocFile = "Translation alignment of 8 sequences With Oligos 20 7 8 Forward REFERENCE test.geneious";
        String newDocFile = "Translation alignment of 8 sequences With Oligos 20 7 8 Forward NEW.geneious";
        TestUtils.compareDocuments(new File(TestUtils.RELATIVE_TEST_DATA_DIR + oldDocFile), new File(TestUtils.RELATIVE_TEST_DATA_DIR + newDocFile));
    }

    //    @Test
//    public void testGenerateOptimizedNucleotideSequencesX() {
//        CodonState codon = Codons.getState("ATG");//ATG is the "code"
//        //ring codon = "ATG";
//        AminoAcidState aminoAcid = GenerateOligosOperation.toAminoAcid(codon);
//        Assert.assertEquals("M", aminoAcid.getCode());
//        codon = Codons.getState("TGC");
//        aminoAcid = GenerateOligosOperation.toAminoAcid(codon);
//        Assert.assertEquals("C", aminoAcid.getCode());
//        codon = Codons.getState("UAA");
//        aminoAcid = GenerateOligosOperation.toAminoAcid(codon);
//        Assert.assertEquals("*", aminoAcid.getCode());
//    }
//    @Test
//    public void testGenerateOligosShort() {
//        generateOligosShort("ATGGCAGCYGAYGGCACAGATCAGATTGYCTCGAGGACAMYCTTT");
//    }
//
//    @Test
//    public void testGenerateOligosLonger() {
//        String bigSequence = "ATGGCTGCYGAYGGTTATCTTCCAGATTGGCTCGAGGACAMYCTYTCTGARGGMATWMGHSAGTGGTGGRMBYTSAAACCTGGMSYMCCDMMRCCMAARSCSRMMSARCRRMABMAGGACRACVGYMGGGGTCTKGTGCTTCCKGGBTAC";
//        generateOligosShort(bigSequence);
//    }
//
//    @Test
//    public void testGenerateOligos123() {
//        String bigSequence = "ATGGCTGCYGAYGGTTATCTTCCAGATTGGCTCGAGGACAMYCTYTCTGARGGMATWMGHSAGTGGTGGRMBYTSAAACCTGGMSYMCCDMMRCCMAARSCSRMMSARCRRMABMAGGACRACVGYMGGGGTCTKGTGCTTCCKGGBTACAARTACCTCGGACCCKKYAACGGACTCGACAARGGRGAGCCSGTCAACGMGGCRGACGCVGCRGCCCTCGARCACGACAARGCYTACGACCRGCAGCTCRAVRSSGGWGACAAYCCGTACCTSMRGTAYAACCACGCCGACGCSGAGTTTCAGGAGCGYCTKMAAGAAGATACGTCTTTTGGGGGCAACCTYGGVMGAGCAGTCTTCCAGGCSAARAAGMGGRTYCTYGARCCTCTBGGYCTGGTTGAGGAASSHGY";
//        generateOligosShort(bigSequence);
//    }
//
//    public void generateOligosShort(String bigSequence) {
//        GenerateOligosOperation generateOligosOperation = new GenerateOligosOperation();
//        List<DefaultNucleotideSequence> list = generateOligosOperation.generateOligos(bigSequence, null, bigSequence.length()/*sequence length*/);
//        GenerateOligosOperation.scale();
//        System.out.println();
//        for (DefaultNucleotideSequence sequence : list) {
//            System.out.format("Sequence:        %s\n", sequence.getSequenceString());
//        }
//        System.out.format("Sequence:        %s\n", bigSequence);
//        GenerateOligosOperation.scale();
//    }
//
//    @Test
//    public void testGenerateOptimizedNucleotideSequence() {
//        DefaultAlignmentDocument defaultAlignmentDocument = new DefaultAlignmentDocument();
//        defaultAlignmentDocument.addSequence(new DefaultNucleotideSequence("Test Sequence", "ATGCAATAA"));
//        CodonState codon = Codons.getState("ATG");//ATG is the "code"
//        //ring codon = "ATG";
//        AminoAcidState aminoAcid = GenerateOligosOperation.toAminoAcid(codon);
//        Assert.assertEquals("M", aminoAcid.getCode());
//        codon = Codons.getState("TGC");
//        aminoAcid = GenerateOligosOperation.toAminoAcid(codon);
//        Assert.assertEquals("C", aminoAcid.getCode());
//        codon = Codons.getState("UAA");
//        aminoAcid = GenerateOligosOperation.toAminoAcid(codon);
//        Assert.assertEquals("*", aminoAcid.getCode());
//    }
//
//    @Test
//    public void testGenerateOptimizedNucleotideSequenceX() throws Exception {
//        List<String> seqs = Arrays.asList(
//                "CGTCCC",
//                "CGCCCC",
//                "TCCCCC",
//                "TTTCCC",
//                "CGTCCC",
//                "CGGCCC",
//                "TCTCCC",
//                "TCACCC",
//                "AGACCC",
//                "CGACCC");
//        DefaultAlignmentDocument defaultAlignmentDocument = new DefaultAlignmentDocument();
//        AtomicInteger i=new AtomicInteger(0);
//        seqs.forEach(s -> {
//            defaultAlignmentDocument.addSequence(new DefaultNucleotideSequence("Test Sequence"+i.getAndIncrement(), s));
//        });
////        List<DefaultNucleotideSequence> optimizedNucleotideSequences = GenerateOligosOperation.generateOptimizedSequences(defaultAlignmentDocument);
////        optimizedNucleotideSequences.stream().forEach(System.out::println );
//    }
    @Test
    public void testCalculateNucleotideTotal() throws Exception {
        DefaultNucleotideSequence testSequence = new DefaultNucleotideSequence("Test Sequence", "ATGDYKDYYGWTGRTYAYCYTCCAGATTGGCTCGAGGA");
        BigInteger total = VariantStatistics.calculateVariant(testSequence.getCharSequence(), new Interval(1, testSequence.getSequenceLength()));
        Assert.assertEquals(total, new BigInteger("4608"));
        DefaultNucleotideSequence testSequence1 = new DefaultNucleotideSequence("Test Sequence", "ATGDYK");
        BigInteger total1 = VariantStatistics.calculateVariant(testSequence1.getCharSequence(), new Interval(1, testSequence1.getSequenceLength()));
        Assert.assertEquals(total1, new BigInteger("12"));
        //
    }

    @Before
    public void setup() throws Exception {
        TestGeneious.initialize();
        defaultOptions = (DirectedEvolutionOperationOptions) TestUtils.getDefaultOptions(new DirectedEvolutionOperationOptions());
        generateOligosOperation = new GenerateOligosOperation();
        generateOligosOperation.setOptions(defaultOptions);
        GenerateOligosOperation generateOligosOperation = new GenerateOligosOperation();
        generateOligosOperation.setOptions(defaultOptions);
    }
}