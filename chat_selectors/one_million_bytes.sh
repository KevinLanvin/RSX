#!/bin/bash

#initialisation
make
xterm -hold -e "java Server" &
touch times_1by1.txt
touch times_all_in_1.txt
touch abscisses.txt
touch results.txt

sleep 2
#executions

for i in 1 5 10 20 35 50 75 100 150 200 250 300 400 500 750 1000 1500 2000 3000 4000 5000
do
echo $i >>abscisses.txt
/usr/bin/time -f "%e" -o times_1by1.txt -a ./telnet_1by1.sh localhost 7654 $i
/usr/bin/time -f "%e" -o times_all_in_1.txt -a ./telnet_all_in_1.sh localhost 7654 $i
done


#traitement gnuplot
paste abscisses.txt times_1by1.txt times_all_in_1.txt > results.txt
rm -f abscisses.txt
rm -f times_1by1.txt
rm -f times_all_in_1.txt

gnuplot plot

#rm -f results.txt


