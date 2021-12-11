import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class RecordUtil {
    private static List<Record> recordList;
    private static final String SAVE_FILE = "Record.txt";

    static {
        init();
    }

    private static void init() {
        try {
            recordList = new ArrayList<>();

            BufferedReader bufferedReader = new BufferedReader(new FileReader(SAVE_FILE));
            while (true) {
                String time = bufferedReader.readLine();
                if (time == null) {
                    return;
                }
                if (time.isEmpty()) {
                    continue;
                }

                String name = bufferedReader.readLine();
                int size = Integer.parseInt(bufferedReader.readLine());
                String[][] data = new String[size][size];
                for (int i = 0; i < size; i++) {
                    String[] infos = bufferedReader.readLine().split(" ");
                    for (int j = 0; j < size; j++) {
                        data[i][j] = infos[j];
                    }
                }
                recordList.add(new Record(name, time, size, data));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void save(Record record) {
        recordList.add(record);
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(SAVE_FILE, true));
            writer.println(record.toString());
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<Record> getRecordList(String name) {
        ArrayList<Record> list = new ArrayList<>();
        for (Record record : recordList) {
            if (record.getName().equals(name)) {
                list.add(record);
            }
        }
        return list;
    }


}