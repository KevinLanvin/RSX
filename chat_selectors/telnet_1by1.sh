#!/usr/bin/expect -f


set number [lindex $argv 2]
set i 0
set timeout 5
spawn telnet [lindex $argv 0] [lindex $argv 1]
expect "hello"


send "/echo $number \r"
expect "echo :"
while { $i < $number } {
	send "a\r"
	incr i
}
expect "ok"


