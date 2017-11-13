package directedevolution.logging;

import com.biomatters.geneious.publicapi.components.Dialogs;
import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;
import com.biomatters.geneious.publicapi.implementations.DefaultAlignmentDocument;
import com.biomatters.geneious.publicapi.plugin.*;
import jebl.util.ProgressListener;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Operation that show documentation about how to configure logging.
 */
public class LoginDocumentationOperation extends DocumentOperation {

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
        Dialogs.showMessageDialog(
                "On MacOS, this is Info.plist in the Contents folder inside the Geneious.app bundle (Ctrl+Click on Geneious and select Show package contents). Open Info.plist using the default program (Property List Editor.app) and go in to the Java then the Properties node. Change the key java.util.logging.config.file to contain the path to your logging properties file, eg:\n" +
                        "/Users/marti/logging.properties\n" +
                        "\n" +
                        " \n" +
                        "Your logging.properties file may contain something like:\n" +
                        ".level=WARNING\n" +
                        "com.biomatters.level=INFO\n" +
                        "directedevolution.level=FINE\n" +
                        "handlers=java.util.logging.FileHandler\n" +
                        "java.util.logging.FileHandler.append=true\n" +
                        "java.util.logging.FileHandler.formatter=java.util.logging.SimpleFormatter\n" +
                        "java.util.logging.FileHandler.pattern=/Users/marti/geneious.log");
        return Arrays.asList(documents);
    }

    /**
     * The action options specify that this operation is available from the File menu.
     */
    public GeneiousActionOptions getActionOptions() {
        return new GeneiousActionOptions("How to configure logging...").
                setMainMenuLocation(GeneiousActionOptions.MainMenu.Help).
                setInMainToolbar(true);
    }

    /**
     * Get a brief help content.
     *
     * @return the brief help content
     */
    public String getHelp() {
        return "This shows you how to enable logging ";
    }

    @Override
    public DocumentSelectionSignature[] getSelectionSignatures() {
        return new DocumentSelectionSignature[0];
    }
}