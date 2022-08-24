package directedevolution;

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
import jebl.util.ProgressListener;
import jebl.evolution.sequences.*;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import static directedevolution.DirectedEvolutionOperationOptions.MOST_COMMON_CODON_OPTION;
import static directedevolution.OligoUtils.complementDNANucleotides;
import static directedevolution.OligoUtils.padEnds;
import static directedevolution.VariantStatistics.calculateVariant;

/**
 */
public class GenerateOligosOperation extends DocumentOperation {

    private static String newDocumentName = "Unknown";//only available after changeName!

    private static DirectedEvolutionOperationOptions storedDirectedEvolutionOperationOptions;

    private List<OligoModel> oligos;

    //
    private static final Logger logger = Logger.getLogger(GenerateOligosOperation.class.getName());

    @Override
    public Options getOptions(AnnotatedPluginDocument... documents) {//called by the framework and passed into perform operation
        return new DirectedEvolutionOperationOptions(documents);
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
        this.storedDirectedEvolutionOperationOptions = (DirectedEvolutionOperationOptions) directedEvolutionOperationOptions;
        try {
            // Our selection signature guarantees we have just alignment documents here.
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(OligoUtils.dumpAllDocuments(documents));
            }
            //we accept only alignments in plugin
            DefaultAlignmentDocument finalDocument = ((DefaultAlignmentDocument) DocumentUtilities.duplicateDocument(documents[0], true).getDocumentOrCrash());
            if (!(finalDocument.getSequences().get(0) instanceof NucleotideSequenceDocument)) {//no need to check all sequence types as only one type can be contained in an alignment
                return exitOperationWithError("You need to select an alignment of nucleotide sequences");
            }
            // sanity check for proper lengths. Should never fail.
            Integer sequencesLength = checkLengths(finalDocument.getSequences());
            if (sequencesLength == 0) {
                return exitOperationWithError("Sequences length is either 0 or we have no Sequences selected");
            }
            if (finalDocument.getSequences().stream().filter(s -> s instanceof OptimizedNucleotideSequence).count() > 0L) {
                return exitOperationWithError("Can not re-process a sequence with optimized sequences and Oligos. Please select an Aligned Nucleotide sequence with a translation Alignment.");
            }
            //Generate optimized sequences and multi purpose annotations
            Integer sequenceIgnoredDuringOptimizationIndex = storedDirectedEvolutionOperationOptions.getSequenceIgnoredDuringOptimization();
            MyPair<List<OptimizedNucleotideSequence>, List<List<OptimizationAnnotationInfo>>> listOptimizedSequencesListOptimizationAnnotationInfosPair = generateOptimizedSequences(finalDocument, sequenceIgnoredDuringOptimizationIndex);
            List<OptimizedNucleotideSequence> optimizedNucleotideSequences = listOptimizedSequencesListOptimizationAnnotationInfosPair.getKey();// calculate and add codon changed sequences
            List<List<OptimizationAnnotationInfo>> optimizationAnnotationInfoList = listOptimizedSequencesListOptimizationAnnotationInfosPair.getValue();// calculate and add codon changed sequences
            // apply annotations regarding sequences that will be modified on original sequences
            OligoUtils.applyAnnotationsOnTargetSequences(finalDocument, /*first*/ 0, finalDocument.getNumberOfSequences(), optimizationAnnotationInfoList, false/*original sequences*/);
            //Consensus on Optimized sequences
            AnnotatedPluginDocument tempDocWithOptimizedSequencesForConsensus = createTempDocWithOptimizedSequencesForConsensus(documents[0], finalDocument.getNumberOfSequences(), optimizedNucleotideSequences);
            DefaultNucleotideSequence consensusSequenceDocumentOnOptimizedSequences = createConsensusUsingCustomOptions(tempDocWithOptimizedSequencesForConsensus);//  consensusSequenceDocumentOnOptimizedSequences.setName("Adrian generated consensus");
            consensusSequenceDocumentOnOptimizedSequences.setName("Optimized Consensus");
            String consensusSequenceString = consensusSequenceDocumentOnOptimizedSequences.getSequenceString();
            // checkConsensus(consensusSequenceDocumentOnOptimizedSequences)
            // apply annotations  on optimized sequences
            OligoUtils.applyAnnotationsOnTargetSequences(optimizedNucleotideSequences, optimizationAnnotationInfoList, true/*it's for optimized sequences*/);
            //add newly optimized sequences to the final document
            finalDocument.addSequence(OligoUtils.emptySequence(sequencesLength));// add a blank separator
            optimizedNucleotideSequences.forEach(finalDocument::addSequence);
            finalDocument.addSequence(OligoUtils.emptySequence(sequencesLength));// add a blank separator
            finalDocument.addSequence(consensusSequenceDocumentOnOptimizedSequences);   // Add consensus
            finalDocument.addSequence(OligoUtils.emptySequence(sequencesLength));// add a blank separator
            //Oligo part
            if (storedDirectedEvolutionOperationOptions.getGenerateOligos()) {
                logger.fine("Options:" + storedDirectedEvolutionOperationOptions);
                oligos = generateOligos(consensusSequenceString, storedDirectedEvolutionOperationOptions.getBackwardsAndForwards(), storedDirectedEvolutionOperationOptions.getMinOligoSize());
                TreeSet<GapBasedOligoModel> gapBasedOligos = null;
                if (storedDirectedEvolutionOperationOptions.getGenerateGapBasedOligos()) {
                    gapBasedOligos = generateGapBasedOligos(oligos, optimizedNucleotideSequences);
                }
                //exportOligosAsFasta(oligos, "Oligos generated from " + finalDocument.getName()); this is now called from the separate saveOperation
                AtomicInteger oligoIndex = new AtomicInteger(0);
                if (storedDirectedEvolutionOperationOptions.getCompactSequences()) {
                    StringBuilder oligosDirect = new StringBuilder();
                    StringBuilder oligosComplement = new StringBuilder();
                    boolean directRow = true;
                    List<MyPair<Integer, Integer>> direct5To3s = new ArrayList<>();
                    List<MyPair<Integer, Integer>> complement5To3s = new ArrayList<>();
                    List<SequenceAnnotation> directAnnotations = new ArrayList<>();
                    List<SequenceAnnotation> complementAnnotations = new ArrayList<>();
                    for (OligoModel oligo : oligos) {
                        //checkAndAddDuplicateOligo(finalDocument, oligosDirect.length(), oligo);
                        if (directRow) {//direct
                            OligoUtils.padEnds(oligosDirect, oligo.getStartIndex() - oligosDirect.length());
                            createTotalAnnotationForCompactSequences(oligo.getCharSequence().toString(), oligosDirect.length() + 1, directAnnotations);
                            oligosDirect.append(oligo.getCharSequence());
                            direct5To3s.add(new MyPair<>(oligo.getStartIndex() + 1/*I guess annotation indexes are 1 based*/, oligo.getStartIndex() + oligo.getCharSequence().length()));
                            directRow = false;
                        } else {// complements
                            OligoUtils.padEnds(oligosComplement, oligo.getStartIndex() - oligosComplement.length());
                            createTotalAnnotationForCompactSequences(oligo.getCharSequence().toString(), oligosComplement.length() + 1, complementAnnotations);
                            oligosComplement.append(oligo.getCharSequence());
                            complement5To3s.add(new MyPair<>(oligo.getStartIndex() + oligo.getCharSequence().length(), oligo.getStartIndex() + 1/*I guess annotation indexes are 1 based*/));
                            directRow = true;
                        }
                    }
                    DefaultNucleotideSequence directSequenceDocument = new DefaultNucleotideSequence("Direct Oligos", OligoUtils.padEnds(0, oligosDirect, sequencesLength));
                    OligoUtils.fiveAndThereAnnotations(directSequenceDocument, direct5To3s);
                    OligoUtils.annotateSequence(directSequenceDocument, directAnnotations);
                    finalDocument.addSequence(directSequenceDocument);
                    DefaultNucleotideSequence complementSequenceDocument = new DefaultNucleotideSequence("Complement Oligos", padEnds(0, complementDNANucleotides(oligosComplement), sequencesLength));
                    OligoUtils.fiveAndThereAnnotations(complementSequenceDocument, complement5To3s);
                    OligoUtils.annotateSequence(complementSequenceDocument, complementAnnotations);
                    finalDocument.addSequence(complementSequenceDocument);
                } else {// one on each line
                    oligos.forEach(oligo -> {
                        //checkAndAddDuplicateOligo(finalDocument, sequencesLength, oligo);
                        DefaultNucleotideSequence standaloneOligo = new DefaultNucleotideSequence("Oligo " + oligoIndex.incrementAndGet(), oligo.getPaddedCharSequence(sequencesLength));
                        annotateWithTotal(standaloneOligo);
                        finalDocument.addSequence(standaloneOligo);
                    });
                }  //end else-on-each-line
                //gapBasedOligos
                if (storedDirectedEvolutionOperationOptions.getGenerateOligos() && storedDirectedEvolutionOperationOptions.getGenerateGapBasedOligos()) {
                    gapBasedOligos.forEach(oligoModel -> {
                        GapBasedOligoModel useOligoModel;
                        if (!storedDirectedEvolutionOperationOptions.getShowGapsInGapBasedOligos()) {
                            useOligoModel = oligoModel.getGapBasedOligoModelWithoutGaps();
                        } else {
                            useOligoModel = oligoModel;
                        }
                        DefaultNucleotideSequence standaloneOligo = new DefaultNucleotideSequence("Gap Based Oligo " + oligoIndex.incrementAndGet(),
                                useOligoModel.isShouldBeComplemented() ? useOligoModel.getPaddedComplementedCharSequence(sequencesLength) : useOligoModel.getPaddedCharSequence(sequencesLength));
                        annotateOverlappingEnds(standaloneOligo, useOligoModel);
                        annotateWithTotal(standaloneOligo);
                        finalDocument.addSequence(standaloneOligo);
                    });
                }
                //end gapBasedOligos
                //end adding the oligos
                if ((storedDirectedEvolutionOperationOptions.getDoubleOptimizedConsensus())) {
                    //add again the optimized consensus
                    finalDocument.addSequence(OligoUtils.emptySequence(sequencesLength));
                    finalDocument.addSequence(consensusSequenceDocumentOnOptimizedSequences); // Add another consensus for easy reading CONSENSUS CONSENSUS
                }
            }
            //just re-wrap and finish
            if (storedDirectedEvolutionOperationOptions.getSequenceIgnoredDuringOptimization() != -1) {
                String ignoredSeqName = finalDocument.getSequences().get(storedDirectedEvolutionOperationOptions.getSequenceIgnoredDuringOptimization()).getName();
                changeName(finalDocument, storedDirectedEvolutionOperationOptions, " with ignored sequence:" + ignoredSeqName);
            } else {
                changeName(finalDocument, storedDirectedEvolutionOperationOptions, "");
            }
            turnOffConsensusOnNonOptimizedSequences(finalDocument);
            AnnotatedPluginDocument newAnnotatedPluginDocument = DocumentUtilities.createAnnotatedPluginDocument(finalDocument);
            //dumpSequencesWithTurnedOffConsensus(finalDocument); //todo see how to turn these off
            return Collections.singletonList(newAnnotatedPluginDocument);
        } catch (DocumentOperationException e) {
            return exitOperationWithError(e.getMessage());
        } catch (Exception e) {
            StringWriter out = new StringWriter();
            e.printStackTrace(new PrintWriter(out));
            return exitOperationWithError("Exception:" + e.getMessage() + "\n" + out.toString());
        }
    }

    /**
     * Lat's say the optimized(? TODO or unoptimized) oligos are
     * T---T
     * TC--T
     * TCCAT
     * so valid alternatives are TCCAT, TT, and TCT
     * so for each length of --- we produce a trimmed copy where we remove the letters corresponding to - in the long oligo and we add that to a set
     * <p>
     * we could also have:
     * T---T
     * T-A-T
     * TCCAT
     * so valid alternatives are TCCAT, TT, and TAT so each sequence that has gaps in this interval will produce one and only one alternative
     * <p>
     * TODO:What if we split the oligo in the middle of a gap(s)?
     * Note: Is it possible to have a gap in the overlapping part? That could mess up the hybridization. TODO need to
     * <p>
     * Algorithm: for each optimizedNucleotideSequence, find first gap, find corresponding oligo and create a smaller oligo by removing all the corresponding gaps
     *
     * @param oligos
     * @param optimizedNucleotideSequences
     * @return some new oligos by truncating a concatenation of one or more of the standard oligos
     */
    private TreeSet<GapBasedOligoModel> generateGapBasedOligos(@Nonnull List<OligoModel> oligos, List<OptimizedNucleotideSequence> optimizedNucleotideSequences) {
        TreeSet<GapBasedOligoModel> gapBasedOligos = new TreeSet<>();
        for (int optimizedNucleotideSequenceIndex = 0; optimizedNucleotideSequenceIndex < optimizedNucleotideSequences.size(); optimizedNucleotideSequenceIndex++) {
            OptimizedNucleotideSequence optimizedNucleotideSequence = optimizedNucleotideSequences.get(optimizedNucleotideSequenceIndex);
            Integer startGapIndexInOptimizedSequence = 0;
            String optimizedNucleotideSequenceString = optimizedNucleotideSequence.getCharSequence().toString();
            SequenceGapInformation sequenceGapInformation = optimizedNucleotideSequence.getSequenceGapInformation();
            List<Gap> gapsList = findGapsList(optimizedNucleotideSequence, optimizedNucleotideSequenceIndex);
            for (Gap gap : gapsList) {
                if (logger.getLevel() == Level.FINE) {
                    logger.fine("calculating trimmed oligo for gap:" + gap + " for sequence index:" + startGapIndexInOptimizedSequence + " on optimized sequence:" + optimizedNucleotideSequence);
                }
                ConcatenatedOligoModel matchingOligo = findMatchingConcatenatedOligo(oligos, gap);
                // front actual StartDelta is gap.getStartIndex() - firstOverlappedOligo.getStartIndex()
                // End EndDelta ((gap.getStartIndex() + gap.getSize()) - (lastOverlappedOligo.getStartIndex() + lastOverlappedOligo.getSize()))
                CharSequence charSequence = matchingOligo.getCharSequence().toString();
//                logger.fine("Created special gap-based oligo due to - found in optimizedNucleotideSequence:" + optimizedNucleotideSequence.getName() +
//                        " at startGapIndexInOptimizedSequence:" + startGapIndexInOptimizedSequence + "\nwhere optimizedNucleotideSequenceString:\n" + optimizedNucleotideSequenceString +
//                        " \nand transformed the matching oligo:\n" + matchingOligo.getCharSequence() + " starting at:" + matchingOligo.getStartIndex() + " into:\n" + alternativeOligoString + '\n');
//                if (alternativeOligoString != null) {
                //if(gap.getStartIndex() - matchingOligo.getStartIndex(),  matchingOligo.getStartOverlapSize()){ todo chop oligos
                try {//todo remove completely this try
                    gapBasedOligos.add(new GapBasedOligoModel(charSequence.subSequence(0, gap.getStartIndex() - matchingOligo.getStartIndex())
                            + StringUtils.leftPad("", gap.getSize(), '-') +
                            charSequence.subSequence(gap.getStartIndex() - matchingOligo.getStartIndex() + gap.getSize(), charSequence.length()),/* todo HERE XXXX*/
                            matchingOligo.getStartIndex(),
                        /*the start overhang*/
                            Math.min(gap.getStartIndex() - matchingOligo.getStartIndex(), matchingOligo.getStartOverlapSize()),
                        /*the end overhang*/
                            Math.min((matchingOligo.getStartIndex() + matchingOligo.getCharSequence().length()) - (gap.getStartIndex() + gap.getSize()), matchingOligo.getEndOverlapSize())
                            , gap, matchingOligo.getShouldBeComplemented()));
                } catch (Exception e) {
                    System.out.println();//eat
                }
            }
        }
        return gapBasedOligos;
    }

    // helper
    @Nonnull
    public ConcatenatedOligoModel findMatchingConcatenatedOligo(List<OligoModel> oligos, Gap gap) {
        List<OligoModel> concatenatedOligos = Lists.newArrayList();
        int matchingOligoIndex = -1;
        OligoModel matchingOverlappedOligo = null;
        boolean shouldBeComplemented;
        //finds the first that still have a mrgin of MinOligo size to the left
        for (int oligoIndex = oligos.size() - 1; oligoIndex >= 0; oligoIndex--) {
            //GS<OE for first and GE<=OE-Min. Since  GS<GE<=Oe-Min<OE means that GE<=OE-Min satisfies also the GS<OE condition
            //gap.geEndIndexPlusOne() <= oligo.getEndIndexPlusOne() - storedDirectedEvolutionOperationOptions.getMinOverlap()
            OligoModel oligo = oligos.get(oligoIndex);
            if (oligo.getStartIndex() + (oligoIndex != 0 ? storedDirectedEvolutionOperationOptions.getMinOverlap() : 0) <= gap.getStartIndex()) {
                matchingOverlappedOligo = oligo;
                matchingOligoIndex = oligoIndex;
                shouldBeComplemented = (matchingOligoIndex % 2 == 1);
                concatenatedOligos.add(matchingOverlappedOligo);
                //todo below maybe the minOverlap for the last oligo should be 0;
                if (gap.geEndIndexPlusOne() <= oligo.getEndIndexPlusOne() - storedDirectedEvolutionOperationOptions.getMinOverlap()) {//happy case where the oligo is within gapStart-minOverlapgap and gapEnd+minOverlapgap
                    // Front actual StartDelta is gap.getStartIndex() - matchingOverlappedOligo.getStartIndex()
                    // End EndDelta ((gap.getStartIndex() + gap.getSize()) - (lastOverlappedOligo.getStartIndex() + lastOverlappedOligo.getSize()))
                    return new ConcatenatedOligoModel(ImmutableList.copyOf(concatenatedOligos), shouldBeComplemented);//--------------> JUST one Oligo
                }
                break;
            }
        }//end finding the first suitable oligo
        if (matchingOverlappedOligo == null) {
            throw new RuntimeException("We must have a first matching oligo assertion is not met");
        }
        shouldBeComplemented = (matchingOligoIndex % 2 == 1);
        if (!(matchingOligoIndex == oligos.size() - 1)) {//we are Not at the end
            concatenatedOligos.add(oligos.get(matchingOligoIndex + 1));// add one more which might not be enough
        }
        return new ConcatenatedOligoModel(ImmutableList.copyOf(concatenatedOligos), shouldBeComplemented);
//        if (matchingOverlappedOligo.getEndIndexPlusOne() - (gap.getStartIndex() + gap.getSize()) < (storedDirectedEvolutionOperationOptions.getMinOverlap())) {// End overlap not enough so we append more oligos after
//            if (matchingOligoIndex == oligos.size() - 1) {//we are already at the end
//                //I wonder if we should ever get here since the endOverhang for last oligo should be 0 or -1 or God knows.
//                System.out.println("I wonder if we should ever get here since the endOverhang for last oligo should be 0 or -1 or God knows.");
//                return new ConcatenatedOligoModel(ImmutableList.copyOf(concatenatedOligos), shouldBeComplemented);
//            }else{
//                concatenatedOligos.add(oligos.get(matchingOligoIndex + 1));
//            }
//            return new ConcatenatedOligoModel(ImmutableList.copyOf(concatenatedOligos), shouldBeComplemented);
//        } else {
//            throw new RuntimeException("We must have a Gap that spans moore than 2 oligos");
//        }
    }

    private static void changeName(DefaultAlignmentDocument newDocument, DirectedEvolutionOperationOptions storedDirectedEvolutionOperationOptions, String ignoredSequenceName) {
        String fileNameSuffix = storedDirectedEvolutionOperationOptions.getFileNameSuffix();
        String newDocumentName = (storedDirectedEvolutionOperationOptions.getUseOriginalNameAsPrefix() ? newDocument.getName() : "") +
                ((int) fileNameSuffix.charAt(0) == (int) ' ' ? "" : " ") + fileNameSuffix;
        newDocument.setName((newDocumentName + ignoredSequenceName).trim());
    }

    @Nonnull
    private List<AnnotatedPluginDocument> exitOperationWithError(@Nonnull String s) {
        Dialogs.showMessageDialog(s);
        return Collections.emptyList();
    }

    public static List<Gap> findGapsList(DefaultNucleotideSequence sequence, int sequenceIndex) {
        List<Gap> gapList = new ArrayList<>();
        SequenceCharSequence charSequence = sequence.getCharSequence();
        int index = 0;
        //noinspection NestedAssignment
        while ((index = charSequence.indexOf("-", index)) != -1) {
            int gapSize = 1;
            //noinspection ImplicitNumericConversion
            while ((charSequence.length() > index + gapSize) && charSequence.charAt(index + gapSize) == '-') {
                gapSize++;
            }
            gapList.add(new Gap(index, gapSize, sequence, sequenceIndex));
            index += gapSize;
        }
        return gapList;
    }

    /**
     * @param newDefaultAlignmentDocument Document to add to
     * @param sequenceLengths             lengths
     * @param oligoModel                  oligoModel to check and maybe trim in the middle
     */

    private void checkAndAddDuplicateOligoXXX(DefaultAlignmentDocument newDefaultAlignmentDocument, Integer sequenceLengths, OligoModel oligoModel) {
        Integer oligoStartIndex = oligoModel.getStartIndex();//todo use to check for ----
        DefaultNucleotideSequence duplicateOligo = new DefaultNucleotideSequence("Duplicate Oligo ", oligoModel.getPaddedCharSequence(sequenceLengths));
        annotateWithTotal(duplicateOligo);
        newDefaultAlignmentDocument.addSequence(duplicateOligo);
    }

    private static void annotateWithTotal(DefaultNucleotideSequence standaloneOligo) {
        BigInteger total = VariantStatistics.calculateVariant(standaloneOligo.getCharSequence(), new Interval(0, standaloneOligo.getSequenceLength()));
        //annotate with total
        List<SequenceAnnotation> annotations = new ArrayList<>();
        annotations.add(new SequenceAnnotation(VariantStatistics.formatTotalNumber(total, "Variants: %s"), SequenceAnnotation.TYPE_STATISTICS, new SequenceAnnotationInterval(1, standaloneOligo.getSequenceLength())));//todo can I add it on the beginning?
        OligoUtils.annotateSequence(standaloneOligo, annotations);
    }

    private static void annotateOverlappingEnds(DefaultNucleotideSequence standaloneOligo, GapBasedOligoModel gapBasedOligoModel) {
        List<SequenceAnnotation> annotations = new ArrayList<>();
        //todo mark the other annotation as overhang
        annotations.add(new SequenceAnnotation(gapBasedOligoModel.getStartOverlapSize() + " bases for the start overlap", SequenceAnnotation.TYPE_OVERHANG,
                new SequenceAnnotationInterval(gapBasedOligoModel.getStartIndex(), gapBasedOligoModel.getStartIndex() + gapBasedOligoModel.getStartOverlapSize())));
        annotations.add(new SequenceAnnotation(gapBasedOligoModel.getStartOverlapSize() + "5", SequenceAnnotation.TYPE_UTR_5,
                new SequenceAnnotationInterval(gapBasedOligoModel.getStartIndex(), gapBasedOligoModel.getStartIndex() + 1)));
        annotations.add(new SequenceAnnotation(gapBasedOligoModel.getEndOverlapSize() + " bases for the end overlap", SequenceAnnotation.TYPE_OVERHANG,
                new SequenceAnnotationInterval(gapBasedOligoModel.getStartIndex() + gapBasedOligoModel.getCharSequence().length() - gapBasedOligoModel.getEndOverlapSize() + 1,
                        gapBasedOligoModel.getStartIndex() + gapBasedOligoModel.getCharSequence().length())));
        OligoUtils.annotateSequence(standaloneOligo, annotations);
    }

    private static void createTotalAnnotationForCompactSequences(String partialSequence, int startIndex, List<SequenceAnnotation> annotations) {
        BigInteger total = VariantStatistics.calculateVariant(partialSequence);
        annotations.add(new SequenceAnnotation(VariantStatistics.formatTotalNumber(total, "Variants: %s"), SequenceAnnotation.TYPE_STATISTICS, new SequenceAnnotationInterval(startIndex, startIndex + partialSequence.length() - 1)));
    }

    private AnnotatedPluginDocument createTempDocWithOptimizedSequencesForConsensus(AnnotatedPluginDocument originalAnnotatedPluginDocument, int originalAlignedSequencesNumber, List<OptimizedNucleotideSequence> optimizedSequences) {
        AnnotatedPluginDocument tempOfOriginalAnnotatedPluginDocumentForConsensus = DocumentUtilities.duplicateDocument(originalAnnotatedPluginDocument, true);//we do this so we don't modify the original document
        DefaultAlignmentDocument tempOfOriginalAnnotatedWrappedDocumentForConsensus = (DefaultAlignmentDocument) tempOfOriginalAnnotatedPluginDocumentForConsensus.getDocumentOrCrash();
        for (int i = 0; i < originalAlignedSequencesNumber; i++) {
            tempOfOriginalAnnotatedWrappedDocumentForConsensus.removeSequence(0);
        }
        //add new optimized sequences
        optimizedSequences.forEach(tempOfOriginalAnnotatedWrappedDocumentForConsensus::addSequence);
        return tempOfOriginalAnnotatedPluginDocumentForConsensus;
    }

    private List<OligoModel> generateOligos(String optimizedConsensusSequence, boolean backwardsAndForwards, int minOligoSize) {
        List<OligoModel> oligos = new ArrayList<>(2);
        int lastCutEndsAt = 0;
        int previousOverlapLength = 0;
        while (true) {
            Integer decentOligoSize = storedDirectedEvolutionOperationOptions.getApproximateOligoSize();
            if (decentOligoSize >= optimizedConsensusSequence.length() - lastCutEndsAt) {//boundary case for LAST OLIGO
                oligos.add(new OligoModel(optimizedConsensusSequence.substring(lastCutEndsAt - previousOverlapLength, optimizedConsensusSequence.length()), lastCutEndsAt - previousOverlapLength, previousOverlapLength, OligoModel.ProblemCode.NO_PROBLEM));
                break;
            }
            StartStopScore startStopScore = findAcceptableCut(optimizedConsensusSequence, lastCutEndsAt + decentOligoSize, backwardsAndForwards, minOligoSize);
            if (startStopScore == null) {//Boundary case. Can not find any cut that meets the criteria so take this as the last one
                logger.info("Could not find a good solution after lastCutEndsAt:" + lastCutEndsAt);//todo add an annotation
                oligos.add(new OligoModel(optimizedConsensusSequence.substring(lastCutEndsAt - previousOverlapLength, optimizedConsensusSequence.length()), lastCutEndsAt - previousOverlapLength, previousOverlapLength, OligoModel.ProblemCode.TOO_LONG));
                break;//to test this one
            }
            oligos.add(new OligoModel(optimizedConsensusSequence.substring(lastCutEndsAt - previousOverlapLength, startStopScore.getStop() + 1), lastCutEndsAt - previousOverlapLength, previousOverlapLength, OligoModel.ProblemCode.NO_PROBLEM));
            previousOverlapLength = startStopScore.getOverlapLength();
            lastCutEndsAt = startStopScore.getStop() + 1;
        }
        for (int i = 0; i < oligos.size() - 1; i++) {
            oligos.get(i).setEndOverlapSize(oligos.get(i + 1).getStartOverlapSize());
        }
        return oligos;
    }

    @Nullable
    private StartStopScore findAcceptableCut(String bigSequence, int decentOligoSize, boolean backwardsAndForwards, int minOligoSize) {
        int currentSpot = decentOligoSize;
        while (currentSpot < bigSequence.length()) {
            StartStopScore startStopScore = findStartStopScore(currentSpot, bigSequence, backwardsAndForwards, minOligoSize);
            if (startStopScore != null && startStopScore.getScore() >= storedDirectedEvolutionOperationOptions.getMinOverlap()) {
                logger.fine("Found a score:" + startStopScore.getScore() + " and a good place to cut:" + startStopScore.getStart() + "," + startStopScore.getStop() + " for:" + bigSequence.substring(startStopScore.getStart(), startStopScore.getStop() + 1) + " out of:" + bigSequence);
                return startStopScore;
            }
            currentSpot++;
        }
        return null;
    }

    private StartStopScore findStartStopScore(final int currentSpot, String bigSequence, boolean backwardsAndForwards, int minOligoSize) {
        int score = 0;
        int start = currentSpot;
        int stop = currentSpot;
        int maxUsefulOverlap = 19;
        if (getMatchScore(bigSequence.charAt(0)) == 0) {
            return null;
        }
        if (backwardsAndForwards) {
            //going backwards
            for (int i = currentSpot; i > minOligoSize; i--) {
                if (getMatchScore(bigSequence.charAt(i)) == 0) {
                    start = i + 1;//reposition start on last non ambiguous nucleotide
                    break;
                }
                start = i;// a good nucleotide
                score++;
                if (score >= maxUsefulOverlap) {
                    return new StartStopScore(start, currentSpot, score);
                }
            }
        }
        logger.finer("\nsecond half ");
        //going forwards
        if (currentSpot < bigSequence.length()) {
            for (int i = currentSpot + 1; i < bigSequence.length(); i++) {
                logger.finest(i + ":");
                if (getMatchScore(bigSequence.charAt(i)) == 0) {
                    break;
                }
                stop = i;
                score++;
                if (score >= maxUsefulOverlap) {
                    return new StartStopScore(start, stop, score);
                }
            }
        } else {
            stop = currentSpot;
        }
        return new StartStopScore(start, stop, score);
    }

    private int getMatchScore(char c) {
        logger.finest("calculating matchScore for:" + c);
        if ('A' == c || 'T' == c || 'G' == c || 'C' == c) {
            return 1;
        }
        return 0;
    }

    private DefaultNucleotideSequence createConsensusUsingCustomOptions(AnnotatedPluginDocument annotatedPluginDocumentContainingOnlyTargetSequences) throws DocumentOperationException {
        DocumentOperation generate_consensus_operation = PluginUtilities.getDocumentOperation("Generate_Consensus");
        Options consensusOptions = generate_consensus_operation.getOptions(annotatedPluginDocumentContainingOnlyTargetSequences);
        if (!consensusOptions.setValue("thresholdPercent", "100")) {
            throw new DocumentOperationException("Failed to set threshold");
        }
        if (!consensusOptions.setValue("noConsensusGaps", Boolean.TRUE)) {
            throw new DocumentOperationException("Failed to set ignore gaps");
        }
        if (!consensusOptions.setValue("textToAppend", "Regenerated consensus with 100 threshold and noConsensusGaps")) {
            throw new DocumentOperationException("Failed to set textToAppend");
        }
        List<AnnotatedPluginDocument> newAnnotatedPluginDocuments = generate_consensus_operation.performOperation(ProgressListener.EMPTY, consensusOptions, annotatedPluginDocumentContainingOnlyTargetSequences);
        PluginDocument documentOrCrash = newAnnotatedPluginDocuments.get(0).getDocumentOrCrash();
        logger.info("Generated Consensus using custom storedDirectedEvolutionOperationOptions is:" + ((SequenceDocument) documentOrCrash).getSequenceString());
        return (DefaultNucleotideSequence) documentOrCrash;
    }

    @Nonnull
    private Integer checkLengths(List<SequenceDocument> sequenceDocuments) {
        if (sequenceDocuments.size() == 0) {
            logger.fine("No sequences selected");
            return 0;
        }
        int len = sequenceDocuments.get(0).getSequenceLength();
        if (sequenceDocuments.stream().filter(sequence -> (sequence.getSequenceLength() != len)).count() > 0) {
            logger.fine("Original sequences have more than one length");
            return null;
        }
        return len;
    }

    public static MyPair<List<OptimizedNucleotideSequence>, List<List<OptimizationAnnotationInfo>>> generateOptimizedSequences(DefaultAlignmentDocument copyOfOriginalDefaultAlignmentDocument,
                                                                                                                             int ignoredSequence) throws Exception {
        // List<SequenceDocument> optimizedSequences = new ArrayList<>(copyOfOriginalDefaultAlignmentDocument.getSequences().size());
        int commonSequenceLength = copyOfOriginalDefaultAlignmentDocument.getSequences().get(0).getSequenceLength();
        if (commonSequenceLength % 3 != 0) {
            Dialogs.showMessageDialog("To optimize we need the length to have  only complete groups of 3 bases so the length should be divisible by 3 and it's not:" + commonSequenceLength);
            throw new DocumentOperationException("To optimize we need the length to have  only complete groups of 3 bases so the length should be divisible by 3 and it's not:" + commonSequenceLength);
        }
        int numberOfSequences = copyOfOriginalDefaultAlignmentDocument.getSequences().size();
//        List<CharSequence> translations = copyOfOriginalDefaultAlignmentDocument.getSequences().stream().map(sequence ->
//                SequenceUtilities.asTranslation(sequence.getSequenceString(), GeneticCode.UNIVERSAL, false)).collect(Collectors.toList());
        List<List<OptimizationAnnotationInfo>> optimizationAnnotationInfosForAllSequencesList = new ArrayList<>(commonSequenceLength / 3 + 1);
        for (int sequenceNumber = 0; sequenceNumber < numberOfSequences; sequenceNumber++) {
            optimizationAnnotationInfosForAllSequencesList.add(new ArrayList<>());
        }
        //put all codons for all sequences in a Big Structure
        List<CodonState[]> codonArraysForAllSequences = new ArrayList<>(numberOfSequences);//todo below use map and return the list directly
        copyOfOriginalDefaultAlignmentDocument.getSequences().stream().map(sequence -> Codons.toStateArray(sequence.getSequenceString())).forEach(codonArraysForAllSequences::add);
        //
        for (int currentAminoAcidPosition = 0; currentAminoAcidPosition < commonSequenceLength / 3; currentAminoAcidPosition++) {
            logger.finest("Processing current Amino Acid (Codon) position:" + currentAminoAcidPosition);
            ///walk the codonArraysForAllSequences for all sequences
            Map<String, LinkedHashMap<String, Integer>> aminoAcidsMap = new HashMap<>();//the key is the one letter for AminoAcid like M
            for (int sequenceNumber = 0; sequenceNumber < numberOfSequences; sequenceNumber++) {
                CodonState codon = codonArraysForAllSequences.get(sequenceNumber)[currentAminoAcidPosition];
                if (codon == null) {//todo this is probably wrong
                    throw new Exception("Codon:" + codon + " found in sequence:" + sequenceNumber + " at position:" + currentAminoAcidPosition + " is not a valid codon.\n Probably you used an initial sequence who had gaps.");
                }
                AminoAcidState acidForCurrentSequenceAtCurrentAminoacidPosition = OligoUtils.toAminoAcid(codon);
                if (acidForCurrentSequenceAtCurrentAminoacidPosition == null) {// we found a region with --- and we ignore it
                    continue;
                }
                int incrementWith = sequenceNumber != ignoredSequence ? 1 : 0;
                if (aminoAcidsMap.containsKey(acidForCurrentSequenceAtCurrentAminoacidPosition.getCode())) {//increment score by 1 or 0 if this is an ignored sequence
                    LinkedHashMap<String, Integer> codonCodeScoreHashMap = aminoAcidsMap.get(acidForCurrentSequenceAtCurrentAminoacidPosition.getCode());//unused but could be useful later if we want to show some score
                    if (codonCodeScoreHashMap.containsKey(codon.getCode())) {
                        codonCodeScoreHashMap.replace(codon.getCode(), incrementWith + codonCodeScoreHashMap.get(codon.getCode()));//increase the score for the codon by 1
                    } else {
                        codonCodeScoreHashMap.put(codon.getCode(), incrementWith);//register new codon at this horizontal position and initialize it's score with one
                    }
                } else {
                    LinkedHashMap<String, Integer> nucleotides = new LinkedHashMap<>();
                    nucleotides.put(codon.getCode(), incrementWith);//getCode gets 3 letters like ATG
                    aminoAcidsMap.put(acidForCurrentSequenceAtCurrentAminoacidPosition.getCode(), nucleotides);
                }
            }//end for each sequence
            //calculate the amino scores and determine the correct substitution for current amino acid position
            logger.finest("Calculate most frequent AminoAcid codons for the specific horizontal position");
            Map<String, String> finalMap = new HashMap<>();//todo performance we don't need to calculate the final map if the optimization option is not MOST_COMMON_CODON
            for (String aminoAcidKeyLetter : aminoAcidsMap.keySet()) {
                if (aminoAcidKeyLetter.equals("-")) {
                    continue;//------------------------------------->
                }
                LinkedHashMap<String, Integer> scorePerAminoAcidAndCodonMap = aminoAcidsMap.get(aminoAcidKeyLetter);
                String maxCodonKeyLetters = "---";
                int maxCodonTally = 0;
                for (String codonKeyLetters : scorePerAminoAcidAndCodonMap.keySet()) {
                    if (scorePerAminoAcidAndCodonMap.get(codonKeyLetters) > maxCodonTally) {//todo if use last change the sign  to >= //todo (scorePerAminoAcidAndCodonMap.get(codonKeyLetters) should not count ignoredSequence
                        maxCodonTally = scorePerAminoAcidAndCodonMap.get(codonKeyLetters);
                        maxCodonKeyLetters = codonKeyLetters;
                    }
                }
                finalMap.put(aminoAcidKeyLetter, maxCodonKeyLetters);
                logger.finest("For currentNucleotidePosition:" + (currentAminoAcidPosition * 3 + 1) + " Amino acid:" + aminoAcidKeyLetter + " should be encoded like:" + finalMap.get(aminoAcidKeyLetter));
            }
            //substitute. For each sequence check the AA on translated and replace if necessary the codonCode
            logger.finest("Substitute");
            for (int sequenceNumber = 0; sequenceNumber < numberOfSequences; sequenceNumber++) {//change the codonArraysForAllSequences
                AminoAcidState acidForCurrentSequenceAtCurrentAminoacidPosition = OligoUtils.toAminoAcid(codonArraysForAllSequences.get(sequenceNumber)[currentAminoAcidPosition]);
                if (acidForCurrentSequenceAtCurrentAminoacidPosition == null) {// we found a region with --- and we ignore it
                    continue;
                }
                CodonState[] codonsForSequence = codonArraysForAllSequences.get(sequenceNumber);
                String rightCodonCode = calculateNewCodonCode(finalMap, acidForCurrentSequenceAtCurrentAminoacidPosition);
                if (codonsForSequence[currentAminoAcidPosition] != Codons.getState(rightCodonCode) && !rightCodonCode.equals("---")) {
                    logger.finest("Replacing:" + codonsForSequence[currentAminoAcidPosition] + " with:" + rightCodonCode +
                            " for:" + acidForCurrentSequenceAtCurrentAminoacidPosition.getCode() + "(" + acidForCurrentSequenceAtCurrentAminoacidPosition.getThreeLetterName() + ") in sequence:"
                            + sequenceNumber + " at start codon( nucleotide offset) position:" + currentAminoAcidPosition * 3 + 1);
                    optimizationAnnotationInfosForAllSequencesList.get(sequenceNumber).add(
                            new OptimizationAnnotationInfo(currentAminoAcidPosition * 3, codonsForSequence[currentAminoAcidPosition], rightCodonCode, acidForCurrentSequenceAtCurrentAminoacidPosition));
                    codonsForSequence[currentAminoAcidPosition] = Codons.getState(rightCodonCode);
                }
            }
        }//end big for loop
        //reassemble all sequences from all codons
        AtomicInteger atomicIndex = new AtomicInteger(0);
        List<OptimizedNucleotideSequence> optimizedNucleotideSequences =
                codonArraysForAllSequences.stream().map(arr -> new OptimizedNucleotideSequence(copyOfOriginalDefaultAlignmentDocument.getSequences().get(atomicIndex.getAndIncrement()).getName() + " Optimized", Utils.toString(arr)))
                        .collect(Collectors.toList());
        copyOfOriginalDefaultAlignmentDocument.getSequences().stream().map(sequence -> Codons.toStateArray(sequence.getSequenceString())).forEach(codonArraysForAllSequences::add);
        Preferences.userRoot().node("com/biomatters/geneious/publicapi/plugin/Options/com/biomatters/geneious/common/consensusSequence/ConsensusOptions").putBoolean("ignoreReadsMappedToMultipleLocations", true);
        return new MyPair<>(optimizedNucleotideSequences, optimizationAnnotationInfosForAllSequencesList);
    }

    private static String calculateNewCodonCode(Map<String, String> finalMap, AminoAcidState acidForCurrentSequenceAtCurrentAminoacidPosition) {
        String rightCodonCode = finalMap.get(acidForCurrentSequenceAtCurrentAminoacidPosition.getCode());
        switch (storedDirectedEvolutionOperationOptions.getOptimizationType()) {
            case MOST_COMMON_CODON_OPTION:
            default:
                rightCodonCode = finalMap.get(acidForCurrentSequenceAtCurrentAminoacidPosition.getCode());
        }
        return rightCodonCode;
    }

    static void exportOligosAsFasta(List<OligoModel> oligos, String fileName, ProgressListener progressListener) {
        File file = getFileFromLastOligoSaveLocation(fileName);
        AtomicInteger oligoIndex = new AtomicInteger(0);
        List<AnnotatedPluginDocument> pluginDocuments = new ArrayList<>(oligos.size());
        oligos.forEach(oligo -> {
            //checkAndAddDuplicateOligo(newDefaultAlignmentDocument, sequencesLength, oligo);
            DefaultNucleotideSequence standaloneOligo = new DefaultNucleotideSequence("Oligo " + oligoIndex.incrementAndGet(), oligo.getCharSequence());
            BigInteger total = calculateVariant(standaloneOligo.getCharSequence(), new Interval(0, standaloneOligo.getSequenceLength()));
            standaloneOligo.setName(standaloneOligo.getName() + " " + total);
            annotateWithTotal(standaloneOligo);
            AnnotatedPluginDocument document = DocumentUtilities.createAnnotatedPluginDocument(standaloneOligo);
            pluginDocuments.add(document);
        });
        //BasicProgressListener progressListener = new BasicProgressListener();//todo what to do with this?
        ExampleFastaExporter fastaExporter = new ExampleFastaExporter();
        AnnotatedPluginDocument[] documentsToExport = pluginDocuments.toArray(new AnnotatedPluginDocument[0]);
        Options options = fastaExporter.getOptions(documentsToExport);
        try {
            fastaExporter.export(file, documentsToExport, progressListener, options); //also could try ExampleFastaExporter
        } catch (IOException e) {
            StringWriter out = new StringWriter();
            e.printStackTrace(new PrintWriter(out));
            logger.severe("Export oligos failed due to:\n" + out.toString());
        }
    }

    private void turnOffConsensusOnNonOptimizedSequences(SequenceAlignmentDocument sequences) {//it seems it turns off everything at this point but leaves something todo
        for (int i = 0; i < sequences.getNumberOfSequences(); i++) {
            SequenceDocument sequence = sequences.getSequence(i);
            if (!(sequence instanceof OptimizedNucleotideSequence)) {
                if (!sequences.canSetFieldValue(i, DocumentField.NUMBER_OF_MAPPED_LOCATIONS)) {
                    logger.fine("PROBLEM turning off participation for sequence:" + sequence.getName());
                }
                sequences.setFieldValue(i, DocumentField.NUMBER_OF_MAPPED_LOCATIONS, 2, false/*if this field change may be propagated to a referenced sequence*/);
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("I'm trying to turn off participation in consensus for Sequence:" + sequence.getName() + " by faking it to be mapped to more than one location");
                }
            }
        }
        OligoUtils.dumpSequencesWithTurnedOffConsensus(sequences);
    }
