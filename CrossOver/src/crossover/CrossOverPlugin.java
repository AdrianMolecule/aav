package crossover;

import com.biomatters.geneious.publicapi.components.Dialogs;
import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;
import com.biomatters.geneious.publicapi.documents.DocumentField;
import com.biomatters.geneious.publicapi.implementations.DefaultAlignmentDocument;
import com.biomatters.geneious.publicapi.plugin.*;

import java.util.stream.Stream;

public class CrossOverPlugin extends GeneiousPlugin {

    public static final SequenceGraphFactory[] EMPTY_SEQUENCE_GRAPH_FACTORIES = {};
    public static final SequenceViewerExtension.Factory[] EMPTY_SEQUENCE_VIEWER_EXTENSION_FACTORIES = {};

    public String getName() {
        return "CrossOverPlugin";
    }

    public String getHelp() {
        return "CrossOverPlugin";
    }

    public String getDescription() {
        return "CrossOverPlugin";
    }

    public String getAuthors() {
        return "CMRI";
    }

    public String getVersion() {
        return "2.0";
    }

    public String getMinimumApiVersion() {
        return "4.1";
    }

    public int getMaximumApiVersion() {
        return 4;
    }

    public SequenceGraphFactory[] getSequenceGraphFactories() {
        boolean chimeraFirst = true;//((CrossoverPluginOptions) (getPluginPreferences().get(0).getActiveOptions())).isChimeraFirst();
        return Stream.of(new CrossoverGraphFactory(chimeraFirst)).toArray(SequenceGraphFactory[]::new);
    }
    //todo would this be better? toArray(SequenceGraphFactory::createDocumentBasedGraphForAlignment());

    @Override
    public SequenceViewerExtension.Factory[] getSequenceViewerExtensionFactories() {
        boolean chimeraFirst = true;// ((CrossoverPluginOptions) (getPluginPreferences().get(0).getActiveOptions())).isChimeraFirst();
        return new SequenceViewerExtension.Factory[]{new CrossoverStatistics(chimeraFirst)};
    }

    static boolean shouldNotBeEnabled(AnnotatedPluginDocument annotatedPluginDocument) {
        Object fieldValue = annotatedPluginDocument.getFieldValue(TagAsCrossoverOperation.CROSSOVER_FIELD);
        if (fieldValue != null && (Boolean) fieldValue) {
            try {
                if ((((DefaultAlignmentDocument) annotatedPluginDocument.getDocument())).getNumberOfSequences() < 3) {
                    Dialogs.showMessageDialog("For a valid crossover alignment you need to have one resulting sequence and at least two parent sources.");
                    return true;
                } else {
                    return false;
                }
            } catch (DocumentOperationException ex) {
                return true;
            }
        } else {
            return true;
        }
    }

    public DocumentOperation[] getDocumentOperations() {
        return new DocumentOperation[]{new TagAsCrossoverOperation()};
    }
}
