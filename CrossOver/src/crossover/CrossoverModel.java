package crossover;

import com.biomatters.geneious.publicapi.documents.sequence.SequenceDocument;
import com.google.common.base.Objects;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import crossover.utils.CrossoverUtils;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class CrossoverModel extends ArrayList<CrossoverModel.Column> {
    private static final Logger logger = Logger.getLogger(CrossoverModel.class.getName());
    private static CrossoverModel instance = null;
    private Lengths lengths = new Lengths();
    private int chimeraSequenceSize;
    private List<CharSequence> parents;
    private CharSequence chimera;
    private List<SequenceDocument> parentsAsFullNucleotideSequences;
    private SequenceDocument chimeraAsFullNucleotideSequence;
    private final ArrayList<Integer> levenshteinDistances = Lists.newArrayList();
    private final ArrayList<Integer> uniqueLengthsUngappedFragments = Lists.newArrayList();
    private final ArrayList<Switch> switches = Lists.newArrayList();
//    public static @Nonnull
//    CrossoverModel getInstance(boolean refresh, CharSequence chimera, List<CharSequence> parentalSequences, List<String> sequenceNames) {
//        if (instance != null && !refresh) {
//            return instance;
//        } else {
//            instance = new CrossoverModel(parentalSequences, chimera, sequenceNames);
//            return instance;
//        }
//    }

    public static @Nonnull
    CrossoverModel getInstance(boolean refresh, List<SequenceDocument> realSequences, boolean chimeraIsFirst) {
        SequenceDocument chimera;
        if (instance != null && !refresh) {
            return instance;
        } else {
            List<SequenceDocument> parentalSequences;
            if (chimeraIsFirst) {
                parentalSequences = realSequences.subList(1, realSequences.size());
                chimera = realSequences.get(0);
            } else {
                parentalSequences = realSequences.subList(0, realSequences.size() - 1);
                chimera = realSequences.get(realSequences.size() - 1);
            }
            instance = new CrossoverModel(parentalSequences, chimera);
            System.out.println("getInstance(true) called and chimeraIsFirst:" + chimeraIsFirst);
            return instance;
        }
    }

    private CrossoverModel(List<SequenceDocument> parentalSequencesAsFullNucleotideSequences, SequenceDocument chimeraAsFullNucleotideSequence) {
        this.parentsAsFullNucleotideSequences = parentalSequencesAsFullNucleotideSequences;
        this.chimeraAsFullNucleotideSequence = chimeraAsFullNucleotideSequence;
        List<CharSequence> parents = parentalSequencesAsFullNucleotideSequences.stream().map(SequenceDocument::getCharSequence).collect(Collectors.toList());
        CharSequence chimera = chimeraAsFullNucleotideSequence.getCharSequence();
        Stopwatch stopwatch = Stopwatch.createStarted();
        this.parents = parents;
        this.chimera = chimera;
        chimeraSequenceSize = chimera.length();
        int parentalSequencesSize = parents.size();
        for (int nucleotideIndex = 0; nucleotideIndex < chimeraSequenceSize; nucleotideIndex++) {
            Column column = new Column();
            add(column);
            List<Integer> sequenceMatches = Lists.newArrayList();
            int total = 0;
            for (CharSequence parentalSequence : parents) {
                if (parentalSequence.charAt(nucleotideIndex) == chimera.charAt(nucleotideIndex) && chimera.charAt(nucleotideIndex) != '-') {
                    sequenceMatches.add(1);
                    total++;
                } else {
                    sequenceMatches.add(0);
                }
            }
            //set mutant/Gaps fields
            if (total == 0) {//mutantRun or gap
                if (chimera.charAt(nucleotideIndex) == '-') {
                    column.setGapInChimera();
                } else {
                    column.setMutant(chimera.charAt(nucleotideIndex));
                }
            }
            for (int parentalSequenceIndex = 0; parentalSequenceIndex < parentalSequencesSize; parentalSequenceIndex++) {
                column.add(new SequenceData(total != 0 ? sequenceMatches.get(parentalSequenceIndex) / (double) total : 0.00, column.isGap()));
            }
            sequenceMatches.clear();
        }
        //LENGTHS START. finished calculating the matches lets do the source lengths
        Map<Integer, Integer> potentialSources = new HashMap<>();
        Set<Integer> blockedSequencesMatches = Sets.newHashSet();
        int lengthStart = -1;
        for (int nucleotideIndex = 0; nucleotideIndex < chimeraSequenceSize; nucleotideIndex++) {
            for (int parentalSequenceIndex = 0; parentalSequenceIndex < parentalSequencesSize; parentalSequenceIndex++) {
                Integer count = potentialSources.get(parentalSequenceIndex);
                if (getSequenceData(nucleotideIndex, parentalSequenceIndex).isChimericMatch() && !blockedSequencesMatches.contains(parentalSequenceIndex)) {//is Match and is not already discarded
                    if (lengthStart == -1) {
                        lengthStart = nucleotideIndex;
                    }
                    if (count != null) {
                        potentialSources.put(parentalSequenceIndex, ++count);
                    } else {
                        potentialSources.put(parentalSequenceIndex, 1);
                    }
                } else {
                    blockedSequencesMatches.add(parentalSequenceIndex);
                }
            }//end sequence For
            if (nucleotideIndex == chimeraSequenceSize - 1) {//deal with Last one
                if (!potentialSources.isEmpty()) {//last length is Not mutant or Gap
                    processMaximumsAndAddLength(potentialSources, lengthStart);
                }
                break;//--------------------------------------------------------------->
            }
            if ((blockedSequencesMatches.size() == parentalSequencesSize)) {//search ends because we finished finding matches on All sequences
                if (!potentialSources.isEmpty()) {//Mutants and probably Gaps
                    processMaximumsAndAddLength(potentialSources, lengthStart);
                    if (nucleotideIndex > 0) {
                        nucleotideIndex--;//Yes, I know this is weird but we need to reestablish potentialSources, blockedSequencesMatches and lengthStart for the CURRENT nucleotideIndex
                        // (need to go once through the sequence loop)
                    }
                }
                lengthStart = -1;
                potentialSources.clear();
                blockedSequencesMatches.clear();
            }
        }
        //update SequenceData source field from Lengths. Any sequence source that is in a length is source.
        Length lastNonGroupSource = null;
        for (Length length : lengths) {
            for (int nucleotideInLength = length.getStartIndex(); nucleotideInLength <= length.getEndIndex(); nucleotideInLength++) {
                SequenceData sequenceData = getSequenceData(nucleotideInLength, length.getSequenceIndex());
                sequenceData.setLength(length);
                if (nucleotideInLength == length.getStartIndex()) {
                    sequenceData.setFirstInSource();
                    if (lastNonGroupSource != null && lastNonGroupSource.getEndIndex() + 1 == length.getStartIndex()) {//todo rename overlapping with grouped. Todo rename Source with parent to rename first to firstX or y
                        sequenceData.setLastNonOverlappingSource(lastNonGroupSource);
                    }
                }//if could be the first and the last
                if (nucleotideInLength == length.getEndIndex()) {
                    lastNonGroupSource = length;
                    sequenceData.setLastInSource();
                    //System.out.println("last in source sequence data for nucleotideInLength:" + nucleotideInLength + " is:" + sequenceData);
                }
            }
        }
        System.out.println("End recalculating whole model after " + stopwatch);
    }// visual end of constructor

    private void processMaximumsAndAddLength(Map<Integer, Integer> potentialSources, int firstLengthStart) {
        int max = Collections.max(potentialSources.entrySet(), Map.Entry.comparingByValue()).getValue();
        for (Map.Entry<Integer, Integer> entry : potentialSources.entrySet()) {
            if (entry.getValue() == max) {
                Length length = new Length(firstLengthStart, max, entry.getKey());
                int size = lengths.size();
                if (size != 0) {
                    Length previousLength = lengths.get(size - 1);
                    if (previousLength.getStartIndex() == firstLengthStart) {
                        previousLength.setPartOfGroup(true);
                        previousLength.setLastInGroup(false);
                        length.setPartOfGroup(true);
                        length.setLastInGroup(true);
                    }
                }
                lengths.add(length);
            }
        }
    }

    SequenceData getSequenceData(int nucleotideIndex, int parentalSequenceIndex) {
        Column ColumSequencesData;
        ColumSequencesData = get(nucleotideIndex);
        return ColumSequencesData.get(parentalSequenceIndex);
    }

    public void calculateStatisticsIfNotAlreadyIn() {
        boolean noModelAffectingOptionChanged = false;//todo for crossoverConsideringMutations calculation
        if (lengths.size() > 0 && noModelAffectingOptionChanged) {//we already have the statistics calculated
            return;
        }//we need to reset all the arrays otherwise we keep adding
        levenshteinDistances.clear();
        uniqueLengthsUngappedFragments.clear();
        switches.clear();
        for (CharSequence parentalSequence : parents) {
            levenshteinDistances.add(CrossoverUtils.calculateLevenshteinDistanceAfterRemovingGaps(parentalSequence, chimera));
        }
        //end resetting variables that maintain state by being cumulative
        //Calculate fragments and crossover Discounting Mutants or Including mutants
        Set<Integer> currentSourceCandidates = Sets.newHashSet();
        Set<Integer> sources = Sets.newHashSet();
        for (Length length : lengths) {
            int startIndex = length.getStartIndex();
            if (startIndex == 0) {
                uniqueLengthsUngappedFragments.add((chimera.subSequence(startIndex, startIndex + length.getSize()).toString().replace("-", "").length()));
                if (length.isPartOfGroup()) {
                    sources.add(length.getSequenceIndex());
                    if (length.isLastInGroup()) {
                        continue;//--------------------->
                    }
                } else {
                    sources.add(length.getSequenceIndex());
                }
                continue;//------------------------------------------------------------->correct
            }
            if (length.isPartOfGroup()) {//Group Currently
                currentSourceCandidates.add(length.getSequenceIndex());
                if (length.isLastInGroup()) {
                    ImmutableSet<Integer> intersection = ImmutableSet.copyOf(Sets.intersection(sources, currentSourceCandidates));
                    if (intersection.isEmpty()) {//SWITCH of Parent to we don't know who
                        uniqueLengthsUngappedFragments.add((chimera.subSequence(startIndex, startIndex + length.getSize()).toString().replace("-", "").length()));
                        switches.add(new Switch(startIndex, ImmutableSet.copyOf(sources), ImmutableSet.copyOf(currentSourceCandidates)));
                        if (logger.isLoggable(Level.FINEST)) {
                            System.out.println("Switch A:" + switches.get(switches.size() - 1));
                        }
                        sources.clear();
                        sources.addAll(currentSourceCandidates);
                        currentSourceCandidates.clear();
                    } else {
                        if (logger.isLoggable(Level.FINEST)) {
                            System.out.println("Continuation at startIndex B:" + startIndex + " master is:" + sources);
                        }
                        //current group is just a continuation
                        //no change in master or master
                        sources.clear();// despite being a group we already chose the master for this group to do this might be redundant todo
                        sources.addAll(intersection);
                        currentSourceCandidates.clear();
                    }
                } else {
                    continue;//keep accumulating curr until we hit the last in group
                }
            } else {//current is not part of group
                uniqueLengthsUngappedFragments.add((chimera.subSequence(startIndex, startIndex + length.getSize()).toString().replace("-", "").length()));
                if (!sources.contains(length.getSequenceIndex())) {//SWITCH of Parent
                    switches.add(new Switch(startIndex, ImmutableSet.copyOf(sources), ImmutableSet.of(length.getSequenceIndex())));
                    if (logger.isLoggable(Level.FINEST)) {
                        System.out.println("Switch F:" + switches.get(switches.size() - 1));
                    }
                    sources.clear();
                    sources.add(length.getSequenceIndex());
                } else {
                    //master remains the same if it was unique or becomes unique if it was not
                    sources.clear();
                    sources.add(length.getSequenceIndex());
                    if (logger.isLoggable(Level.FINEST)) {
                        System.out.println("Continuation at startIndex E:" + startIndex + " master stays and becomes unique if not already:" + length.getSequenceIndex());
                    }
                }
            }
        }
    }//END Constructor


    public static void reset() {
        System.out.println("Reset was called");
        instance = null;
    }

    //simple accessors
    int getChimeraSequenceSize() {
        return chimeraSequenceSize;
    }

    public ArrayList<Integer> getLevenshteinDistances() {
        return levenshteinDistances;
    }

    public ArrayList<Integer> getUniqueLengthsUngappedFragments() {
        return uniqueLengthsUngappedFragments;
    }

    public int getCrossoverConsideringMutations() {
        return uniqueLengthsUngappedFragments.size() - 1;
    }

    public long getPointMutations() {
        return this.stream().filter(c -> c.isMutant()).count();
    }

    public Number getCrossoverDiscountingMutation() {
        return switches.size();
    }

    public List<CharSequence> getParents() {
        return parents;
    }

    public CharSequence getChimera() {
        return chimera;
    }

    public List<SequenceDocument> getParentsAsFullNucleotideSequences() {
        return Collections.unmodifiableList(parentsAsFullNucleotideSequences);
    }

    public SequenceDocument getChimeraAsFullNucleotideSequence() {
        return chimeraAsFullNucleotideSequence;
    }

    public ArrayList<Switch> getSwitches() {
        return switches;
    }

    static class SequenceData {
        private final double matchProportion;
        /**
         * This is the last non overlapping length that was a source.
         */
        private Length lastNonOverlappingSource = null;
        /**
         * This is the first nucleotide in the source (length)
         */
        private boolean firstInSource = false;
        private boolean chimeraIsGap = false;
        private Length length = null;//the length it belongs to
        private boolean lastInSource;

        SequenceData(double matchProportion, boolean chimeraIsGap) {
            this.matchProportion = matchProportion;
            this.chimeraIsGap = chimeraIsGap;
        }

        boolean isChimericMatch() {
            return matchProportion > 0.00 || chimeraIsGap;//consider all chimera gaps as matches!!!
        }

        double getMatchProportion() {
            return matchProportion;
        }

        boolean isSource() {
            return length != null;
        }

        void setLength(Length length) {
            this.length = length;
        }

        void setLastNonOverlappingSource(Length lastNonOverlappingSource) {
            this.lastNonOverlappingSource = lastNonOverlappingSource;
        }

        Length getLastNonOverlappingSource() {
            return lastNonOverlappingSource;
        }

        void setFirstInSource() {
            firstInSource = true;
        }

        boolean isFirstInSource() {
            return firstInSource;
        }

        public boolean isLastInSource() {
            return lastInSource;
        }

        public void setLastInSource() {
            this.lastInSource = true;
        }

        boolean isGroup() {
            return length != null;
        }

        boolean isFirstGroupOnlyAfterCheckingForGroup() {
            return length.isPartOfGroup();
        }

        public boolean isLastInGroupOnlyAfterCheckingForGroup() {
            return length.isLastInGroup();
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("matchProportion", matchProportion)
                    .add("lastNonOverlappingSource", lastNonOverlappingSource)
                    .add("firstInSource", firstInSource)
                    .add("chimeraIsGap", chimeraIsGap)
                    .add("length", length)
                    .add("lastInSource", lastInSource)
                    .toString();
        }
    }

    static class Column extends ArrayList<SequenceData> {
        private boolean gap = false;
        private char mutant = (char) 0;

        Column() {
        }

        void setGapInChimera() {
            this.gap = true;
        }

        void setMutant(char mutant) {
            this.mutant = mutant;
        }

        public char getMutant() {
            return mutant;
        }

        boolean isGap() {
            return gap;
        }

        boolean isMutant() {
            return (int) mutant != 0;
        }
    }

    static class Length {
        private final int startIndex;
        private final int size;
        private final int sequenceIndex;
        private boolean partOfGroup;
        private boolean lastInGroup;

        Length(int startIndex, int size, int sequenceIndex) {
            this.startIndex = startIndex;
            this.size = size;
            this.sequenceIndex = sequenceIndex;
        }

        int getStartIndex() {
            return startIndex;
        }

        int getSize() {
            return size;
        }

        int getSequenceIndex() {
            return sequenceIndex;
        }

        /**
         * @return Gets the index for the lst char in the length
         */
        int getEndIndex() {
            return startIndex + getSize() - 1;
        }

        public boolean isPartOfGroup() {
            return partOfGroup;
        }

        public void setPartOfGroup(boolean partOfGroup) {
            this.partOfGroup = partOfGroup;
        }

        public boolean isLastInGroup() {
            return lastInGroup;
        }

        public void setLastInGroup(boolean lastInGroup) {
            this.lastInGroup = lastInGroup;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("startIndex", startIndex)
                    .add("size", size)
                    .add("sequenceIndex", sequenceIndex)
                    .add("partOfGroup", partOfGroup)
                    .add("lastInGroup", lastInGroup)
                    .toString();
        }
    }

    static class Lengths extends ArrayList<CrossoverModel.Length> {
        public CrossoverModel.Length getNextSibblingOrSelf(int currentLengthIndex, CrossoverModel.Length currentLength) {
            if (currentLengthIndex + 1 < size()) {
                CrossoverModel.Length nextLength = get(currentLengthIndex + 1);
                if (nextLength.getStartIndex() == currentLength.getStartIndex()) {
                    return nextLength;
                }
            }
            return currentLength;
        }
    }

    static class Switch {
        private int fromIndex;
        private ImmutableSet<Integer> fromSequence;
        private ImmutableSet<Integer> toSequence;

        public Switch(int fromIndex, ImmutableSet<Integer> fromSequence, ImmutableSet<Integer> toSequence) {
            this.fromIndex = fromIndex;
            this.fromSequence = fromSequence;
            this.toSequence = toSequence;
        }

        public ImmutableSet<Integer> getFromSequence() {
            return fromSequence;
        }

        public void setFromSequence(ImmutableSet<Integer> fromSequence) {
            this.fromSequence = fromSequence;
        }

        public ImmutableSet<Integer> getToSequence() {
            return toSequence;
        }

        public void setToSequence(ImmutableSet<Integer> toSequence) {
            this.toSequence = toSequence;
        }

        public int getFromIndex() {
            return fromIndex;
        }

        public void setFromIndex(int fromIndex) {
            this.fromIndex = fromIndex;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("fromIndex", fromIndex)
                    .add("fromSequence", fromSequence)
                    .add("toSequence", toSequence)
                    .toString();
        }
    }
}
