import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

import java.text.DecimalFormat;
public class Calculator extends JFrame {

    // ---------- Apple-inspired color palette ----------
    private static final Color BG_DARK        = new Color(0x1C1C1E);
    private static final Color DISPLAY_BG      = new Color(0x1C1C1E);
    private static final Color NUM_BTN         = new Color(0x3A3A3C);
    private static final Color NUM_BTN_HOVER   = new Color(0x4A4A4C);
    private static final Color FUNC_BTN        = new Color(0xA5A5A5);
    private static final Color FUNC_BTN_HOVER  = new Color(0xBFBFBF);
    private static final Color SCI_BTN         = new Color(0x2C2C2E);
    private static final Color SCI_BTN_HOVER   = new Color(0x3C3C3E);
    private static final Color OP_BTN          = new Color(0xFF9F0A);
    private static final Color OP_BTN_HOVER    = new Color(0xFFB340);
    private static final Color OP_BTN_ACTIVE   = new Color(0xFFFFFF);
    private static final Color TEXT_WHITE      = Color.WHITE;
    private static final Color TEXT_BLACK      = Color.BLACK;
    private static final Color HISTORY_BG      = new Color(0x121214);

    // ---------- State ----------
    private double currentValue = 0;
    private double storedValue = 0;
    private String pendingOp = null;
    private boolean startNewNumber = true;
    private boolean errorState = false;

    private final DecimalFormat fmt = new DecimalFormat("#,##0.##########");

    private JLabel expressionLabel;
    private JLabel displayLabel;
    private DefaultListModel<String> historyModel;
    private JList<String> historyList;
    private JPanel historyPanel;
    private boolean historyVisible = false;
    private JButton opActive = null; // currently highlighted operator button

    public Calculator() {
        super("Calculator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(760, 560);
        setMinimumSize(new Dimension(560, 520));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout());

        add(buildDisplayPanel(), BorderLayout.NORTH);
        add(buildButtonPanel(), BorderLayout.CENTER);
        add(buildHistoryPanel(), BorderLayout.EAST);

        // Keyboard support
        setupKeyBindings();

        updateDisplay();
    }

    // ================= DISPLAY =================
    private JPanel buildDisplayPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(DISPLAY_BG);
        panel.setBorder(new EmptyBorder(20, 24, 10, 24));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(DISPLAY_BG);
        JButton historyToggle = new RoundedButton("🕘", SCI_BTN, SCI_BTN_HOVER, TEXT_WHITE, 16f);
        historyToggle.setPreferredSize(new Dimension(40, 36));
        historyToggle.addActionListener(e -> toggleHistory());
        JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        togglePanel.setBackground(DISPLAY_BG);
        togglePanel.add(historyToggle);
        topBar.add(togglePanel, BorderLayout.WEST);

        expressionLabel = new JLabel(" ");
        expressionLabel.setForeground(new Color(0x8E8E93));
        expressionLabel.setFont(new Font("SF Pro Display", Font.PLAIN, 18));
        expressionLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        expressionLabel.setBorder(new EmptyBorder(6, 0, 0, 0));

