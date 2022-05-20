import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;


public class Controller {
    private ServerSocket contSocket;
    private int cport;
    private int R;
    private int timeout;
    private int rebalance;

    public Controller(int cport, int R, int timeout, int rebalance) {
        this.cport = cport;
        this.R = R;
        this.timeout = timeout;
        this.rebalance = rebalance;
    }

    public static void main(String[] args) {
        try {
            int cport = Integer.parseInt(args[0]);
            int R = Integer.parseInt(args[1]);
            int timeout = Integer.parseInt(args[2]);
            int balance = Integer.parseInt(args[3]);
            new Controller(cport, R, timeout, balance).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start() {
        try {
            contSocket = new ServerSocket(cport);
            while (true) {
                Socket socket = contSocket.accept();
                new EchoController(socket, R, timeout, rebalance).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class EchoController extends Thread { //removed static, maybe broke idk
        private static ArrayList<Integer> activePorts = new ArrayList<>();
        private static HashMap<String, ArrayList<Integer>> filePorts = new HashMap<>();
        private static HashMap<String, Integer> fileSizes = new HashMap<>();
        private static String index = "";
        private static int count = 0;

        private Socket clientSocket;
        private BufferedReader reader;
        private PrintWriter writer;
        private ArrayList<Socket> storeSockets;
        private ArrayList<Socket> removeSockets;
        private int time;
        private Timer timer;
        private String[] nextLine;
        private int loadAttempt;

        private int R;
        private int timeout;
        private int rebalancePeriod;


        public EchoController(Socket socket, int R, int timeout, int rebalancePeriod) {
            this.clientSocket = socket;
            this.R = R;
            this.timeout = timeout;
            this.rebalancePeriod = rebalancePeriod;
        }

        public synchronized void run() {
            try {
                reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                writer = new PrintWriter(clientSocket.getOutputStream(), true);

                time = rebalancePeriod;
                timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        time--;
                    }
                }, 1000, 1000);

                while (true) {
                    if (time <= 0) {
                        time = 0;
                        rebalance();
                        continue;
                    }

                    String inLine;
                    while ((inLine = reader.readLine()) != null) {
                        nextLine = inLine.split(" ");
                        if (index.equals("store in progress") || index.equals("remove in progress")) {
                            progress();
                        } else if (nextLine[0].equals("JOIN")) {
                            activePorts.add(Integer.parseInt(nextLine[1]));
                            if (activePorts.size() > R) {
                                rebalance();
                            }
                        } else if (nextLine[0].equals("STORE")) {
                            store();
                        } else if (nextLine[0].equals("LOAD")) {
                            load();
                        } else if (nextLine[0].equals("RELOAD")) {
                            reload();
                        } else if (nextLine[0].equals("REMOVE") && filePorts.containsKey(nextLine[1])) {
                            remove();
                        } else if (nextLine[0].equals("REMOVE")) {
                            writer.println("ERROR_FILE_DOES_NOT_EXIST");
                        } else if (nextLine[0].equals("LIST")) {
                            StringBuilder builder = new StringBuilder("LIST");
                            for (String f : filePorts.keySet()) {
                                builder.append(" ").append(f);
                            }
                            writer.println(builder);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void load() {
            if (!filePorts.containsKey(nextLine[1])) {
                writer.println("ERROR_FILE_DOES_NOT_EXIST");
                return;
            }

            writer.println("LOAD_FROM " + filePorts.get(nextLine[1]).get(0) + " " + fileSizes.get(nextLine[1]));
            loadAttempt = 0;
        }

        private void reload() {
            if (loadAttempt >= R - 1 || loadAttempt >= activePorts.size()) {
                if (loadAttempt < R - 1) {
                    writer.println("ERROR_NOT_ENOUGH_DSTORES");
                } else {
                    writer.println("ERROR_FILE_DOES_NOT_EXIST"); //idk when its supposed to do ERROR_LOAD
                }
                return;
            }

            writer.println("LOAD_FROM " + filePorts.get(nextLine[1]).get(loadAttempt) + " " + fileSizes.get(nextLine[1]));
            loadAttempt++;

        }

        private void remove() {
            removeSockets = new ArrayList<>();
            try {
                for (int n : filePorts.get(nextLine[1])) {
                    Socket removeSocket = new Socket();
                    removeSocket.connect(new InetSocketAddress(InetAddress.getLocalHost(), n), timeout);
                    removeSockets.add(removeSocket);
                    new PrintWriter(removeSockets.get(removeSockets.size() - 1).getOutputStream(), true).println("REMOVE " + nextLine[1]);
                }
                removeSockets.size();
                count = 0;
                index = "remove in progress";
            } catch (Exception e) {
                index = "";
                count = 0;
            }
        }

        private void store() {
            ArrayList<Integer> ports = new ArrayList<>();
            if (R > activePorts.size() || filePorts.containsKey(nextLine[1])) {
                if (R <= activePorts.size()) {
                    writer.println("ERROR_FILE_ALREADY_EXISTS");
                } else {
                    writer.println("ERROR_NOT_ENOUGH_DSTORES");
                }
                return;
            }

            for (int i = 0; i < R; i++) {
                ports.add(activePorts.get(i));
            }
            filePorts.put(nextLine[1], ports);
            fileSizes.put(nextLine[1], Integer.parseInt(nextLine[2]));

            storeSockets = new ArrayList<>();
            StringBuilder builder = new StringBuilder("STORE_TO");
            try {
                for (Integer port : ports) {
                    builder.append(" ").append(port);
                    Socket storeSocket = new Socket();
                    storeSocket.connect(new InetSocketAddress(InetAddress.getLocalHost(), port), timeout);
                    storeSockets.add(storeSocket);
                }
                writer.println(builder);
                count = 0;
                index = "store in progress";
            } catch (Exception e) {//timeout
                index = "";
                count = 0;
                filePorts.remove(nextLine[1]);
                fileSizes.remove(nextLine[1]);
            }

        }

        private void progress() {
            switch (nextLine[0]) {
                case "STORE_ACK" -> {
                    count++;
                    if (count == R) {
                        index = "store complete";
                        writer.println("STORE_COMPLETE");
                        count = 0;
                    }
                }
                case "REMOVE_ACK" -> {
                    count++;
                    System.out.println(count);
                    if (count == R) {
                        index = "remove complete";
                        writer.println("REMOVE_COMPLETE");
                        filePorts.remove(nextLine[1]);
                        count = 0;
                    }
                }
                case "STORE" -> writer.println("ERROR_FILE_ALREADY_EXISTS");
                case "LIST" -> {
                    StringBuilder s = new StringBuilder("LIST");
                    for (int i = 0; i < filePorts.keySet().size() - 2; i++) {
                        s.append(" ").append(filePorts.keySet().toArray()[i]);
                    }
                    writer.println(s);
                }
                default -> writer.println("ERROR_FILE_DOES_NOT_EXIST");
            }
        }

        public void rebalance() {
            HashMap<Integer, ArrayList<String>> fileLists = new HashMap<>();
            try {
                for (int p : activePorts) {
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(InetAddress.getLocalHost(), p), timeout);
                    new PrintWriter(clientSocket.getOutputStream(), true).println("LIST");
                    fileLists.put(p, new ArrayList<>(Arrays.asList(new BufferedReader(new InputStreamReader(socket.getInputStream())).readLine().split(" "))));
                    fileLists.remove(0); //gets rid of "LIST"
                    socket.close();
                }

                HashMap<Integer, ArrayList<String>> newFileLists = new HashMap<>(fileLists);
                HashMap<String, ArrayList<Integer>> toSend = new HashMap<>();
                int val = R * filePorts.values().size() / activePorts.size();
                ArrayList<Integer> unbalanced = new ArrayList<>(newFileLists.keySet());
                ArrayList<Integer> fbalanced = new ArrayList<>();
                ArrayList<Integer> cbalanced = new ArrayList<>();
                ArrayList<Integer> unbalancedOver = new ArrayList<>();
                ArrayList<Integer> unbalancedUnder = new ArrayList<>();
                for (String s : filePorts.keySet()) {
                    toSend.put(s, new ArrayList<>());
                    while (filePorts.get(s).size() < R) {
                        for (int p : activePorts) {
                            if (!filePorts.get(s).contains(p)) {
                                filePorts.get(s).add(p);
                                toSend.get(s).add(p);
                                break;
                            }
                        }
                    }
                }
                for (int p : unbalanced) {
                    if (newFileLists.get(p).size() == val) {
                        fbalanced.add(p);
                    } else if (newFileLists.get(p).size() == val + 1) {
                        cbalanced.add(p);
                    } else if (newFileLists.get(p).size() < val) {
                        unbalancedUnder.add(p);
                    } else {
                        unbalancedOver.add(p);
                    }
                }

                boolean complete = false;
                while (unbalancedOver.size() > 0) {
                    for (int p : unbalancedOver) {
                        if (unbalancedUnder.size() > 0) {
                            complete = false;
                            for (int up : unbalancedUnder) {
                                for (String f : newFileLists.get(p)) {
                                    if (!newFileLists.get(up).contains(f)) {
                                        newFileLists.get(up).add(f);
                                        newFileLists.get(p).remove(f);
                                        toSend.get(f).add(up);
                                        if (newFileLists.get(up).size() == val) {
                                            unbalancedUnder.remove(up);

                                            fbalanced.add(up);
                                        }
                                        if (newFileLists.get(p).size() == val + 1) {
                                            unbalancedOver.remove(p);
                                            cbalanced.add(p);
                                            complete = true;
                                        }
                                        break;
                                    }
                                }
                                if (complete) {
                                    break;
                                }
                            }
                        } else {
                            complete = false;
                            for (int fp : fbalanced) {
                                for (String f : newFileLists.get(p)) {
                                    if (!newFileLists.get(fp).contains(f)) {
                                        newFileLists.get(fp).add(f);
                                        newFileLists.get(p).remove(f);
                                        toSend.get(f).add(fp);
                                        if (newFileLists.get(p).size() == val + 1) {
                                            unbalancedOver.remove(p);
                                            cbalanced.add(p);
                                            complete = true;
                                        }
                                        cbalanced.add(fp);
                                        fbalanced.remove(fp);
                                        break;
                                    }
                                }
                                if (complete) {
                                    break;
                                }
                            }
                        }
                    }
                }
                while (unbalancedUnder.size() > 0) {
                    for (int p : unbalancedUnder) {
                        complete = false;
                        for (int cp : cbalanced) {
                            for (String f : newFileLists.get(p)) {
                                if (newFileLists.get(cp).contains(f)) {
                                    newFileLists.get(cp).remove(f);
                                    newFileLists.get(p).add(f);
                                    toSend.get(f).add(p);
                                    if (newFileLists.get(p).size() == val) {
                                        unbalancedUnder.remove(p);
                                        fbalanced.add(p);
                                        complete = true;
                                    }
                                    cbalanced.remove(cp);
                                    fbalanced.add(cp);
                                    break;
                                }
                            }
                            if (complete) {
                                break;
                            }
                        }
                    }
                }
                for (int p : activePorts) {
                    String msgToSend = "";
                    String msgToSendpt = "";
                    String msgToRemove = "";
                    int numFilesSend = 0;
                    int numFilesRemove = 0;
                    int numSend = 0;
                    for (String s : toSend.keySet()) {
                        if (fileLists.get(p).contains(s)) {
                            numFilesSend++;
                            msgToSend = msgToSend + " " + s;
                            for (int tp : toSend.get(s)) {
                                numSend++;
                                msgToSendpt = msgToSendpt + " " + tp;
                            }
                            msgToSend = msgToSend + " " + numSend + msgToSendpt;
                            msgToSendpt = "";
                            numSend = 0;
                        }
                    }
                    for (String s : fileLists.get(p)) {
                        if (!newFileLists.get(p).contains(s)) {
                            msgToRemove = msgToRemove + " " + s;
                            numFilesRemove++;
                        }
                    }
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(InetAddress.getLocalHost(), p), timeout);
                    PrintWriter pw = new PrintWriter(clientSocket.getOutputStream(), true);
                    BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    pw.println("REBALANCE " + numFilesSend + msgToSend + " " + numFilesRemove + msgToRemove);
                    if (br.readLine().equals("REBALANCE_COMPLETE")) {
                        pw.close();
                        br.close();
                        socket.close();
                    }
                }
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

}
