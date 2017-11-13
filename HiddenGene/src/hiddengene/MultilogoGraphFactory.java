package hiddengene;

import com.biomatters.geneious.publicapi.documents.sequence.SequenceDocument;
import com.biomatters.geneious.publicapi.plugin.GeneiousPlugin;
import com.biomatters.geneious.publicapi.plugin.SequenceGraph;
import com.biomatters.geneious.publicapi.plugin.SequenceGraphFactory;

class MultilogoGraphFactory extends SequenceGraphFactory {
    private FrameLogoSequenceGraph.Frame frame;

    public MultilogoGraphFactory(FrameLogoSequenceGraph.Frame frame) {
        this.frame = frame;
    }

    @Override
    public SequenceGraph createResidueBasedGraph(SequenceDocument.Alphabet alphabet, boolean isAlignment, boolean isChromatogram, boolean isContig) {
        if (!isAlignment || isContig) {
            return null; // This graph only appears on alignments (not unaligned sequences and not contigs)
        }
        return new FrameLogoSequenceGraph(alphabet, frame);
    }
}
