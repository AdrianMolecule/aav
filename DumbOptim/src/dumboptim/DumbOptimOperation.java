package dumboptim;

import com.biomatters.geneious.publicapi.components.Dialogs;
import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;
import com.biomatters.geneious.publicapi.documents.DocumentField;
import com.biomatters.geneious.publicapi.documents.DocumentUtilities;
import com.biomatters.geneious.publicapi.documents.PluginDocument;
import com.biomatters.geneious.publicapi.documents.sequence.*;
import com.biomatters.geneious.publicapi.implementations.DefaultAlignmentDocument;
import com.biomatters.geneious.publicapi.implementations.SequenceGapInformation;
import com.biomatters.geneious.publicapi.implementations.sequence.DefaultNucleotideSequence;
import com.biomatters.geneious.publicapi.plugin.*;
import com.biomatters.geneious.publicapi.utilities.FileUtilities;
import com.biomatters.geneious.publicapi.utilities.Interval;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import javafx.util.Pair;
import jebl.evolution.sequences.AminoAcidState;
import jebl.evolution.sequences.CodonState;
import jebl.evolution.sequences.Codons;
import jebl.evolution.sequences.Utils;
import jebl.util.ProgressListener;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 */
public class DumbOptimOperation extends DocumentOperation {

    private static String newDocumentName = "Unknown";//only available after changeName!

    //private static DirectedEvolutionOperationOptions storedDirectedEvolutionOperationOptions;

    //private List<OligoModel> oligos;

    //
//    private static final Logger logger = Logger.getLogger(GenerateOligosOperation.class.getName());
//
//    @Override
//    public Options getOptions(AnnotatedPluginDocument... documents) {//called by the framework and passed into perform operation
//        return new DirectedEvolutionOperationOptions(documents);
//    }

    @Override
    public GeneiousActionOptions getActionOptions() {
        return null;
    }

    @Override
    public String getHelp() {
        return null;
    }

    /**
     * The document selection signature tells Geneious to enable GenerateOligos when a Alignment of nucleotides is selected.
     * todo It would be better if we further restrict that to only Translation type alignments
     */
    @Override
    public DocumentSelectionSignature[] getSelectionSignatures() {
        return new DocumentSelectionSignature[]{
                new DocumentSelectionSignature(DefaultAlignmentDocument.class, 1, 1)//we allow only 1 Alignment Documents
        };
    }

