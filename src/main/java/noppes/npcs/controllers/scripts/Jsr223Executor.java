package noppes.npcs.controllers.scripts;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.shared.common.CommonUtil;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.Util;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;

public class Jsr223Executor
        implements IScriptExecutor {

    private ScriptEngine engine;
    private boolean init = false;
    private boolean errored = false;
    private final HashSet<String> unknownFunctions = new HashSet<>();
    private String scriptCode;
    private static Method luaCoerce;
    private static Method luaCall;

    @Override
    public void initialize(String language, Map<String, Object> globals) {
        engine = ScriptController.Instance.getEngineByName(language);
        if (engine == null) { errored = true; }
        else {
            for (Map.Entry<String, Object> entry : globals.entrySet()) { engine.put(entry.getKey(), entry.getValue()); }
        }
    }

    @Override
    public ScriptEngine getEngine() { return engine; }

    @Override
    public void setScript(String fullScriptCode) {
        scriptCode = fullScriptCode;
        init = false;
        errored = false;
        unknownFunctions.clear();
    }

    @Override
    public String run(IScriptHandler handler, String functionName, Object event) {
        String result = "";
        if (!errored && engine != null && !unknownFunctions.contains(functionName)) {
            StringWriter sw = new StringWriter();
            try (PrintWriter pw = new PrintWriter(sw)){
                engine.getContext().setWriter(pw);
                engine.getContext().setErrorWriter(pw);
                try {
                    if (!init) {
                        engine.eval(scriptCode);
                        init = true;
                    }
                    if (engine.getFactory().getLanguageName().equals("lua")) {
                        Object ob = engine.get(functionName);
                        if (ob != null) {
                            if (luaCoerce == null) {
                                luaCoerce = Class.forName("org.luaj.vm2.lib.jse.CoerceJavaToLua").getMethod("coerce", Object.class);
                                luaCall = ob.getClass().getMethod("call", Class.forName("org.luaj.vm2.LuaValue"));
                            }
                            luaCall.invoke(ob, luaCoerce.invoke(null, event));
                        }
                        else { unknownFunctions.add(functionName); }
                    }
                    else { ((Invocable) engine).invokeFunction(functionName, event); }
                }
                catch (NoSuchMethodException e) { unknownFunctions.add(functionName); }
                catch (Throwable t) {
                    errored = true;
                    MutableComponent notice = handler.noticeString(functionName, event);
                    String noticeToLog = Util.instance.deleteColor(notice.getString());
                    pw.write(noticeToLog + "\n");
                    t.printStackTrace(pw);
                    // to admins
                    Throwable cause = t.getCause();
                    String errorText = cause != null ? cause.getLocalizedMessage().replaceAll("" + ((char) 13), "") : t.toString();
                    StringBuilder error = new StringBuilder();
                    if (errorText.contains("" + (char) 10)) {
                        for (int c = 0; c < errorText.length(); c++) {
                            error.append(errorText.charAt(c));
                            if (errorText.charAt(c) == 10) { error.append(ChatFormatting.DARK_GRAY); }
                        }
                    }
                    else { error = new StringBuilder(ChatFormatting.DARK_GRAY + "" + t); }
                    MutableComponent errInfo = Component.literal("Script " + (cause != null ? cause.getClass().getSimpleName() : "") + ": " + error);
                    errInfo.setStyle(errInfo.getStyle().withColor(ChatFormatting.DARK_GRAY));
                    CommonUtil.NotifyOPs(notice.append("\n").append(errInfo), true);
                    LogWriter.error(noticeToLog + " ", t);
                }
            }
            result = sw.toString();
        }
        return result;
    }

    @Override
    public boolean isErrored() { return errored; }

    public void setErrored(boolean isErrored) { errored = isErrored; }

    @Override
    public boolean isInit() { return init; }

    @Override
    public void setInit(boolean isInit) {
        init = isInit;
        unknownFunctions.clear();
    }

    @Override
    public boolean isUnknownFunction(String functionName) { return unknownFunctions.contains(functionName); }

    @Override
    public void close() { engine = null; }

}
