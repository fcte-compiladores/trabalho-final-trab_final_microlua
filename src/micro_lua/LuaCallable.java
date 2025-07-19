package micro_lua;

import java.util.List;

interface LuaCallable {
    int arity();
    Object call(LuaInterpreter interpreter, List<Object> arguments);
}