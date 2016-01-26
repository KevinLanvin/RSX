#!/usr/bin/expect -f

set number [lindex $argv 2]
set i 1
set timeout 10
set string "a"
spawn telnet [lindex $argv 0] [lindex $argv 1]
expect "hello"
	
while { $i < $number } {
	append string "a"
	incr i
}
send "/echo $number \r"
expect "echo :"
send "$string\r"
expect "ok"
