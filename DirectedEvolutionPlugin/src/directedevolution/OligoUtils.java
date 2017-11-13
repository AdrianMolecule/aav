package directedevolution;

import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;
import com.biomatters.geneious.publicapi.documents.DocumentField;
import com.biomatters.geneious.publicapi.documents.PluginDocument;
import com.biomatters.geneious.publicapi.documents.sequence.*;
import com.biomatters.geneious.publicapi.implementations.DefaultAlignmentDocument;
import com.biomatters.geneious.publicapi.implementations.sequence.DefaultNucleotideSequence;
import com.biomatters.geneious.publicapi.utilities.SequenceUtilities;
import javafx.util.Pair;
import jebl.evolution.sequences.*;
import jebl.evolution.taxa.Taxon;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.biomatters.geneious.publicapi.documents.sequence.SequenceAnnotation.TYPE_OPTIMIZED_CODON;

public class OligoUtils {
    private static final Logger logger = Logger.getLogger(OligoUtils.class.getName());

    @Nullable
    public static SequenceCharSequence padEnds(int startIndex, CharSequence myCharOligo, Integer neededLength) {
        if (neededLength == null) {
            return null;
        }
        int endGap = neededLength - myCharOligo.length() - startIndex;
        return SequenceCharSequence.withTerminalGaps(startIndex, myCharOligo, endGap);
    }

    public static SequenceDocument emptySequence(Integer sequenceLength) {
        return new DefaultNucleotideSequence("", padEnds(0, "", sequenceLength));
    }

    @Nonnull
    public static String complementDNANucleotides(CharSequence nucleotideSequence) {
        Sequence seq = new BasicSequence(SequenceType.NUCLEOTIDE, Taxon.getTaxon("x"), nucleotideSequence);
        int length = seq.getLength();
        StringBuilder results = new StringBuilder();
        for (int i = 0; i < length; i++) {
            State state = seq.getState(i);
            NucleotideState complementaryState = Nucleotides.COMPLEMENTARY_STATES[state.getIndex()];
            results.append(complementaryState.getCode());
        }
        return results.toString();
    }

    public static void padEnds(StringBuilder oligo, int len) {//could use Strings.padEnd(string, len, paddingCHar from guava
        for (int i = 0; i < len; i++) {
            oligo.append('-');
        }
    }

    public static void padBeginning(StringBuilder oligo, int len) {//could use Strings.padEnd(string, len, paddingCHar from guava
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < len; i++) {
            result.append('-');
        }
        result.append(oligo);
    }

    @Nullable
    public static AminoAcidState toAminoAcid(CodonState codon) {
        String code = codon.getCode();
        if ("---".equals(code) || code == null) {
            return null;
        }
        CharSequence charSequence = SequenceUtilities.asTranslation(code, GeneticCode.UNIVERSAL, false);//todo not sure what the boolean is for
        return AminoAcids.getState(charSequence.toString());
    }
