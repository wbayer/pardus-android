-keep public class at.pardus.android.browser.js.* {
    <methods>;
}

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
