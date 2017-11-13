package crossover;

import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import javafx.util.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FrameGraphOptions extends MoreOptions {
    private static final Logger logger = Logger.getLogger(FrameGraphOptions.class.getName());
    final static java.util.List<Color> COLORS = Stream.of(Color.CYAN, Color.GREEN, Color.YELLOW, Color.ORANGE, Color.MAGENTA, Color.PINK, Color.BLUE, Color.CYAN, Color.GREEN, Color.YELLOW).collect(Collectors.toList());
    //    final Pair<BooleanOption, Boolean> displayAsNumberPair = new Pair(addBooleanOption("displayAsNumber", "Display As Number", true), true);
//    final Pair<BooleanOption, Boolean> displayBaseFramePair = new Pair(addBooleanOption("displayBaseFrame", "Add and Display Base Frame", true), true);
//    final Pair<BooleanOption, Boolean> displayAsGradientPair = new Pair(addBooleanOption("displayAsGradient", "Display Bars As Color Gradient", true), true);
//    final Pair<BooleanOption, Boolean> displayAsPlainColorPair = new Pair(addBooleanOption("displayAsPlainColor", "Display Bars In Plain Color", true), true);
    //
    //final ComboBoxOption baseFrameOption;
    //final OptionValue noSelectionValue = new OptionValue(baseFrame.toString(), " ");
    private List<LabelOption> sequenceColorOptions = Lists.newArrayList();
    private List<Color> sequenceColors = Lists.newArrayList();//needs refresh to work
    private LabelOption crossoverLineColorOption;
    private Color crossoverLineColor = null;//needs refresh to work
    private LabelOption mutantLineColorOption;
    private Color mutantLineColor = null;//needs refresh to work


    FrameGraphOptions(AnnotatedPluginDocument... documents) {
        addDivider("Crossover Line Color");
        crossoverLineColorOption = createAndAddColorLabel("crossoverLineColorOption", "Crossover line color", Color.RED);
        mutantLineColorOption = createAndAddColorLabel("mutantLineColorOption", "Mutant line color", Color.RED);
        ///////////
        addDivider("Sequence Colors");
        for (int i = 0; i < 10; i++) {
            sequenceColorOptions.add(createAndAddColorLabel("Sequence" + (i + 1) + "Option", "Sequence " + (i + 1) + " color", COLORS.get(i)));
        }
        ButtonOption resetOptionsButton = addButtonOption("resetOptions", " ", "Reset Options");
        refreshFineOptions();
        resetOptionsButton.addActionListener(this::resetOptions);
    }

    private LabelOption createAndAddColorLabel(String optionName, String labelPrefix, Color defaultColor) {
        LabelOption labelOption = new LabelOption(optionName, labelPrefix + ":" + getStringForColor(defaultColor));
        //labelOption.setDefaultValue(getStringForColor(defaultColor));//no need the default value as combine string is set from the first initialization
        labelOption = addCustomOption(labelOption);
        JLabel label = labelOption.getComponent();
        label.setOpaque(true);
        label.setBackground(defaultColor);
        label.setPreferredSize(new Dimension(10, (int) label.getPreferredSize().getHeight()));
        label.setMaximumSize(new Dimension(10, 10));
        label.setSize(new Dimension(10, (int) label.getPreferredSize().getHeight()));
        addListener(labelOption);
        return labelOption;
    }

    private void addListener(LabelOption labelOption) {
        JLabel label = labelOption.getComponent();
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Color newColor = JColorChooser.showDialog(label.getParent(), "Choose a color", null);//initial color is White
                if (newColor == null) {//cancel was hit
                    return;
                }
                label.setBackground(newColor);
                String[] split = labelOption.getValue().split(":");
                labelOption.setValue(split[0] + ":" + getStringForColor(newColor));
                refreshFineOptions();
            }
        });
    }

    @VisibleForTesting
    private void resetOptions(ActionEvent actionEvent) {
        sequenceColorOptions.forEach(l -> {
            String defaultValue = l.getDefaultValue();
            l.setValue(defaultValue);
            l.getComponent().setBackground(labelToColor(defaultValue));
        });
        String defaultValue = crossoverLineColorOption.getDefaultValue();
        crossoverLineColorOption.setValue(defaultValue);
        crossoverLineColorOption.getComponent().setBackground(labelToColor(defaultValue));
        String defaultMutantValue = mutantLineColorOption.getDefaultValue();
        mutantLineColorOption.setValue(defaultMutantValue);
        mutantLineColorOption.getComponent().setBackground(labelToColor(defaultMutantValue));
        refreshFineOptions();
    }

    Color getCrossoverLineColor() {
        return crossoverLineColor;
    }

    Color getMutantLineColor() {
        return mutantLineColor;
    }

    public List<Color> getSequenceColors() {
        return sequenceColors;
    }

    private Color labelToColor(String labelToDecode) {
        return Color.decode(labelToDecode.split(":")[1]);
    }

    private void refreshFineOptions() {
        sequenceColors = sequenceColorOptions.stream().map(labelOption -> labelToColor(labelOption.getValue())).collect(Collectors.toList());
        crossoverLineColor = labelToColor(crossoverLineColorOption.getValue());
        mutantLineColor = labelToColor(mutantLineColorOption.getValue());
    }
