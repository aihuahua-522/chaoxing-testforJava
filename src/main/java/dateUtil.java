import java.text.SimpleDateFormat;
import java.util.Date;

public class dateUtil {
    static String getThisTime() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(new Date());
    }
}
