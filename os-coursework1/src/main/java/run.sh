rm -rf classes
mkdir classes
javac -d classes *.java
cd classes

for i in {1..4}
do
  for j in {1..4}
  do
    java InputGenerator ../../../../experiment1/seed$i/input_parameters$j.prp ../../../../experiment1/seed$i/input$j.in
    java InputGenerator ../../../../experiment2/seed$i/input_parameters$j.prp ../../../../experiment2/seed$i/input$j.in
    java InputGenerator ../../../../experiment3/seed$i/input_parameters$j.prp ../../../../experiment3/seed$i/input$j.in
  done
done

for k in {1..3}
do
  for i in {1..4}
  do
    for j in {1..4}
    do
      java Simulator ../../../../experiment$k/seed$i/simulator_parameters_FCFS.prp ../../../../experiment$k/seed$i/output${j}_FCFS.out ../../../../experiment$k/seed$i/input$j.in
      java Simulator ../../../../experiment$k/seed$i/simulator_parameters_Feedback.prp ../../../../experiment$k/seed$i/output${j}_Feedback.out ../../../../experiment$k/seed$i/input$j.in
      java Simulator ../../../../experiment$k/seed$i/simulator_parameters_IdealSJFS.prp ../../../../experiment$k/seed$i/output${j}_IdealSJFS.out ../../../../experiment$k/seed$i/input$j.in
      java Simulator ../../../../experiment$k/seed$i/simulator_parameters_RRS.prp ../../../../experiment$k/seed$i/output${j}_RRS.out ../../../../experiment$k/seed$i/input$j.in
      java Simulator ../../../../experiment$k/seed$i/simulator_parameters_SJFS.prp ../../../../experiment$k/seed$i/output${j}_SJFS.out ../../../../experiment$k/seed$i/input$j.in
    done
  done
done
