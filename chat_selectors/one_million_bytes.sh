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

for i in 1000 10000 100000 250000 500000 750000 900000
do
echo $i >>abscisses.txt
/usr/bin/time -f "%U" -o times_1by1.txt -a ./telnet_1by1.sh localhost 7654 $i
/usr/bin/time -f "%U" -o times_all_in_1.txt -a ./telnet_all_in_1.sh localhost 7654 $i
done


#traitement gnuplot
paste abscisses.txt times_1by1.txt times_all_in_1.txt > results.txt
rm -f abscisses.txt
rm -f times_1by1.txt
rm -f times_all_in_1.txt

gnuplot plot

rm -f results.txt


