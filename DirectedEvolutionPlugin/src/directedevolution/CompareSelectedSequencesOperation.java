package directedevolution;

import com.biomatters.geneious.publicapi.components.Dialogs;
import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;
import com.biomatters.geneious.publicapi.documents.PluginDocument;
import com.biomatters.geneious.publicapi.documents.sequence.*;
import com.biomatters.geneious.publicapi.plugin.*;

import jebl.util.ProgressListener;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyEvent;

import java.util.*;
import java.util.List;

import java.util.stream.Collectors;

/**
 * Operation that diffs selected residues.
 */
public class CompareSelectedSequencesOperation extends DocumentOperation {

    private static final String WHAT_TO_SELECT = "You need to select exactly two compatible sequences in the Document Table or compatible sequences part of an alignment or one sequence (or sub-sequence) and clipboard data.";

    /**
     * The document selection signature tells Geneious to enable this when anything is selected
     */
    public DocumentSelectionSignature[] getSelectionSignatures() {
        return new DocumentSelectionSignature[]{
                new DocumentSelectionSignature(PluginDocument.class, 1, 1000)
        };
    }

    /**
     * This method does the actual work of running the operation.
     * <p>
     * This method needs to be threadsafe as it is possible for time-consuming
     * operations to be running concurrently. It is invoked by user interaction.
     *
     * @param documents        always contains one AminoAcidSequenceDocument due to the selection signature
     * @param progressListener is ignored in this example
     * @param options          as returned by ((OperationOptions)options)
     * @return a list containing original sequences, optimized sequences, consensus sequence on the optimized sequences and generated oligos.
     * @throws DocumentOperationException if any error occurs
     */
    public List<AnnotatedPluginDocument> performOperation(AnnotatedPluginDocument[] documents, ProgressListener progressListener, Options options, SequenceSelection sequenceSelection) throws DocumentOperationException {
        try {
            String sequence0String = null;
            String sequence1String = null;
            if (documents.length == 1 && documents[0].getDocumentOrCrash() instanceof SequenceDocument) {
                String data = getClipboardStringOrNull();
                if (data != null) {
                    sequence0String = ((SequenceDocument) documents[0].getDocumentOrCrash()).getSequenceString();
                    String compareResult = compare(sequence0String, data);
                    Dialogs.showMessageDialog(compareResult, "Comparing sequence " + documents[0].getDocumentOrCrash().getName() + " with Clipboard contents");
                    return Arrays.asList(documents);
                }
            }
            if (documents.length != 1) {//select in the document table
                if (documents.length != 2) {
                    Dialogs.showMessageDialog("You selected no sequences or more than two sequences. " + WHAT_TO_SELECT);
                    return Arrays.asList(documents);
                }
                if (documents[0].getDocumentOrCrash() instanceof SequenceDocument && documents[1].getDocumentOrCrash() instanceof SequenceDocument) {
                    sequence0String = ((SequenceDocument) documents[0].getDocumentOrCrash()).getSequenceString();
                    sequence1String = ((SequenceDocument) documents[1].getDocumentOrCrash()).getSequenceString();
                    String compareResult = compare(sequence0String, sequence1String);
                    Dialogs.showMessageDialog(compareResult, "Comparing sequence " + documents[0].getDocumentOrCrash().getName() + " with " + documents[1].getDocumentOrCrash().getName());
                    return Arrays.asList(documents);
                } else {
                    Dialogs.showMessageDialog("You need to select two compatible sequences to compare. " + WHAT_TO_SELECT);
                    return Arrays.asList(documents);
                }
            } else {//documents.length == 1 selected only one sub-sequence from sequence view
                if (sequenceSelection.getSelectedSequenceIndices().stream().count() == 1L) {// clipboard and sequence from alignment
                    String data = getClipboardStringOrNull();
                    if (data != null) {
                        PluginDocument documentOrCrash = documents[0].getDocumentOrCrash();
                        if (documentOrCrash instanceof SequenceAlignmentDocument) {
                            List<SequenceSelection.SequenceIndex> selectedSequencesIndexes = sequenceSelection.getSelectedSequenceIndices().stream().collect(Collectors.toList());
                            sequence0String = ((SequenceAlignmentDocument) documentOrCrash).getSequence(selectedSequencesIndexes.get(0).getSequenceIndex()).getSequenceString();
                            String compareResult = compare(sequence0String, data);
                            Dialogs.showMessageDialog(compareResult, "Comparing sequence " + documents[0].getDocumentOrCrash().getName() + " with Clipboard contents");
                            return Arrays.asList(documents);
                        }
                    }
                }
                if (sequenceSelection.getSelectedSequenceIndices().stream().count() != 2L) {
                    Dialogs.showMessageDialog("You need to select exactly two sub-sequences, unless you have selected something in the clipboard and one extra sequence.");
                    return Arrays.asList(documents);
                }
                PluginDocument documentOrCrash = documents[0].getDocumentOrCrash();
                List<SequenceSelection.SequenceIndex> selectedSequencesIndexes = sequenceSelection.getSelectedSequenceIndices().stream().collect(Collectors.toList());
                if (documentOrCrash instanceof SequenceAlignmentDocument) {
                    if (sequenceSelection.getNonZeroLengthIntervals().size() > 2) {
                        Dialogs.showMessageDialog("You need to select just two sub-sequences interval. " + WHAT_TO_SELECT);
                        return Arrays.asList(documents);
                    }
                    if (sequenceSelection.getNonZeroLengthIntervals().size() == 2) {// different columns in 2 sequences or same column in 2 sequences
                        SequenceSelection.SelectionInterval selection0Interval = sequenceSelection.getNonZeroLengthIntervals().get(0);
                        SequenceSelection.SelectionInterval selection1Interval = sequenceSelection.getNonZeroLengthIntervals().get(1);
                        int firstSequenceIndex = sequenceSelection.getNonZeroLengthIntervals().get(0).getSequenceIndex().getSequenceIndex();
                        int secondSequenceIndex = sequenceSelection.getNonZeroLengthIntervals().get(1).getSequenceIndex().getSequenceIndex();
                        sequence0String = ((SequenceAlignmentDocument) documentOrCrash).getSequence(firstSequenceIndex).getSequenceString().substring(selection0Interval.getFromResidue(), selection0Interval.getToResidue());
                        sequence1String = ((SequenceAlignmentDocument) documentOrCrash).getSequence(secondSequenceIndex).getSequenceString().substring(selection1Interval.getFromResidue(), selection1Interval.getToResidue());
                    }
                    String compareResult = compare(sequence0String, sequence1String);
                    Dialogs.showMessageDialog(compareResult, "Comparing sequence " + ((SequenceAlignmentDocument) documentOrCrash).getSequence(selectedSequencesIndexes.get(0).getSequenceIndex()).getName() +
                            " with " + ((SequenceAlignmentDocument) documentOrCrash).getSequence(selectedSequencesIndexes.get(0).getSequenceIndex()).getName());
                    return Arrays.asList(documents);
                }
            }
            return Arrays.asList(documents);
        } catch (Exception e) {
            e.printStackTrace();
            Dialogs.showMessageDialog("Current selection does not allow comparison. " + WHAT_TO_SELECT);
            return Arrays.asList(documents);
        }
    }

