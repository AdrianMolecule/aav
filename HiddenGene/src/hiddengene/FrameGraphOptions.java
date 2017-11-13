package hiddengene;

import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;
import com.biomatters.geneious.publicapi.plugin.Options;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import javafx.util.Pair;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.logging.Logger;

public class FrameGraphOptions extends Options {

    private static final Logger logger = Logger.getLogger(FrameGraphOptions.class.getName());

    final Pair<BooleanOption, Boolean> displayAsNumberPair = new Pair(addBooleanOption("displayAsNumber", "Display As Number", true), true);
    final Pair<BooleanOption, Boolean> displayBaseFramePair = new Pair(addBooleanOption("displayBaseFrame", "Add and Display Base Frame", true), true);
    final Pair<BooleanOption, Boolean> displayAsGradientPair = new Pair(addBooleanOption("displayAsGradient", "Display Bars As Color Gradient", true), true);
    final Pair<BooleanOption, Boolean> displayAsPlainColorPair = new Pair(addBooleanOption("displayAsPlainColor", "Display Bars In Plain Color", true), true);
    //
    final ComboBoxOption baseFrameOption;
    final Integer baseFrame = -1;//during optimization
    final OptionValue noSelectionValue = new OptionValue(baseFrame.toString(), " ");

    public FrameGraphOptions(AnnotatedPluginDocument... documents) {
        addDivider("General options");
        int frameIndex = 0;
        List<OptionValue> optionValues = Lists.newArrayList();
        optionValues.add(new OptionValue("" + frameIndex++, FrameLogoSequenceGraph.Frame.ZERO.toDisplayString()));
        optionValues.add(new OptionValue("" + frameIndex++, FrameLogoSequenceGraph.Frame.ONE.toDisplayString()));
        optionValues.add(new OptionValue("" + frameIndex++, FrameLogoSequenceGraph.Frame.TWO.toDisplayString()));
        optionValues.add(new OptionValue("" + frameIndex++, FrameLogoSequenceGraph.Frame.REVERSE0.toDisplayString()));
        optionValues.add(new OptionValue("" + frameIndex++, FrameLogoSequenceGraph.Frame.REVERSE1.toDisplayString()));
        optionValues.add(new OptionValue("" + frameIndex++, FrameLogoSequenceGraph.Frame.REVERSE2.toDisplayString()));
        baseFrameOption = addComboBoxOption("baseFrame", "Base Frame", optionValues, optionValues.get(0));
        getOption("baseFrame").getComponent().setToolTipText("Frame 1, Frame 2, Frame 3, Reverse Frame 0, Reverse Frame 2, Reverse Frame 3");
        ///////////
        addDivider("Others");
        ButtonOption resetOptionsButton = addButtonOption("resetOptions", " ", "Reset Options");
        resetOptionsButton.addActionListener(this::resetOptions);
    }

    @VisibleForTesting
    private void resetOptions(ActionEvent actionEvent) {
        resetPair(displayBaseFramePair);
        resetPair(displayBaseFramePair);
        resetPair(displayAsGradientPair);
        resetPair(displayAsPlainColorPair);
        baseFrameOption.setValue(noSelectionValue);
    }

    public Boolean getDisplayAsNumber() {
        return displayAsNumberPair.getKey().getValue();
    }

    public Boolean getDisplayBaseFrame() {
        return displayBaseFramePair.getKey().getValue();
    }

    public Boolean getDisplayAsGradient() {
        return displayAsGradientPair.getKey().getValue();
    }

    public Boolean getDisplayAsPlainColor() {
        return displayAsPlainColorPair.getKey().getValue();
    }

    public Integer getBaseFrame() {
        return Integer.valueOf(baseFrameOption.getValueAsString());
    }

    @Override
    public String toString() {
        return "FrameGraphOptions{" +
                ", displayAsNumber=" + getDisplayAsNumber() +
                ", displayBaseFrame=" + getDisplayBaseFrame() +
                ", displayAsGradient=" + getDisplayAsGradient() +
                ", displayAsPlainColor=" + getDisplayAsPlainColor() +
                ", baseFrame=" + getBaseFrame() +
                '}';
    }

    private void resetPair(Pair<BooleanOption, Boolean> pair) {
        pair.getKey().setValue(pair.getValue());
    }
}
