import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;

public class VueControleurGrille extends JFrame {
    private static final int PIXEL_PER_SQUARE = 60;
    private static final String SAVE_FILE = "Record.txt";

    private VueModeleGrille vueModeleGrille;
    private String name = "NOBODY";

    private JMenu loadMenu;
    private JMenu topMenu;

    public VueControleurGrille(int size) {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(size * PIXEL_PER_SQUARE, size * PIXEL_PER_SQUARE);

        setJMenuBar(initMenu());
        vueModeleGrille = new VueModeleGrille(size);
        setContentPane(vueModeleGrille);

        this.setLocationRelativeTo(null);
        setResizable(false);
    }

    private JMenuBar initMenu() {
        JMenuBar bar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.addActionListener((e) -> {
            saveGame();
        });

        loadMenu = new JMenu ("Load");
        initLoadItemSubItem();
        fileMenu.add(saveItem);
        fileMenu.add(loadMenu);
        bar.add(fileMenu);

        topMenu = new JMenu("Top");
        initTopMenuSubItem();
        bar.add(topMenu);
        return bar;
    }

    private void initTopMenuSubItem() {
        topMenu.removeAll();
        topMenu.add(new JMenuItem("30s  AAA"));
        topMenu.add(new JMenuItem("63s  BBB"));
        topMenu.add(new JMenuItem("80s  CCC"));
    }

    private void initLoadItemSubItem() {
        loadMenu.removeAll();
        ArrayList<Record> recordList = RecordUtil.getRecordList(name);
        Collections.reverse(recordList);
        for (Record record : recordList) {
            JMenuItem item = new JMenuItem(record.getName() + "  " + record.getTime());
            item.addActionListener((e) -> showGame(record));
            loadMenu.add(item);
        }

    }

    private void showGame(Record record) {
        VueCase[][] vueCases = vueModeleGrille.getTabCV();
        for (int i = 0; i < record.getSize(); i++) {
            for (int j = 0; j < record.getSize(); j++) {
                String type = record.getData()[i][j];
                vueCases[i][j].getCaseModele().setType(CaseType.valueOf(type));
            }
        }
    }

    private void saveGame() {
        Record record = new Record(name, vueModeleGrille.getTabCV());
        RecordUtil.save(record);
        initLoadItemSubItem();
    }




}
