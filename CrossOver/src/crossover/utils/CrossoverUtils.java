package crossover.utils;

import com.biomatters.geneious.publicapi.utilities.SequenceUtilities;
import jebl.evolution.sequences.AminoAcidState;
import jebl.evolution.sequences.AminoAcids;
import jebl.evolution.sequences.CodonState;
import jebl.evolution.sequences.GeneticCode;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

public class CrossoverUtils {
    public static int calculateLevenshteinDistanceAfterRemovingGaps(CharSequence first, CharSequence second) {
        return StringUtils.getLevenshteinDistance(first.toString().replace("-", ""), second.toString().replace("-", ""));
    }


    @Nullable
    public static AminoAcidState toAminoAcid(CodonState codon) {//this is also in DirectedEvolution
        String code = codon.getCode();
        if ("---".equals(code) || code == null) {
            return null;
        }
        CharSequence charSequence = SequenceUtilities.asTranslation(code, GeneticCode.UNIVERSAL, false);//todo not sure what the boolean is for
        return AminoAcids.getState(charSequence.toString());
    }
}
