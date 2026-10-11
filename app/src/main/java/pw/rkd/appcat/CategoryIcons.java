package pw.rkd.appcat;

import android.content.Context;
import android.graphics.drawable.Drawable;
import java.util.*;

/** Bundled Google Material Icons Rounded; no network or font installation needed. */
final class CategoryIcons {
    record Option(String id,String label,int drawable) {}
    static final List<Option> OPTIONS=Arrays.asList(
        new Option("gamepad","Games",R.drawable.ic_gamepad),new Option("music_note","Music",R.drawable.ic_music_note),
        new Option("movie","Video",R.drawable.ic_movie),new Option("photo_camera","Camera",R.drawable.ic_photo_camera),
        new Option("people","Social",R.drawable.ic_people),new Option("article","News",R.drawable.ic_article),
        new Option("map","Maps",R.drawable.ic_map),new Option("event_note","Calendar",R.drawable.ic_event_note),
        new Option("accessibility_new","Accessibility",R.drawable.ic_accessibility_new),new Option("chat","Chat",R.drawable.ic_chat),
        new Option("build","Tools",R.drawable.ic_build),new Option("shopping_bag","Shopping",R.drawable.ic_shopping_bag),
        new Option("account_balance","Finance",R.drawable.ic_account_balance),new Option("favorite","Health",R.drawable.ic_favorite),
        new Option("school","Education",R.drawable.ic_school),new Option("restaurant","Food",R.drawable.ic_restaurant),
        new Option("folder","Folder",R.drawable.ic_folder),new Option("home","Home",R.drawable.ic_home),
        new Option("lock","Security",R.drawable.ic_lock),new Option("book","Books",R.drawable.ic_book),
        new Option("flight_takeoff","Flights",R.drawable.ic_flight_takeoff),new Option("work","Work",R.drawable.ic_work),
        new Option("star","Star",R.drawable.ic_star),new Option("pets","Pets",R.drawable.ic_pets),
        new Option("apps","Apps",R.drawable.ic_apps),new Option("palette","Art",R.drawable.ic_palette),
        new Option("science","Science",R.drawable.ic_science),new Option("brush","Brush",R.drawable.ic_brush),
        new Option("code","Code",R.drawable.ic_code),new Option("directions_car","Car",R.drawable.ic_directions_car));
    static String defaultIcon(String categoryId) {
        String[] ids={"gamepad","music_note","movie","photo_camera","people","article","map","event_note","accessibility_new","chat","build","shopping_bag","account_balance","favorite","school","restaurant","folder"};
        try{int n=Integer.parseInt(categoryId.substring(1));if(categoryId.startsWith("c")&&n>=0&&n<ids.length)return ids[n];}catch(Exception ignored){}
        return "folder";
    }
    static boolean valid(String id){for(var option:OPTIONS)if(option.id().equals(id))return true;return false;}
    static Drawable drawable(Context context,String id){for(var option:OPTIONS)if(option.id().equals(id))return context.getDrawable(option.drawable());return context.getDrawable(R.drawable.ic_folder);}
}
