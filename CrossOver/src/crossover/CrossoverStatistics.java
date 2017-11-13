package crossover;

import com.biomatters.geneious.publicapi.documents.sequence.SequenceCharSequence;
import com.biomatters.geneious.publicapi.documents.sequence.SequenceDocument;
import com.biomatters.geneious.publicapi.implementations.sequence.DefaultNucleotideSequence;
import com.biomatters.geneious.publicapi.plugin.SequenceViewerExtension;
import com.google.common.collect.Lists;
import jebl.util.ProgressListener;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static crossover.CrossoverUtil.standardDeviation;

public class CrossoverStatistics extends SequenceViewerExtension.StatisticsFactory {
    private static final double VERTICAL_POSITION = SequenceViewerExtension.StatisticsSection.POSITION_PERCENTAGE_PAIRWISE_IDENTITY + SequenceViewerExtension.StatisticsSection.RELATIVE_POSITION_DIRECTLY_BELOW; // Make it appear just below
    private static final Logger logger = Logger.getLogger(CrossoverStatistics.class.getName());
    boolean chimeraIsFirst;

    public CrossoverStatistics(boolean chimeraIsFirst) {
        this.chimeraIsFirst=chimeraIsFirst;
    }

    @Override
    public List<SequenceViewerExtension.StatisticsSection> createStatistics(SequenceViewerExtension.PropertyRetrieverAndEditor propertyRetriever, ProgressListener progressListener) {
        if (!propertyRetriever.isAlignmentOrContig()) {
            return Collections.emptyList();//bail out unceremoniously
        }
        if (CrossOverPlugin.shouldNotBeEnabled(propertyRetriever.getAnnotatedPluginDocument(0))) return Collections.emptyList();
        try {
            List<SequenceDocument> allSequences = Lists.newArrayList();
            int sequencesNumber = propertyRetriever.getNumberOfSequences();
            CharSequence chimera;
            if (chimeraIsFirst) {
                allSequences.add(new DefaultNucleotideSequence("Chimera", propertyRetriever.getSequenceCharSequence(0)));
                for (int i = 1; i < sequencesNumber; i++) {
                    SequenceCharSequence sequenceCharSequence = propertyRetriever.getSequenceCharSequence(i);
                    allSequences.add(new DefaultNucleotideSequence(propertyRetriever.getSequenceName(i), sequenceCharSequence));
                }
            } else {
                for (int i = 0; i < sequencesNumber - 1; i++) {
                    SequenceCharSequence sequenceCharSequence = propertyRetriever.getSequenceCharSequence(i);
                    allSequences.add(new DefaultNucleotideSequence(propertyRetriever.getSequenceName(i), sequenceCharSequence));
                }
                allSequences.add(new DefaultNucleotideSequence("Chimera", propertyRetriever.getSequenceCharSequence(sequencesNumber - 1)));
            }
            crossover.CrossoverModel crossoverModel = crossover.CrossoverModel.getInstance(false, allSequences, chimeraIsFirst);
            crossoverModel.calculateStatisticsIfNotAlreadyIn();
            return createStatisticsSections(crossoverModel);
        } catch (Exception e) {
            e.printStackTrace();
            logger.fine(e.getMessage());
            return notGoodSelection("We don't know" + e.getLocalizedMessage());
        }
    }

