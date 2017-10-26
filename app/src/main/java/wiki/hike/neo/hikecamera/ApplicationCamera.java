package wiki.hike.neo.hikecamera;
import android.app.Application;



/**
 * Created by Neo on 26/10/17.
 */

public class ApplicationCamera extends Application {

    private static ApplicationCamera _instance;

    public void onCreate()
    {
        super.onCreate();
        _instance = this;
    }

    public static ApplicationCamera getInstance()
    {
        return _instance;
    }

}
