# Estruturas da U1 (AED) — Rota Vital
#   make            compila testes e demo
#   make test       roda os testes com AddressSanitizer (vazamento / uso após free)
#   make valgrind   roda testes e demo no valgrind
#   make run        roda a demonstração
#   make clean

CC      = gcc
CFLAGS  = -std=c99 -Wall -Wextra -pedantic -g
SAN     = -fsanitize=address,undefined -fno-omit-frame-pointer

LIB = dominio.c lista_estoque.c fila_requisicoes.c pilha_historico.c gestao_estoque.c arvore_bolsas.c
HDR = $(LIB:.c=.h)

all: testes demo

testes: testes.c $(LIB) $(HDR)
	$(CC) $(CFLAGS) -o $@ testes.c $(LIB)

demo: demo.c $(LIB) $(HDR)
	$(CC) $(CFLAGS) -o $@ demo.c $(LIB)

testes_asan: testes.c $(LIB) $(HDR)
	$(CC) $(CFLAGS) $(SAN) -o $@ testes.c $(LIB)

test: testes_asan
	./testes_asan

valgrind: testes demo
	valgrind --leak-check=full --error-exitcode=1 ./testes
	valgrind --leak-check=full --error-exitcode=1 ./demo

run: demo
	./demo

clean:
	rm -f testes demo testes_asan *.exe

.PHONY: all test valgrind run clean
