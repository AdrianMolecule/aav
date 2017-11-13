package crossover;

import com.biomatters.geneious.publicapi.plugin.Options;

import java.awt.*;

import static crossover.FrameGraphOptions.getStringForColor;

public class MoreOptions extends Options {

    public final LabelOption addLabelOption(String optionName, String label, Color defaultColor) {
        LabelOption labelOption = new LabelOption(optionName, label);
        labelOption.setDefaultValue(getStringForColor(defaultColor));
        return addCustomOption(labelOption);
    }
//
//    public final MoreOptions.ColorOption addColorOption(String optionName, String label, Color defaultColor) {
//        MoreOptions.ColorOption var4 = new MoreOptions.ColorOption(optionName, label, defaultColor);
//        return (MoreOptions.ColorOption) this.addCustomOption(var4);
//    }
//    public static class ColorOption extends Options.Option<Color, JPanel> {
//        //        String a;
//        private Color color = Color.RED;
//
//        protected ColorOption(Element var1) throws XMLSerializationException {
//            super(var1);
//            //this.color = var1.getChildText("layoutNextToOption");
//        }
//
//        protected ColorOption(String optionName, String label, Color defaultColor) {
//            super(optionName, label, defaultColor);
//        }
//
//        public String getDisplayedLabel() {
//            return null;
//        }
//
//        public Color getValueFromString(String rgbValue) {
//            try {
//                return new Color(Integer.valueOf(rgbValue));
//            } catch (Exception var3) {
//                var3.printStackTrace();
//                return (Color) this.getDefaultValue();
//            }
//        }
//
//        protected JPanel createComponent() {
//            final JPanel panel = new JPanel();
////            panel.setSelected(((Boolean) this.getValue()).booleanValue());
////            panel.addItemListener(new ItemListener() {
////                public void itemStateChanged(ItemEvent var1x) {
////                    if (!Options.BooleanOption.this.a) {
////                        Options.BooleanOption.this.a = true;
////                        Options.BooleanOption.this.setValue(Boolean.valueOf(var1.isSelected()), true);
////                        Options.BooleanOption.this.a = false;
////                    }
////                }
////            });
//            panel.setOpaque(true);
//            panel.setBackground(color);
//            //if (super.a.isVerticallyCompact(this.isAdvanced())) {
//            panel.setPreferredSize(new Dimension(10, 10));
//            // }
//            return panel;
//        }
////
////        protected String getPossibleValues() {
////            return "'" + Boolean.TRUE.toString() + "' or '" + Boolean.FALSE.toString() + "'";
////        }
//
//        protected void setValueOnComponent(JPanel panel, Color color) {
//            this.color = color;
//            panel.setBackground(color);
//            this.color = color;
//        }
////
////        public void addDependent(Options.Option var1, boolean var2) {
////            super.addDependent(var1, Boolean.valueOf(var2));
////        }
////
////        public void addDependent(Options.Option var1, boolean var2, boolean var3) {
////            super.addDependent(var1, Boolean.valueOf(var2));
////            if (var3) {
////                if (this.a != null) {
////                    throw new IllegalStateException("You cannot call addDependent() with layoutCheckboxNextToDependent set as true more than once.");
////                }
////                if (var1.l) {
////                    throw new IllegalArgumentException("The option " + var1.getName() + " has already been added as a dependent of some option");
////                }
////                this.a = var1.getName();
////                var1.l = true;
////            }
////        }
////
////        public Element toXML() {
////            Element var1 = super.toXML();
////            if (this.a != null) {
////                var1.addContent((new Element("layoutNextToOption")).setText(this.a));
////            }
////            return var1;
////        }
////
////        public boolean moveToOptions(Options var1, String var2, String var3) {
////            this.a = null;
////            return super.moveToOptions(var1, var2, var3);
////        }
////    }
//    }

    public static class ColorLabelOption {
        final Options.LabelOption innerLabel;
        final Color defaultColor;

        public ColorLabelOption(Options.LabelOption innerLabel, Color defaultColor) {
            this.innerLabel = innerLabel;
            this.defaultColor = defaultColor;
        }

        public Options.LabelOption getInnerLabelOption() {
            return innerLabel;
        }

