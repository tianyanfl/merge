import java.util.*;

public class Hashi {

    private static Square[][] BOARD;
    private static int ROW = 0;
    private static int COL = 0;


    public static void main(String[] args) {
        BOARD = parseInput();
        // 第一个island
        Island first = null;
        for (Square[] squares : BOARD) {
            for (int j = 0; j < BOARD[0].length; j++) {
                if (squares[j] instanceof Island) {
                    if (first == null) {
                        first = (Island) squares[j];
                    }
                }
            }
        }

        Square[][] solution = findSolution(BOARD, first.rowIndex, first.colIndex, new HashSet<>());
        if (solution == null) {
            System.out.println("No solution found!!!");
            return;
        }
        System.out.println("\nFound solution:");
        display(solution);
    }

    /**
     * findSolution
     *
     * @param board
     * @param row
     * @param col
     * @param failedDirections 当前island不可行的方向
     * @return
     */
    private static Square[][] findSolution(Square[][] board, int row, int col, Set<Direction> failedDirections) {
//        System.out.println();
//        System.out.println("Current: " + row + "," + col);
        // 是否已经找到
        if (checkSolution(board)) {
            return board;
        }

        if (board[row][col] instanceof Empty) {
            return findSolution(board, row, col + 1, new HashSet<>());
        }

        for (Direction direction : Direction.values()) {
            if (failedDirections.contains(direction)) {
                continue;
            }
            // 四个方向连续移动
            Square[][] newBoard = copy(board);
            Island current = (Island) newBoard[row][col];
            Island connectedIsland = current.setBridge(newBoard, direction);
            Square[][] newBoardOther = copy(newBoard);
            if (connectedIsland != null) {
//                String move = String.format("Move Dir: (%s,%s) ==%s==> (%s,%s)", row, col, direction, connectedIsland.rowIndex, connectedIsland.colIndex);
//                System.out.println(move);
//                display(board);
//                display(newBoard);
                // 从桥的另一边重新开始寻找
                Square[][] solution = findSolution(newBoard, connectedIsland.rowIndex, connectedIsland.colIndex, new HashSet<>());
                if (solution != null) {
                    return solution;
                }
            } else {
                failedDirections.add(direction);
            }


            // 自己重新移动（连续移动）
            current = (Island) newBoardOther[row][col];
            if (!current.isSatisfied()) {
                Square[][] solution = findSolution(newBoardOther, row, col, new HashSet<>(failedDirections));
                if (solution != null) {
                    return solution;
                }
            }
        }
        return null;
    }