    @Override
    public List<AnnotatedPluginDocument> performOperation(AnnotatedPluginDocument[] documents, ProgressListener progressListener, Options directedEvolutionOperationOptions) throws DocumentOperationException {
        System.out.println("HERE");
        return null;
//        this.storedDirectedEvolutionOperationOptions = (DirectedEvolutionOperationOptions) directedEvolutionOperationOptions;
//        try {
//            // Our selection signature guarantees we have just alignment documents here.
//            if (logger.isLoggable(Level.FINE)) {
//                logger.fine(OligoUtils.dumpAllDocuments(documents));
//            }
//            //we accept only alignments in plugin
//            DefaultAlignmentDocument finalDocument = ((DefaultAlignmentDocument) DocumentUtilities.duplicateDocument(documents[0], true).getDocumentOrCrash());
//            if (!(finalDocument.getSequences().get(0) instanceof NucleotideSequenceDocument)) {//no need to check all sequence types as only one type can be contained in an alignment
//                return exitOperationWithError("You need to select an alignment of nucleotide sequences");
//            }
//            // sanity check for proper lengths. Should never fail.
//            Integer sequencesLength = checkLengths(finalDocument.getSequences());
//            if (sequencesLength == 0) {
//                return exitOperationWithError("Sequences length is either 0 or we have no Sequences selected");
//            }
//            if (finalDocument.getSequences().stream().filter(s -> s instanceof OptimizedNucleotideSequence).count() > 0L) {
//                return exitOperationWithError("Can not re-process a sequence with optimized sequences and Oligos. Please select an Aligned Nucleotide sequence with a translation Alignment.");
//            }
//            //Generate optimized sequences and multi purpose annotations
//            Integer sequenceIgnoredDuringOptimizationIndex = storedDirectedEvolutionOperationOptions.getSequenceIgnoredDuringOptimization();
//            Pair<List<OptimizedNucleotideSequence>, List<List<OptimizationAnnotationInfo>>> listOptimizedSequencesListOptimizationAnnotationInfosPair = generateOptimizedSequences(finalDocument, sequenceIgnoredDuringOptimizationIndex);
//            List<OptimizedNucleotideSequence> optimizedNucleotideSequences = listOptimizedSequencesListOptimizationAnnotationInfosPair.getKey();// calculate and add codon changed sequences
//            List<List<OptimizationAnnotationInfo>> optimizationAnnotationInfoList = listOptimizedSequencesListOptimizationAnnotationInfosPair.getValue();// calculate and add codon changed sequences
//            // apply annotations regarding sequences that will be modified on original sequences
//            OligoUtils.applyAnnotationsOnTargetSequences(finalDocument, /*first*/ 0, finalDocument.getNumberOfSequences(), optimizationAnnotationInfoList, false/*original sequences*/);
//            //Consensus on Optimized sequences
//            AnnotatedPluginDocument tempDocWithOptimizedSequencesForConsensus = createTempDocWithOptimizedSequencesForConsensus(documents[0], finalDocument.getNumberOfSequences(), optimizedNucleotideSequences);
//            DefaultNucleotideSequence consensusSequenceDocumentOnOptimizedSequences = createConsensusUsingCustomOptions(tempDocWithOptimizedSequencesForConsensus);//  consensusSequenceDocumentOnOptimizedSequences.setName("Adrian generated consensus");
//            consensusSequenceDocumentOnOptimizedSequences.setName("Optimized Consensus");
//            String consensusSequenceString = consensusSequenceDocumentOnOptimizedSequences.getSequenceString();
//            // checkConsensus(consensusSequenceDocumentOnOptimizedSequences)
//            // apply annotations  on optimized sequences
//            OligoUtils.applyAnnotationsOnTargetSequences(optimizedNucleotideSequences, optimizationAnnotationInfoList, true/*it's for optimized sequences*/);
//            //add newly optimized sequences to the final document
//            finalDocument.addSequence(OligoUtils.emptySequence(sequencesLength));// add a blank separator
//            optimizedNucleotideSequences.forEach(finalDocument::addSequence);
//            finalDocument.addSequence(OligoUtils.emptySequence(sequencesLength));// add a blank separator
//            finalDocument.addSequence(consensusSequenceDocumentOnOptimizedSequences);   // Add consensus
//            finalDocument.addSequence(OligoUtils.emptySequence(sequencesLength));// add a blank separator
//            //Oligo part
//            if (storedDirectedEvolutionOperationOptions.getGenerateOligos()) {
//                logger.fine("Options:" + storedDirectedEvolutionOperationOptions);
//                oligos = generateOligos(consensusSequenceString, storedDirectedEvolutionOperationOptions.getBackwardsAndForwards(), storedDirectedEvolutionOperationOptions.getMinOligoSize());
//                TreeSet<GapBasedOligoModel> gapBasedOligos = null;
//                if (storedDirectedEvolutionOperationOptions.getGenerateGapBasedOligos()) {
//                    gapBasedOligos = generateGapBasedOligos(oligos, optimizedNucleotideSequences);
//                }
//                //exportOligosAsFasta(oligos, "Oligos generated from " + finalDocument.getName()); this is now called from the separate saveOperation
//                AtomicInteger oligoIndex = new AtomicInteger(0);
//                if (storedDirectedEvolutionOperationOptions.getCompactSequences()) {
//                    StringBuilder oligosDirect = new StringBuilder();
//                    StringBuilder oligosComplement = new StringBuilder();
//                    boolean directRow = true;
//                    List<Pair<Integer, Integer>> direct5To3s = new ArrayList<>();
//                    List<Pair<Integer, Integer>> complement5To3s = new ArrayList<>();
//                    List<SequenceAnnotation> directAnnotations = new ArrayList<>();
//                    List<SequenceAnnotation> complementAnnotations = new ArrayList<>();
//                    for (OligoModel oligo : oligos) {
//                        //checkAndAddDuplicateOligo(finalDocument, oligosDirect.length(), oligo);
//                        if (directRow) {//direct
//                            OligoUtils.padEnds(oligosDirect, oligo.getStartIndex() - oligosDirect.length());
//                            createTotalAnnotationForCompactSequences(oligo.getCharSequence().toString(), oligosDirect.length() + 1, directAnnotations);
//                            oligosDirect.append(oligo.getCharSequence());
//                            direct5To3s.add(new Pair<>(oligo.getStartIndex() + 1/*I guess annotation indexes are 1 based*/, oligo.getStartIndex() + oligo.getCharSequence().length()));
//                            directRow = false;
//                        } else {// complements
//                            OligoUtils.padEnds(oligosComplement, oligo.getStartIndex() - oligosComplement.length());
//                            createTotalAnnotationForCompactSequences(oligo.getCharSequence().toString(), oligosComplement.length() + 1, complementAnnotations);
//                            oligosComplement.append(oligo.getCharSequence());
//                            complement5To3s.add(new Pair<>(oligo.getStartIndex() + oligo.getCharSequence().length(), oligo.getStartIndex() + 1/*I guess annotation indexes are 1 based*/));
//                            directRow = true;
//                        }
//                    }
//                    DefaultNucleotideSequence directSequenceDocument = new DefaultNucleotideSequence("Direct Oligos", OligoUtils.padEnds(0, oligosDirect, sequencesLength));
//                    OligoUtils.fiveAndThereAnnotations(directSequenceDocument, direct5To3s);
//                    OligoUtils.annotateSequence(directSequenceDocument, directAnnotations);
//                    finalDocument.addSequence(directSequenceDocument);
//                    DefaultNucleotideSequence complementSequenceDocument = new DefaultNucleotideSequence("Complement Oligos", padEnds(0, complementDNANucleotides(oligosComplement), sequencesLength));
//                    OligoUtils.fiveAndThereAnnotations(complementSequenceDocument, complement5To3s);
//                    OligoUtils.annotateSequence(complementSequenceDocument, complementAnnotations);
//                    finalDocument.addSequence(complementSequenceDocument);
//                } else {// one on each line
//                    oligos.forEach(oligo -> {
//                        //checkAndAddDuplicateOligo(finalDocument, sequencesLength, oligo);
//                        DefaultNucleotideSequence standaloneOligo = new DefaultNucleotideSequence("Oligo " + oligoIndex.incrementAndGet(), oligo.getPaddedCharSequence(sequencesLength));
//                        annotateWithTotal(standaloneOligo);
//                        finalDocument.addSequence(standaloneOligo);
//                    });
//                }  //end else-on-each-line
//                //gapBasedOligos
//                if (storedDirectedEvolutionOperationOptions.getGenerateOligos() && storedDirectedEvolutionOperationOptions.getGenerateGapBasedOligos()) {
//                    gapBasedOligos.forEach(oligoModel -> {
//                        GapBasedOligoModel useOligoModel;
//                        if (!storedDirectedEvolutionOperationOptions.getShowGapsInGapBasedOligos()) {
//                            useOligoModel = oligoModel.getGapBasedOligoModelWithoutGaps();
//                        } else {
//                            useOligoModel = oligoModel;
//                        }
//                        DefaultNucleotideSequence standaloneOligo = new DefaultNucleotideSequence("Gap Based Oligo " + oligoIndex.incrementAndGet(),
//                                useOligoModel.isShouldBeComplemented() ? useOligoModel.getPaddedComplementedCharSequence(sequencesLength) : useOligoModel.getPaddedCharSequence(sequencesLength));
//                        annotateOverlappingEnds(standaloneOligo, useOligoModel);
//                        annotateWithTotal(standaloneOligo);
//                        finalDocument.addSequence(standaloneOligo);
//                    });
//                }
//                //end gapBasedOligos
//                //end adding the oligos
//                if ((storedDirectedEvolutionOperationOptions.getDoubleOptimizedConsensus())) {
//                    //add again the optimized consensus
//                    finalDocument.addSequence(OligoUtils.emptySequence(sequencesLength));
//                    finalDocument.addSequence(consensusSequenceDocumentOnOptimizedSequences); // Add another consensus for easy reading CONSENSUS CONSENSUS
//                }
//            }
//            //just re-wrap and finish
//            if (storedDirectedEvolutionOperationOptions.getSequenceIgnoredDuringOptimization() != -1) {
//                String ignoredSeqName = finalDocument.getSequences().get(storedDirectedEvolutionOperationOptions.getSequenceIgnoredDuringOptimization()).getName();
//                changeName(finalDocument, storedDirectedEvolutionOperationOptions, " with ignored sequence:" + ignoredSeqName);
//            } else {
//                changeName(finalDocument, storedDirectedEvolutionOperationOptions, "");
//            }
//            turnOffConsensusOnNonOptimizedSequences(finalDocument);
//            AnnotatedPluginDocument newAnnotatedPluginDocument = DocumentUtilities.createAnnotatedPluginDocument(finalDocument);
//            //dumpSequencesWithTurnedOffConsensus(finalDocument); //todo see how to turn these off
//            return Collections.singletonList(newAnnotatedPluginDocument);
//        } catch (DocumentOperationException e) {
//            return exitOperationWithError(e.getMessage());
//        } catch (Exception e) {
//            StringWriter out = new StringWriter();
//            e.printStackTrace(new PrintWriter(out));
//            return exitOperationWithError("Exception:" + e.getMessage() + "\n" + out.toString());
//        }
    }


    private static void createTotalAnnotationForCompactSequences(String partialSequence, int startIndex, List<SequenceAnnotation> annotations) {
        //BigInteger total = VariantStatistics.calculateVariant(partialSequence);
        //annotations.add(new SequenceAnnotation(VariantStatistics.formatTotalNumber(total, "Variants: %s"), SequenceAnnotation.TYPE_STATISTICS, new SequenceAnnotationInterval(startIndex, startIndex + partialSequence.length() - 1)));
    }


}