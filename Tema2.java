import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import java.nio.MappedByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.CyclicBarrier;

/* O clasa ajutatoare care faciliteaza claritatea codului*/

class HelperObject {
    private Vector<MappedByteBuffer> buffer;
    private long dimension;
    private int position;

    public HelperObject(Vector<MappedByteBuffer> buffer, long dimension, int position) {
        this.buffer = buffer;
        this.dimension = dimension;
        this.position = position;
    }

    /* Fiecare thread va avea propriul buffer pentru a evita erori */

    public Vector<MappedByteBuffer> getBuffer() {
        return buffer;
    }

    public void setElemBuffer(MappedByteBuffer bufferElem, int index) {

        buffer.setElementAt(bufferElem, index);
    }

    public long getDimension() {
        return dimension;
    }

    public void setDimension(long dimension) {
        this.dimension = dimension;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

}

/* Clasa principala care implementeaza tema */

public class Tema2 {

    /* Elemente ce isi vor dovedi utilitatea pe parcurs */

    public static ExecutorService tpe;
    public static CyclicBarrier barrier;
    public static Semaphore sem;
    public static BufferedWriter writerProd;
    public static BufferedWriter writerOrders;

    /* O metoda speciala care implementeaza logica bonusului */

    public static void setNewBuffer(FileChannel channel, HelperObject helper, int i)
            throws IOException {
        int oldDim = (int) helper.getDimension();

        /* Partea de corectie din algoritm */

        if ((char) helper.getBuffer().get(i).get((int) helper.getDimension() - 1) != 'o' ||
                ((char) helper.getBuffer().get(i).get((int) helper.getDimension() - 1) == 'o' &&
                        (char) helper.getBuffer().get(i).get((int) helper.getDimension() - 2) != '\n'))

        /* Se corecteaza buffer-ul */

        {
            while ((char) helper.getBuffer().get(i).get((int) helper.getDimension() - 1) != '\n') {
                helper.setDimension((helper.getDimension() + 1));
                helper.setElemBuffer(
                        channel.map(FileChannel.MapMode.READ_ONLY, helper.getPosition(),
                                helper.getDimension()),
                        i);

            }

            /* Celalalt caz din corectie */

        } else {
            helper.setDimension((helper.getDimension() - 1));
            helper.setElemBuffer(
                    channel.map(FileChannel.MapMode.READ_ONLY, helper.getPosition(),
                            helper.getDimension()),
                    i);

        }

        /* Se muta cursorul din fisier mai departe */

        helper.setPosition((int) helper.getDimension() + helper.getPosition());
        helper.setDimension(oldDim);

    }

    public static void main(String[] args) throws IOException {
        File dirObj = new File(args[0]);
        if (dirObj.isDirectory()) {
            Path dir = FileSystems.getDefault().getPath(args[0]);
            DirectoryStream<Path> stream = Files.newDirectoryStream(dir);
            List<Path> listFiles = new ArrayList<>();
            for (Path path : stream)
                listFiles.add(path);
            int nrThreads = Integer.parseInt(args[1]);
            writerProd = new BufferedWriter(new PrintWriter("order_products_out.txt", "UTF-8"));
            writerOrders = new BufferedWriter(new PrintWriter("orders_out.txt", "UTF-8"));
            Vector<MappedByteBuffer> vecBuffer = new Vector<>();
            vecBuffer.setSize(nrThreads);
            HelperObject helper = new HelperObject(vecBuffer, 0, 0);
            barrier = new CyclicBarrier(nrThreads);
            sem = new Semaphore(-(nrThreads - 2));
            tpe = Executors.newFixedThreadPool(nrThreads);
            Thread[] threads = new Thread[nrThreads];

            /* Cele 2 canale folosite pentru maparea in memorie a unor chunk-uri de date */

            FileChannel channelOrders = FileChannel.open(listFiles.get(1), StandardOpenOption.CREATE,
                    StandardOpenOption.READ);
            FileChannel channelProducts = FileChannel.open(listFiles.get(0), StandardOpenOption.CREATE,
                    StandardOpenOption.READ);
            long fileOrderSize = channelOrders.size();
            long fileProdSize = channelProducts.size();
            long dimensionOrder = fileOrderSize / nrThreads;
            long dimensionProd = fileProdSize / nrThreads;
            helper.setDimension(dimensionOrder);
            helper.setPosition(0);

            /* Se pornesc thread-urile */

            for (int i = 0; i < nrThreads; i++) {
                threads[i] = new Thread(
                        new RunnableOrderThread(channelOrders, helper, fileOrderSize, nrThreads, i, channelProducts,
                                fileProdSize, dimensionProd));
                threads[i].start();
            }

            /* Se asteapta thread-urile */

            for (Thread iter : threads) {
                try {
                    iter.join();
                } catch (InterruptedException exception) {
                    exception.printStackTrace();
                }
            }

            /* Se inchide executorul */
            Tema2.tpe.shutdown();

            /* Se inchid fisierele / canalele */

            writerOrders.close();
            writerProd.close();
            channelOrders.close();
            channelProducts.close();

        } else {
            System.out.println("The first argument is not a directory!");
        }
    }
}