import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 时间工具类
 */
public class DateUtil {

    public static String getTime() {
        return LocalDateTime.now(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH时mm分ss秒"));
    }
}
