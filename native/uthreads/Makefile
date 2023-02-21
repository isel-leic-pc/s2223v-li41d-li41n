main: main.c list.c uthread.c context_switch.s list.h uthread.h
	gcc -Wall -pedantic -g -o main main.c context_switch.s uthread.c list.c

clean:
	rm -f main
