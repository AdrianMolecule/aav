package directedevolution;

import com.biomatters.geneious.publicapi.plugin.DocumentOperation;
import com.biomatters.geneious.publicapi.plugin.GeneiousPlugin;
import com.biomatters.geneious.publicapi.plugin.SequenceViewerExtension;
import directedevolution.logging.LoginDocumentationOperation;

/**
 * The bootstrap class for the DirectedEvolutionPlugin. Its function is to act as a
 * factory class for the GenerateOligosOperation class and to provide basic
 * information about this plugin to Geneious.
 * <p>
 * This plugins generates a list oligonucleotide sequences for directed evolution.
 * It starts with a translation alignment on variant nucleotides sequences we want to evolve.
 * It produces optimized nucleotide sequences to increase homology.
 * It produces a consensus sequence of the combined optimized sequences.
 * It segments the consensus sequence in overlapping nucleotides to allow hybridization.
 * Finally. it produces a list of oligonucleotides that can be used for rapid directed evolution.
 * <p>
 * Since the number of generated variants could easily exceed production capacity it's
 * necessary to assess the number of variants based on the target nucleotide sequence position and length.
 * Therefore, an additional feature of the plugin calculates and displays (in the statistics tab) the number of variants of a selected alignment of proteins.
 * These proteins must be the translations of the variant nucleotide sequences we want to evolve.
 *
 * @author Adrian Filip
 */
public class DirectedEvolutionPlugin extends GeneiousPlugin {
    private final String HELP = "  The bootstrap class for the DirectedEvolutionPlugin. Its function is to act as a\n" +
            "  factory class for the GenerateOligosOperation class and to provide basic\n" +
            "  information about this plugin to Geneious.\n" +
            "  \n" +
            "  This plugins generates a list oligonucleotide sequences for directed evolution.\n" +
            "  It starts with a translation alignment on variant nucleotides sequences we want to evolve.\n" +
            "  It produces optimized nucleotide sequences to increase homology.\n" +
            "  It produces a consensus sequence of the combined optimized sequences.\n" +
            "  It segments the consensus sequence in overlapping nucleotides to allow hybridization.\n" +
            "  Finally. it produces a list of oligonucleotides that can be used for rapid directed evolution.\n" +
            "  To assess the number of variant combinations produced by oligos we added the \"Total Variants\" on the statistics tab.\n " +
            "  That becomes active when selecting any subsequence from the Optimized Consensus sequence.\n" +
            " \n" +
            "  An additional similar feature available for protein only calculates and displays (in the statistics tab) the number of variants of a selected alignment of proteins or nucleotides.\n" +
            "  These proteins must be the translations of the variant nucleotide sequences we want to evolve.\n\n" +
            "  Compare Selected Sequences allows to compare two selected sequences or a selected sequence and a sequence in the clipboard. Works on most sequence types";

    public String getAuthors() {
        return "Adrian F.";
    }

    public String getDescription() {
        return "A tool to generate oligonucleotides based on a translation alignment of multiple variant nucleotide sequences.\n\n" +
                "Select several nucleotide sequences from known variants\n" +
                "Click Align/Assemble from Top Menu and Select Translation Menu and choose Alignment Type: Global alignment. This will solve potential start codon misalignment.\n" +
                "That will generate a translation alignment.\n" +
                "Select the Alignment generated in previous step and do Tools/Generate Oligos.\n" +
                "If you like the result you could save it with File/Save Generated Oligos.\n\n" +
                "Statistics calculating the total number of variants can be obtained by selecting a region of a sequence and display the statistics\n\n" +
                "Compare Selected Sequences allows to compare two selected sequences or a selected sequence and a sequence in the clipboard. Works on most sequence types.\n"+
                "Under Sequence ... Compare Selected Sequences";
    }

    public DocumentOperation[] getDocumentOperations() {
        return new DocumentOperation[]{new GenerateOligosOperation(), new SaveOligosOperation(), new CompareSelectedSequencesOperation(), new LoginDocumentationOperation()};
    }

    public String getHelp() {
        return HELP;
    }

    /**
     * The maximum API version is checked only against the major version number.
     * Thus, if getMaximumApiVersion() returns 4, the plugin will work with API
     * versions 4.1, 4.2, 4.3 and any other version 4.x, but not version 5.0
     * or above.
     */
    public int getMaximumApiVersion() {
        return 4;
    }

    /**
     * The minimum API version is checked against the entire API version string.
     * If the minimum API version is 4.0, the plugin will not work with API
     * versions 3.9 or below.
     */
    public String getMinimumApiVersion() {
        return "4.0";
    }

    public String getName() {
        return "Directed Evolution";
    }

    public String getVersion() {
        return "2.4.0";
    }

    @Override
    public SequenceViewerExtension.Factory[] getSequenceViewerExtensionFactories() {
        return new SequenceViewerExtension.Factory[]{new VariantStatistics()};
    }

    @Override
    public String getEmailAddressForCrashes() {
        return "tada@specyal.com";
    }
}
