import getopt
import sys

import numpy as np
from sklearn import datasets
from sklearn.neighbors import KNeighborsClassifier


def process_knn(X, y, test_times, train_size, test_size, k, metric, logfile):
    """
    使用给定的数据集，在多维空间中执行knn算法
    :param X: 使用的数据的特征向量
    :param y: 使用的数据的标签
    :param test_times: 要执行的次数
    :param train_size: 每次切割的训练集大小
    :param test_size: 每次切割的测试集大小
    :param k: knn中k的大小
    :param logfile: 调试信息的输出位置
    :param metric: 可选值有'chebyshev' or 'euclidean
    :return: 平均准确度
    """
    
    # 为了使用一部分数据训练，生成一个随机数组取一部分数据进行训练，剩下的作为测试集。
    def sample(size):
        index = np.random.randint(np.size(datasets.data, 0), size=size)
        return index
    
    average_accuracy = 0
    
    with open(logfile, mode='a', encoding="utf-8") as log:
        for i in range(test_times):
            # 随机采样
            # index = sample(train_size)
            # train_X = datasets.data[index, :]
            # train_y = datasets.target[index]
            #
            # index = sample(test_size)
            # test_X = datasets.data[index, :]
            # test_y = datasets.target[index]
            
            # 将原数据分为一半测试集，一半训练集
            X_y = np.c_[X, y]
            np.random.shuffle(X_y)
            
            train_X = X_y[: train_size, :-1]
            train_y = X_y[: train_size, -1]
            
            test_X = X_y[train_size: train_size + test_size, :-1]
            test_y = X_y[train_size: train_size + test_size, -1]
            
            # 训练
            clf = KNeighborsClassifier(n_neighbors=k, algorithm='brute', p=2, metric=metric, n_jobs=-1)
            clf.fit(train_X, train_y)
            
            # 预测
            predict_y = clf.predict(test_X)
            
            accuracy = 1 - np.count_nonzero(predict_y - test_y) / test_y.size
            print("{} k={} 第{}次的准确率是：{:.5%}".format(logfile, k, i, accuracy))
            log.write("{} k={} 第{}次的准确率是：{:.5%}\n".format(logfile, k, i, accuracy))
            average_accuracy += accuracy
        average_accuracy /= test_times
        print("{} k={} 平均准确率是：{:.5%}".format(logfile, k, average_accuracy))
        log.write("{} k={} 平均准确率是：{:.5%}\n\n".format(logfile, k, average_accuracy))
    return average_accuracy


def load_sonar(path):
    """
    ***************
    Attribute   Classes     Size
    60          2           208
    ***************
    从指定文件中加载sonar数据集
    :param path: 待加载文件的绝对路径
    :return: X sonar数据集的特征向量   y sonar数据集的标签
    """
    X = np.loadtxt(path, delimiter=",", usecols=range(60))
    y = np.loadtxt(path, delimiter=",", dtype=str, usecols=60)
    y = [0 if t == 'R' else 1 for t in y]
    return X, y


def save_text_X(fileName, X):
    """
    将指定数据集的特征向量X保存的fileName文件中，方便Java程序映射
    :param fileName: 保存的文件名
    :param X: 特征向量X
    :return: null
    """
    with open(fileName, 'w', encoding='utf-8') as file:
        rowNumber = np.size(X, 0)
        colNumber = np.size(X, 1)
        file.write("{}\t{}\n".format(colNumber, rowNumber))
        for row in range(np.size(X, 0)):
            for col in range(colNumber):
                file.write("{}".format(X[row, col]))
                if col != (colNumber - 1):
                    file.write("\t")
                else:
                    file.write("\n")


def save_text_Y(fileName, Y):
    """
    将指定数据集的特征向量X保存的fileName文件中，方便Java程序映射
    :param Y: 标签Y
    :param fileName: 保存的文件名
    :return: null
    """
    with open(fileName, 'w', encoding='utf-8') as file:
        rowNumber = np.size(Y, 0)
        file.write("{}\t{}\n".format(1, rowNumber))
        for row in range(np.size(Y, 0)):
            for col in range(1):
                file.write("{}".format(Y[row]))
                file.write("\n")


def loadX_from_text(fileName):
    """
    从Java映射后的fileName文件加载数据集的特征向量X
    :param fileName: Java映射后的文件
    :return: 加载之后的X
    """
    X = np.loadtxt(fileName, skiprows=1, delimiter="\t")
    return X




def main(argv):
    """
    -h 获取帮助
    -v 向量文件
    -l 标签文件
    -t 训练次数
    -e 每次切割的测试集大小
    -r 每次切割的训练集大小
    -k knn的k
    -m 可选值有'chebyshev' or 'euclidean
    """
    inputfile_X = ''
    inputfile_Y = ''
    test_times = ''
    train_size = ''
    test_size = ''
    k = ''
    metric = ''
    try:
        opts, args = getopt.getopt(argv, '-h:-v:-l:-o:-t:-e:-r:-k:-m:',
                                   ['help', 'ifile_vector=', 'ifile_label=', 'ofile=', 'test_times=', 'test_size',
                                    'train_size=', 'k='
                                       , 'metric='])
    except getopt.GetoptError:
        print('knn-vspace.py \n -v <ifile_vector> \n -l<ifile_label>\n -t<test_times>\n -e<test_size>\n -r<train_size>\n '
              '-k<k>\n -m<metric>')
        sys.exit(2)
    
    for opt, arg in opts:
        if opt in ("-h", '--help'):
            print('knn-vspace.py \n -v <ifile_vector> \n -l<ifile_label>\n -t<test_times>\n -e<test_size>\n -r<train_size>\n '
              '-k<k>\n -m<metric>')
            sys.exit()
        elif opt in ("-v", '--ifile_vector'):
            inputfile_X = arg
        elif opt in ("-l", '--ifile_label'):
            inputfile_Y = arg
        elif opt in ("-t", '--test_times'):
            test_times = arg
        elif opt in ("-r", '--train_size'):
            train_size = arg
        elif opt in ("-e", '--test_size'):
            test_size = arg
        elif opt in ("-k", '--k'):
            k = arg
        elif opt in ("-m", '--metric'):
            metric = arg
    process_knn(loadX_from_text(inputfile_X), loadX_from_text(inputfile_Y), int(test_times), int(train_size), int(test_size), int(k),
                metric, "./logfile.txt")

if __name__ == "__main__":
    print("  _    _   __  __              _____  ")
    print(" | |  | | |  \\/  |     /\\     |  __ \\ ")
    print(" | |  | | | \\  / |    /  \\    | |  | |")
    print(" | |  | | | |\\/| |   / /\\ \\   | |  | |")
    print(" | |__| | | |  | |  / ____ \\  | |__| |")
    print("  \\____/  |_|  |_| /_/    \\_\\ |_____/ ")
    print("                                      ")
    print("                                      ")
    main(sys.argv[1:])

