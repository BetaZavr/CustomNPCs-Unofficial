package noppes.npcs.api.wrapper;

import jdk.dynalink.beans.StaticClass;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.handler.IDataObject;
import noppes.npcs.api.handler.data.IDataElement;
import noppes.npcs.api.wrapper.data.DataElement;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.CustomNPCsScheduler;
import noppes.npcs.util.Util;

import javax.annotation.Nonnull;
import java.lang.reflect.*;
import java.util.*;

public class DataObject implements IDataObject {

    private static final Map<String, String> obfuscated = new HashMap<>();

    public static void load() {
        CustomNPCsScheduler.runTack(() -> {
            obfuscated.clear();
            String data = Util.instance.getDataFile("obf.dat");
            if (data.isEmpty()) { return; }
            for (String line : data.split("\n")) {
                if (line.contains("=")) {
                    String[] d = line.split("=");
                    if (d[0].contains("\n")) { d[0] = d[0].replace("\n", ""); }
                    if (d[1].contains("\n")) { d[1] = d[1].replace("\n", ""); }
                    obfuscated.put(d[0], d[1]);
                }
            }
        });
    }

    public static @Nonnull String getObfuscatedName(String name) {
        if (obfuscated.containsKey(name)) { return obfuscated.get(name); }
        return "";
    }

    public static @Nonnull List<String> getObfuscatedKeys() { return new ArrayList<>(obfuscated.keySet()); }

    public static String getAgrName(Class<?> classType, Type genericType, Object obj) {
        StringBuilder key = new StringBuilder(classType.getName());
        if (List.class.isAssignableFrom(classType) || Map.class.isAssignableFrom(classType)) {
            key.append("<");
            boolean has = false;
            if (obj != null) {
                if (obj instanceof List) {
                    if (!((List<?>) obj).isEmpty()) {
                        for (Object o : (List<?>) obj) {
                            if (o == null) { continue; }
                            key.append(getAgrName(o.getClass(), o.getClass().getGenericSuperclass(), obj));
                            has = true;
                            break;
                        }
                    }
                }
                else if (obj instanceof Map) {
                    if (!((Map<?, ?>) obj).isEmpty()) {
                        Class<?> k = null;
                        Class<?> v = null;
                        for (Object o : ((Map<?, ?>) obj).keySet()) {
                            if (o != null) {
                                if (k == null) { k = o.getClass(); }
                                if (((Map<?, ?>) obj).get(o) != null) {
                                    v = ((Map<?, ?>) obj).get(o).getClass();
                                }
                            }
                            if (k != null && v != null) { break; }
                        }
                        if (k != null && v != null) {
                            key.append(getAgrName(k, k.getGenericSuperclass(), null))
                                    .append(", ")
                                    .append(getAgrName(k, k.getGenericSuperclass(), null));
                            has = true;
                        }
                    }
                }
            }
            if (!has && genericType instanceof ParameterizedType) {
                Type[] actualTypes = ((ParameterizedType) genericType).getActualTypeArguments();
                for (int i = 0; i < actualTypes.length; i++) {
                    String name = actualTypes[i].toString();
                    if (name.contains(".")) { name = name.substring(name.lastIndexOf(".") + 1); }
                    key.append(name);
                    if (i < actualTypes.length - 1) { key.append(", "); }
                }
            }
            key.append(">");
        }
        else {
            TypeVariable<?>[] typeParams = classType.getTypeParameters();
            if (typeParams.length != 0) {
                key.append("<");
                for (int i = 0; i < typeParams.length; i++) {
                    key.append(typeParams[i].getName()).append(" extends ");
                    for (Type bound : typeParams[i].getBounds()) {
                        Class<?> boundClass = null;
                        if (bound instanceof Class) {
                            boundClass = (Class<?>) bound;
                        } else {
                            try { boundClass = Class.forName(bound.getTypeName()); }
                            catch (Exception ignored) {}
                        }
                        if (boundClass != null) {
                            key.append(getAgrName(boundClass, boundClass.getGenericSuperclass(), null));
                            break;
                        }
                    }
                    if (i < typeParams.length - 1) { key.append(", "); }
                }
                key.append(">");
            }
        }
        if (classType.isArray()) {
            Class<?> ct = classType.getComponentType();
            key = new StringBuilder(getAgrName(ct, ct.getGenericSuperclass(), obj));
            key.append("[]");
        }
        return key.toString();
    }

