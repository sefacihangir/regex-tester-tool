package com.vgrazi.regextester.component;

import com.vgrazi.regextester.action.Calculator;
import com.vgrazi.regextester.action.Renderer;
import com.vgrazi.regextester.action.UnmatchedLeftParenException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.vgrazi.regextester.component.Constants.GROUP_COLOR;
import static com.vgrazi.regextester.component.Constants.RED_BORDER;
import static com.vgrazi.regextester.component.Constants.WHITE_BORDER;

/**
 * This is the JTextPane that renders the regex pattern. When user positions the cursor just after a parenthesis, highlights that paren and the paired opening or closing paren
 */
public class PatternPane extends JTextPane {
    /**
     * As we navigate the pattern pane, we update the character pane accordingly. Therefore we need a reference
     */
    private JTextPane characterPane;
    private JTextPane auxiliaryPane;
    private int flags;

    public PatternPane(final JTextPane characterPane, final JTextPane auxiliaryPane) {
        this.characterPane = characterPane;
        this.auxiliaryPane = auxiliaryPane;
        getStyledDocument().addStyle("highlights", null);

        addKeyListener(
                new KeyAdapter() {
                    @Override
                    public synchronized void keyReleased(KeyEvent e) {
                        render();
                    }
                });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                render();
            }

            @Override
            public void focusLost(FocusEvent e) {
                Renderer.resetColor(getStyledDocument());
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                render();
            }
        });
    }

    /**
     * Renders the matching groups, if any, in the character pane with additional highlighting
     */
    private void render() {
        String text = getText();
        Renderer.resetColor(getStyledDocument());
        try {
            setBorder(WHITE_BORDER);
            auxiliaryPane.setLayout(new BoxLayout(auxiliaryPane, BoxLayout.Y_AXIS));
            List<String> names = getNamedGroups(text);
            auxiliaryPane.removeAll();
            ButtonGroup buttonGroup = new ButtonGroup();
            names.forEach(name->addRadioButton(name, buttonGroup, auxiliaryPane));

            ColorRange[] colorRanges = Calculator.parseGroupRanges(text, GROUP_COLOR);
            // if the cursor is at the start or end of any range, colorize that one
            int position = getCaretPosition();
            for (int groupIndex = 0; groupIndex < colorRanges.length; groupIndex++) {
                ColorRange range = colorRanges[groupIndex];
                if (range.getStart() == position - 1 || range.getEnd() == position - 1) {
                    Renderer.colorize(getStyledDocument(), true, range);
                    // group index starts at 1, so add 1 to the list position
                    int finalGroupIndex = groupIndex;
                    Runnable runnable = () -> {
                        try {
                            Renderer.renderMatchingGroupsHighlightsInCharacterPane(characterPane, finalGroupIndex + 1, getText(), flags);
                        } catch (PatternSyntaxException e) {
                            setBorder(RED_BORDER);
                        }
                    };
                    SwingUtilities.invokeLater(runnable);
                    break;
                }
            }
        } catch (UnmatchedLeftParenException e1) {
            ColorRange range = new ColorRange(Color.red, e1.getPosition(), text.length() - 1);
            Renderer.colorize(getStyledDocument(), true, range);
        }
    }

    private void addRadioButton(String name, ButtonGroup buttonGroup, JComponent parent) {
        JRadioButton button = new JRadioButton(name);
        button.setActionCommand(name);
        buttonGroup.add(button);
        parent.add(button);
        button.setBackground(Color.white);
    }

    private List<String> getNamedGroups(String text) {
        List<String> list = new ArrayList<>();
        try {
            Pattern pattern = Pattern.compile("\\?<(.+?)>");
            Matcher matcher = pattern.matcher(text);
            while(matcher.find()) {
                list.add(matcher.group(1));
            }

        } catch (Exception e) {
            // swallow the error, this is not yet ready to compile
        }
        return list;
    }

    public void setFlags(int flags) {
        this.flags = flags;
        render();
    }
}
