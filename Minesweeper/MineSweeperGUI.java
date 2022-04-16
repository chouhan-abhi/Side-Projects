package Minesweeper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.*;

public class MineSweeperGUI {
    // The value assigned to cells marked as mines. 10 works
    // because no cell will have more than 8 neighbouring mines.
    private static final int MINE = 1000;
    // The size in pixels for the frame.
    private static final int SIZE = 640;

    // The number of mines at generated is the grid size * this constant
    private static final double POPULATION_CONSTANT = 1.5;

    // This fixed amount of memory is to avoid repeatedly declaring
    // new arrays every time a cell's neighbours are to be retrieved.
    private static Cell[] reusableStorage = new Cell[8];

    private int gridLength;
    private int gridWidth;
    private int mines;

    private Cell[][] cells;

    private JFrame  frame;
    private JButton reset;
    private JButton giveUp;

    private final ActionListener actionListener = actionEvent -> {
        Object source = actionEvent.getSource();
        if (source == reset) {
            createMines();
        } else if (source == giveUp) {
            revealBoardAndDisplay("You gave up.");
        } else {
            handleCell((Cell) source);
        }
    };

    private class Cell extends JButton {
        private final int row;
        private final int col;
        private       int value;

        Cell(final int row, final int col,
             final ActionListener actionListener) {
            this.row = row;
            this.col = col;
            addActionListener(actionListener);
            setText("");
        }

        int getValue() {
            return value;
        }

        void setValue(int value) {
            this.value = value;
        }

        boolean isAMine() {
            return value == MINE;
        }

        void reset() {
            setValue(0);
            setEnabled(true);
            setText("");
        }

        void reveal() {
            setEnabled(false);
            setText(isAMine() ? "X" : String.valueOf(value));
        }

        void updateNeighbourCount() {
            getNeighbours(reusableStorage);
            for (Cell neighbour : reusableStorage) {
                if (neighbour == null) {
                    break;
                }
                if (neighbour.isAMine()) {
                    value++;
                }
            }
        }

        void getNeighbours(final Cell[] container) {
            // Empty all elements first
            for (int i = 0; i < reusableStorage.length; i++) {
                reusableStorage[i] = null;
            }

            int index = 0;

            for (int rowOffset = -1; rowOffset <= 1; rowOffset++) {
                for (int colOffset = -1; colOffset <= 1; colOffset++) {
                    // Make sure that we don't count ourselves
                    if (rowOffset == 0 && colOffset == 0) {
                        continue;
                    }
                    int rowValue = row + rowOffset;
                    int colValue = col + colOffset;

                    if (rowValue < 0 || rowValue >= gridLength
                        || colValue < 0 || colValue >= gridWidth) {
                        continue;
                    }

                    container[index++] = cells[rowValue][colValue];
                }
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            Cell cell = (Cell) obj;
            return row == cell.row &&
                   col == cell.col;
        }

        @Override
        public int hashCode() {
            return Objects.hash(row, col);
        }
    }

