public class test {


    public static void main(String[] args) {

        String[] strings = new String[]{};
        abce(strings);

    }

    public static void abce(String[] args) {

        while (true) {


            try {
                System.out.println(1 / 0);
                System.out.println(123);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

}
