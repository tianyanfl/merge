def cross(x, y):
    if len(x) > 3 or len(y) > 3:
        raise ValueError()
    xx = []
    yy = []
    for i in range(len(x)):
        if type(x[i]) != int and type(x[i]) != float:
            raise TypeError()
        xx.append(x[i])
    while len(xx) < 3:
        xx.append(0)

    for i in range(len(y)):
        if type(y[i]) != int and type(y[i]) != float:
            raise TypeError()
        yy.append(y[i])
    while len(yy) < 3:
        yy.append(0)

    return [xx[1] * yy[2] - xx[2] * yy[1], xx[2] * yy[0] - xx[0] * yy[2], xx[0] * yy[1] - xx[1] * yy[0]]


def magic(L):
    n = len(L)
    value = sum(L[0])
    diagonal1 = 0
    diagonal2 = 0
    for i in range(n):
        # row
        if (sum(L[i])) != value:
            return False
        # diagonal sum
        diagonal1 += L[i][i]
        diagonal2 += L[n - 1 - i][i]
    if diagonal1 != value or diagonal2 != value:
        return False

    for col in range(n):
        # column sum
        col_sum = 0
        for row in range(n):
            col_sum += L[row][col]
        if col_sum != value:
            return False

    return True


def benford(s):
    numbers = parse_numbers(s)
    # print(numbers)
    first_numbers = []
    for number in numbers:
        for i in range(len(number)):
            first = number[i]
            if first != '0' and first != ".":
                first_numbers.append(int(first))
                break

    result_list = []
    for first in set(first_numbers):
        count = first_numbers.count(first)
        result_list.append((first, count))

    # print(result_list)
    return my_sort(result_list)


def my_sort(list_tuple):
    for i in range(len(list_tuple) - 1):
        maxIndex = i
        j = i + 1
        while j < len(list_tuple):
            max_element = list_tuple[maxIndex]
            current_element = list_tuple[j]
            if (max_element[1] < current_element[1] or
                    (max_element[1] == current_element[1] and max_element[0] > current_element[0])):
                maxIndex = j
            j += 1

        if maxIndex != i:
            tmp = list_tuple[i]
            list_tuple[i] = list_tuple[maxIndex]
            list_tuple[maxIndex] = tmp
    return list_tuple


def parse_numbers(s):
    numbers = []
    value = ''
    for i in range(len(s)):
        tmp = s[i]
        if tmp.isnumeric():
            value += tmp
        elif tmp == "." and value.count(tmp) == 0:
            value += tmp
        else:
            if len(value) > 0 and value != '.':
                numbers.append(value)
            value = ""
            if tmp == '.':
                value += tmp
    return numbers


def benfordTest():
    print(benford("test 123.456.78.003... test 32-16,2.3 .00 test."))


def magicTest():
    L = [[2, 7, 6], [9, 5, 1], [4, 3, 8]]
    print(magic(L))


def crossTest():
    try:
        L = cross([1, 3.2], [-1.9, 3.7, 2.1, 9])
    except ValueError:
        print("at least one...1")
    except TypeError:
        print("at least one...2")

    L = cross([1, 3], [-1.2, 3.2, 2])
    print(L)


def main():
    crossTest()
    magicTest()
    benfordTest()

main()
