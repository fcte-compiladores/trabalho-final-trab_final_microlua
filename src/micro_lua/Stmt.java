package micro_lua;

import java.util.List;

public abstract class Stmt {
    public interface Visitor<R> {
        R visitBlockStmt(Block stmt);
        R visitExpressionStmt(Expression stmt);
        R visitFunctionStmt(Function stmt);
        R visitIfStmt(If stmt);
        R visitReturnStmt(Return stmt);
        R visitLocalVarStmt(LocalVar stmt);
        R visitWhileStmt(While stmt);
        R visitRepeatStmt(Repeat stmt);
        R visitForStmt(For stmt);
        R visitBreakStmt(Break stmt);
    }

    public abstract <R> R accept(Visitor<R> visitor);

    // 1. Bloco de código
    public static class Block extends Stmt {
        public final List<Stmt> statements;

        public Block(List<Stmt> statements) {
            this.statements = statements;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitBlockStmt(this);
        }
    }

    // 2. Statement de expressão
    public static class Expression extends Stmt {
        public final Expr expression;

        public Expression(Expr expression) {
            this.expression = expression;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitExpressionStmt(this);
        }
    }

    // 3. Declaração de função
    public static class Function extends Stmt {
        public final Token name;
        public final List<Token> params;
        public final List<Stmt> body;
        public final boolean isLocal;

        public Function(Token name, List<Token> params, List<Stmt> body) {
            this(name, params, body, false);
        }

        public Function(Token name, List<Token> params, List<Stmt> body, boolean isLocal) {
            this.name = name;
            this.params = params;
            this.body = body;
            this.isLocal = isLocal;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitFunctionStmt(this);
        }
    }

    // 4. Statement if
    public static class If extends Stmt {
        public final Expr condition;
        public final Stmt thenBranch;
        public final Stmt elseBranch;

        public If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitIfStmt(this);
        }
    }

    // 5. Statement return
    public static class Return extends Stmt {
        public final Token keyword;
        public final Expr value;

        public Return(Token keyword, Expr value) {
            this.keyword = keyword;
            this.value = value;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitReturnStmt(this);
        }
    }

    // 6. Declaração de variável local
    public static class LocalVar extends Stmt {
        public final Token name;
        public final Expr initializer;

        public LocalVar(Token name, Expr initializer) {
            this.name = name;
            this.initializer = initializer;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitLocalVarStmt(this);
        }
    }

    // 7. Loop while
    public static class While extends Stmt {
        public final Expr condition;
        public final Stmt body;

        public While(Expr condition, Stmt body) {
            this.condition = condition;
            this.body = body;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitWhileStmt(this);
        }
    }

    // 8. Loop repeat until
    public static class Repeat extends Stmt {
        public final Expr condition;
        public final Stmt body;

        public Repeat(Expr condition, Stmt body) {
            this.condition = condition;
            this.body = body;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitRepeatStmt(this);
        }
    }

    // 9. Loop for
    public static class For extends Stmt {
        public final Token name;
        public final Expr initializer;
        public final Expr condition;
        public final Expr increment;
        public final Stmt body;

        public For(Token name, Expr initializer, Expr condition, Expr increment, Stmt body) {
            this.name = name;
            this.initializer = initializer;
            this.condition = condition;
            this.increment = increment;
            this.body = body;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitForStmt(this);
        }
    }

    // 10. Statement break
    public static class Break extends Stmt {
        public final Token keyword;

        public Break(Token keyword) {
            this.keyword = keyword;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitBreakStmt(this);
        }
    }
}