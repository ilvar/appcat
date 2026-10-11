package pw.rkd.appcat;

import java.util.*;

/** Narrow purpose definitions keep generic Android "Productivity" from becoming a catch-all. */
final class CategoryCriteria {
    static boolean eligible(String name) { return !name.trim().equalsIgnoreCase("Other"); }
    static Map<String,String> choices(List<Catalog.Category> categories) {
        Map<String,String> result=new LinkedHashMap<>();
        for(var category:categories) if(eligible(category.name()))result.put(category.id(),describe(category.name()));
        if(result.isEmpty())throw new IllegalArgumentException("Create a category other than Other before classifying.");
        return result;
    }
    static String describe(String name) {
        String purpose;
        switch(name.toLowerCase(Locale.ROOT)) {
            case "games": purpose="Video games and game launchers";break;
            case "music & audio": purpose="Music players, podcasts, radio, audio recording and editing";break;
            case "video": purpose="Video playback, streaming TV and movies, video editing";break;
            case "photography": purpose="Camera, photo gallery and image editing";break;
            case "social": purpose="Social networks, communities and social feeds";break;
            case "news": purpose="News, journalism, magazines and current affairs";break;
            case "maps & travel": purpose="Maps, navigation, transit, taxis, flights and travel booking";break;
            case "productivity": purpose="Office documents, notes, calendars, task lists and work planning. Not a generic category for every useful app or utility";break;
            case "accessibility": purpose="Assistive apps for disabilities, screen readers and accessibility aids";break;
            case "communication": purpose="Phone, SMS, email, messaging and video calls";break;
            case "tools": purpose="Web browsers, file managers, calculators, device settings, launchers, security, VPN, system utilities and developer tools";break;
            case "shopping": purpose="Retail stores, marketplaces, shopping and product purchases";break;
            case "finance": purpose="Banking, payments, budgets, investments and insurance";break;
            case "health & fitness": purpose="Exercise, sports tracking, medical care and wellbeing";break;
            case "education": purpose="Learning, reference, languages and educational resources";break;
            case "food & drink": purpose="Recipes, restaurants, groceries, food delivery and drinks";break;
            default: purpose="Apps whose primary purpose fits the custom category "+name;
        }
        return name+": "+purpose;
    }
}