        public Color getDefault() {
            return defaultColor;
        }
    }
//
//    protected MyXOption(String s, String s1, String s2) {
//        super(s, s1,
//
//public static class MyXOption extends Options.Option<String, JLabel> {
//
//    protected MyXOption(String s, String s1, String s2) {
//        super(s, s1, s2);
//    }
//
//    @Override
//    public String getValueFromString(String s) {
//        return null;
//    }
//
//    @Override
//    protected void setValueOnComponent(JLabel jLabel, String s) {
//    }
//
//    @Override
//    protected JLabel createComponent() {
//        return null;
//    }
//}
//
//public static class MyLabelOption extends Options.Option<String, JLabel> {
//    private boolean b;
//    private JLabel a;
//
//    public boolean isRestorePreferenceApplies() {
//        return false;
//    }
//
//    public boolean shouldSaveValue() {
//        return false;
//    }
//
//    public Element toXML() {
//        Element var1 = super.toXML();
//        return var1;
//    }
//
//    protected MyLabelOption(Element var1) throws XMLSerializationException {
//        super(var1);
//        this.b = false;
//        this.b = var1.getAttribute("notVisibleWhenDisabled") != null;
//    }
//
//    public MyLabelOption(String var1, String var2) {
//        super(var1, "", var2);
//        this.b = false;
//    }
//
//    private MyLabelOption(String var1, int var2, boolean var3, boolean var4) {
//        super("label_" + var2, "", var1);
//        this.b = false;
//        this.setRestoreDefaultApplies(false);
//        this.setRestorePreferenceApplies(false);
//        this.setFillHorizontalSpace(var4 || var3);
//        this.setSpanningComponent(var4);
//    }
//
//    public String getValueFromString(String var1) {
//        return var1;
//    }
//
//    protected void setValueOnComponent(JLabel var1, String var2) {
//        var1.setText(var2);
//        this.a(var1, var2);
//    }
//
//    protected JLabel createComponent() {
//
//
//        return this.a;
//    }
//
//    private void a(JLabel var1, String var2) {
//        var1.setCursor(Cursor.getPredefinedCursor(b(var2) != null?12:0));
//    }
//
//    private static boolean a(String var0) {
//        var0 = var0.toLowerCase();
//        return var0.startsWith("<html>") && var0.contains("<a href=\"");
//    }
//
//    private static URL b(String var0) {
//        if(var0.toLowerCase().startsWith("<html>")) {
//            String var1 = "<a href=\"";
//            int var2 = var0.toLowerCase().indexOf(var1);
//            if(var2 >= 0) {
//                int var3 = var0.toLowerCase().indexOf(var1, var2 + 1);
//                if(var3 < 0) {
//                    int var4 = var2 + var1.length();
//                    int var5 = var0.indexOf("\"", var4);
//                    if(var5 > 0) {
//                        String var6 = var0.substring(var4, var5);
//
//                        try {
//                            return new URL(var6);
//                        } catch (MalformedURLException var8) {
//                            ;
//                        }
//                    }
//                }
//            }
//        }
//
//        return null;
//    }
//
//    public boolean isSelectable() {
//        return false;
//    }
//
//    public void setSelectable(boolean var1) {
//
//    }
//
//    public void setCenterText(boolean var1) {
//
//    }
//
//    public boolean isCenterText() {
//        return false;
//    }
//
//    public void setIcon(Icon var1) {
//
//    }
//
//    public Icon getIcon() {
//        return null;
//    }
//
//    public void setVisible(boolean var1) {
//        super.setVisible(var1);
//        if(this.a != null) {
//            this.a.repaint();
//        }
//
//    }
//
//    public void setVisibleOnlyWhenEnabled(boolean var1) {
//        this.b = var1;
//    }
//
//    public boolean isVisibleOnlyWhenEnabled() {
//        return this.b;
//    }
//
//    protected void handleSetEnabled(JLabel var1, boolean var2) {
//        super.handleSetEnabled(var1, var2);
//        SystemColor var3 = var2?SystemColor.textText:SystemColor.textInactiveText;
//        var1.setForeground(var3);
//
//
//    }
//
//    private static class JLabelWrapper extends GLabel {
//        private final GTextPane textPane;
//        private Dimension preferredSize;
//
//        private JLabelWrapper(String var1) {
//            super(var1);
//            this.textPane = GTextPane.createHtmlPane(var1);
//            this.textPane.setCursor(Cursor.getPredefinedCursor(0));
//            this.setLayout(new BorderLayout());
//            this.add(this.textPane, "Center");
//        }
//
//        public void paint(Graphics var1) {
//            this.textPane.paint(var1);
//        }
//
//        public void setText(String var1) {
//            super.setText(var1);
//            if(this.textPane != null) {
//                this.textPane.setText("<html>" + GuiUtilities.getDialogHtmlHead() + var1 + "</html>");
//            }
//
//        }
//
//        public Dimension getMinimumSize() {
//            return this.textPane.getPreferredSize();
//        }
//
//        public Dimension getPreferredSize() {
//            return this.preferredSize != null?this.preferredSize:this.textPane.getPreferredSize();
//        }
//
//        public void setPreferredSize(Dimension var1) {
//            this.preferredSize = var1;
//        }
//
//        public Dimension getMaximumSize() {
//            return this.textPane.getMaximumSize();
//        }
//    }
//}
}