        displayLabel = new JLabel("0");
        displayLabel.setForeground(TEXT_WHITE);
        displayLabel.setFont(new Font("SF Pro Display", Font.PLAIN, 56));
        displayLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        panel.add(topBar);
        panel.add(expressionLabel);
        panel.add(displayLabel);
        return panel;
    }

    // ================= HISTORY PANEL =================
    private JPanel buildHistoryPanel() {
        historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBackground(HISTORY_BG);
        historyPanel.setPreferredSize(new Dimension(220, 0));
        historyPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("History");
        title.setForeground(TEXT_WHITE);
        title.setFont(new Font("SF Pro Display", Font.BOLD, 18));
        title.setBorder(new EmptyBorder(4, 4, 10, 4));

        JButton clearBtn = new JButton("Clear");
        clearBtn.setFocusPainted(false);
        clearBtn.setBackground(SCI_BTN);
        clearBtn.setForeground(TEXT_WHITE);
        clearBtn.addActionListener(e -> historyModel.clear());

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(HISTORY_BG);
        top.add(title, BorderLayout.WEST);
        top.add(clearBtn, BorderLayout.EAST);

        historyModel = new DefaultListModel<>();
        historyList = new JList<>(historyModel);
        historyList.setBackground(HISTORY_BG);
        historyList.setForeground(TEXT_WHITE);
        historyList.setFont(new Font("SF Pro Display", Font.PLAIN, 14));
        historyList.setSelectionBackground(new Color(0x2C2C2E));
        historyList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && historyList.getSelectedIndex() != -1) {
                String entry = historyList.getSelectedValue();
                String[] parts = entry.split("=");
                if (parts.length == 2) {
                    try {
                        currentValue = Double.parseDouble(parts[1].trim().replace(",", ""));
                        startNewNumber = true;
                        pendingOp = null;
                        expressionLabel.setText(" ");
                        updateDisplay();
                    } catch (NumberFormatException ignored) {}
                }
            }
        });

        JScrollPane scroll = new JScrollPane(historyList);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(HISTORY_BG);

        historyPanel.add(top, BorderLayout.NORTH);
        historyPanel.add(scroll, BorderLayout.CENTER);
        historyPanel.setVisible(false);
        return historyPanel;
    }

    private void toggleHistory() {
        historyVisible = !historyVisible;
        historyPanel.setVisible(historyVisible);
        revalidate();
        repaint();
    }

    // ================= BUTTON GRID =================
    private JPanel buildButtonPanel() {
        JPanel container = new JPanel(new GridLayout(7, 5, 10, 10));
        container.setBackground(BG_DARK);
        container.setBorder(new EmptyBorder(10, 20, 20, 20));

        // Row 1: scientific
        container.add(sciButton("2nd"));
        container.add(sciButton("x²"));
        container.add(sciButton("x^y"));
        container.add(sciButton("eˣ"));
        container.add(sciButton("10ˣ"));

        // Row 2: scientific
        container.add(sciButton("1/x"));
        container.add(sciButton("√x"));
        container.add(sciButton("∛x"));
        container.add(sciButton("ln"));
        container.add(sciButton("log₁₀"));

        // Row 3: scientific
        container.add(sciButton("x!"));
        container.add(sciButton("sin"));
        container.add(sciButton("cos"));
        container.add(sciButton("tan"));
        container.add(sciButton("π"));

        // Row 4: function row + AC
        container.add(funcButton("AC"));
        container.add(funcButton("±"));
        container.add(funcButton("%"));
        container.add(sciButton("e"));
        container.add(opButton("÷"));

        // Row 5: 7 8 9 ×
        container.add(numButton("7"));
        container.add(numButton("8"));
        container.add(numButton("9"));
        container.add(opButton("×"));
        container.add(sciButton("("));

        // Row 6: 4 5 6 -
        container.add(numButton("4"));
        container.add(numButton("5"));
        container.add(numButton("6"));
        container.add(opButton("−"));
        container.add(sciButton(")"));

        // Row 7: 1 2 3 + and 0 . =
        container.add(numButton("1"));
        container.add(numButton("2"));
        container.add(numButton("3"));
        container.add(opButton("+"));
        container.add(sciButton("Rand"));

        // Extra bottom row (0, ., =)
        JPanel bottomRow = new JPanel(new GridLayout(1, 5, 10, 10));
        bottomRow.setBackground(BG_DARK);
        bottomRow.add(numButton("0"));
        bottomRow.add(numButton("."));
        bottomRow.add(opButton("="));

        JPanel wrapper = new JPanel(new BorderLayout(0, 10));
        wrapper.setBackground(BG_DARK);
        wrapper.add(container, BorderLayout.CENTER);
        return wrapUpWithBottomRow(wrapper, container);
    }

    
    private JPanel wrapUpWithBottomRow(JPanel wrapper, JPanel grid) {
    
        JPanel finalGrid = new JPanel(new GridLayout(8, 5, 10, 10));
        finalGrid.setBackground(BG_DARK);
        for (Component c : grid.getComponents()) {
            finalGrid.add(c);
        }
        finalGrid.add(numButton("0"));
        finalGrid.add(numButton("."));
        finalGrid.add(funcButton("⌫"));
        finalGrid.add(opButton("="));
        finalGrid.add(sciButton("Ans"));

        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(BG_DARK);
        outer.setBorder(new EmptyBorder(10, 20, 20, 20));
        outer.add(finalGrid, BorderLayout.CENTER);
        return outer;
    }

    // ================= BUTTON FACTORIES =================
    private JButton numButton(String label) {
        RoundedButton b = new RoundedButton(label, NUM_BTN, NUM_BTN_HOVER, TEXT_WHITE, 22f);
        b.addActionListener(e -> onNumberOrDot(label));
        return b;
    }

    private JButton funcButton(String label) {
        RoundedButton b = new RoundedButton(label, FUNC_BTN, FUNC_BTN_HOVER, TEXT_BLACK, 20f);
        b.addActionListener(e -> onFunction(label));
        return b;
    }

    private JButton opButton(String label) {
        RoundedButton b = new RoundedButton(label, OP_BTN, OP_BTN_HOVER, TEXT_WHITE, 24f);
        b.addActionListener(e -> onOperator(label, b));
        return b;
    }

    private JButton sciButton(String label) {
        RoundedButton b = new RoundedButton(label, SCI_BTN, SCI_BTN_HOVER, TEXT_WHITE, 16f);
        b.addActionListener(e -> onScientific(label));
        return b;
    }

    // ================= LOGIC =================
    private void onNumberOrDot(String label) {
        if (errorState) { clearAll(); }
        if (opActive != null) resetOpHighlight();
        String current = displayLabel.getText().replace(",", "");
        if (startNewNumber) {
            current = label.equals(".") ? "0." : label;
            startNewNumber = false;
        } else {
            if (label.equals(".") && current.contains(".")) return;
            if (current.equals("0") && !label.equals(".")) {
                current = label;
            } else {
                current = current + label;
            }
        }
        try {
            currentValue = current.endsWith(".") ? Double.parseDouble(current + "0") : Double.parseDouble(current);
        } catch (NumberFormatException ex) {
            currentValue = 0;
        }
        displayLabel.setText(formatRaw(current));
    }

    private void onFunction(String label) {
        resetOpHighlight();
        switch (label) {
            case "AC":
                clearAll();
                break;
            case "±":
                currentValue = -currentValue;
                displayLabel.setText(fmt.format(currentValue));
                break;
            case "%":
                currentValue = currentValue / 100.0;
                displayLabel.setText(fmt.format(currentValue));
                break;
            case "⌫":
                String cur = displayLabel.getText().replace(",", "");
                if (cur.length() > 1) {
                    cur = cur.substring(0, cur.length() - 1);
                } else {
                    cur = "0";
                    startNewNumber = true;
                }
                try { currentValue = Double.parseDouble(cur); } catch (Exception ex) { currentValue = 0; }
                displayLabel.setText(formatRaw(cur));
                break;
        }
    }

    private void onOperator(String label, JButton source) {
        if (errorState) clearAll();
        resetOpHighlight();
        if (label.equals("=")) {
            compute();
            opActive = null;
            return;
        }
        if (pendingOp != null && !startNewNumber) {
            compute();
        } else {
            storedValue = currentValue;
        }
        pendingOp = label;
        startNewNumber = true;
        expressionLabel.setText(fmt.format(storedValue) + " " + label);
        if (source != null) {
            opActive = (RoundedButton) source;
            source.setBackground(OP_BTN_ACTIVE);
            source.setForeground(TEXT_BLACK);
        }
    }

    private void resetOpHighlight() {
        if (opActive != null) {
            opActive.setBackground(OP_BTN);
            opActive.setForeground(TEXT_WHITE);
            opActive = null;
        }
    }

    private void compute() {
        if (pendingOp == null) return;
        double a = storedValue;
        double b = currentValue;
        double result;
        String opSymbol = pendingOp;
        switch (pendingOp) {
            case "+": result = a + b; break;
            case "−": result = a - b; break;
            case "×": result = a * b; break;
            case "÷":
                if (b == 0) { showError(); return; }
                result = a / b;
                break;
            case "^": result = Math.pow(a, b); break;
            default: result = b;
        }
        String histEntry = fmt.format(a) + " " + opSymbol + " " + fmt.format(b) + " = " + fmt.format(result);
        historyModel.add(0, histEntry);
        currentValue = result;
        storedValue = result;
        displayLabel.setText(fmt.format(result));
        expressionLabel.setText(fmt.format(a) + " " + opSymbol + " " + fmt.format(b) + " =");
        startNewNumber = true;
        pendingOp = null;
    }

    private void onScientific(String label) {
        if (errorState) clearAll();
        resetOpHighlight();
        double x = currentValue;
        double result = x;
        String exprText = null;
        try {
            switch (label) {
                case "x²": result = x * x; exprText = "sqr(" + fmt.format(x) + ")"; break;
                case "x^y":
                    storedValue = currentValue;
                    pendingOp = "^";
                    startNewNumber = true;
                    expressionLabel.setText(fmt.format(storedValue) + " ^");
                    return;
                case "eˣ": result = Math.exp(x); exprText = "exp(" + fmt.format(x) + ")"; break;
                case "10ˣ": result = Math.pow(10, x); exprText = "10^(" + fmt.format(x) + ")"; break;
                case "1/x":
                    if (x == 0) { showError(); return; }
                    result = 1.0 / x; exprText = "1/(" + fmt.format(x) + ")"; break;
                case "√x":
                    if (x < 0) { showError(); return; }
                    result = Math.sqrt(x); exprText = "sqrt(" + fmt.format(x) + ")"; break;
                case "∛x": result = Math.cbrt(x); exprText = "cbrt(" + fmt.format(x) + ")"; break;
                case "ln":
                    if (x <= 0) { showError(); return; }
                    result = Math.log(x); exprText = "ln(" + fmt.format(x) + ")"; break;
                case "log₁₀":
                    if (x <= 0) { showError(); return; }
                    result = Math.log10(x); exprText = "log10(" + fmt.format(x) + ")"; break;
                case "x!":
                    result = factorial(x);
                    if (Double.isNaN(result)) { showError(); return; }
                    exprText = fmt.format(x) + "!"; break;
                case "sin": result = Math.sin(Math.toRadians(x)); exprText = "sin(" + fmt.format(x) + ")"; break;
                case "cos": result = Math.cos(Math.toRadians(x)); exprText = "cos(" + fmt.format(x) + ")"; break;
                case "tan": result = Math.tan(Math.toRadians(x)); exprText = "tan(" + fmt.format(x) + ")"; break;
                case "π":
                    currentValue = Math.PI;
                    displayLabel.setText(fmt.format(Math.PI));
                    startNewNumber = true;
                    return;
                case "e":
                    currentValue = Math.E;
                    displayLabel.setText(fmt.format(Math.E));
                    startNewNumber = true;
                    return;
                case "Rand":
                    currentValue = Math.random();
                    displayLabel.setText(fmt.format(currentValue));
                    startNewNumber = true;
                    return;
                case "(":
                case ")":
                    
                    return;
                case "Ans":
                    if (!historyModel.isEmpty()) {
                        String entry = historyModel.get(0);
                        String[] parts = entry.split("=");
                        if (parts.length == 2) {
                            currentValue = Double.parseDouble(parts[1].trim().replace(",", ""));
                            displayLabel.setText(fmt.format(currentValue));
                            startNewNumber = true;
                        }
                    }
                    return;
                default: return;
            }
        } catch (Exception ex) {
            showError();
            return;
        }

        
        if ("^".equals(pendingOp)) {
            
        }

        currentValue = result;
        displayLabel.setText(fmt.format(result));
        expressionLabel.setText(exprText == null ? " " : exprText + " =");
        historyModel.add(0, (exprText != null ? exprText : label) + " = " + fmt.format(result));
        startNewNumber = true;
    }

    private double factorial(double d) {
        if (d < 0 || d != Math.floor(d) || d > 170) return Double.NaN;
        long n = (long) d;
        double result = 1;
        for (long i = 2; i <= n; i++) result *= i;
        return result;
    }

    private void showError() {
        errorState = true;
        displayLabel.setText("Error");
        expressionLabel.setText(" ");
        startNewNumber = true;
        pendingOp = null;
    }

    private void clearAll() {
        currentValue = 0;
        storedValue = 0;
        pendingOp = null;
        startNewNumber = true;
        errorState = false;
        displayLabel.setText("0");
        expressionLabel.setText(" ");
    }

    private void updateDisplay() {
        displayLabel.setText(fmt.format(currentValue));
    }

    private String formatRaw(String raw) {
    
        if (raw.contains(".")) return raw;
        try {
            return fmt.format(Long.parseLong(raw));
        } catch (Exception e) {
            return raw;
        }
    }
    {
        
    }

    // ================= KEYBOARD SUPPORT =================
    private void setupKeyBindings() {
        JRootPane root = getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        bindKey(im, am, "0", () -> onNumberOrDot("0"));
        bindKey(im, am, "1", () -> onNumberOrDot("1"));
        bindKey(im, am, "2", () -> onNumberOrDot("2"));
        bindKey(im, am, "3", () -> onNumberOrDot("3"));
        bindKey(im, am, "4", () -> onNumberOrDot("4"));
        bindKey(im, am, "5", () -> onNumberOrDot("5"));
        bindKey(im, am, "6", () -> onNumberOrDot("6"));
        bindKey(im, am, "7", () -> onNumberOrDot("7"));
        bindKey(im, am, "8", () -> onNumberOrDot("8"));
        bindKey(im, am, "9", () -> onNumberOrDot("9"));
        bindKey(im, am, "PERIOD", () -> onNumberOrDot("."));
        bindKey(im, am, "PLUS", () -> onOperator("+", null));
        bindKey(im, am, "ADD", () -> onOperator("+", null));
        bindKey(im, am, "MINUS", () -> onOperator("−", null));
        bindKey(im, am, "SUBTRACT", () -> onOperator("−", null));
        bindKey(im, am, "SLASH", () -> onOperator("÷", null));
        bindKey(im, am, "DIVIDE", () -> onOperator("÷", null));
        bindKey(im, am, "MULTIPLY", () -> onOperator("×", null));
        bindKey(im, am, "ENTER", () -> onOperator("=", null));
        bindKey(im, am, "EQUALS", () -> onOperator("=", null));
        bindKey(im, am, "BACK_SPACE", () -> onFunction("⌫"));
        bindKey(im, am, "ESCAPE", this::clearAll);
    }

    private void bindKey(InputMap im, ActionMap am, String keyName, Runnable action) {
        KeyStroke ks = KeyStroke.getKeyStroke("pressed " + keyName);
        if (ks == null) return;
        im.put(ks, keyName);
        am.put(keyName, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }

    // ================= ROUNDED BUTTON (Apple style) =================
    static class RoundedButton extends JButton {
        private final Color base;
        private final Color hover;
        private boolean hovering = false;

        RoundedButton(String text, Color base, Color hover, Color fg, float fontSize) {
            super(text);
            this.base = base;
            this.hover = hover;
            setForeground(fg);
            setFont(new Font("SF Pro Display", Font.PLAIN, (int) fontSize));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setBackground(base);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovering = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hovering = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fillColor = getBackground();
            if (hovering) {
                fillColor = (fillColor.equals(base)) ? hover : fillColor.brighter();
            }
            int arc = Math.min(getWidth(), getHeight());
            g2.setColor(fillColor);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), arc, arc));
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        public void setBackground(Color bg) {
            super.setBackground(bg);
            repaint();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new Calculator().setVisible(true);
        });
    }
}