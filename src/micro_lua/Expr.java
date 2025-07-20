package micro_lua;

import java.util.List;

public abstract class Expr {
    public interface Visitor<R> {
        R visitAssignExpr(Assign expr);
        R visitBinaryExpr(Binary expr);
        R visitCallExpr(Call expr);
        R visitLiteralExpr(Literal expr);
        R visitGroupingExpr(Grouping expr);
        R visitLogicalExpr(Logical expr);
        R visitUnaryExpr(Unary expr);
        R visitVariableExpr(Variable expr);
        R visitTableExpr(Table expr);
        R visitTableIndexExpr(TableIndex expr);
        R visitTableFieldExpr(TableField expr);
    }

    public abstract <R> R accept(Visitor<R> visitor);

    public static class Assign extends Expr {
        public final Token name;
        public final Expr value;

        public Assign(Token name, Expr value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitAssignExpr(this);
        }
    }

    public static class Binary extends Expr {
        public final Expr left;
        public final Token operator;
        public final Expr right;

        public Binary(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitBinaryExpr(this);
        }
    }

    public static class Call extends Expr {
        public final Expr callee;
        public final Token paren;
        public final List<Expr> arguments;

        public Call(Expr callee, Token paren, List<Expr> arguments) {
            this.callee = callee;
            this.paren = paren;
            this.arguments = arguments;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitCallExpr(this);
        }
    }

    public static class Literal extends Expr {
        public final Object value;

        public Literal(Object value) {
            this.value = value;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitLiteralExpr(this);
        }
    }

    public static class Grouping extends Expr {
        public final Expr expression;

        public Grouping(Expr expression) {
            this.expression = expression;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitGroupingExpr(this);
        }
    }

    public static class Logical extends Expr {
        public final Expr left;
        public final Token operator;
        public final Expr right;

        public Logical(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitLogicalExpr(this);
        }
    }

    public static class Unary extends Expr {
        public final Token operator;
        public final Expr right;

        public Unary(Token operator, Expr right) {
            this.operator = operator;
            this.right = right;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitUnaryExpr(this);
        }
    }

    public static class Variable extends Expr {
        public final Token name;

        public Variable(Token name) {
            this.name = name;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitVariableExpr(this);
        }
    }

    public static class Table extends Expr {
        public final Token brace;
        public final List<Field> fields;

        public Table(Token brace, List<Field> fields) {
            this.brace = brace;
            this.fields = fields;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitTableExpr(this);
        }
    }

    public static class Field {
        public final Expr key;
        public final Expr value;

        public Field(Expr key, Expr value) {
            this.key = key;
            this.value = value;
        }
    }

    public static class TableIndex extends Expr {
        public final Expr table;
        public final Expr index;
        public final Token bracket;

        public TableIndex(Expr table, Expr index, Token bracket) {
            this.table = table;
            this.index = index;
            this.bracket = bracket;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitTableIndexExpr(this);
        }
    }

    public static class TableField extends Expr {
        public final Expr table;
        public final Token field;

        public TableField(Expr table, Token field) {
            this.table = table;
            this.field = field;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitTableFieldExpr(this);
        }
    }
}