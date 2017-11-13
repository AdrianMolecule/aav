package crossover;

import com.google.common.collect.Maps;
import crossover.utils.CrossoverUtils;
import jebl.evolution.sequences.CodonState;
import jebl.evolution.sequences.Codons;

import java.util.Map;
import java.util.Optional;

class MaxBag {
    private Map<String, Integer> codonStateIntegerMap = Maps.newHashMap();

    void add(CodonState codon) {
        if (codon == Codons.GAP_STATE || codon == null) return;
        String threeLetterName = CrossoverUtils.toAminoAcid(codon).getThreeLetterName();
        if (codonStateIntegerMap.containsKey(threeLetterName)) {
            codonStateIntegerMap.put(threeLetterName, codonStateIntegerMap.get(threeLetterName) + 1);
        } else {
            codonStateIntegerMap.put(threeLetterName, 1);
        }
    }

    Integer getMax() {
        //noinspection ConstantConditions
        if (codonStateIntegerMap.isEmpty()) {
            return 0;
        }
        Optional<Map.Entry<String, Integer>> max = codonStateIntegerMap.entrySet().stream().max((entry1, entry2) -> Integer.compare(entry1.getValue(), entry2.getValue()));
        return max.map(Map.Entry::getValue).orElse(0);
    }
}