    /**
     * 复制board
     * @param board
     * @return
     */
    private static Square[][] copy(Square[][] board) {
        Square[][] boardTmp = new Square[board.length][board[0].length];
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                boardTmp[i][j] = board[i][j].copy();
            }
        }

        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                Square tmp = boardTmp[i][j];
                Set<Square> connects = new HashSet<>();
                for (Square oldSquare : tmp.connects) {
                    connects.add(boardTmp[oldSquare.rowIndex][oldSquare.colIndex]);
                }
                tmp.connects = connects;
            }
        }
        return boardTmp;
    }

    /**
     * 是否已经完成
     * @param board
     * @return
     */
    private static boolean checkSolution(Square[][] board) {
        Island first = null;
        int islandCount = 0;
        for (Square[] squares : board) {
            for (int j = 0; j < board[0].length; j++) {
                if (!squares[j].isSatisfied()) {
                    return false;
                }
                if (squares[j] instanceof Island) {
                    islandCount++;
                    if (first == null) {
                        first = (Island) squares[j];
                    }
                }
            }
        }

        // check connect island count
        Queue<Square> queue = new LinkedList<>();
        Set<Square> visits = new HashSet<>();
        queue.add(first);
        int connectCount = 0;
        while (queue.size() > 0) {
            Square cur = queue.remove();
            if (visits.contains(cur)) {
                continue;
            }
            visits.add(cur);
            if (cur instanceof Island) {
                connectCount++;
            }
            queue.addAll(cur.connects);
        }
        return connectCount == islandCount;
    }

    private static void display(Square[][] board) {
        System.out.println("============================");
        for (Square[] squares : board) {
            for (int j = 0; j < board[0].length; j++) {
                System.out.print(squares[j].display());
            }
            System.out.println();
        }
    }

    /**
     * parse input
     */
    private static Square[][] parseInput() {
        Scanner scanner = new Scanner(System.in);
        List<String> lines = new ArrayList<>();
        while (true) {
            String line = scanner.nextLine();
            if (line.length() == 0) {
                break;
            }
            lines.add(line);
        }

        Square[][] board = new Square[lines.size()][lines.get(0).length()];
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Square[] squares = new Square[line.length()];
            for (int j = 0; j < line.length(); j++) {
                char ch = line.charAt(j);
                if (ch >= '1' && ch <= '9') {
                    squares[j] = new Island(i, j, ch - '0');
                } else if (ch >= 'a' && ch <= 'z') {
                    squares[j] = new Island(i, j, (ch - 'a') + 10);
                } else {
                    squares[j] = new Empty(i, j);
                }
            }
            board[i] = squares;
        }
        ROW = board.length;
        COL = board[0].length;
        display(board);
        return board;
    }

    enum Direction {
        LEFT(true, 0, -1), RIGHT(true, 0, 1), UP(false, -1, 0), DOWN(false, 1, 0);
        final boolean isHorizon;
        final int rowMove;
        final int colMove;

        Direction(boolean isHorizon, int rowMove, int colMove) {
            this.isHorizon = isHorizon;
            this.rowMove = rowMove;
            this.colMove = colMove;
        }

        public Direction getOpposite() {
            if (this.equals(LEFT)) {
                return RIGHT;
            } else if (this.equals(RIGHT)) {
                return LEFT;
            } else if (this.equals(UP)) {
                return DOWN;
            } else if (this.equals(DOWN)) {
                return UP;
            }
            return null;
        }
    }

    abstract static class Square {
        protected int rowIndex;
        protected int colIndex;
        protected Set<Square> connects;

        public Square(int rowIndex, int colIndex) {
            this.rowIndex = rowIndex;
            this.colIndex = colIndex;
            this.connects = new HashSet<>();
        }

        public Square(int rowIndex, int colIndex, Set<Square> connects) {
            this.rowIndex = rowIndex;
            this.colIndex = colIndex;
            this.connects = connects;
        }

        public abstract Square copy();

        public boolean isSatisfied() {
            return true;
        }

        public abstract String display();

        public abstract boolean canSetBridge(Direction direction);
    }

    /**
     * empty space
     */
    static class Empty extends Square {
        private int horizonCount;
        private int verticalCount;

        public Empty(int rowIndex, int colIndex) {
            super(rowIndex, colIndex);
        }

        public Empty(int rowIndex, int colIndex, Set<Square> connects, int horizonCount, int verticalCount) {
            super(rowIndex, colIndex, connects);
            this.horizonCount = horizonCount;
            this.verticalCount = verticalCount;
        }

        @Override
        public Square copy() {
            return new Empty(rowIndex, colIndex, connects, horizonCount, verticalCount);
        }

        @Override
        public boolean canSetBridge(Direction direction) {
            if (horizonCount == 3 || verticalCount == 3) {
                return false;
            }
            return (direction.isHorizon && verticalCount == 0) || (!direction.isHorizon && horizonCount == 0);
        }

        @Override
        public String toString() {
            return "(" + rowIndex + "," + colIndex + ") " + display();
        }

        @Override
        public String display() {
            if (horizonCount == 0 && verticalCount == 0) {
                return ".";
            } else if (horizonCount > 0) {
                if (horizonCount == 1) {
                    return "-";
                } else if (horizonCount == 2) {
                    return "=";
                }
                if (horizonCount == 3) {
                    return "E";
                } else {
                    throw new IllegalArgumentException("No more than 3 bridges!!");
                }
            } else {
                if (verticalCount == 1) {
                    return "|";
                } else if (verticalCount == 2) {
                    return "”";
                }
                if (verticalCount == 3) {
                    return "#";
                } else {
                    throw new IllegalArgumentException("No more than 3 bridges!!");
                }
            }
        }
    }

    /**
     * island
     */
    static class Island extends Square {
        private int number;
        private int left;
        private int right;
        private int up;
        private int down;

        public Island(int rowIndex, int colIndex, int number) {
            super(rowIndex, colIndex);
            this.number = number;
        }

        public Island(int rowIndex, int colIndex, Set<Square> connects, int number, int left, int right, int up, int down) {
            super(rowIndex, colIndex, connects);
            this.number = number;
            this.left = left;
            this.right = right;
            this.up = up;
            this.down = down;
        }

        public Island setBridge(Square[][] board, Direction direction) {
            int rowMax = board.length;
            int colMax = board[0].length;

            if (!canSetBridge(direction)) {
                return null;
            }

            List<Empty> bridge = new ArrayList<>();
            Square current = this;
            Island target = null;
            while (true) {
                int nextRow = current.rowIndex + direction.rowMove;
                int nextCol = current.colIndex + direction.colMove;
                if (nextRow >= rowMax || nextRow < 0 || nextCol >= colMax || nextCol < 0) {
                    return null;
                }
                Square next = board[nextRow][nextCol];
                if (!next.canSetBridge(direction.getOpposite())) {
                    return null;
                }
                if (next instanceof Island) {
                    target = (Island) next;
                    break;
                }
                bridge.add((Empty) next);
                current = next;
            }

            // 桥至少要跨水are
            if (bridge.size() == 0) {
                return null;
            }

            this.addOneBridge(direction, target);
            target.addOneBridge(direction.getOpposite(), this);
            for (Empty empty : bridge) {
                if (direction.isHorizon) {
                    empty.horizonCount++;
                } else {
                    empty.verticalCount++;
                }
                empty.connects.add(this);
                empty.connects.add(target);
            }
            return target;
        }

        private void addOneBridge(Direction direction, Island target) {
            connects.add(target);
            if (direction.equals(Direction.UP)) {
                up++;
            } else if (direction.equals(Direction.DOWN)) {
                down++;
            } else if (direction.equals(Direction.RIGHT)) {
                right++;
            } else if (direction.equals(Direction.LEFT)) {
                left++;
            }
        }

        public boolean canSetBridge(Direction direction) {
            if (left + right + up + down >= number) {
                return false;
            }

            // 同方向上最多三个桥
            if (direction.equals(Direction.UP) && up < 3) {
                return true;
            } else if (direction.equals(Direction.DOWN) && down < 3) {
                return true;
            } else if (direction.equals(Direction.RIGHT) && right < 3) {
                return true;
            } else if (direction.equals(Direction.LEFT) && left < 3) {
                return true;
            }
            return false;
        }

        @Override
        public Square copy() {
            return new Island(rowIndex, colIndex, connects, number, left, right, up, down);
        }


        @Override
        public String display() {
            if (number <= 9) {
                return number + "";
            }
            char ch = (char) ('a' + number - 10);
            return String.valueOf(ch);
        }

        @Override
        public String toString() {
            return "(" + rowIndex + "," + colIndex + ") " + display();
        }

        @Override
        public boolean isSatisfied() {
            return left + right + up + down == number;
        }
    }

}