    @Nonnull
    private static List<SequenceViewerExtension.StatisticsSection> createStatisticsSections(CrossoverModel crossoverModel) {
        List<SequenceDocument> parentsAsFullNucleotideSequences = crossoverModel.getParentsAsFullNucleotideSequences();
        double verticalPosition = VERTICAL_POSITION;
        SequenceViewerExtension.StatisticsSection topSeparator = new SequenceViewerExtension.StatisticsSection("topseparator", null, "", verticalPosition);
        verticalPosition += SequenceViewerExtension.StatisticsSection.RELATIVE_POSITION_DIRECTLY_BELOW;
        SequenceViewerExtension.StatisticsSection sectionTitle = new SequenceViewerExtension.StatisticsSection("header", -1, "Crossover Statistics: ", verticalPosition);
        verticalPosition += SequenceViewerExtension.StatisticsSection.RELATIVE_POSITION_BELOW;
        SequenceViewerExtension.StatisticsSection sectionSeparator = new SequenceViewerExtension.StatisticsSection("separator", null, "", verticalPosition);
        verticalPosition += SequenceViewerExtension.StatisticsSection.RELATIVE_POSITION_DIRECTLY_BELOW;
        SequenceViewerExtension.StatisticsSection sectionCrossoverDiscountingMutations = new SequenceViewerExtension.StatisticsSection("crossoverDiscountingMutation", crossoverModel.getCrossoverDiscountingMutation(), "Crossover Number Discounting Mutations: " + crossoverModel.getCrossoverDiscountingMutation(), verticalPosition);
        verticalPosition += SequenceViewerExtension.StatisticsSection.RELATIVE_POSITION_DIRECTLY_BELOW;
        SequenceViewerExtension.StatisticsSection sectionCrossover = new SequenceViewerExtension.StatisticsSection("crossoverConsideringMutations", crossoverModel.getCrossoverConsideringMutations(), "Crossover Number Considering Mutations: " + crossoverModel.getCrossoverConsideringMutations(), verticalPosition);
        verticalPosition += SequenceViewerExtension.StatisticsSection.RELATIVE_POSITION_DIRECTLY_BELOW;
        SequenceViewerExtension.StatisticsSection sectionPointMutations = new SequenceViewerExtension.StatisticsSection("pointmutations", crossoverModel.getPointMutations(), "Point Mutations: " + crossoverModel.getPointMutations(), verticalPosition);
        verticalPosition += SequenceViewerExtension.StatisticsSection.RELATIVE_POSITION_DIRECTLY_BELOW;
        List<SequenceViewerExtension.StatisticsSection> statisticsSections = Stream.of(topSeparator, sectionTitle, sectionSeparator, sectionCrossoverDiscountingMutations, sectionCrossover, sectionPointMutations).collect(Collectors.toList());
        ArrayList<Integer> levenshteinDistances = crossoverModel.getLevenshteinDistances();
        int effectiveMutation = Integer.MAX_VALUE;
        String effectiveMutationSequence = "";
        for (int i = 0; i < levenshteinDistances.size(); i++) {
            Integer distance = levenshteinDistances.get(i);
            if (distance < effectiveMutation) {
                effectiveMutation = distance;
                effectiveMutationSequence = parentsAsFullNucleotideSequences.get(i).getName();
            }
            statisticsSections.add(new SequenceViewerExtension.StatisticsSection("levenshteinDistance", distance, "Levenshtein for " + parentsAsFullNucleotideSequences.get(i).getName() + ": " + distance, verticalPosition));
            verticalPosition += SequenceViewerExtension.StatisticsSection.RELATIVE_POSITION_DIRECTLY_BELOW;
        }
        statisticsSections.add(new SequenceViewerExtension.StatisticsSection("effectiveMutation", effectiveMutation, "Effective Mutation: " + effectiveMutation + " for " + effectiveMutationSequence, verticalPosition));
        verticalPosition += SequenceViewerExtension.StatisticsSection.RELATIVE_POSITION_DIRECTLY_BELOW;
        ArrayList<Integer> fragments = crossoverModel.getUniqueLengthsUngappedFragments();
        double meanSizeOfFrag = fragments.stream().mapToInt(i -> i).average().getAsDouble();
        statisticsSections.add(new SequenceViewerExtension.StatisticsSection("fragments", meanSizeOfFrag, "Fragments:" + fragments.toString(), verticalPosition));
        verticalPosition += SequenceViewerExtension.StatisticsSection.RELATIVE_POSITION_DIRECTLY_BELOW;
        statisticsSections.add(new SequenceViewerExtension.StatisticsSection("meansizeoffragments", meanSizeOfFrag, "Mean Size of Fragments: " + String.format("%.2f", meanSizeOfFrag), verticalPosition));
        verticalPosition += SequenceViewerExtension.StatisticsSection.RELATIVE_POSITION_DIRECTLY_BELOW;
        int minimumSizeOfFrag = fragments.stream().mapToInt(i -> i).min().getAsInt();
        statisticsSections.add(new SequenceViewerExtension.StatisticsSection("minsizeoffragments", meanSizeOfFrag, "Minimum Size of Fragments: " + minimumSizeOfFrag, verticalPosition));
        verticalPosition += SequenceViewerExtension.StatisticsSection.RELATIVE_POSITION_DIRECTLY_BELOW;
        int maximumSizeOfFrag = fragments.stream().mapToInt(i -> i).max().getAsInt();
        statisticsSections.add(new SequenceViewerExtension.StatisticsSection("maxsizeofragments", meanSizeOfFrag, "Maximum Size of Fragments: " + maximumSizeOfFrag, verticalPosition));
        verticalPosition += SequenceViewerExtension.StatisticsSection.RELATIVE_POSITION_DIRECTLY_BELOW;
        double stdDeviation = standardDeviation(fragments);
        statisticsSections.add(new SequenceViewerExtension.StatisticsSection("stddeviation", stdDeviation, "Standard Deviation: " + String.format("%.2f", stdDeviation), verticalPosition));
        return statisticsSections;
    }

