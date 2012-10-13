package edu.macalester.wpsemsim.matrix;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntLongHashMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SparseMatrixWriter {

    public static final byte ROW_PADDING = Byte.MIN_VALUE;

    private static final Logger LOG = Logger.getLogger(SparseMatrixWriter.class.getName());

    private File path;
    private TIntLongHashMap rowOffsets = new TIntLongHashMap();
    private TIntArrayList rowIndexes = new TIntArrayList();
    private File bodyPath;
    private BufferedOutputStream body;
    private long bodyOffset = 0;

    public SparseMatrixWriter(File path) throws IOException {
        this.path = path;
        info("writing matrix to " + path);

        // write tmp matrix file
        this.bodyPath = File.createTempFile("matrix", null);
        this.bodyPath.deleteOnExit();
        this.body = new BufferedOutputStream(new FileOutputStream(bodyPath));

        info("writing body to tmp file at " + bodyPath);
    }

    public void writeRow(SparseMatrixRow row) throws IOException {
        rowOffsets.put(row.getRowIndex(), bodyOffset);
        rowIndexes.add(row.getRowIndex());

        row.getBuffer().rewind();
        byte[] bytes = new byte[row.getBuffer().remaining()];
        row.getBuffer().get(bytes, 0, bytes.length);
        body.write(bytes);
        bodyOffset += bytes.length;

        // pad rows to 8 byte offsets to speed things up.
        while (bodyOffset % 8 != 0) {
            bodyOffset++;
            body.write(ROW_PADDING);
        }
    }

    public void finish() throws IOException {
        body.close();
        info("wrote " + bodyOffset + " bytes in body of matrix");

        // write offset file
        info("generating header");
        int sizeHeader = 8 + rowOffsets.size() * 12;
        body = new BufferedOutputStream(new FileOutputStream(path));
        body.write(intToBytes(SparseMatrix.FILE_HEADER));
        body.write(intToBytes(rowOffsets.size()));
        for (int i = 0; i < rowIndexes.size(); i++) {
            int rowIndex = rowIndexes.get(i);
            long rowOffset = rowOffsets.get(rowIndex);
            body.write(intToBytes(rowIndex));
            body.write(longToBytes(rowOffset + sizeHeader));
        }

        InputStream r = new FileInputStream(bodyPath);

        // append other file
        IOUtils.copyLarge(r, body);
        r.close();
        body.flush();
        body.close();

        info("wrote " + FileUtils.sizeOf(path) + " bytes to " + path);
    }

    private void info(String message) {
        LOG.log(Level.INFO, "sparse matrix writer " + path + ": " + message);
    }

    public static void write(File file, Iterator<SparseMatrixRow> rows) throws IOException {
        SparseMatrixWriter w = new SparseMatrixWriter(file);
        while (rows.hasNext()) {
            w.writeRow(rows.next());
        }
        w.finish();
    }


    private static byte[] intToBytes(int i) {
        return ByteBuffer.allocate(4).putInt(i).array();
    }

    private static byte[] longToBytes(long i) {
        return ByteBuffer.allocate(8).putLong(i).array();
    }
}