//    public static String toAminoAcid(String threeBases) {
//        CodonState[] codonStates = Codons.toStateArray(threeBases);
//        //System.out.println(codonStates[0].getCode());
//        // I can set this to true too //translateFirstCodonUsingFirstCodonTable -
//        // each genetic code specifies a set of codons which get translated as M if they are the first codon even though they normally wouldn't translate as an M when occurring elsewhere a coding region.
//        // If this parameter is true the first codon will be translated using this alternative translation table for the genetic code.
//        CharSequence charSequence = SequenceUtilities.asTranslation(codonStates[0].getCode(), GeneticCode.UNIVERSAL, false);
//        AminoAcidState acid = AminoAcids.getState(charSequence.toString());
//        return acid.getCode();
//    }
//    public static void scale() {
//        System.out.print("Index    " + "       " + ":");
//        for (int j = 0; j < 41; j++) {
//            for (int i = 0; i < 10; i++) {
//                System.out.print(i);
//            }
//        }
//        System.out.println();
//    }

    public static void fiveAndThereAnnotations(SequenceDocumentWithEditableAnnotations sequence, List<Pair<Integer, Integer>> ends) {
        List<SequenceAnnotation> annotations = new ArrayList<>(sequence.getSequenceAnnotations());
        ends.forEach(pair -> {
            annotations.add(new SequenceAnnotation("5", SequenceAnnotation.TYPE_UTR_5, new SequenceAnnotationInterval(pair.getKey(), pair.getKey())));
            annotations.add(new SequenceAnnotation("3", SequenceAnnotation.TYPE_UTR_3, new SequenceAnnotationInterval(pair.getValue(), pair.getValue())));
        });
        sequence.setAnnotations(annotations);
    }

    public static void annotateSequence(SequenceDocument sequence, List<SequenceAnnotation> annotationsToAdd) {
        List<SequenceAnnotation> annotations = new ArrayList<>(sequence.getSequenceAnnotations());
        annotationsToAdd.forEach(annotations::add);
        ((SequenceDocumentWithEditableAnnotations) sequence).setAnnotations(annotations);
        // document.saveDocument();
    }

    /**
     * Applies the given annotations on the given sequences. For optimized sequences we get a different formatting of the annotation string.
     *
     * @param sequences                              sequences to annotate
     * @param allOptimizedAnnotationsForAllSequences annotations
     * @param optimizedSequence                      flag to customize the text
     */
    public static void applyAnnotationsOnTargetSequences(List<OptimizedNucleotideSequence> sequences, List<List<GenerateOligosOperation.OptimizationAnnotationInfo>> allOptimizedAnnotationsForAllSequences, boolean optimizedSequence) {
        for (int sequenceIndex = 0; sequenceIndex < sequences.size(); sequenceIndex++) {
            List<SequenceAnnotation> sequenceAnnotations = allOptimizedAnnotationsForAllSequences.get(sequenceIndex).stream().
                    map(optimizationAnnotationInfosForOneStream ->
                            new SequenceAnnotation(optimizationAnnotationInfosForOneStream.getText(optimizedSequence), TYPE_OPTIMIZED_CODON,
                                    new SequenceAnnotationInterval(optimizationAnnotationInfosForOneStream.getInterval())))
                    .collect(Collectors.toList());
            annotateSequence(sequences.get(sequenceIndex), sequenceAnnotations);
        }
    }

    /**
     * Applies the given annotations on the given sequence interval. For optimized sequences we get a different formatting of the annotation string.
     *
     * @param sequenceAlignmentDocument
     * @param firstSequenceToAnnotateIndex
     * @param numberOfSequencesToAnnotate
     * @param allOptimizedAnnotationsForAllSequences
     * @param optimizedSequence
     */
    public static void applyAnnotationsOnTargetSequences(SequenceAlignmentDocument sequenceAlignmentDocument, int firstSequenceToAnnotateIndex,
                                                         int numberOfSequencesToAnnotate, List<List<GenerateOligosOperation.OptimizationAnnotationInfo>> allOptimizedAnnotationsForAllSequences, boolean optimizedSequence) {
        for (int sequenceIndex = 0; sequenceIndex < numberOfSequencesToAnnotate; sequenceIndex++) {
            List<SequenceAnnotation> sequenceAnnotations = allOptimizedAnnotationsForAllSequences.get(sequenceIndex).stream().
                    map(optimizationAnnotationInfosForOneStream ->
                            new SequenceAnnotation(optimizationAnnotationInfosForOneStream.getText(optimizedSequence), TYPE_OPTIMIZED_CODON,
                                    new SequenceAnnotationInterval(optimizationAnnotationInfosForOneStream.getInterval())))
                    .collect(Collectors.toList());
            sequenceAlignmentDocument.setAnnotations(firstSequenceToAnnotateIndex + sequenceIndex, sequenceAnnotations, false/*don't change the original referenced documents*/);
        }
    }

    //todo A PluginDocument may return a value from PluginDocument.getFieldValue(String) with this fieldCodeName to change the class used to get the document type of the document.
// The value must be the fully qualified class name. Eg. this can be used to change the icon of a document depending on its contents as opposed to just its class.
    //creates un unnecessary new property
    private static void setNewFieldValue(SequenceAlignmentDocument sequenceAlignmentDocument, int sequenceIndex) {
        sequenceAlignmentDocument.setFieldValue(sequenceIndex, DocumentField.createStringField("Adrian 1", "Adrian2 ", "Adrian 3"), "HAHA", false);//add a new field
    }

    @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
    public static String dumpAllDocuments(AnnotatedPluginDocument[] documents) {
        StringBuilder resultingString = new StringBuilder();
        for (AnnotatedPluginDocument annotatedPluginDocument : documents) {
            PluginDocument wrappedDocument = annotatedPluginDocument.getDocumentOrCrash();
            resultingString.append(" Wrapped document:").append(wrappedDocument.getName()).append(" of class :").append(wrappedDocument.getClass()).append("html:").append(wrappedDocument.toHTML());
            if (wrappedDocument instanceof DefaultAlignmentDocument) {
                DefaultAlignmentDocument defaultAlignmentDocument = (DefaultAlignmentDocument) wrappedDocument;
                resultingString.append(" GetAlignmentDataForSequencesNotInMemory:" + defaultAlignmentDocument.getAlignmentDataForSequencesNotInMemory());
                resultingString.append(" getAnnotationsOnConsensus:" + defaultAlignmentDocument.getAnnotationsOnConsensus().stream().map(SequenceAnnotation::toString).collect(Collectors.joining(",")));
                resultingString.append(" Streams:" + defaultAlignmentDocument.getSequences().stream().map(Object::toString).collect(Collectors.joining("\n")));
                for (SequenceDocument sequence : defaultAlignmentDocument.getSequences()) {
                    SequenceCharSequence sequenceCharSequence = sequence.getCharSequence();
                    resultingString.append("getReferencedSequence:" + defaultAlignmentDocument.getReferencedSequence(0));
                    resultingString.append(" SequenceCharSequence for stream:" + sequence.getName() + " is:" + sequenceCharSequence + " length Without Gaps:" + sequenceCharSequence.getUngappedLength());
                }
            }
        }
        return resultingString.toString();
    }

    public static void dumpSequencesWithTurnedOffConsensus(SequenceAlignmentDocument sequences) {//it seems it turns off everything
        if (logger.isLoggable(Level.FINE)) {
            sequences.getSequences().forEach(s ->
                    logger.fine("dumpSequencesWithTurnedOffConsensus: Sequence:" + s.getName() + " Field value:" + s.getFieldValue(DocumentField.NUMBER_OF_MAPPED_LOCATIONS.getCode()) + " isInstanceofOptimized:" + (s instanceof OptimizedNucleotideSequence))
            );
        }
    }
}