//
//    public Boolean getDisplayBaseFrame() {
//        return displayBaseFramePair.getKey().getValue();
//    }
//
//    public Boolean getDisplayAsGradient() {
//        return displayAsGradientPair.getKey().getValue();
//    }
//
//    public Boolean getDisplayAsPlainColor() {
//        return displayAsPlainColorPair.getKey().getValue();
//    }
    // public Integer getBaseFrame() {
    //     return Integer.valueOf(baseFrameOption.getValueAsString());
    //  }

    static String getStringForColor(Color color) {
        String red = Integer.toHexString(color.getRed());
        String green = Integer.toHexString(color.getGreen());
        String blue = Integer.toHexString(color.getBlue());
        return "0X" + (red.length() == 1 ? "0" + red : red) +
                (green.length() == 1 ? "0" + green : green) +
                (blue.length() == 1 ? "0" + blue : blue);
    }

    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this)
                .add("sequenceColors", sequenceColors)
                .add("crossoverLineColorOption", crossoverLineColorOption)
                .add("mutantLineColorOption", mutantLineColorOption)
                .toString();
    }

}

/*
    public FrameGraphOptions(AnnotatedPluginDocument... documents) {
        addDivider("General options");
        int colorIndex = 0;
        //addCustomOption("colorList",)
        List<OptionValue> optionValues = Lists.newArrayList();
//        optionValues.add(new OptionValue("" + colorIndex++, Color.RED));
//        optionValues.add(new OptionValue("" + colorIndex++,Color.BLUE));
        String arr[] = {"RED", "BLUE"};
        EditableComboBoxOption editableComboBoxOptionForColor = addEditableComboBoxOption("colorlist", "Color List Label", "RED", arr);
        GComboBox gComboBox = editableComboBoxOptionForColor.getComponent();
        gComboBox.add(new JLabel("label"), Color.BLUE);
        gComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Color) {
                    Color realValue = (Color) value;
                    setText(realValue.toString());
                    setBackground(realValue);
                    if (isSelected) {
                        setBackground(getBackground().darker());
                    }
                } else {
                    if (value.equals("RED")) setBackground(Color.RED);
                    if (value.equals("BLUE")) setBackground(Color.BLUE);
                    System.out.println(value);
                }
                return c;
            }
        });
//        getOption("baseFrame").getComponent().setToolTipText("Select your COLORS");
        ///////////
        addDivider("Others");
        ButtonOption resetOptionsButton = addButtonOption("resetOptions", " ", "Reset Options");
        for (int i = 0; i < 10; i++) {
            LabelOption LabelOption1 = createColorLabel("sequence" + i+1 + "Option", "Sequence " + i+1 + " color",CrossoverSequenceGraph.COLORS.get(i));
        }
        JPanel component = resetOptionsButton.getComponent();
        component.setOpaque(true);
        component.setBackground(Color.CYAN);
        JButton theButton = (JButton) component.getComponent(0);
        theButton.setContentAreaFilled(false);
        theButton.setOpaque(true);
        theButton.setBackground(Color.GREEN);
        theButton.setOpaque(true);
        component.setForeground(Color.RED);
        resetOptionsButton.addActionListener(this::resetOptions);
    }
 */