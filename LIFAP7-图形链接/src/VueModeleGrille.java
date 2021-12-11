import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;

public class VueModeleGrille extends JPanel {

    private VueCase[][] tabCV;
    private HashMap<VueCase, Point> hashmap; // voir (*)
    private int size = 0;

    public VueModeleGrille(int size) {
        this.size = size;
        this. tabCV = new VueCase[size][size];
        this. hashmap = new HashMap<>();

        this.setLayout(new GridLayout(size, size));
        init(this.tabCV);
    }

    public void init(VueCase[][] tabCV) {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {

                tabCV[i][j] = new VueCase(i, j);
                this.add(tabCV[i][j]);

                hashmap.put(tabCV[i][j], new Point(j, i));

                tabCV[i][j].addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        //Point p = hashmap.get(e.getSource()); // (*) permet de récupérer les coordonnées d'une caseVue
                        ((VueCase) e.getSource()).nextCase();
                        System.out.println("mousePressed : " + e.getSource());

                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        // (**) - voir commentaire currentComponent
//                        currentComponent = (JComponent) e.getSource();
                        System.out.println("mouseEntered : " + e.getSource());
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        // (**) - voir commentaire currentComponent
                        System.out.println("mouseReleased : " + e.getSource());
                    }
                });
            }
        }
    }

    public VueCase[][] getTabCV() {
        return tabCV;
    }
}
