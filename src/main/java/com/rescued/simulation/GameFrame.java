package com.rescued.simulation;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;

/**
 * The main window: game grid on the left, HUD dashboard and controls on
 * the right.
 *
 * <p>The sidebar shows mission status (budget, global health, efficiency,
 * golden-hour countdown, currently-selected vehicle), vehicle dispatch
 * controls, repair mode toggle, audio controls, restart button, and a
 * short "how to play" help text.</p>
 *
 * @author Ahsan Haris Ahmed
 */
public class GameFrame extends JFrame {

    private final GamePanel gamePanel;

    private final JLabel budgetLabel = new JLabel();
    private final JLabel healthLabel = new JLabel();
    private final JLabel efficiencyLabel = new JLabel();
    private final JLabel timeLabel = new JLabel();
    private final JLabel selectedLabel = new JLabel();
    private final JLabel highScoreLabel = new JLabel();
    private final JProgressBar healthBar = new JProgressBar();
    private final JProgressBar budgetBar = new JProgressBar();

    private final JToggleButton repairBtn = new JToggleButton("Repair Mode (click debris)");
    private final JToggleButton muteBtn = new JToggleButton("Mute Music");

    /**
     * Constructs the main window, builds the sidebar UI, and starts the
     * game loop inside {@link GamePanel}.
     */
    public GameFrame() {
        super("RescuED - Disaster Relief Logistics Simulation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        gamePanel = new GamePanel(this::refreshHud);
        add(gamePanel, BorderLayout.CENTER);
        add(buildSidebar(), BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);
        setResizable(true);
        setMinimumSize(new Dimension(720, 480));
        refreshHud();
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        side.setPreferredSize(new Dimension(300, 0));
        side.setBackground(new Color(245, 245, 245));

        side.add(sectionLabel("Mission HUD"));
        side.add(Box.createVerticalStrut(8));
        styleHudLabel(budgetLabel);
        styleHudLabel(healthLabel);
        styleHudLabel(efficiencyLabel);
        styleHudLabel(timeLabel);
        styleHudLabel(selectedLabel);
        side.add(budgetLabel);
        side.add(budgetBar);
        side.add(Box.createVerticalStrut(6));
        side.add(healthLabel);
        side.add(healthBar);
        side.add(Box.createVerticalStrut(6));
        side.add(efficiencyLabel);
        side.add(timeLabel);
        side.add(Box.createVerticalStrut(4));
        side.add(selectedLabel);
        side.add(Box.createVerticalStrut(8));
        styleHudLabel(highScoreLabel);
        highScoreLabel.setForeground(new Color(255, 180, 40));
        side.add(highScoreLabel);

        side.add(Box.createVerticalStrut(18));
        side.add(sectionLabel("Dispatch"));
        side.add(buildDispatchButtons());

        side.add(Box.createVerticalStrut(18));
        side.add(sectionLabel("Engineering"));
        repairBtn.setAlignmentX(LEFT_ALIGNMENT);
        repairBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, repairBtn.getPreferredSize().height));
        repairBtn.addActionListener(e -> gamePanel.setRepairMode(repairBtn.isSelected()));
        side.add(repairBtn);

