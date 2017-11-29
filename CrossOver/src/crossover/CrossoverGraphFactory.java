package crossover;

import com.biomatters.geneious.publicapi.components.Dialogs;
import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;
import com.biomatters.geneious.publicapi.documents.sequence.SequenceDocument;
import com.biomatters.geneious.publicapi.implementations.DefaultAlignmentDocument;
import com.biomatters.geneious.publicapi.plugin.DocumentOperationException;
import com.biomatters.geneious.publicapi.plugin.SequenceGraph;
import com.biomatters.geneious.publicapi.plugin.SequenceGraphFactory;
import jebl.evolution.sequences.SequenceType;

class CrossoverGraphFactory extends SequenceGraphFactory {
    boolean isChimeraFirst;

    public CrossoverGraphFactory(boolean isChimeraFirst) {
        System.out.println("in CrossoverGraphFactory and isChimeraFirst:" + isChimeraFirst);
        this.isChimeraFirst = isChimeraFirst;
    }

    public CrossoverGraphFactory() {
        System.out.println("in CrossoverGraphFactory default constructor and isChimeraFirst:" + isChimeraFirst);
    }

    @Override
    public SequenceGraph createResidueBasedGraph(SequenceDocument.Alphabet alphabet, boolean isAlignment, boolean isChromatogram, boolean isContig) {
        return null;
    }

    @Override
    public SequenceGraph createDocumentBasedGraphForAlignment(AnnotatedPluginDocument annotatedPluginDocument) {
        CrossoverModel.reset();
        //System.out.println("in createDocumentBasedGraphForAlignment and isChimeraFirst:" + isChimeraFirst);
        if (CrossOverPlugin.shouldNotBeEnabled(annotatedPluginDocument)) {
            return null;
        } else {
            try {
                if (!(annotatedPluginDocument.getDocument() instanceof DefaultAlignmentDocument || ((DefaultAlignmentDocument) annotatedPluginDocument.getDocument()).getSequenceType() != SequenceType.NUCLEOTIDE)) {//needs to be alignment
                    return null; // This graph only appears on alignments
                }
                if (((DefaultAlignmentDocument) (annotatedPluginDocument.getDocument())).getSequences().size() < 3) {
                    Dialogs.showMessageDialog("You need to have one resulting chimera and at least two parents.");
                }
            } catch (DocumentOperationException e) {
                e.printStackTrace();
                return null;
            }
            return new CrossoverSequenceGraph(annotatedPluginDocument, isChimeraFirst);
        }
    }
}
