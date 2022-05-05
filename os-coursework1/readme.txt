在项目的main/java文件夹下 执行 run.sh 脚本
在参数文件夹experiment1下，有seed1到see4文件夹，
seed1下，有input_parameters1.prp到input_parameters4.prp的四个输入文件，这里的每个prp文件里 meanCpuBurst 不一样，seed 值一样，都是 270826029269605
而在seed2下，有和seed1文件夹一样的结构，这里的只是seed=100000，seed3与seed4文件夹类似

input*.in 与 output*.out，前者脚本通过InputGenerator.java生成的参数文件，后面是Simulator.java跑出来的各个执行器的执行结果