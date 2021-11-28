def cross(x, y):
    if len(x) > 3 or len(y) > 3:
        raise ValueError()
    check = lambda a: 0 if type(a) != int and type(a) != float else 1
    check_list = [check(a) for a in x] + [check(a) for a in y]
    if check_list.count(1) != len(x) + len(y):
        raise TypeError()

    x += [0 for i in range(3-len(x))]
    y += [0 for i in range(3-len(y))]
    return [x[1] * y[2] - x[2] * y[1], x[2] * y[0] - x[0] * y[2], x[0] * y[1] - x[1] * y[0]]


def magic(L):
    n = len(L)
    value = sum(L[0])

    diagonal1 = 0
    diagonal2 = 0
    LL = list(zip(*L))
    for i in range(n):
        # row and column sum
        if (sum(L[i])) != value or (sum(LL[i])) != value:
            return False
        # diagonal sum
        diagonal1 += L[i][i]
        diagonal2 += L[n - 1 - i][i]

    return diagonal1 == value and diagonal2 == value

def benford(s):
    numbers = []
    value = ''
    for tmp in s:
        if tmp.isnumeric() or (tmp == "." and value.count(tmp) == 0):
            value += tmp
        else:
            if len(value) > 0 and value != '.':
                numbers.append(value)
            value = ""
            if tmp == '.':
                value += tmp
    # print(numbers)

    first_numbers = []
    for number in numbers:
        first = next((x for x in number if x != '0' and x != "."), None)
        if first is not None:
            first_numbers.append(int(first))
    result_list = [(x, first_numbers.count(x)) for x in set(first_numbers)]

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
