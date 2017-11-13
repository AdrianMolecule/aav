package hiddengene;

import com.biomatters.geneious.publicapi.plugin.GeneiousPlugin;
import com.biomatters.geneious.publicapi.plugin.SequenceGraphFactory;
import com.google.common.collect.Lists;
import jebl.evolution.sequences.Codons;

import java.util.List;
import java.util.stream.Stream;

public class HiddenGenePlugin extends GeneiousPlugin {

    public String getName() {
        return "HiddenGenePlugin";
    }

    public String getHelp() {
        return "HiddenGenePlugin";
    }

    public String getDescription() {
        return "HiddenGenePlugin";
    }

    public String getAuthors() {
        return "Biomatters";
    }

    public String getVersion() {
        return "0.1";
    }

    public String getMinimumApiVersion() {
        return "4.1";
    }

    public int getMaximumApiVersion() {
        return 4;
    }

    public SequenceGraphFactory[] getSequenceGraphFactories() {
        return Stream.of(
                new MultilogoGraphFactory(FrameLogoSequenceGraph.Frame.ONE),
                new MultilogoGraphFactory(FrameLogoSequenceGraph.Frame.TWO),
                new MultilogoGraphFactory(FrameLogoSequenceGraph.Frame.REVERSE0),
                new MultilogoGraphFactory(FrameLogoSequenceGraph.Frame.REVERSE1),
                new MultilogoGraphFactory(FrameLogoSequenceGraph.Frame.REVERSE2)
        ).
                toArray(SequenceGraphFactory[]::new);
    }


}
