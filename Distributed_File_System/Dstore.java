import java.net.*;
import java.io.*;

public class Dstore {

    private int port;
    private int cport;
    private int timeout;
    private String fileFolder;
    private PrintWriter printWriter;

    public Dstore(int port, int cport, int timeout, String fileFolder) {
        this.port = port;
        this.cport = cport;
        this.timeout = timeout;
        this.fileFolder = fileFolder;
    }

    public static void main(String[] args) {
        try {
            int port = Integer.parseInt(args[0]);
            int cport = Integer.parseInt(args[1]);
            int timeout = Integer.parseInt(args[2]);
            String fileFolder = args[3];
            new Dstore(port, cport, timeout, fileFolder).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);

            Socket controllerClient = new Socket();
            controllerClient.connect(new InetSocketAddress(InetAddress.getLocalHost(), cport));
            printWriter = new PrintWriter(controllerClient.getOutputStream(), true);
            printWriter.println("JOIN " + port);

            while (true) {
                Socket accept = serverSocket.accept();
                new EchoDstore(accept, timeout, fileFolder, printWriter).start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class EchoDstore extends Thread {
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        private BufferedReader clientReader;
        private PrintWriter clientWrite;
        private PrintWriter writer;
        private String[] inputInfos;

        private int timeout;
        private String fileFolder;

        public EchoDstore(Socket socket, int timeout, String fileFolder, PrintWriter writer) {
            this.socket = socket;
            this.timeout = timeout;
            this.fileFolder = fileFolder;
            this.writer = writer;
        }

        public void run() {
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                clientReader = new BufferedReader(new InputStreamReader(inputStream));
                clientWrite = new PrintWriter(outputStream, true);

                while (true) {
                    String inputLine = null;
                    while ((inputLine = clientReader.readLine()) != null) {
                        inputInfos = inputLine.split(" ");
                        switch (inputInfos[0]) {
                            case "STORE" -> store();
                            case "LOAD_DATA" -> load();
                            case "REMOVE" -> remove();
                            case "REBALANCE" -> rebalance();
                            case "REBALANCE_STORE" -> rebalanceStore();
                        }
                    }
                }
            } catch (SocketTimeoutException e) {
                clost();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void rebalanceStore() throws IOException {
            File file = new File(fileFolder + inputInfos[1]);
            if (file.exists()) {
                return;
            }

            clientWrite.println("ACK");
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(inputStream.readNBytes(Integer.parseInt(inputInfos[2])));
            fos.close();
        }

        private void rebalance() throws IOException {
            int disp = 0;
            for (int i = 0; i < Integer.parseInt(inputInfos[1]); i++) {
                for (int j = 0; j < Integer.parseInt(inputInfos[disp + 3]); j++) {
                    Socket s = new Socket();
                    s.connect(new InetSocketAddress(InetAddress.getLocalHost(), Integer.parseInt(inputInfos[disp + 4])), timeout);
                    OutputStream os = s.getOutputStream();
                    PrintWriter pw = new PrintWriter(os, true);
                    BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    File file = new File(fileFolder + inputInfos[disp + 2]);
                    if (file.exists()) {
                        pw.println("REBALANCE_STORE " + inputInfos[disp + 2] + " " + file.length());
                        if (br.readLine().equals("ACK")) {
                            FileInputStream fis = new FileInputStream(file);
                            os.write(fis.read());
                            fis.close();
                        }
                        file.delete();
                    }
                    disp++;
                }
            }

            for (int i = 0; i < Integer.parseInt(inputInfos[disp + 4]); i++) {
                File file = new File(fileFolder + inputInfos[disp + 5]);
                if (file.exists()) {
                    file.delete();
                    disp++;
                }
            }
            clientWrite.write("REBALANCE_COMPLETE");
        }

        private void remove() {
            File file = new File(fileFolder + inputInfos[1]);
            if (!file.exists()) {
                writer.println("ERROR_FILE_DOES_NOT_EXIST " + inputInfos[1]);
                return;
            }

            file.delete();
            writer.println("REMOVE_ACK " + inputInfos[1]);

        }

        private void load() throws IOException {
            File file = new File(fileFolder + inputInfos[1]);
            if (!file.exists()) {
                clost();
                return;
            }
            System.out.println("exists");
            FileInputStream fis = new FileInputStream(file);
            outputStream.write(fis.readAllBytes());
            fis.close();

        }

        private void store() throws IOException {
            File file = new File(fileFolder + inputInfos[1]);
            if (file.exists()) {
                return;
            }

            clientWrite.println("ACK");
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(inputStream.readNBytes(Integer.parseInt(inputInfos[2])));
            fos.close();
            writer.println("STORE_ACK " + inputInfos[1]);
        }

        public void clost() {
            try {
                clientReader.close();
                clientWrite.close();
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

}