package directedevolution;

import com.biomatters.geneious.publicapi.components.Dialogs;
import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;
import com.biomatters.geneious.publicapi.implementations.DefaultAlignmentDocument;
import com.biomatters.geneious.publicapi.plugin.*;
import jebl.util.ProgressListener;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Operation that saves the generated oligos.
 */
public class SaveOligosOperation extends DocumentOperation {


    /**
     * The document selection signature tells Geneious to enable this when anything is selected
     */
    @Override
    public DocumentSelectionSignature[] getSelectionSignatures() {
        return new DocumentSelectionSignature[]{
                new DocumentSelectionSignature(DefaultAlignmentDocument.class, 1, 1)//todo restrict
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
        Optional<DocumentOperation> documentOperation = PluginUtilities.getDocumentOperations().stream().filter(o -> o instanceof GenerateOligosOperation).findFirst();
        if (!documentOperation.isPresent()) {
            Dialogs.showMessageDialog("Cannot get GenerateOligosOperation");
            return Arrays.asList(documents);
        }
        GenerateOligosOperation generateOligosOperation = (GenerateOligosOperation) documentOperation.get();
        List<OligoModel> oligos = generateOligosOperation.getOligos();
        if (oligos == null) {
            Dialogs.showMessageDialog("Oligos have not been generated yet");
            return Arrays.asList(documents);
        }
        GenerateOligosOperation.exportOligosAsFasta(oligos, GenerateOligosOperation.getNewDocumentName(),  progressListener);
        return Arrays.asList(documents);
    }

    /**
     * The action options specify that this operation is available from the File menu.
     */
    public GeneiousActionOptions getActionOptions() {
        return new GeneiousActionOptions("Save Generated Oligos...").
                setMainMenuLocation(GeneiousActionOptions.MainMenu.File).
                setInMainToolbar(true);
        //.setShortcutKey(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.ALT_DOWN_MASK));//ALT S for save?
    }

    /**
     * Get a brief help content.
     *
     * @return the brief help content
     */
    public String getHelp() {
        return "Saves Generated Oligos. Remebers the folder where tha last save was done.";
    }
}