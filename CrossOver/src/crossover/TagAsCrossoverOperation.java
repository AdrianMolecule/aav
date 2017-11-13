package crossover;

import com.biomatters.geneious.publicapi.components.Dialogs;
import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;
import com.biomatters.geneious.publicapi.documents.DocumentField;
import com.biomatters.geneious.publicapi.implementations.DefaultAlignmentDocument;
import com.biomatters.geneious.publicapi.plugin.*;
import jebl.util.ProgressListener;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class TagAsCrossoverOperation extends DocumentOperation {

    public static final DocumentField CROSSOVER_FIELD = DocumentField.createBooleanField("crossover document", "Whether or not this document contains an alignment tht needs to be treated as a Crossover document",
            "Crossover", true, false);

    /**
     * The document selection signature tells Geneious to enable this when anything is selected
     */
    @Override
    public DocumentSelectionSignature[] getSelectionSignatures() {
        return new DocumentSelectionSignature[]{
                new DocumentSelectionSignature(DefaultAlignmentDocument.class, 1, 1000)
        };
    }

    /**
     * This method does the actual work of running the operation.
     * <p>
     * This method needs to be threadsafe as it is possible for time-consuming
     * operations to be running concurrently. It is invoked by user interaction.
     *
     * @param documents        the documents
     * @param progressListener is ignored in this example
     * @param options          as returned by ((OperationOptions)options)
     * @return a list containing original sequences, optimized sequences, consensus sequence on the optimized sequences and generated oligos.
     * @throws DocumentOperationException if any error occurs
     */
    @Override
    public List<AnnotatedPluginDocument> performOperation(AnnotatedPluginDocument[] documents, ProgressListener progressListener, Options options, SequenceSelection sequenceSelection) throws DocumentOperationException {
        for (AnnotatedPluginDocument document : documents) {
            Object fieldValue = document.getFieldValue(CROSSOVER_FIELD);
            if (fieldValue == null) {
                document.setFieldValue(CROSSOVER_FIELD, true);
                //System.out.println("setting document:" + documents[0].getName() + " field crossover to true");
            } else if (((boolean) fieldValue)) {
                document.setFieldValue(CROSSOVER_FIELD, false);
            } else {
                document.setFieldValue(CROSSOVER_FIELD, true);
            }
        }
        return Arrays.asList(documents);
    }

    /**
     * The action options specify that this operation is available from the File menu.
     */
    public GeneiousActionOptions getActionOptions() {
        return new GeneiousActionOptions("Mark/Unmark the selected document(s) as Crossover...").
                setMainMenuLocation(GeneiousActionOptions.MainMenu.Sequence).
                setInMainToolbar(true);
        //.setShortcutKey(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.ALT_DOWN_MASK));//ALT S for save?
    }

    /**
     * Get a brief help content.
     *
     * @return the brief help content
     */
    public String getHelp() {
        return "Marks/Unmarks the selected alignment document as a crossover document so crossover graphs and stats could be shown";
    }
}