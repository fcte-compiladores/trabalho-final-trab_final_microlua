MicroLua - Interpretador de Lua Simplificado
Integrantes

    Davi dos Santos Brito Nobre
    Matrícula: 211062929
    turma: 16h

    João Victor de Carvalho Nobre 
    Matrícula: 202017423 
    turma: 16h

    DANIELLE RODRIGUES SILVA
    Matrícula: 211061574 
    turma: 16h


## Introdução

O MicroLua é um interpretador para um subconjunto da linguagem Lua, implementado em Java. O projeto implementa as principais características da linguagem Lua, incluindo:

    Tipos básicos: nil, boolean, number, string, function, table

    Estruturas de controle: if, while, repeat, for

    Funções e escopos locais/globais

    Tabelas básicas

    Funções nativas: print(), clock(), type(), table.insert(), etc.

Exemplos de sintaxe
lua:



  -- Função Fibonacci
  function fib(n)
  if n < 2 then return n end
  return fib(n-1) + fib(n-2)
  end

  print(fib(10))

-- Tabelas:
  pessoa = {
    nome = "João",
    idade = 30,
  }

  print(pessoa.nome)


## Instalação

Pré-requisitos:

    Java JDK 11 ou superior


passo 1: clone nosso repo:

git clone https://github.com/seu-usuario/microlua.git
cd microlua


2 - compilar o projeto inteiro:
javac -d bin src/micro_lua/*.java

passo 3: execute exemplos no interpretador ou no modo interativo:


exemplo para uso da pasta exemplos: java -cp bin micro_lua.Lua exemplos/hello.mlua

exemplo do iterativo: java -cp bin micro_lua.Lua


## Estrutura do Código

O projeto está organizado nos seguintes componentes principais:
Análise Léxica

    Scanner.java: Converte código fonte em tokens

    Token.java: Representação dos tokens

    TokenType.java: Enumeração dos tipos de tokens

Análise Sintática

    Parser.java: Constrói a AST (Abstract Syntax Tree) a partir dos tokens

    Expr.java: Representação de expressões

    Stmt.java: Representação de statements

Análise Semântica

    Resolver.java: Resolução de escopos e ligação de variáveis

Execução

    LuaInterpreter.java: Interpreta e executa a AST

    Environment.java: Gerencia ambientes de execução (escopos)

    LuaTable.java: Implementação de tabelas Lua

    LuaFunction.java: Representação de funções

    LuaCallable.java: Interface para funções chamáveis

    Sistema de Tipos e Operações

    LuaTable.java: Implementa tabelas com metatabelas

    Operadores com suporte a metamétodos (__add, __sub, etc.)

Controle de Fluxo

    Break.java, Return.java: Implementam controle de fluxo

    RuntimeError.java: Tratamento de erros em tempo de execução


## Referências Bibliográficas e bibliografia

Foi usada duas referências mãe para o desenvolvimento do projeto, além de claro os exercicios e as aulas de compiladores 1, esses sendo: 

Implementação de Lox feita em Rust por Darksecond:
https://github.com/Darksecond/lox
Foi usada como base para a estrutura geral do interpretador, incluindo o padrão Visitor para a AST e o sistema de resolução de escopos. Adaptamos a implementação para a sintaxe e semântica específicas de Lua.

Linguagem LUA oficial:

foi referência para o que fariamos e o escopo e semântica (tabelas, comportamentos etc).

Bugs e Limitações Conhecidas

    Metatabelas limitadas: Suporte básico a metamétodos, mas definitivamente não implementamos todos os recursos

    Performance: Não há otimizações para chamadas recursivas

    Coleta de lixo: Não foi feita pelos membros. 

    Biblioteca padrão: Apenas funções básicas disponíveis (foi pensado em seguiur o mesmo escopo do Lox, ou seja o suficiente pra aprendizado)


## Exemplos

nossos exemplos estão todos na pasta exemplos, o modo de as utilizar foi descrito na instalação. 
