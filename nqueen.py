def display(board):
    """
    打印棋盘
    :param board:
    :return:
    """
    for row in board:
        # 打印每一行
        for col in row:
            print(col, end=" ")
        print()
    print()


def checkBoard(board, rowIndex, colIndex):
    """
    检测当前位置的放置是否合法
    :param board: 棋盘
    :param rowIndex: 当前行索引
    :param colIndex: 当前列索引
    :return: True合法，False不合法
    """
    # 在我们的算法中，能保证一行只有一个，所有行检测就略过

    # 列检测
    for rowIndexTmp in range(len(board)):
        if rowIndexTmp == rowIndex:
            # 是当前的行，略过
            continue
        if board[rowIndexTmp][colIndex] == "Q":
            # 有其他行的相同列已经放置了Q，当前放置不合法，返回False
            return False

    # 左上到右下的对角检测
    for i in range(1, len(board)):
        # 对角线下半部分
        rowIndexTmp = rowIndex + i
        colIndexTmp = colIndex + i
        if hasQueen(board, rowIndexTmp, colIndexTmp):
            # 对角上存在
            return False

        # 对角线上半部分
        rowIndexTmp = rowIndex - i
        colIndexTmp = colIndex - i
        if hasQueen(board, rowIndexTmp, colIndexTmp):
            # 对角上存在
            return False

    # 右上到左下的对角检测
    for i in range(1, len(board)):
        # 对角线下半部分，行增大，列减小
        rowIndexTmp = rowIndex + i
        colIndexTmp = colIndex - i
        if hasQueen(board, rowIndexTmp, colIndexTmp):
            # 对角上存在
            return False

        # 对角线上半部分，行减小，列增大
        rowIndexTmp = rowIndex - i
        colIndexTmp = colIndex + i
        if hasQueen(board, rowIndexTmp, colIndexTmp):
            # 对角上存在
            return False

    return True


def hasQueen(board, rowIndex, colIndex):
    """
    检测当前的位置是否有queen
    :param board:
    :param rowIndex:
    :param colIndex:
    :return: True有皇后，False无皇后或无效位置
    """
    if rowIndex < 0 or colIndex < 0 or rowIndex >= len(board) or colIndex >= len(board):
        # 超出棋盘，位置无效
        return False

    return board[rowIndex][colIndex] == "Q"


def queenSolveRecursion(board, rowIndex, count):
    """
    递归尝试
    :param board: 棋盘
    :param rowIndex: 当前行的索引
    :param count: 当前已找到合法排列数
    :return: 所有合法的排列数
    """
    if rowIndex >= len(board):
        # 行索引已超过行大小，表示前面的所有行已经合法排列
        count += 1
        display(board) # 打印棋盘
        return count

    # 对当前行的所有列进行放置尝试
    for colIndex in range(len(board)):
        board[rowIndex][colIndex] = "Q"
        if checkBoard(board, rowIndex, colIndex):
            # 当前列放置合法，进行下一行的放置的尝试
            count = queenSolveRecursion(board, rowIndex + 1, count)

        # 清除当前列的放置，for循环再进行下一列放置的尝试
        board[rowIndex][colIndex] = "*"

    return count


def queenSolve(board):
    """
    打到所有合法的排列
    :param board: 棋盘
    :return: 所有合法的排列数
    """
    return queenSolveRecursion(board, 0, 0)


def main():
    """
    主方法
    :return:
    """
    nStr = input("Input the size N: ")
    n = int(nStr)

    # 初始化棋盘，所有位置都是星号(*)
    board = []
    for i in range(n):
        row = []  # 棋盘的每一行
        for j in range(n):
            row.append("*")
        board.append(row)

    count = queenSolve(board)
    print("The max count: ", count)


if __name__ == '__main__':
    main()
