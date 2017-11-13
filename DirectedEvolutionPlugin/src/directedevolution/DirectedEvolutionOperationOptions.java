package directedevolution;

import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;
import com.biomatters.geneious.publicapi.documents.sequence.SequenceDocument;
import com.biomatters.geneious.publicapi.implementations.DefaultAlignmentDocument;
import com.biomatters.geneious.publicapi.plugin.Options;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DirectedEvolutionOperationOptions extends Options {
    private static final Logger logger = Logger.getLogger(DirectedEvolutionOperationOptions.class.getName());
    //the following block is just defaults when useOptions is false
    final BooleanOption generateOligosOption;
    final BooleanOption generateGapBasedOligosOption;
    final IntegerOption approximateOligoSizeOption;
    final IntegerOption minOverlapOption;
    final BooleanOption compactSequencesOption;
    final BooleanOption backwardsAndForwardsOption;
    final IntegerOption minOligoSizeOption;
    final BooleanOption doubleOptimizedConsensusOption;
    final Options.ComboBoxOption ignoredSequenceOption;
    final OptionValue noSelectionValue = new OptionValue("-1", " ");
    final BooleanOption useOriginalNameAsPefixOption;
    final StringOption fileNameSuffixOption;
    final BooleanOption showGapsInGapBasedOligosOption;
    //
    public static final String MOST_COMMON_CODON_OPTION="mostCommonCodonOption";
    public static final String SELECTED_SEQUENCE_OPTION="useSelectedSequenceOption";
    final static ImmutableList<OptionValue> optimizationTypeOptionValues = ImmutableList.of(new OptionValue(MOST_COMMON_CODON_OPTION, "Most Common Codon"),
            new OptionValue(SELECTED_SEQUENCE_OPTION, "Use Selected Sequence")).asList();
    final Options.ComboBoxOption optimizationTypeOption ;


    public DirectedEvolutionOperationOptions(AnnotatedPluginDocument... documents) {
        addDivider("General options");
        generateOligosOption = addBooleanOption("generateOligos", "Generate Oligos", true);
        generateGapBasedOligosOption = addBooleanOption("generateGapBasedOligos", "Generate Gap Based Oligos", true);
        showGapsInGapBasedOligosOption = addBooleanOption("showGapsInGapBasedOligos", "Show Gaps in Gap Based Oligos", true);
        generateOligosOption.addChangeListener(() -> {
            if (!generateOligosOption.getValue()) generateGapBasedOligosOption.setValue(false);
        });
        generateGapBasedOligosOption.addChangeListener(() -> {
            if (generateGapBasedOligosOption.getValue()) generateOligosOption.setValue(true);
        });
        addDivider("Oligo Generation Options");//----------------------------------------------------
        approximateOligoSizeOption = addIntegerOption("approximateOligoSize", "Approximate Oligonucleotide size without the overlapped sequences", 20);
        minOverlapOption = addIntegerOption("minOverlap", "Minimal Oligonucleotide Overlap", 7);
        compactSequencesOption = addBooleanOption("compactSequences", "Compact Sequences", true);
        backwardsAndForwardsOption = addBooleanOption("backwardsAndForwards", "Look for cut places Backward of Oligo size first and then Forward", true);
        minOligoSizeOption = addIntegerOption("minOligoSize", "Minimal Oligo size (without the overlap) ", 8);
        addDivider("Visual Layout options");
        doubleOptimizedConsensusOption = addBooleanOption("doubleOptimizedConsensus", "Repeat the display of Optimized Consensus at the end", false);
        ///
        addDivider("Sequence Optimization Options");//------------------------------------------------
        // optimizationType
        optimizationTypeOption = addComboBoxOption("optimizationType", "Optimization Type", optimizationTypeOptionValues,
                optimizationTypeOptionValues.get(0));
        getOption("optimizationType").getComponent().setToolTipText("Most Common Codon replaces identical amino acid codons with the consensus codon for the identity position\n" +
                " Use Selected Sequence replaces identical amino acid codons with the the codon of the selected amino acids for the identity position");
        //end optimizationType
        AtomicInteger sequenceIndex = new AtomicInteger(0);
        List<Options.OptionValue> optionValues = Lists.newArrayList();
        optionValues.add(0, noSelectionValue);
        if (documents.length != 0) {
            //ignoreSequenceOptionValues.add
            List<OptionValue> ignoredSequenceoptions = ((DefaultAlignmentDocument) documents[0].getDocumentOrCrash()).<SequenceDocument>getSequences().stream().map(sequence ->
                    new OptionValue("" + sequenceIndex.getAndIncrement(), sequence.getName())).collect(Collectors.toList());
            optionValues.addAll(ignoredSequenceoptions);
        }
        ignoredSequenceOption = addComboBoxOption("sequenceIgnoredDuringOptimization", "Sequence that should be ignored as input into calculation of optimized codons", optionValues,
                optionValues.get(0));
        getOption("sequenceIgnoredDuringOptimization").getComponent().setToolTipText("TTG, CTC, CTG, CTG, CTT, TTG,  CTG,  TTG\n that normally produce  TTG, TTG, TTG, TTG, CTT, TTG,  TTG, TTG\n" +
                " will produce TTG, TTG, TTG, TTG, CTT, TTG,  TTG,  TTG\n" +
                " if last value is marked as ignored in calculation");
        ///////////
        addDivider("File name Generation options");//-------------------------------------------------------------
        useOriginalNameAsPefixOption = addBooleanOption("useOriginalNameAsPefix", "Use Original Filename As Prefix", true);
        fileNameSuffixOption = addStringOption("fileNameSuffix", "File Name Suffix", "with Oligos");
        addDivider("Others");
        ButtonOption resetOptionsButton = addButtonOption("resetOptions", " ", "Reset Options");
        resetOptionsButton.addActionListener(this::resetOptions);
    }

    @VisibleForTesting
    private void resetOptions(ActionEvent actionEvent) {
        generateOligosOption.setValue(generateOligosOption.getDefaultValue());
        generateGapBasedOligosOption.setValue(generateGapBasedOligosOption.getDefaultValue());
        showGapsInGapBasedOligosOption.setValue(showGapsInGapBasedOligosOption.getDefaultValue());
        approximateOligoSizeOption.setValue(approximateOligoSizeOption.getDefaultValue());
        minOverlapOption.setValue(minOverlapOption.getDefaultValue());
        compactSequencesOption.setValue(compactSequencesOption.getDefaultValue());
        backwardsAndForwardsOption.setValue(backwardsAndForwardsOption.getDefaultValue());
        minOligoSizeOption.setValue(minOligoSizeOption.getDefaultValue());
        doubleOptimizedConsensusOption.setValue(doubleOptimizedConsensusOption.getDefaultValue());
        optimizationTypeOption.setValue(optimizationTypeOption.getDefaultValue());
        ignoredSequenceOption.setValue(ignoredSequenceOption.getDefaultValue());
        useOriginalNameAsPefixOption.setValue(useOriginalNameAsPefixOption.getDefaultValue());
        fileNameSuffixOption.setValue(fileNameSuffixOption.getDefaultValue());
    }

    public Boolean getGenerateOligos() {
        return generateOligosOption.getValue();
    }

    public Boolean getGenerateGapBasedOligos() {
        return generateGapBasedOligosOption.getValue();
    }

    public Boolean getShowGapsInGapBasedOligos() {
        return showGapsInGapBasedOligosOption.getValue();
    }

    public Boolean getDoubleOptimizedConsensus() {
        return doubleOptimizedConsensusOption.getValue();
    }

    public int getMinOligoSize() {
        return Integer.valueOf(minOligoSizeOption.getValueAsString());
    }

    public int getApproximateOligoSize() {
        return Integer.valueOf(approximateOligoSizeOption.getValueAsString());
    }

    public int getMinOverlap() {
        return Integer.valueOf(minOverlapOption.getValueAsString());
    }

    public boolean getCompactSequences() {
        return compactSequencesOption.getValue();
    }

    public boolean getBackwardsAndForwards() {
        return backwardsAndForwardsOption.getValue();
    }

    public boolean getUseOriginalNameAsPrefix() {
        return useOriginalNameAsPefixOption.getValue();
    }

    public Integer getSequenceIgnoredDuringOptimization() {
        return Integer.valueOf(ignoredSequenceOption.getValueAsString());
    }

    public String getOptimizationType() {
        return optimizationTypeOption.getValue().toString();
    }

    public String getFileNameSuffix() {
        return fileNameSuffixOption.getValue();
    }

    @Override
    public String toString() {
        return "DirectedEvolutionOperationOptions{" +
                ", generateOligos=" + getGenerateOligos() +
                ", generateGapBasedOligos=" + getGenerateGapBasedOligos() +
                ", approximateOligoSize=" + getApproximateOligoSize() +
                ", minOverlap=" + getMinOverlap() +
                ", compactSequences=" + getCompactSequences() +
                ", backwardsAndForwards=" + getBackwardsAndForwards() +
                ", minOligoSize=" + getMinOligoSize() +
                ", doubleOptimizedConsensus=" + getDoubleOptimizedConsensus() +
                ", sequenceIgnoredDuringOptimization=" + getSequenceIgnoredDuringOptimization() +
                '}';
    }
}