        side.add(Box.createVerticalStrut(18));
        side.add(sectionLabel("Audio"));
        muteBtn.setAlignmentX(LEFT_ALIGNMENT);
        muteBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, muteBtn.getPreferredSize().height));
        muteBtn.addActionListener(e -> {
            if (muteBtn.isSelected()) {
                Sounds.stopMusic();
            } else {
                Sounds.playMusic("theme.wav");
            }
        });
        side.add(muteBtn);

        side.add(Box.createVerticalStrut(18));
        JButton restartBtn = new JButton("Restart Mission");
        restartBtn.setAlignmentX(LEFT_ALIGNMENT);
        restartBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, restartBtn.getPreferredSize().height));
        restartBtn.addActionListener(e -> gamePanel.newGame());
        side.add(restartBtn);

        side.add(Box.createVerticalStrut(8));
        JButton fitBtn = new JButton("Reset Window Size");
        fitBtn.setAlignmentX(LEFT_ALIGNMENT);
        fitBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, fitBtn.getPreferredSize().height));
        fitBtn.addActionListener(e -> {
            pack();
            setLocationRelativeTo(null);
        });
        side.add(fitBtn);

        side.add(Box.createVerticalStrut(18));
        side.add(sectionLabel("How to play"));
        JTextArea help = new JTextArea(
            "MISSION LOOP:\n" +
            "1. All vehicles start at the BASE, pre-loaded with supplies.\n" +
            "   Click a vehicle, then click any cell to dispatch.\n" +
            "2. On hospital arrival, the vehicle unloads, banks lives saved,\n" +
            "   then rests. Send it back to BASE to reload.\n" +
            "3. Drive over a FUEL STATION to refuel ($15 per visit).\n" +
            "4. Trucks can't cross flood/debris/building. Use Repair Mode\n" +
            "   ($40 per debris tile) or send the Drone.\n" +
            "5. Emergency SPIKES flash a hospital red. Reach it in time\n" +
            "   for a 1.5x bonus, or global health takes a hit.\n" +
            "6. Each hospital spawns with extra debris around it - that's\n" +
            "   where Repair Mode becomes critical.\n" +
            "7. Beat your best score (saved between runs) to climb the\n" +
            "   leaderboard.\n" +
            "8. Drag the window edges to resize. Cells scale with the window."
        );
        help.setEditable(false);
        help.setLineWrap(true);
        help.setWrapStyleWord(true);
        help.setOpaque(false);
        help.setFont(help.getFont().deriveFont(12f));
        help.setAlignmentX(LEFT_ALIGNMENT);
        help.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        JScrollPane helpScroll = new JScrollPane(help);
        helpScroll.setOpaque(false);
        helpScroll.getViewport().setOpaque(false);
        helpScroll.setBorder(null);
        helpScroll.setAlignmentX(LEFT_ALIGNMENT);
        helpScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        side.add(helpScroll);

        return side;
    }

    private JPanel buildDispatchButtons() {
        JButton droneBtn = new JButton("Select Drone");
        droneBtn.addActionListener(e -> gamePanel.selectVehicleOfType(Drone.class));
        JButton truckBtn = new JButton("Select Truck");
        truckBtn.addActionListener(e -> gamePanel.selectVehicleOfType(Truck.class));
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, droneBtn.getPreferredSize().height));
        droneBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, droneBtn.getPreferredSize().height));
        truckBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, truckBtn.getPreferredSize().height));
        row.add(droneBtn);
        row.add(Box.createHorizontalStrut(8));
        row.add(truckBtn);
        return row;
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel("<html><b>" + text + "</b></html>");
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        return label;
    }

    private void styleHudLabel(JLabel label) {
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 13f));
    }

    private void refreshHud() {
        if (gamePanel == null) return; // GamePanel is still mid-construction, ignore
        GameState s = gamePanel.getState();
        budgetLabel.setText(String.format("Budget: $%d", s.getBudget()));
        healthLabel.setText(String.format("Global Health: %.0f", s.getGlobalHealth()));
        efficiencyLabel.setText(String.format("Efficiency: %.2f lives / fuel", s.efficiency()));
        timeLabel.setText(String.format("Golden Hour left: %ds    Score: %d",
                gamePanel.getRemainingSeconds(), s.missionScore()));
        Vehicle v = gamePanel.getSelectedVehicle();
        if (v == null) {
            selectedLabel.setText("Selected: none");
        } else {
            String stateStr;
            switch (v.getLoadState()) {
                case LOADED:
                    stateStr = String.format("LOADED (%d/%d supplies)",
                            v.getSupplies(), v.getMaxSupplies());
                    break;
                case EXHAUSTED:
                    stateStr = String.format("EXHAUSTED (%d ticks left)",
                            v.getExhaustionTicks());
                    break;
                default:
                    stateStr = "EMPTY (return to base)";
            }
            selectedLabel.setText(String.format(
                "Selected: %s  -  %s  -  fuel %d/%d",
                v.getName(), stateStr, v.getFuel(), v.getMaxFuel()));
        }

        budgetBar.setMaximum(GameState.STARTING_BUDGET);
        budgetBar.setValue(s.getBudget());
        budgetBar.setForeground(new Color(60, 160, 60));

        healthBar.setMaximum((int) GameState.STARTING_HEALTH);
        healthBar.setValue((int) s.getGlobalHealth());
        double healthRatio = s.getGlobalHealth() / GameState.STARTING_HEALTH;
        if (healthRatio > 0.6) {
            healthBar.setForeground(new Color(60, 160, 60));
        } else if (healthRatio > 0.3) {
            healthBar.setForeground(new Color(220, 160, 40));
        } else {
            healthBar.setForeground(new Color(200, 50, 50));
        }

        highScoreLabel.setText(String.format("Best score to beat: %d", HighScore.get()));
    }
}