    /**
     * Get the string content of the clipboard.
     *
     * @return the string in the clipboard or null.
     */
    @Nullable
    private String getClipboardStringOrNull() {
        String data = null;
        try {
            data = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
        } catch (Exception ignored) {
            //eat it
        }
        return data;
    }

    /**
     * Compare 2 strings and returns a string containing the result of the comparison.
     *
     * @param string0 first string to compare
     * @param string2 second string to compare
     * @return result of the comparison
     */
    @Nonnull
    private String compare(@Nonnull String string0, @Nonnull String string2) {
        StringBuilder result = new StringBuilder();
        if (string0.length() != string2.length()) {
            result.append("Sequences have different lengths. The first one has:").append(string0.length()).append(" and the second one:").append(string2.length()).append(".\n\n");
        }
        int levenshteinDistance = StringUtils.getLevenshteinDistance(string0, string2);
        if (levenshteinDistance == 0) {
            result.append("Sequences are the same");
        } else {
            int indexOfDifference = StringUtils.indexOfDifference(string0, string2);
            result.append("Sequences are different starting at index:").
                    append(indexOfDifference).append("\nWhere first sequence is: ").append(string0.substring(indexOfDifference, Integer.min(string0.length(), indexOfDifference + 10))).append("...").
                    append(" and the second sequence is: ").append(string2.substring(indexOfDifference, Integer.min(string2.length(), indexOfDifference + 10))).append("...\n There are ").
                    append(StringUtils.getLevenshteinDistance(string0, string2)).append(" changes needed to change one sequence into another, where each change is a single character modification (deletion, insertion or substitution).");
        }
        return result.toString();
    }

    /**
     * The action options specify that this operation is available from the Sequence menu.
     */
    public GeneiousActionOptions getActionOptions() {
        return new GeneiousActionOptions("Compare Selected Sequences...").
                setMainMenuLocation(GeneiousActionOptions.MainMenu.Sequence).
                setInMainToolbar(true).setShortcutKey(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.ALT_DOWN_MASK));//CTRL D for diff
    }

    /**
     * Get a brief help content.
     *
     * @return the brief help content
     */
    public String getHelp() {
        return "Compare Sequences or sub-sequences for differences. " + WHAT_TO_SELECT;
    }
}