package crossover;

import com.google.common.annotations.VisibleForTesting;

import java.awt.event.ActionEvent;
import java.util.logging.Logger;

public class CrossoverPluginOptions extends MoreOptions {
    private static final Logger logger = Logger.getLogger(CrossoverPluginOptions.class.getName());
    private BooleanOption chimeraFirst;

    CrossoverPluginOptions() {
        addDivider("Order");
        chimeraFirst = addBooleanOption("chimeraFirst", "Chimera is first sequence when checked or last sequence otherwise", true);
        ButtonOption resetOptionsButton = addButtonOption("resetOptions", " ", "Reset Options");
        resetOptionsButton.addActionListener(this::resetOptions);
    }

    @VisibleForTesting
    private void resetOptions(ActionEvent actionEvent) {
        chimeraFirst.setValue(chimeraFirst.getDefaultValue());
    }

    public boolean isChimeraFirst() {
        return chimeraFirst.getValue();
    }
}
