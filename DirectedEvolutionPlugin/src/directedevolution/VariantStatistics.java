package directedevolution;

import com.biomatters.geneious.publicapi.documents.sequence.SequenceCharSequence;
import com.biomatters.geneious.publicapi.plugin.SequenceSelection;
import com.biomatters.geneious.publicapi.plugin.SequenceViewerExtension;
import com.biomatters.geneious.publicapi.utilities.Interval;
import jebl.util.ProgressListener;

import javax.annotation.Nonnull;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static jebl.evolution.sequences.SequenceType.AMINO_ACID;
import static jebl.evolution.sequences.SequenceType.NUCLEOTIDE;

public class VariantStatistics extends SequenceViewerExtension.StatisticsFactory {

    private static final double VERTICAL_POSITION = SequenceViewerExtension.StatisticsSection.POSITION_SEQUENCE_LENGTH + SequenceViewerExtension.StatisticsSection.RELATIVE_POSITION_DIRECTLY_BELOW; // Make it appear just below the sequence length statistic
    private static final double VERTICAL_POSITION_2 = SequenceViewerExtension.StatisticsSection.POSITION_SEQUENCE_LENGTH + 2.0 * SequenceViewerExtension.StatisticsSection.RELATIVE_POSITION_DIRECTLY_BELOW;
    private static final double VERTICAL_POSITION_3 = SequenceViewerExtension.StatisticsSection.POSITION_SEQUENCE_LENGTH + 3.0 * SequenceViewerExtension.StatisticsSection.RELATIVE_POSITION_DIRECTLY_BELOW;
    private static final Logger logger = Logger.getLogger(VariantStatistics.class.getName());