    private MineSweeperGUI(final int gridLength, final int gridWidth, final int totalMines) {
        this.gridLength = gridLength;
        this.gridWidth = gridWidth;
        this.mines = totalMines;
        cells = new Cell[gridLength][gridWidth];

        frame = new JFrame("Minesweeper");
        frame.setSize(SIZE, SIZE);
        frame.setLayout(new BorderLayout());

        initializeButtonPanel();
        initializeGrid();

        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private void initializeButtonPanel() {
        JPanel buttonPanel = new JPanel();

        reset = new JButton("Reset");
        giveUp = new JButton("Give Up");

        reset.addActionListener(actionListener);
        giveUp.addActionListener(actionListener);

        buttonPanel.add(reset);
        buttonPanel.add(giveUp);
        frame.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void initializeGrid() {
        Container grid = new Container();
        grid.setLayout(new GridLayout(gridLength, gridWidth));

        for (int row = 0; row < gridLength; row++) {
            for (int col = 0; col < gridWidth; col++) {
                cells[row][col] = new Cell(row, col, actionListener);
                grid.add(cells[row][col]);
            }
        }
        createMines();
        frame.add(grid, BorderLayout.CENTER);
    }

    private void resetAllCells() {
        for (int row = 0; row < gridLength; row++) {
            for (int col = 0; col < gridWidth; col++) {
                cells[row][col].reset();
            }
        }
    }

    private void createMines() {
        resetAllCells();

        final int mineCount = (int) POPULATION_CONSTANT * mines;
        final Random random    = new Random();

        // Map all (row, col) pairs to unique integers
        Set<Integer> positions = new HashSet<>(gridLength * gridWidth);
        for (int row = 0; row < gridLength; row++) {
            for (int col = 0; col < gridWidth; col++) {
                positions.add(row * gridLength + col);

                //need to check later
            }
        }

        // Initialize mines
        for (int index = 0; index < mineCount; index++) {
            int choice = random.nextInt(positions.size());
            int row    = choice / gridLength;
            int col    = choice % gridWidth;
            cells[row][col].setValue(MINE);
            positions.remove(choice);
        }

        // Initialize neighbour counts
        for (int row = 0; row < gridLength; row++) {
            for (int col = 0; col < gridWidth; col++) {
                if (!cells[row][col].isAMine()) {
                    cells[row][col].updateNeighbourCount();
                }
            }
        }
    }

    private void handleCell(Cell cell) {
        if (cell.isAMine()) {
            cell.setForeground(Color.RED);
            cell.reveal();
            revealBoardAndDisplay("You clicked on a mine!");
            return;
        }
        if (cell.getValue() == 0) {
            Set<Cell> positions = new HashSet<>();
            positions.add(cell);
            cascade(positions);
        } else {
            cell.reveal();
        }
        checkForWin();
    }

    private void revealBoardAndDisplay(String message) {
        for (int row = 0; row < gridLength; row++) {
            for (int col = 0; col < gridWidth; col++) {
                if (!cells[row][col].isEnabled()) {
                    cells[row][col].reveal();
                }
            }
        }

        JOptionPane.showMessageDialog(
                frame, message, "Game Over",
                JOptionPane.ERROR_MESSAGE
        );

        createMines();
    }

    private void cascade(Set<Cell> positionsToClear) {
        while (!positionsToClear.isEmpty()) {
            // Set does not have a clean way for retrieving
            // a single element. This is the best way I could think of.
            Cell cell = positionsToClear.iterator().next();
            positionsToClear.remove(cell);
            cell.reveal();

            cell.getNeighbours(reusableStorage);
            for (Cell neighbour : reusableStorage) {
                if (neighbour == null) {
                    break;
                }
                if (neighbour.getValue() == 0
                    && neighbour.isEnabled()) {
                    positionsToClear.add(neighbour);
                } else {
                    neighbour.reveal();
                }
            }
        }
    }

    private void checkForWin() {
        boolean won = true;
        outer:
        for (Cell[] cellRow : cells) {
            for (Cell cell : cellRow) {
                if (!cell.isAMine() && cell.isEnabled()) {
                    won = false;
                    break outer;
                }
            }
        }

        if (won) {
            JOptionPane.showMessageDialog(
                    frame, "You have won!", "Congratulations",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    private static void run(final int level) {
        try {
            // Totally optional. But this applies the look and
            // feel for the current OS to the a application,
            // making it look native.
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignore) { }
        // Launch the program
        int boardLength, boardWidth, minesCount; 
        switch (level) {
            case 3:
                boardLength = 22;
                boardWidth = 25;
                minesCount = 91;
                break;
            case 2:
                boardLength = 13;
                boardWidth = 18;
                minesCount = 35;
                break;
            default:
                boardLength = 7;
                boardWidth = 9;
                minesCount  =10;
                break;
        }
        new MineSweeperGUI(boardLength, boardWidth, minesCount);
    }

    public static void main(String[] args) {

        Scanner input = new Scanner(System.in);
        System.out.println("Select Level");
        int level = input.nextInt();
        SwingUtilities.invokeLater(() -> MineSweeperGUI.run(level));
        input.close();
    }
}