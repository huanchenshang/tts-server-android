-keepclassmembers class * {
    @com.github.jing332.script.annotation.ScriptInterface <methods>;
}

-keep class com.github.jing332.script.runtime.**{ *;}

-keep class org.mozilla.** { *; }
-keep class !org.mozilla.classfile.**
-keep class !org.mozilla.javascript.optimizer.**