package pw.rkd.appcat;

import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.Icon;
import java.util.*;

final class Shortcuts {
    static ShortcutInfo info(Context c,Catalog.Category category){
        Bitmap bitmap=Bitmap.createBitmap(192,192,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.rgb(83,109,254));canvas.drawCircle(96,96,90,p);p.setColor(Color.WHITE);p.setTextSize(68);p.setTextAlign(Paint.Align.CENTER);
        String name=category.name();android.graphics.drawable.Drawable symbol=CategoryIcons.drawable(c,category.icon()).mutate();symbol.setTint(Color.WHITE);symbol.setBounds(48,48,144,144);symbol.draw(canvas);
        Intent intent=new Intent(c,CategoryActivity.class).setAction(Intent.ACTION_VIEW).putExtra("category",category.id());
        return new ShortcutInfo.Builder(c,"category:"+category.id()).setShortLabel(name).setIcon(Icon.createWithBitmap(bitmap)).setIntent(intent).build();
    }
    static void pin(Context c,Catalog.Category category){
        ShortcutManager manager=c.getSystemService(ShortcutManager.class);
        if(!manager.isRequestPinShortcutSupported()){Ui.toast(c,"Your launcher does not support pinned shortcuts");return;}
        if(!manager.requestPinShortcut(info(c,category),null))Ui.toast(c,"Launcher could not accept the shortcut request");
    }
    static void refresh(Context c){
        ShortcutManager manager=c.getSystemService(ShortcutManager.class);List<ShortcutInfo> updates=new ArrayList<>();Set<String> pinned=new HashSet<>();for(var i:manager.getPinnedShortcuts())pinned.add(i.getId());
        for(var cat:new Catalog(c).categories())if(pinned.contains("category:"+cat.id()))updates.add(info(c,cat));
        if(!updates.isEmpty())manager.updateShortcuts(updates);
    }
}
