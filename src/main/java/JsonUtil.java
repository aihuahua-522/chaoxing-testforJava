import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.sun.org.apache.bcel.internal.generic.RET;
import com.sun.org.apache.regexp.internal.RE;

import java.lang.reflect.Type;
import java.util.HashMap;

public class JsonUtil {
    static Gson gson;

    static {
        gson = new Gson();
    }

    public static String objectToJson(Object obj) {
        return gson.toJson(obj);
    }


    public static String mapToString(HashMap<String, String> map) {
        return new Gson().toJson(map);

    }

    public static HashMap<String, String> getMapByString(String str) {
        Type type = new TypeToken<HashMap<String, String>>() {
        }.getType();
        return new Gson().fromJson(str, type);
    }


}
