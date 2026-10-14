package pw.rkd.appcat;

import android.content.Context;
import android.content.res.Configuration;

final class PopupTheme extends androidx.appcompat.view.ContextThemeWrapper {
    static final String[] VALUES={"system","light","dark"};
    private PopupTheme(Context base,boolean dark){
        super(base,dark?R.style.PopupDark:R.style.PopupLight);
        Configuration override=new Configuration();override.uiMode=dark?Configuration.UI_MODE_NIGHT_YES:Configuration.UI_MODE_NIGHT_NO;applyOverrideConfiguration(override);
    }
    static int index(String value){for(int i=0;i<VALUES.length;i++)if(VALUES[i].equals(value))return i;return -1;}
    static boolean dark(Context context){
        String value=context.getSharedPreferences("settings",0).getString("popupTheme","system");
        return "dark".equals(value)||(!"light".equals(value)&&(context.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES);
    }
    static Context context(Context base){return new PopupTheme(base,dark(base));}
    static boolean isPopup(Context context){return context instanceof PopupTheme;}
    static int activityStyle(Context context){return dark(context)?R.style.PickerDark:R.style.PickerLight;}
}