//maybe interesting
//for consensus search for "Merge mapped sequences"
//////////////////////////////////////// newDefaultAlignmentDocument.addSequence(consensusSequenceDocumentOnOptimizedSequences);
//int x=AminoAcid.baseToNumber[bases[cloc]];

    /**
     * The action options specify that this operation is available from the Tools menu.
     */
    public GeneiousActionOptions getActionOptions() {
        return new GeneiousActionOptions("Generate Oligos...").
                setMainMenuLocation(GeneiousActionOptions.MainMenu.Tools).
                setInMainToolbar(true).setShortcutKey(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
    }

    public String getHelp() {
        return "Generates a list of oligonucleotide sequences for directed evolution based on a translation alignment on variant nucleotides sequences we want to evolve.";
    }

    private static File getFileFromLastOligoSaveLocation(String fileName) {
        Preferences prefNode = Preferences.userRoot().node("/com/biomatters/utilities");
        String lastDirectoryPath = prefNode.get("lastOligoSaveLocation", prefNode.get("currentDirectoryPath", null));
        File savedFile = FileUtilities.getUserSelectedSaveFile("Save", "Select Save Location and Format", lastDirectoryPath + "/" + fileName/* or System.getProperty("user.home")*/, ".fasta");
        if (savedFile != null) {
            prefNode.put("lastOligoSaveLocation", savedFile.getParentFile().getPath());
        }
        return savedFile;
        //set breakpoint in exportDocumentOperation:89
//        List<PluginPreferences> pref=getGeneiousPlugin.getPluginPreferences();
//        String defaultPath=pref.get("currentDirectoryPath", (String)null)
//        JFileChooser fileChooser=new JFileChooser(defaultPath);
//        String extension=".fasta"
//        fileChooser.setSelectedFile(new File(defaultPath, extension))
//        fileChooser.setDialogType(var2);
//        fileChooser.setFileSelectionMode(var3);
//        fileChooser.setDialogTitle(1);//todod
//        fileChooser.setApproveButtonText("setApproveButtonText");
//
//
//        fileChooser   setDialogType(1)
//        Window window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
//        if(window == null || !window.isShowing()){
//            window= JOptionPane.getRootFrame());
//        }
//        fileChooser.showDialog(window, "Export");
//
//
//        File selectedFile=  fileChooser.getSelectedFile();
    }

    public List<OligoModel> getOligos() {
        return oligos;
    }

    public static String getNewDocumentName() {
        return newDocumentName;
    }

    public void exportDocument(List<OligoModel> oligos) {//todo use this for exporting the oligos
        AtomicInteger oligoIndex = new AtomicInteger(0);
        oligos.forEach(oligo -> {
            //checkAndAddDuplicateOligo(newDefaultAlignmentDocument, sequencesLength, oligo);
            DefaultNucleotideSequence standaloneOligo = new DefaultNucleotideSequence("Oligo " + oligoIndex.incrementAndGet(), oligo.getCharSequence());
            BigInteger total = calculateVariant(standaloneOligo.getCharSequence(), new Interval(0, standaloneOligo.getSequenceLength()));
            standaloneOligo.setName(standaloneOligo.getName() + " total variants:" + total);
            annotateWithTotal(standaloneOligo);
            AnnotatedPluginDocument document = DocumentUtilities.createAnnotatedPluginDocument(standaloneOligo);
            try {
                PluginUtilities.exportDocuments(new File("C:junk/" + standaloneOligo.getName() + ".fasta"), document);
            } catch (IOException e) {
                StringWriter out = new StringWriter();
                e.printStackTrace(new PrintWriter(out));
                logger.severe("Export oligos failed due to:\n" + out.toString());
            }
        });
    }

    void setOptions(DirectedEvolutionOperationOptions storedDirectedEvolutionOperationOptions) {//todo this should be private
        this.storedDirectedEvolutionOperationOptions = storedDirectedEvolutionOperationOptions;
    }

    private static class StartStopScore {
        final int start;
        final int stop;
        final Integer score;

        public StartStopScore(int start, int stop, Integer score) {
            this.start = start;
            this.stop = stop;
            this.score = score;
        }

        public int getStart() {
            return start;
        }

        public int getStop() {
            return stop;
        }

        public Integer getScore() {
            return score;
        }

        public int getOverlapLength() {
            return score;//todo calculate it
        }
    }

    public static class OptimizationAnnotationInfo {

        final Interval interval;
        static final String textFormatForOriginal = "%s will be the new value changed from %s and it encodes: %s (%s) %s";
        static final String textFormatForOptimized = "%s was changed to %s in the optimized sequence and it encodes: %s (%s) %s";
        final CodonState codonForOriginalValue;
        final AminoAcidState aminoAcidState;
        final String codonForOptimizedValue;

        public OptimizationAnnotationInfo(int startNucleotide, CodonState originalCodon, String codonForOptimizedValue, AminoAcidState aminoAcidState) {
            this.interval = new Interval(startNucleotide, startNucleotide + 3);
            this.codonForOriginalValue = originalCodon;
            this.codonForOptimizedValue = codonForOptimizedValue;
            this.aminoAcidState = aminoAcidState;
        }

        public Interval getInterval() {
            return interval;
        }

        public String getText(boolean isOptimized) {
            if (isOptimized) {
                return String.format(textFormatForOptimized, codonForOriginalValue, codonForOptimizedValue, aminoAcidState.getCode(), aminoAcidState.getThreeLetterName(), aminoAcidState.getFullName());
            } else {
                return String.format(textFormatForOriginal, codonForOptimizedValue, codonForOriginalValue, aminoAcidState.getCode(), aminoAcidState.getThreeLetterName(), aminoAcidState.getFullName());
            }
        }
    }
}