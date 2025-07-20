package micro_lua;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class LuaTable {
    public final Map<Object, Object> elements = new HashMap<>();
    public final List<Object> arrayPart = new ArrayList<>();
    private LuaTable metatable = null;

    public Object get(Object key) {
        // Primeiro verifica se é um índice numérico válido para a parte array
        if (key instanceof Double) {
            int index = ((Double) key).intValue();
            if (index >= 1 && index <= arrayPart.size()) {
                return arrayPart.get(index - 1);
            }
        }
        
        // Depois verifica no hash map
        if (elements.containsKey(key)) {
            return elements.get(key);
        }
        
        // Finalmente verifica metatable
        if (metatable != null && metatable.get("__index") != null) {
            Object handler = metatable.get("__index");
            if (handler instanceof LuaCallable) {
                return ((LuaCallable) handler).call(null, Arrays.asList(this, key));
            } else if (handler instanceof LuaTable) {
                return ((LuaTable) handler).get(key);
            }
        }
        
        return null;
    }

    public void set(Object key, Object value) {
        // Se for um índice numérico sequencial, adiciona à parte array
        if (key instanceof Double) {
            int index = ((Double) key).intValue();
            if (index == arrayPart.size() + 1) {
                arrayPart.add(value);
                return;
            } else if (index >= 1 && index <= arrayPart.size()) {
                arrayPart.set(index - 1, value);
                return;
            }
        }
        
        // Caso contrário, adiciona ao hash map
        elements.put(key, value);
    }

    public void setMetatable(LuaTable mt) {
        this.metatable = mt;
    }

    public LuaTable getMetatable() {
        return metatable;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        
        // Parte array
        for (int i = 0; i < arrayPart.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(stringify(arrayPart.get(i)));
        }
        
        // Parte hash
        boolean first = arrayPart.isEmpty();
        for (Map.Entry<Object, Object> entry : elements.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            
            if (entry.getKey() instanceof String && 
                ((String) entry.getKey()).matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                sb.append(entry.getKey()).append(" = ");
            } else {
                sb.append("[").append(stringify(entry.getKey())).append("] = ");
            }
            sb.append(stringify(entry.getValue()));
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    private String stringify(Object obj) {
        if (obj == null) return "nil";
        if (obj instanceof String) return "\"" + obj + "\"";
        return obj.toString();
    }
}