    public final Set<IDataElement> elements = new LinkedHashSet<>();
    public final Object object;
    public final Class<?> clazz;

    public DataObject(Object objectIn) {
        if (objectIn == null) { throw new CustomNPCsException("NULL is nothing and is not allowed for DUMP"); }
        if (objectIn.getClass() == StaticClass.class) { objectIn = ((StaticClass) objectIn).getRepresentedClass(); }
        object = objectIn;
        Class<?> parent;
        try { parent = (Class<?>) objectIn; }
        catch (Exception e) { parent = objectIn.getClass(); }
        clazz = parent;
        LogWriter.debug("Trying to get all fields, methods and classes from object \"" + objectIn + "\"");
        // Constructors
        int id = 0;
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) { elements.add(new DataElement(constructor, objectIn, id++)); }
        // Classes
        id = 0;
        for (Class<?> cl : clazz.getDeclaredClasses()) { elements.add(new DataElement(cl, objectIn, id++)); }
        // Data
        Set<Field> fields = new LinkedHashSet<>(Arrays.asList(clazz.getDeclaredFields()));
        Set<Method> methods = new LinkedHashSet<>(Arrays.asList(clazz.getDeclaredMethods()));
        List<Class<?>> allParent = new ArrayList<>();
        Class<?> tempClass = clazz;
        allParent.add(tempClass);
        while (tempClass.getSuperclass() != Object.class && !allParent.contains(tempClass.getSuperclass())) {
            allParent.add(tempClass.getSuperclass());
            tempClass = tempClass.getSuperclass();
        }
        for (Class<?> c : allParent) {
            for (Field f : c.getFields()) {
                int modifiers = f.getModifiers();
                if (!Modifier.isStatic(modifiers)) { fields.add(f); }
            }
            for (Method m : c.getMethods()) {
                int modifiers = m.getModifiers();
                if (!Modifier.isStatic(modifiers)) { methods.add(m); }
            }
        }
        // Fields
        id = 0;
        for (Field field : fields) { elements.add(new DataElement(field, objectIn, id++)); }
        // Methods
        id = 0;
        for (Method method : methods) { elements.add(new DataElement(method, objectIn, id++)); }
    }

    @Override
    public IDataElement getConstructor(Object index) {
        List<IDataElement> list = getConstructors();
        if (index == null) { return list.isEmpty() ? null : list.get(0); }
        int id;
        try { id = (int) index; } catch (Exception ignored) {
            try { id = Integer.parseInt(index.toString()); } catch (Exception ignored1) { return null; }
        }
        if (id > 0) {
            for (IDataElement de : list) {
                if (de.getId() == id) { return de; }
            }
        }
        return null;
    }

    @Override
    public IDataElement getClazz(Object index) {
        if (index == null) { return null; }
        String name = index.toString();
        int id = -1;
        try { id = (int) index; } catch (Exception ignored) {
            try { id = Integer.parseInt(index.toString()); } catch (Exception ignored1) { }
        }
        List<IDataElement> list = getClasses();
        if (id > 0) {
            for (IDataElement de : list) {
                if (de.getId() == id) { return de; }
            }
        }
        for (IDataElement de : list) {
            if (de.getName().equals(name)) { return de; }
        }
        return null;
    }

    @Override
    public IDataElement getField(Object index) {
        if (index == null) { return null; }
        String name = index.toString();
        int id = -1;
        try { id = (int) index; } catch (Exception ignored) {
            try { id = Integer.parseInt(index.toString()); } catch (Exception ignored1) { }
        }
        List<IDataElement> list = getFields();
        if (id > 0) {
            for (IDataElement de : list) {
                if (de.getId() == id) { return de; }
            }
        }
        for (IDataElement de : list) {
            if (de.getName().equals(name)) { return de; }
        }
        return null;
    }

    @Override
    public IDataElement getMethod(Object index) {
        if (index == null) { return null; }
        String name = index.toString();
        int id = -1;
        try { id = (int) index; } catch (Exception ignored) {
            try { id = Integer.parseInt(index.toString()); } catch (Exception ignored1) { }
        }
        List<IDataElement> list = getMethods();
        if (id > 0) {
            for (IDataElement de : list) {
                if (de.getId() == id) { return de; }
            }
        }
        for (IDataElement de : list) {
            if (de.getName().equals(name)) { return de; }
        }
        return null;
    }

    @Override
    public List<IDataElement> getConstructors() {
        List<IDataElement> list = new ArrayList<>();
        for (IDataElement de : elements) {
            if (de.getObject() instanceof Constructor) { list.add(de); }
        }
        return list;
    }

    @Override
    public List<IDataElement> getClasses() {
        List<IDataElement> list = new ArrayList<>();
        for (IDataElement de : elements) {
            if (de.getObject() instanceof Class) { list.add(de); }
        }
        return list;
    }

    @Override
    public List<IDataElement> getFields() {
        List<IDataElement> list = new ArrayList<>();
        for (IDataElement de : elements) {
            if (de.getObject() instanceof Field) { list.add(de); }
        }
        list.sort(Comparator.comparing(element -> element.getObfuscatedName().isEmpty() ? element.getName() : element.getObfuscatedName()));
        return list;
    }

    @Override
    public List<IDataElement> getMethods() {
        List<IDataElement> list = new ArrayList<>();
        for (IDataElement de : elements) {
            if (de.getObject() instanceof Method) { list.add(de); }
        }
        list.sort(Comparator.comparing(element -> element.getObfuscatedName().isEmpty() ? element.getName() : element.getObfuscatedName()));
        return list;
    }

    @Override
    public String getConstructorsInfo() {
        StringBuilder builder = new StringBuilder();
        List<IDataElement> list = getConstructors();
        if (!list.isEmpty()) {
            builder.append("Constructors: [").append((char) 10);
            int i = 0;
            for (IDataElement element : list) {
                builder.append(" ");
                addTabs(builder, String.valueOf(list.size()).length() - String.valueOf(i).length()); // tabs
                builder.append(i).append(": ") // pos
                        .append(element.getInfo()).append((char) 10); // info
                i++;
            }
            builder.append("]");
        }
        return builder.toString();
    }

    @Override
    public String getClassesInfo() {
        StringBuilder builder = new StringBuilder();
        List<IDataElement> list = getClasses();
        if (!list.isEmpty()) {
            builder.append("Sub-Classes: [").append((char) 10);
            int i = 0;
            for (IDataElement element : list) {
                builder.append(" ");
                addTabs(builder, String.valueOf(list.size()).length() - String.valueOf(i).length()); // tabs
                builder.append(i).append(": ") // pos
                        .append(element.getInfo()).append((char) 10); // info
                i++;
            }
            builder.append("]");
        }
        return builder.toString();
    }

    @Override
    public String getFieldsInfo() {
        StringBuilder builder = new StringBuilder();
        List<IDataElement> list = getFields();
        if (!list.isEmpty()) {
            List<Field> decFields = Arrays.asList(clazz.getDeclaredFields());
            Map<Integer, String> keys = new HashMap<>();
            Map<Integer, String> names = new HashMap<>();
            Map<Integer, String> values = new HashMap<>();
            Map<Integer, IDataElement> fields = new HashMap<>();
            int i = 0;
            builder.append("Fields: [").append((char) 10);

            StringBuilder numTab = new StringBuilder(" ");
            while (numTab.length() < String.valueOf(list.size()).length() - 1) { numTab.append(" "); }
            numTab.append("ID: (modifiers and type)");
            String valueTab = "(value)";
            String nameTab = "(name / obfuscated)";
            String classTab = "(class to which the field belongs)";
            int wK = numTab.length();
            int wN = valueTab.length();
            int wF = classTab.length();
            String non_pos = String.format("%" + String.valueOf(list.size() - 1).length() + "s", "").replace(" ", "-");
            for(IDataElement element : list) {
                StringBuilder key = new StringBuilder(" ");
                if (decFields.contains((Field) element.getObject())) {
                    while (key.length() - 1 < String.valueOf(list.size()).length() - String.valueOf(element.getId()).length()) { key.append(" "); }
                    key.append(element.getId());
                }
                else { key.append(non_pos); }
                key.append(": ");
                Field field = (Field) element.getObject();
                int modifiers = field.getModifiers();
                if (Modifier.isPublic(modifiers)) { key.append("public "); }
                else if (Modifier.isPrivate(modifiers)) { key.append("private "); }
                else if (Modifier.isProtected(modifiers)) { key.append("protected "); }
                else { key.append("default "); }
                if (Modifier.isStatic(modifiers)) { key.append("static "); }
                if (Modifier.isFinal(modifiers)) { key.append("final "); }
                Object o = null;
                try {
                    field.setAccessible(true);
                    o = field.get(object);
                }
                catch (Exception ignored) { }
                key.append(getAgrName(field.getType(), field.getGenericType(), o));
                keys.put(i, key.toString());
                if (wK < key.toString().length()) { wK = key.toString().length(); }

                StringBuilder name = new StringBuilder();
                if (!element.getObfuscatedName().isEmpty()) { name.append(element.getObfuscatedName()).append(" / "); }
                name.append(field.getName());
                names.put(i, name.toString());
                if (wN < name.toString().length()) { wN = name.toString().length(); }
                String value = o == null ? "null" : o.toString();
                if (value.lastIndexOf("@") > 0) { value = value.substring(0, value.lastIndexOf("@")); }
                values.put(i, value);
                if (wF < value.length()) { wF = value.length(); }
                fields.put(i, element);
                i++;
            }
            // description ceils:
            StringBuilder line = new StringBuilder(numTab);
            addTabs(line, wK - numTab.length());
            line.append(": ").append(nameTab);
            addTabs(line, wN - nameTab.length());
            line.append(": ").append(valueTab);
            addTabs(line, wF - valueTab.length());
            line.append(": ").append(classTab);
            builder.append(line).append((char) 10);
            // Lines
            for (int p : keys.keySet()) {
                String key = keys.get(p);
                line = new StringBuilder(key);
                addTabs(line, wK - key.length());
                String name = names.get(p);
                line.append(": ").append(name);
                addTabs(line, wN - name.length());

                String value = values.get(p);
                line.append(": ").append(value);
                if (!decFields.contains((Field) fields.get(p).getObject())) {
                    addTabs(line, wF - value.length());
                    line.append(": ").append(((Field) fields.get(p).getObject()).getDeclaringClass().getName());
                }
                builder.append(line).append((char) 10);
            }
            builder.append("]");
        }
        return builder.toString();
    }

    @Override
    public String getMethodsInfo() {
        StringBuilder builder = new StringBuilder();
        List<IDataElement> list = getMethods();
        if (!list.isEmpty()) {
            List<Method> decMethods = Arrays.asList(clazz.getDeclaredMethods());
            Map<Integer, String> keys = new TreeMap<>();
            Map<Integer, String> names = new TreeMap<>();
            Map<Integer, IDataElement> methods = new TreeMap<>();
            int i = 0;
            builder.append("Methods: [").append((char) 10);

            StringBuilder numTab = new StringBuilder(" ");
            while (numTab.length() < String.valueOf(list.size()).length() - 1) { numTab.append(" "); }
            numTab.append("ID: (modifiers and return type)");
            String nameTab = "(name / obfuscated)";
            String classTab = "(class to which the field belongs)";
            int wK = numTab.length();
            int wM = nameTab.length();
            String non_pos = String.format("%" + String.valueOf(list.size() - 1).length() + "s", "").replace(" ", "-");
            for(IDataElement element : list) {
                StringBuilder key = new StringBuilder(" ");
                if (decMethods.contains((Method) element.getObject())) {
                    while (key.length() - 1 < String.valueOf(list.size()).length() - String.valueOf(i).length()) { key.append(" "); }
                    key.append(i);
                }
                else { key.append(non_pos); }
                key.append(": ");
                Method method = (Method) element.getObject();
                int modifiers = method.getModifiers();
                if (Modifier.isPublic(modifiers)) { key.append("public "); }
                else if (Modifier.isPrivate(modifiers)) { key.append("private "); }
                else if (Modifier.isProtected(modifiers)) { key.append("protected "); }
                else { key.append("default "); }
                if (Modifier.isStatic(modifiers)) { key.append("static "); }
                if (Modifier.isSynchronized(modifiers)) { key.append("synchronized "); }
                if (Modifier.isFinal(modifiers)) { key.append("final "); }
                key.append(getAgrName(method.getReturnType(), method.getGenericReturnType(), element.getValue()));
                keys.put(i, key.toString());
                if (wK < key.toString().length()) { wK = key.toString().length(); }

                StringBuilder name = new StringBuilder();
                if (!element.getObfuscatedName().isEmpty()) { name.append(element.getObfuscatedName()).append(" / "); }
                name.append(method.getName()).append("(");
                Class<?>[] params = method.getParameterTypes();
                for(int j = 0; j < params.length; j++) {
                    name.append(params[j].getSimpleName());
                    if (j < params.length - 1) { name.append(", "); }
                }
                name.append(")");
                names.put(i, name.toString());
                if (wM < name.length()) { wM = name.length(); }
                methods.put(i, element);
                i++;
            }
            // description ceils:
            StringBuilder line = new StringBuilder(numTab);
            addTabs(line, wK - numTab.length());
            line.append(": ").append(nameTab);
            addTabs(line, wM - nameTab.length());
            line.append(": ").append(classTab);
            builder.append(line).append((char) 10);
            for (int p : keys.keySet()) {
                String key = keys.get(p);
                line = new StringBuilder(key);

                addTabs(line, wK - key.length());
                String name = names.get(p);
                line.append(": ").append(name);

                if (!decMethods.contains((Method) methods.get(p).getObject())) {
                    addTabs(line, wM - name.length());
                    line.append(": ").append(((Method) methods.get(p).getObject()).getDeclaringClass().getName());
                }
                builder.append(line).append((char) 10);
            }
            builder.append("]");
        }
        return builder.toString();
    }

    @Override
    public String getInfo() {
        StringBuilder builder = new StringBuilder();
        int modifiers = clazz.getModifiers();
        String key = "";
        if (Modifier.isPrivate(modifiers)) { key = "Private "; }
        else if (Modifier.isPublic(modifiers)) { key = "Public "; }
        else if (Modifier.isProtected(modifiers)) { key = "Protected "; }
        if (Modifier.isStatic(modifiers)) { key += "Static "; }
        if (Modifier.isAbstract(modifiers)) { key += "Abstract "; }
        if (Modifier.isInterface(modifiers)) { key += "Interface "; }
        builder.append(key).append("Class ").append(clazz.getName());
        if (clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class) {
            builder.append(" extends ").append(DataObject.getAgrName(clazz.getSuperclass(), clazz.getSuperclass().getGenericSuperclass(), object));
        }
        Class<?>[] implementers = object.getClass().getInterfaces();
        if (implementers.length > 0) {
            builder.append(" implements ");
            for (int i = 0; i < implementers.length; i++) {
                builder.append(DataObject.getAgrName(implementers[i], implementers[i].getGenericSuperclass(), null));
                if (i < implementers.length - 1) { builder.append(", "); }
            }
        }
        builder.append(":").append((char) 10).append("As an object: ").append(object).append((char) 10);
        // Constructors
        String values = getConstructorsInfo();
        if (!values.isEmpty()) { builder.append(values).append(";").append((char) 10); }
        // Classes
        values = getClassesInfo();
        if (!values.isEmpty()) { builder.append(values).append(";").append((char) 10); }
        // Fields
        values = getFieldsInfo();
        if (!values.isEmpty()) { builder.append(values).append(";").append((char) 10); }
        // Methods
        values = getMethodsInfo();
        if (!values.isEmpty()) { builder.append(values).append(";"); }
        return builder.toString();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        int modifiers = clazz.getModifiers();
        String key = "";
        if (Modifier.isPrivate(modifiers)) { key = "Private "; }
        else if (Modifier.isPublic(modifiers)) { key = "Public "; }
        else if (Modifier.isProtected(modifiers)) { key = "Protected "; }
        if (Modifier.isStatic(modifiers)) { key += "Static "; }
        if (Modifier.isAbstract(modifiers)) { key += "Abstract "; }
        if (Modifier.isInterface(modifiers)) { key += "Interface "; }
        builder.append(key).append("Class ").append(clazz.getName());
        if (clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class) {
            builder.append(" extends ").append(DataObject.getAgrName(clazz.getSuperclass(), clazz.getSuperclass().getGenericSuperclass(), object));
        }
        Class<?>[] implementers = object.getClass().getInterfaces();
        if (implementers.length > 0) {
            builder.append(" implements ");
            for (int i = 0; i < implementers.length; i++) {
                builder.append(DataObject.getAgrName(implementers[i], implementers[i].getGenericSuperclass(), null));
                if (i < implementers.length - 1) { builder.append(", "); }
            }
        }
        builder.append(":").append((char) 10).append("As an object: ").append(object).append((char) 10);
        // Constructors
        int size = getConstructors().size();
        if (size > 0) { builder.append("Constructors size: ").append(size).append(";").append((char) 10); }
        // Classes
        List<IDataElement> list = getClasses();
        if (!list.isEmpty()) {
            builder.append("Sub-Classes: [");
            StringBuilder names = new StringBuilder();
            for (IDataElement element : list) {
                if (!names.isEmpty()) { names.append(", "); }
                names.append(element.getParentClass().getSimpleName());
            }
            builder.append(names).append("];").append((char) 10);
        }
        // Fields
        list = getFields();
        if (!list.isEmpty()) {
            List<Field> decFields = Arrays.asList(clazz.getDeclaredFields());
            builder.append("Fields: [");
            StringBuilder names = new StringBuilder();
            for (IDataElement element : list) {
                if (!names.isEmpty()) { names.append(", "); }
                names.append(element.getName());
                if (decFields.contains((Field) element.getObject())) {
                    names.append("|").append("ID:").append(element.getId());
                }
                if (!element.getObfuscatedName().isEmpty()) {
                    names.append("|").append(element.getObfuscatedName());
                }
            }
            builder.append(names).append("];").append((char) 10);
        }
        // Methods
        list = getMethods();
        if (!list.isEmpty()) {
            List<Method> decFields = Arrays.asList(clazz.getDeclaredMethods());
            builder.append("Methods: [");
            StringBuilder names = new StringBuilder();
            for (IDataElement element : list) {
                if (!names.isEmpty()) { names.append(", "); }
                names.append(element.getName());
                if (decFields.contains((Method) element.getObject())) {
                    names.append("|").append("ID:").append(element.getId());
                }
                if (!element.getObfuscatedName().isEmpty()) {
                    names.append("|").append(element.getObfuscatedName());
                }
                names.append("(");
                Class<?>[] params = ((Method) element.getObject()).getParameterTypes();
                for(int j = 0; j < params.length; j++) {
                    names.append(params[j].getSimpleName());
                    if (j < params.length - 1) { names.append(", "); }
                }
                names.append(")");
            }
            builder.append(names).append("];").append((char) 10);
        }

        return builder.toString().replaceAll("\\n", System.lineSeparator());
    }

    private void addTabs(StringBuilder builder, int tabs) { builder.append(" ".repeat(tabs)); }

}