    private List<SequenceViewerExtension.StatisticsSection> notGoodSelection(String cause) {
        logger.fine("Not good selection for calculating crossover statistics and cause is:" + cause);
        return Collections.emptyList();
    }

    private static String getText(String format, String number) {
        return String.format(format, number);
    }
    /**
     * Compare 2 strings and returns a string containing the result of the comparison.
     *
     * @param string0 first string to compare
     * @param string2 second string to compare
     * @return result of the comparison
     */
//            if (!propertyRetriever.isAlignmentOrContig()) {
//                return notGoodSelection("Total Variants can be calculated only on alignments of proteins or optimized nucleotide consensus");
//            }
//            Map<Interval, List<Interval>> selectionForAlignmentStatistics = propertyRetriever.getSelectionForAlignmentStatistics();
//            List<SequenceSelection.SelectionInterval> intervals = propertyRetriever.getSelectionForStatistics().getIntervals(false);
//            if (intervals.size() != 1) {
//                return notGoodSelection("Total variants could only be calculated on one interval and now we have " + intervals.size() + " selected");
//            } else if (intervals.get(0).getSequenceIndices().size() == 0) {
//                return notGoodSelection("Total variants could only be calculated when at least one sequence is selected and now we have " + intervals.get(0).getSequenceIndices().size() + " selected");
//            }
//            if (intervals.get(0).getSequenceIndices().size() == 1) {//must be nucleotide
//                String sequenceName = propertyRetriever.getSequenceName(propertyRetriever.getSelectionForStatistics().getIntervals(false).get(0).getSequenceIndices().get(0).getSequenceIndex());
//                if (intervals.get(0).getSequenceIndices().get(0).getSequenceType() != NUCLEOTIDE) {
//                    return notGoodSelection("Total variants on one sequence can only be calculated on nucleotide sequences and the first sequence named " + sequenceName +
//                            " is " + intervals.get(0).getSequenceIndices().get(0).getSequenceType().toString());
//                } else {
//                    total = calculateVariantTotalForNucleotides(propertyRetriever, propertyRetriever.getSequenceCharSequence(intervals.get(0).getSequenceIndices().get(0).getSequenceIndex()));
//                }
//            } else {//must be aminoacid
//                if (intervals.get(0).getSequenceIndices().get(0).getSequenceType() != AMINO_ACID) {
//                    return notGoodSelection("Total variants on more than one sequence can be calculated on more than two Amino Acid sequences and now we have "
//                            + intervals.get(0).getSequenceIndices().size() + " sequences of type " + intervals.get(0).getSequenceIndices().get(0).getSequenceType().toString());
//                } else {
//                    total = calculateVariantTotalForProtein(propertyRetriever, selectionForAlignmentStatistics);
//                }
//            }
}