    @Override
    public List<SequenceViewerExtension.StatisticsSection> createStatistics(SequenceViewerExtension.PropertyRetrieverAndEditor propertyRetriever, ProgressListener progressListener) {
        try {
            //todo getSequenceListSummary()!=null) from getSelectionForAlignmentStatistics docs
            BigInteger total;
            if (!propertyRetriever.isAlignmentOrContig()) {
                return notGoodSelection("Total Variants can be calculated only on alignments of proteins or optimized nucleotide consensus");
            }
            Map<Interval, List<Interval>> selectionForAlignmentStatistics = propertyRetriever.getSelectionForAlignmentStatistics();
            List<SequenceSelection.SelectionInterval> intervals = propertyRetriever.getSelectionForStatistics().getIntervals(false);
            if (intervals.size() != 1) {
                return notGoodSelection("Total variants could only be calculated on one interval and now we have " + intervals.size() + " selected");
            } else if (intervals.get(0).getSequenceIndices().size() == 0) {
                return notGoodSelection("Total variants could only be calculated when at least one sequence is selected and now we have " + intervals.get(0).getSequenceIndices().size() + " selected");
            }
            if (intervals.get(0).getSequenceIndices().size() == 1) {//must be nucleotide
                String sequenceName = propertyRetriever.getSequenceName(propertyRetriever.getSelectionForStatistics().getIntervals(false).get(0).getSequenceIndices().get(0).getSequenceIndex());
                if (intervals.get(0).getSequenceIndices().get(0).getSequenceType() != NUCLEOTIDE) {
                    return notGoodSelection("Total variants on one sequence can only be calculated on nucleotide sequences and the first sequence named " + sequenceName +
                            " is " + intervals.get(0).getSequenceIndices().get(0).getSequenceType().toString());
                } else {
                    total = calculateVariantTotalForNucleotides(propertyRetriever, propertyRetriever.getSequenceCharSequence(intervals.get(0).getSequenceIndices().get(0).getSequenceIndex()));
                }
            } else {//must be aminoacid
                if (intervals.get(0).getSequenceIndices().get(0).getSequenceType() != AMINO_ACID) {
                    return notGoodSelection("Total variants on more than one sequence can be calculated on more than two Amino Acid sequences and now we have "
                            + intervals.get(0).getSequenceIndices().size() + " sequences of type " + intervals.get(0).getSequenceIndices().get(0).getSequenceType().toString());
                } else {
                    total = calculateVariantTotalForProtein(propertyRetriever, selectionForAlignmentStatistics);
                }
            }
            return displayTotal(intervals.get(0), total, propertyRetriever);
        } catch (Exception e) {
            e.printStackTrace();
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(e.getMessage());
            }
            return notGoodSelection("We don't know");
        }
    }

    private BigInteger calculateVariantTotalForNucleotides(SequenceViewerExtension.PropertyRetrieverAndEditor propertyRetriever, SequenceCharSequence sequence) {
        Interval clippedRange = propertyRetriever.getSelectionForStatistics().getIntervals(false).get(0).getResidueInterval().clipToRange(sequence.getLeadingGapsLength(), sequence.getTrailingGapsStartIndex());
        //todo the logger sometimes generates "java.lang.StringIndexOutOfBoundsException: String index out of range: -1
        //        at java.lang.String.substring(String.java:1955)
        //        at directedevolution.VariantStatistics.calculateVariantTotalForNucleotides(VariantStatistics.java:68)"
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("selected:" + sequence.toString().toUpperCase().substring(clippedRange.getFrom(), clippedRange.getToExclusive()));
        }
        return calculateVariant(sequence, clippedRange);
    }

    static BigInteger calculateVariant(SequenceCharSequence sequence, Interval clippedRange) {
        BigInteger total = new BigInteger("1");
        for (Integer residueIndex : clippedRange) {
            char c = sequence.charAt(residueIndex);
            if (c == 'R' || c == 'Y' || c == 'S' || c == 'W' || c == 'K' || c == 'M') {
                total = total.multiply(BigInteger.valueOf(2L));
            } else if (c == 'B' || c == 'D' || c == 'H' || c == 'V') {
                total = total.multiply(BigInteger.valueOf(3L));
            } else if (c == 'N') {
                total = total.multiply(BigInteger.valueOf(4L));
            }//for gap we leave unchanged
        }
        return total;
    }

    static BigInteger calculateVariant(String stringSequence) {
        BigInteger total = new BigInteger("1");
        for (char c : stringSequence.toCharArray()) {
            if (c == 'R' || c == 'Y' || c == 'S' || c == 'W' || c == 'K' || c == 'M') {
                total = total.multiply(BigInteger.valueOf(2L));
            } else if (c == 'B' || c == 'D' || c == 'H' || c == 'V') {
                total = total.multiply(BigInteger.valueOf(3L));
            } else if (c == 'N') {
                total = total.multiply(BigInteger.valueOf(4L));
            }//for gap we leave unchanged
        }
        return total;
    }

    private static BigInteger calculateVariantTotalForProtein(SequenceViewerExtension.PropertyRetrieverAndEditor propertyRetriever, Map<Interval, List<Interval>> selectionForAlignmentStatistics) {
        BigInteger total = new BigInteger("1");
        for (Map.Entry<Interval, List<Interval>> columnIntervalAndSequenceIntervalList : selectionForAlignmentStatistics.entrySet()) {
            //selectionForAlignmentStatistics.forEach((Interval columnInterval, List<Interval> sequenceIntervalList) -> {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("columnInterval:" + columnIntervalAndSequenceIntervalList.getKey().toString() + " and sequenceIntervalList:" + columnIntervalAndSequenceIntervalList.getValue());
            }
            Map<String, Integer> codeToCountMap = new HashMap<>();
            for (Integer residueIndex : columnIntervalAndSequenceIntervalList.getKey()) {
                AtomicInteger locationScore = new AtomicInteger(0);
                columnIntervalAndSequenceIntervalList.getValue().get(0).forEach(sequenceIndex -> {
                    SequenceCharSequence charSequence = propertyRetriever.getSequenceCharSequence(sequenceIndex); //getSequenceCharSequence works for both nucleotide and proteins
                    String c = String.valueOf(charSequence.charAt(residueIndex));
                    if (!c.equals("-") && !codeToCountMap.containsKey(c)) {
                        codeToCountMap.put(c, 0);
                        locationScore.incrementAndGet();
                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine("locationScore:" + locationScore + " after looking at:" + c);
                        }
                    }
                });
                if (locationScore.intValue() == 0) {// for cases where all nucleotides in the column are '-' we never put anything in the hash so the score stays 0.
                    locationScore.set(1); //We force a 1.
                }
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("total location score:" + locationScore + " at residueIndex:" + residueIndex);
                }
                total = total.multiply(BigInteger.valueOf(locationScore.longValue()));
                codeToCountMap.clear();
            }
        }
        return total;
    }

    @Nonnull
    private static List<SequenceViewerExtension.StatisticsSection> displayTotal(SequenceSelection.SelectionInterval selectionIntervalForCols, BigInteger total, SequenceViewerExtension.PropertyRetrieverAndEditor propertyRetriever) {
        String format = "Total variants: %s";
        String textLines[] = new String[2];
        textLines[1] = String.format("      for columns %s-%s and sequence(s) %s",
                selectionIntervalForCols.getResidueInterval().getFrom() + 1,
                selectionIntervalForCols.getResidueInterval().getToInclusive() + 1,
                propertyRetriever.getSelection().getSelectedSequenceIndices().stream().map(i -> "" +
                        propertyRetriever.getSequenceName(i.getSequenceIndex()) + ", ").collect(Collectors.joining()));
        textLines[0] = formatTotalNumber(total, format);
        SequenceViewerExtension.StatisticsSection section = new SequenceViewerExtension.StatisticsSection("totalVariants", total, textLines[0], VERTICAL_POSITION);
        SequenceViewerExtension.StatisticsSection sectionContinuation = new SequenceViewerExtension.StatisticsSection("totalVariantsContinuation", null, textLines[1], VERTICAL_POSITION_2);
        SequenceViewerExtension.StatisticsSection sectionSeparator = new SequenceViewerExtension.StatisticsSection("separator", null, "", VERTICAL_POSITION_3);
        return Arrays.asList(section, sectionContinuation, sectionSeparator);
    }

    static String formatTotalNumber(BigInteger total, String format) {
        boolean tooBig = false;
        try {
            //noinspection ResultOfMethodCallIgnored
            total.longValueExact();
        } catch (Exception ex) {
            tooBig = true;
        }
        if (!tooBig && 100000L > total.longValueExact()) {
            return getText(format, String.valueOf(total.longValueExact()));
        } else {
            return getText(format, new DecimalFormat("0.#####E0").format(total));
        }
    }

    private List<SequenceViewerExtension.StatisticsSection> notGoodSelection(String cause) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Not good selection for calculating variant statistics and cause is " + cause);
        }
        return Collections.emptyList();
    }

    private static String getText(String format, String number) {
        return String.format(format, number);
    }
}
