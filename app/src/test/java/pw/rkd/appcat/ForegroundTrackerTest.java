package pw.rkd.appcat;

import org.junit.Test;
import static org.junit.Assert.*;

public class ForegroundTrackerTest {
    @Test public void showsOnlyDefaultLauncher(){var t=new ForegroundTracker();t.event("launcher",1,10);assertTrue(t.isHome("launcher"));t.event("browser",1,20);assertFalse(t.isHome("launcher"));t.event("launcher",1,30);assertTrue(t.isHome("launcher"));}
    @Test public void hidesWhileLauncherPauses(){var t=new ForegroundTracker();t.event("launcher",1,10);t.event("launcher",2,20);assertFalse(t.isHome("launcher"));}
    @Test public void ignoresDelayedAndUnrelatedPauseEvents(){var t=new ForegroundTracker();t.event("launcher",1,20);t.event("browser",1,10);t.event("browser",2,30);assertTrue(t.isHome("launcher"));}
    @Test public void unknownOrRevokedAccessIsHidden(){var t=new ForegroundTracker();assertFalse(t.isHome("launcher"));t.event("launcher",1,10);t.clear();assertFalse(t.isHome("launcher"));assertFalse(t.isHome(null));}
}
