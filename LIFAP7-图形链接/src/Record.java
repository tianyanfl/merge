import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Record implements Serializable {
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private String name;
    private String time;
    private int size = 0;
    private String[][] data;

    public Record(String name, VueCase[][] vueCases) {
        this.time = DATE_FORMAT.format(new Date());
        this.name = name;
        this.size = vueCases.length;
        this.data = new String[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                VueCase vueCase = vueCases[i][j];
                data[i][j] = vueCase.getCaseModele().getType().name();
            }
        }
    }

    public Record(String name, String time, int size, String[][] data) {
        this.name = name;
        this.time = time;
        this.size = size;
        this.data = data;
    }

    @Override
    public String toString() {
        String message = time + "\n" + name + "\n" + size + "\n";
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                message += data[i][j] + " ";
            }
            message += "\n";
        }
        return message;
    }

    public static DateFormat getDateFormat() {
        return DATE_FORMAT;
    }

    public String getName() {
        return name;
    }

    public String getTime() {
        return time;
    }

    public int getSize() {
        return size;
    }

    public String[][] getData() {
        return data;
    }
}
