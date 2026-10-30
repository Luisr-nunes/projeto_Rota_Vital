# Matriz de compatibilidade ABO/Rh 8x8 — U2 (AED) — Rota Vital
#   make          compila o main de teste
#   make test     compila com AddressSanitizer/UBSan e roda (sai com codigo != 0 se algo falhar)
#   make run      roda o main de teste
#   make valgrind roda no valgrind
#   make clean

CC      = gcc
CFLAGS  = -std=c99 -Wall -Wextra -pedantic -g
SAN     = -fsanitize=address,undefined -fno-omit-frame-pointer

LIB = matriz_compatibilidade.c
HDR = matriz_compatibilidade.h

all: main

main: main.c $(LIB) $(HDR)
	$(CC) $(CFLAGS) -o $@ main.c $(LIB)

main_asan: main.c $(LIB) $(HDR)
	$(CC) $(CFLAGS) $(SAN) -o $@ main.c $(LIB)

test: main_asan
	./main_asan

run: main
	./main

valgrind: main
	valgrind --leak-check=full --error-exitcode=1 ./main

clean:
	rm -f main main_asan *.exe *.o

.PHONY: all test run valgrind clean
